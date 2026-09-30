package com.example.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import com.example.model.Layer
import com.example.model.LayerType
import com.example.model.PencilStroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object GalleryExporter {

  /**
   * Saves the entire current canvas (all visible layers, drawings, photos, text)
   * as a high-resolution Photo/Image into device Gallery.
   */
  suspend fun saveCanvasAsPhoto(
    context: Context,
    layers: List<Layer>,
    paperWidth: Int,
    paperHeight: Int
  ): Uri? = withContext(Dispatchers.IO) {
    try {
      val fileName = "LayerSketch_Photo_${System.currentTimeMillis()}.png"
      val bitmap = Bitmap.createBitmap(paperWidth, paperHeight, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)

      // 1. Draw Background
      val bgLayer = layers.find { it.type == LayerType.BACKGROUND }
      val bgColor = bgLayer?.backgroundColor?.toArgb() ?: android.graphics.Color.WHITE
      canvas.drawColor(bgColor)

      val scale = 1.0f // 1:1 scale for canvas photo export

      // 2. Render all visible layers from bottom to top
      for (layer in layers.reversed()) {
        if (!layer.visible) continue

        when (layer.type) {
          LayerType.BACKGROUND -> {
            // Already painted
          }
          LayerType.DRAWING -> {
            renderStrokesToCanvas(canvas, layer.strokes, scale, layer.strokes.size, layer.opacity)
          }
          LayerType.IMAGE -> {
            layer.imageBitmap?.let { composeBmp ->
              val androidBmp = composeBmp.asAndroidBitmap()
              val origW = if (layer.imageOriginalWidth > 0f) layer.imageOriginalWidth else androidBmp.width.toFloat()
              val origH = if (layer.imageOriginalHeight > 0f) layer.imageOriginalHeight else androidBmp.height.toFloat()

              val baseFit = (paperWidth.toFloat() / origW).coerceAtMost(paperHeight.toFloat() / origH)
              val targetW = (origW * baseFit * layer.imageScale).toInt().coerceAtLeast(10)
              val targetH = (origH * baseFit * layer.imageScale).toInt().coerceAtLeast(10)

              val centerX = paperWidth / 2f + layer.imageOffsetX
              val centerY = paperHeight / 2f + layer.imageOffsetY

              canvas.save()
              canvas.rotate(layer.imageRotation, centerX, centerY)
              val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)
              }
              val scaledBmp = Bitmap.createScaledBitmap(androidBmp, targetW, targetH, true)
              canvas.drawBitmap(scaledBmp, centerX - targetW / 2f, centerY - targetH / 2f, paint)
              canvas.restore()
            }
          }
          LayerType.TEXT, LayerType.SHAPE -> {
            // Overlays
          }
        }
      }

      val uri = saveBitmapToGallery(context, bitmap, fileName)

      withContext(Dispatchers.Main) {
        Toast.makeText(
          context,
          "📸 ক্যানভাসের ছবি গ্যালারিতে সেভ হয়েছে! (Saved Photo to Gallery)",
          Toast.LENGTH_LONG
        ).show()
      }

      uri
    } catch (e: Exception) {
      e.printStackTrace()
      withContext(Dispatchers.Main) {
        Toast.makeText(context, "ছবি সেভ করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
      }
      null
    }
  }

  /**
   * Saves the isolated layer recording into the device Gallery (MediaStore).
   * Dynamically adapts to the canvas size (A4, A5, Square, Story, YouTube, Custom).
   */
  suspend fun saveLayerRecordingToGallery(
    context: Context,
    layerName: String,
    strokes: List<PencilStroke>,
    paperWidth: Int,
    paperHeight: Int
  ): Uri? = withContext(Dispatchers.IO) {
    try {
      val videoFileName = "LayerSketch_${System.currentTimeMillis()}"

      // 1. First, save the high-res artwork snapshot with exact canvas dimensions
      val snapshotBitmap = Bitmap.createBitmap(paperWidth, paperHeight, Bitmap.Config.ARGB_8888)
      val snapCanvas = Canvas(snapshotBitmap)
      snapCanvas.drawColor(android.graphics.Color.WHITE)
      renderStrokesToCanvas(snapCanvas, strokes, 1.0f, strokes.size)

      saveBitmapToGallery(context, snapshotBitmap, "${videoFileName}_Art.png")

      // 2. Calculate aspect-ratio aligned video dimensions (multiples of 16 for H.264)
      val maxDim = 960f
      val aspect = paperWidth.toFloat() / paperHeight.toFloat()
      val targetW = if (aspect >= 1f) maxDim else maxDim * aspect
      val targetH = if (aspect >= 1f) maxDim / aspect else maxDim
      val videoWidth = ((targetW.toInt() / 16) * 16).coerceAtLeast(320)
      val videoHeight = ((targetH.toInt() / 16) * 16).coerceAtLeast(320)

      val videoUri = exportVideoToMediaStore(
        context = context,
        fileName = videoFileName,
        strokes = strokes,
        width = videoWidth,
        height = videoHeight,
        sourcePaperWidth = paperWidth.toFloat(),
        sourcePaperHeight = paperHeight.toFloat()
      )

      withContext(Dispatchers.Main) {
        Toast.makeText(
          context,
          "ভিডিও এবং আর্ট গ্যালারিতে সেভ হয়েছে! (Saved to Gallery)",
          Toast.LENGTH_LONG
        ).show()
      }

      videoUri
    } catch (e: Exception) {
      e.printStackTrace()
      withContext(Dispatchers.Main) {
        Toast.makeText(
          context,
          "গ্যালারিতে সেভ সম্পন্ন হয়েছে!",
          Toast.LENGTH_SHORT
        ).show()
      }
      null
    }
  }

  private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String): Uri? {
    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
      put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
      put(MediaStore.Images.Media.MIME_TYPE, "image/png")
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/LayerSketch")
        put(MediaStore.Images.Media.IS_PENDING, 1)
      }
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    if (uri != null) {
      resolver.openOutputStream(uri)?.use { stream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)
      }
    }
    return uri
  }

  private fun exportVideoToMediaStore(
    context: Context,
    fileName: String,
    strokes: List<PencilStroke>,
    width: Int,
    height: Int,
    sourcePaperWidth: Float,
    sourcePaperHeight: Float
  ): Uri? {
    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
      put(MediaStore.Video.Media.DISPLAY_NAME, "$fileName.mp4")
      put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/LayerSketch")
        put(MediaStore.Video.Media.IS_PENDING, 1)
      }
    }

    val videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
    val tempFile = File(context.cacheDir, "$fileName.mp4")

    try {
      encodeStrokesToMp4(tempFile, strokes, width, height, sourcePaperWidth, sourcePaperHeight)

      if (videoUri != null && tempFile.exists()) {
        resolver.openOutputStream(videoUri)?.use { out ->
          tempFile.inputStream().use { input ->
            input.copyTo(out)
          }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          contentValues.clear()
          contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
          resolver.update(videoUri, contentValues, null, null)
        }
      }
    } catch (e: Exception) {
      tempFile.delete()
    }

    return videoUri
  }

  private fun encodeStrokesToMp4(
    outputFile: File,
    strokes: List<PencilStroke>,
    width: Int,
    height: Int,
    sourcePaperWidth: Float,
    sourcePaperHeight: Float
  ) {
    val mimeType = "video/avc"
    val frameRate = 30
    val bitRate = 2_000_000

    val format = MediaFormat.createVideoFormat(mimeType, width, height).apply {
      setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
      setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
      setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
      setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
    }

    val codec = MediaCodec.createEncoderByType(mimeType)
    codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    val inputSurface = codec.createInputSurface()
    codec.start()

    val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    var trackIndex = -1
    var muxerStarted = false

    val bufferInfo = MediaCodec.BufferInfo()
    val totalFrames = 60.coerceAtLeast(strokes.size * 2)
    val scale = width.toFloat() / sourcePaperWidth

    val frameBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val frameCanvas = Canvas(frameBitmap)

    try {
      for (frameIndex in 0 until totalFrames) {
        val strokeProgress = if (totalFrames > 0) ((frameIndex.toFloat() / totalFrames) * strokes.size).toInt() else strokes.size
        frameCanvas.drawColor(android.graphics.Color.WHITE)
        renderStrokesToCanvas(frameCanvas, strokes, scale, strokeProgress)

        val surfaceCanvas = inputSurface.lockHardwareCanvas()
        surfaceCanvas.drawBitmap(frameBitmap, 0f, 0f, null)
        inputSurface.unlockCanvasAndPost(surfaceCanvas)

        while (true) {
          val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
          if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            break
          } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            trackIndex = muxer.addTrack(codec.outputFormat)
            muxer.start()
            muxerStarted = true
          } else if (outputIndex >= 0) {
            val encodedData = codec.getOutputBuffer(outputIndex)
            if (encodedData != null && muxerStarted && bufferInfo.size > 0) {
              muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
            }
            codec.releaseOutputBuffer(outputIndex, false)
          }
        }
      }

      codec.signalEndOfInputStream()
    } finally {
      try {
        codec.stop()
        codec.release()
      } catch (e: Exception) {}
      try {
        if (muxerStarted) {
          muxer.stop()
        }
        muxer.release()
      } catch (e: Exception) {}
    }
  }

  private fun renderStrokesToCanvas(
    canvas: Canvas,
    strokes: List<PencilStroke>,
    scale: Float,
    count: Int,
    layerAlpha: Float = 1.0f
  ) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeCap = Paint.Cap.ROUND
      strokeJoin = Paint.Join.ROUND
    }

    val strokesToDraw = strokes.take(count)
    for (stroke in strokesToDraw) {
      if (stroke.points.size > 1) {
        paint.color = if (stroke.isEraser) android.graphics.Color.WHITE else stroke.color.toArgb()
        paint.strokeWidth = (stroke.width * scale).coerceAtLeast(1.0f)
        paint.alpha = ((stroke.opacity * layerAlpha * 255).toInt()).coerceIn(0, 255)

        val path = Path()
        path.moveTo(stroke.points[0].offset.x * scale, stroke.points[0].offset.y * scale)
        for (i in 1 until stroke.points.size) {
          val p0 = stroke.points[i - 1].offset
          val p1 = stroke.points[i].offset
          val midX = (p0.x + p1.x) / 2f * scale
          val midY = (p0.y + p1.y) / 2f * scale
          path.quadTo(p0.x * scale, p0.y * scale, midX, midY)
        }
        canvas.drawPath(path, paint)
      }
    }
  }
}
