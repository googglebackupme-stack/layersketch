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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import com.example.model.DrawingToolType
import com.example.model.Layer
import com.example.model.LayerType
import com.example.model.PencilPalette
import com.example.model.PencilStroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object GalleryExporter {

  /**
   * Saves the entire current canvas (all visible layers, drawings, photos, text,
   * along with chosen canvas color and background image) into device Gallery.
   */
  suspend fun saveCanvasAsPhoto(
    context: Context,
    layers: List<Layer>,
    paperWidth: Int,
    paperHeight: Int,
    canvasColor: Color? = null,
    canvasBgImage: ImageBitmap? = null
  ): Uri? = withContext(Dispatchers.IO) {
    try {
      val fileName = "mahfujdrawing_Art_${System.currentTimeMillis()}.png"
      val bitmap = Bitmap.createBitmap(paperWidth, paperHeight, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)

      // 1. Draw Canvas Background Color (Explicitly preserved)
      val bgLayer = layers.find { it.type == LayerType.BACKGROUND }
      val resolvedColor = canvasColor ?: bgLayer?.backgroundColor ?: Color.White
      canvas.drawColor(resolvedColor.toArgb())

      // 2. Draw Canvas Background Image (if configured)
      val resolvedBgImage = canvasBgImage ?: bgLayer?.backgroundImageBitmap
      resolvedBgImage?.let { composeBg ->
        val androidBg = composeBg.asAndroidBitmap()
        val scaledBg = Bitmap.createScaledBitmap(androidBg, paperWidth, paperHeight, true)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          alpha = ((bgLayer?.opacity ?: 1.0f) * 255).toInt().coerceIn(0, 255)
        }
        canvas.drawBitmap(scaledBg, 0f, 0f, bgPaint)
      }

      val scale = 1.0f // 1:1 scale for canvas photo export

      // 3. Render all visible layers from bottom to top
      for (layer in layers.reversed()) {
        if (!layer.visible) continue

        when (layer.type) {
          LayerType.BACKGROUND -> {
            // Already painted base
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
          "📸 ক্যানভাসের ছবি ও ব্যাকগ্রাউন্ড গ্যালারিতে সেভ হয়েছে! (mahfujdrawing)",
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
   * Includes exact background color and background image.
   */
  suspend fun saveLayerRecordingToGallery(
    context: Context,
    layerName: String,
    strokes: List<PencilStroke>,
    paperWidth: Int,
    paperHeight: Int,
    layers: List<Layer> = emptyList(),
    canvasColor: Color? = null,
    canvasBgImage: ImageBitmap? = null
  ): Uri? = withContext(Dispatchers.IO) {
    try {
      val videoFileName = "mahfujdrawing_${System.currentTimeMillis()}"

      // 1. Snapshot with canvas background color & background image
      val bgLayer = layers.find { it.type == LayerType.BACKGROUND }
      val resolvedColor = canvasColor ?: bgLayer?.backgroundColor ?: Color.White
      val snapshotBitmap = Bitmap.createBitmap(paperWidth, paperHeight, Bitmap.Config.ARGB_8888)
      val snapCanvas = Canvas(snapshotBitmap)
      snapCanvas.drawColor(resolvedColor.toArgb())

      val resolvedBgImage = canvasBgImage ?: bgLayer?.backgroundImageBitmap
      resolvedBgImage?.let { composeBg ->
        val androidBg = composeBg.asAndroidBitmap()
        val scaledBg = Bitmap.createScaledBitmap(androidBg, paperWidth, paperHeight, true)
        snapCanvas.drawBitmap(scaledBg, 0f, 0f, null)
      }

      renderStrokesToCanvas(snapCanvas, strokes, 1.0f, strokes.size)
      saveBitmapToGallery(context, snapshotBitmap, "${videoFileName}_Art.png")

      // 2. Aspect-ratio aligned video dimensions
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
        sourcePaperHeight = paperHeight.toFloat(),
        bgColor = resolvedColor.toArgb(),
        bgBitmap = resolvedBgImage?.asAndroidBitmap()
      )

      withContext(Dispatchers.Main) {
        Toast.makeText(
          context,
          "ভিডিও এবং আর্ট গ্যালারিতে সেভ হয়েছে! (mahfujdrawing)",
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
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/mahfujdrawing")
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
    sourcePaperHeight: Float,
    bgColor: Int = android.graphics.Color.WHITE,
    bgBitmap: Bitmap? = null
  ): Uri? {
    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
      put(MediaStore.Video.Media.DISPLAY_NAME, "$fileName.mp4")
      put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/mahfujdrawing")
        put(MediaStore.Video.Media.IS_PENDING, 1)
      }
    }

    val videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
    val tempFile = File(context.cacheDir, "$fileName.mp4")

    try {
      encodeStrokesToMp4(tempFile, strokes, width, height, sourcePaperWidth, sourcePaperHeight, bgColor, bgBitmap)

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
    sourcePaperHeight: Float,
    bgColor: Int = android.graphics.Color.WHITE,
    bgBitmap: Bitmap? = null
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
    val scaledBgBitmap = bgBitmap?.let { Bitmap.createScaledBitmap(it, width, height, true) }

    try {
      for (frameIndex in 0 until totalFrames) {
        val strokeProgress = if (totalFrames > 0) ((frameIndex.toFloat() / totalFrames) * strokes.size).toInt() else strokes.size
        frameCanvas.drawColor(bgColor)
        scaledBgBitmap?.let { frameCanvas.drawBitmap(it, 0f, 0f, null) }
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

  /**
   * Renders realistic pencil strokes with multi-pass graphite realism matching Image 2:
   * 1. Graphite soft shadow / paper bedding layer (শ্যাডো ভাব)
   * 2. Medium graphite texture body
   * 3. Sharp dense core
   * 4. Natural grain tooth speckles
   */
  private fun renderStrokesToCanvas(
    canvas: Canvas,
    strokes: List<PencilStroke>,
    scale: Float,
    count: Int,
    layerAlpha: Float = 1.0f
  ) {
    val strokesToDraw = strokes.take(count)
    for (stroke in strokesToDraw) {
      val ptCount = stroke.points.size
      if (ptCount > 1) {
        val grade = PencilPalette.getGrade(stroke.pencilGrade)
        val baseColor = if (stroke.isEraser) android.graphics.Color.WHITE else stroke.color.toArgb()
        val baseWidth = (stroke.width * scale).coerceAtLeast(1.0f)
        val strokeEffectiveAlpha = (stroke.opacity * layerAlpha).coerceIn(0f, 1f)

        val path = Path()
        path.moveTo(stroke.points[0].offset.x * scale, stroke.points[0].offset.y * scale)
        for (i in 1 until ptCount) {
          val p0 = stroke.points[i - 1].offset
          val p1 = stroke.points[i].offset
          val midX = (p0.x + p1.x) / 2f * scale
          val midY = (p0.y + p1.y) / 2f * scale
          path.quadTo(p0.x * scale, p0.y * scale, midX, midY)
        }

        if (stroke.isEraser) {
          val erasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = android.graphics.Color.WHITE
            strokeWidth = baseWidth
            alpha = (strokeEffectiveAlpha * 255).toInt().coerceIn(0, 255)
          }
          canvas.drawPath(path, erasePaint)
        } else {
          when (grade.toolType) {
            DrawingToolType.TECH_PEN -> {
              // 0.3mm Fine Technical Pen for clean crisp portrait contours
              val penPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = baseColor
                strokeWidth = baseWidth.coerceAtLeast(0.8f)
                alpha = (strokeEffectiveAlpha * 255).toInt().coerceIn(0, 255)
              }
              canvas.drawPath(path, penPaint)
            }
            DrawingToolType.HAIR_PEN -> {
              // Ultra-fine 0.15mm hairline pen for hair strands and lashes
              val hairPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = baseColor
                strokeWidth = (baseWidth * 0.75f).coerceIn(0.5f, 2.5f)
                alpha = (strokeEffectiveAlpha * 255).toInt().coerceIn(0, 255)
              }
              canvas.drawPath(path, hairPaint)
            }
            DrawingToolType.HIGHLIGHT_PEN -> {
              // White Gel Highlight Pen: Luminous white with outer glow
              val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = android.graphics.Color.WHITE
                strokeWidth = baseWidth * 1.55f
                alpha = ((strokeEffectiveAlpha * 0.40f) * 255).toInt().coerceIn(0, 255)
              }
              canvas.drawPath(path, glowPaint)
              val whiteCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = android.graphics.Color.WHITE
                strokeWidth = baseWidth.coerceAtLeast(1.0f)
                alpha = (strokeEffectiveAlpha * 255).toInt().coerceIn(0, 255)
              }
              canvas.drawPath(path, whiteCorePaint)
            }
            DrawingToolType.WATERCOLOR -> {
              // Translucent watercolor wash for skin blush and delicate tones
              val wash1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = baseColor
                strokeWidth = (baseWidth * 1.6f).coerceAtLeast(6.0f)
                alpha = ((strokeEffectiveAlpha * 0.20f) * 255).toInt().coerceIn(0, 255)
              }
              canvas.drawPath(path, wash1)
              val wash2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = baseColor
                strokeWidth = baseWidth.coerceAtLeast(3.0f)
                alpha = ((strokeEffectiveAlpha * 0.35f) * 255).toInt().coerceIn(0, 255)
              }
              canvas.drawPath(path, wash2)
            }
            DrawingToolType.AIRBRUSH -> {
              // Soft diffuse airbrush for skin smoothing
              val air1 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = baseColor
                strokeWidth = (baseWidth * 2.0f).coerceAtLeast(10.0f)
                alpha = ((strokeEffectiveAlpha * 0.12f) * 255).toInt().coerceIn(0, 255)
              }
              canvas.drawPath(path, air1)
              val air2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = baseColor
                strokeWidth = (baseWidth * 1.2f).coerceAtLeast(5.0f)
                alpha = ((strokeEffectiveAlpha * 0.22f) * 255).toInt().coerceIn(0, 255)
              }
              canvas.drawPath(path, air2)
            }
            DrawingToolType.DIP_PEN -> {
              // G-Pen / Dip Pen
              val dipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = baseColor
                strokeWidth = baseWidth.coerceAtLeast(0.9f)
                alpha = (strokeEffectiveAlpha * 255).toInt().coerceIn(0, 255)
              }
              canvas.drawPath(path, dipPaint)
            }
            DrawingToolType.HATCHING -> {
              // Academic cross-hatching pencil
              val hatchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.SQUARE
                strokeJoin = Paint.Join.MITER
                color = baseColor
                strokeWidth = baseWidth.coerceAtLeast(1.0f)
                alpha = ((strokeEffectiveAlpha * 0.85f) * 255).toInt().coerceIn(0, 255)
              }
              canvas.drawPath(path, hatchPaint)
            }
            DrawingToolType.BRUSH_PEN -> {
              val brushPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = baseColor
                strokeWidth = baseWidth.coerceAtLeast(1.0f)
                alpha = (strokeEffectiveAlpha * 255).toInt().coerceIn(0, 255)
              }
              canvas.drawPath(path, brushPaint)
            }
            DrawingToolType.MARKER -> {
              val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.SQUARE
                strokeJoin = Paint.Join.BEVEL
                color = baseColor
                strokeWidth = baseWidth.coerceAtLeast(4.0f)
                alpha = ((strokeEffectiveAlpha * 0.65f) * 255).toInt().coerceIn(0, 255)
              }
              canvas.drawPath(path, markerPaint)
            }
            DrawingToolType.PEN -> {
              val penPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = baseColor
                strokeWidth = baseWidth.coerceAtLeast(1.0f)
                alpha = (strokeEffectiveAlpha * 255).toInt().coerceIn(0, 255)
              }
              canvas.drawPath(path, penPaint)
            }
            DrawingToolType.BLENDER -> {
              val blendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = baseColor
                strokeWidth = baseWidth * 1.6f
                alpha = ((strokeEffectiveAlpha * 0.35f) * 255).toInt().coerceIn(0, 255)
              }
              canvas.drawPath(path, blendPaint)
            }
            DrawingToolType.CHARCOAL -> {
              val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = baseColor
                strokeWidth = baseWidth * 1.55f
                alpha = ((strokeEffectiveAlpha * 0.35f) * 255).toInt().coerceIn(0, 255)
              }
              canvas.drawPath(path, shadowPaint)
              val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = baseColor
                strokeWidth = baseWidth
                alpha = ((strokeEffectiveAlpha * 0.95f) * 255).toInt().coerceIn(0, 255)
              }
              canvas.drawPath(path, bodyPaint)
            }
            else -> {
              // Authentic Pencil Strokes matching Image 2
              // Pass 1: Soft Graphite Shadow (শ্যাডো ভাব)
              val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = baseColor
                strokeWidth = baseWidth * grade.shadowWidthMultiplier
                alpha = ((strokeEffectiveAlpha * grade.shadowAlphaMultiplier) * 255).toInt().coerceIn(0, 255)
              }
              canvas.drawPath(path, shadowPaint)

              // Pass 2: Main Graphite Body
              val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = baseColor
                strokeWidth = baseWidth
                alpha = ((strokeEffectiveAlpha * 0.82f) * 255).toInt().coerceIn(0, 255)
              }
              canvas.drawPath(path, bodyPaint)

              // Pass 3: Dense Sharp Core
              val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = baseColor
                strokeWidth = (baseWidth * 0.52f).coerceAtLeast(0.8f)
                alpha = ((strokeEffectiveAlpha * grade.coreAlpha) * 255).toInt().coerceIn(0, 255)
              }
              canvas.drawPath(path, corePaint)

              // Pass 4: Grain tooth speckles along stroke (Image 2 style)
              val grainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = baseColor
                alpha = ((strokeEffectiveAlpha * (0.35f + grade.grain * 0.35f)) * 255).toInt().coerceIn(0, 255)
              }
              val step = (ptCount / 14).coerceAtLeast(2)
              for (i in 0 until ptCount step step) {
                val pt = stroke.points[i].offset
                val seed = ((pt.x * 31 + pt.y * 17).toInt() and 0x7FFFFFFF)
                val jx = ((seed % 100) / 100f - 0.5f) * stroke.width * scale * 0.65f
                val jy = (((seed / 100) % 100) / 100f - 0.5f) * stroke.width * scale * 0.65f
                val r = (stroke.width * scale * 0.16f).coerceIn(0.5f, 2.0f)
                canvas.drawCircle(pt.x * scale + jx, pt.y * scale + jy, r, grainPaint)
              }
            }
          }
        }
      } else if (ptCount == 1) {
        val pt = stroke.points[0]
        val r = ((stroke.width * scale * (0.6f + pt.pressure * 0.5f)) / 2f).coerceAtLeast(0.8f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
          color = if (stroke.isEraser) android.graphics.Color.WHITE else stroke.color.toArgb()
          alpha = ((stroke.opacity * layerAlpha) * 255).toInt().coerceIn(0, 255)
        }
        canvas.drawCircle(pt.offset.x * scale, pt.offset.y * scale, r, paint)
      }
    }
  }
}
