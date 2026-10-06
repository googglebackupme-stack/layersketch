package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.CanvasSize
import com.example.model.Layer
import com.example.model.PencilStroke
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RecordingPlaybackDialog(
  canvasSize: CanvasSize,
  layerName: String,
  recordedStrokes: List<PencilStroke>,
  canvasColor: Color = Color.White,
  canvasBgImage: ImageBitmap? = null,
  layers: List<Layer> = emptyList(),
  onResumeRecording: () -> Unit,
  onRestartRecording: () -> Unit,
  onFinishRecording: () -> Unit,
  onShareClick: () -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var isPlaying by remember { mutableStateOf(true) }
  var playbackProgress by remember { mutableFloatStateOf(1.0f) }
  var isSavingToGallery by remember { mutableStateOf(false) }
  var isSavedSuccess by remember { mutableStateOf(false) }
  var showRestartConfirm by remember { mutableStateOf(false) }

  val totalStrokes = recordedStrokes.size
  val visibleStrokeCount = (totalStrokes * playbackProgress).toInt().coerceIn(0, totalStrokes)

  // Calculate dynamic aspect-ratio aligned preview canvas dimensions
  val maxPreviewDim = 240.dp
  val aspect = canvasSize.width / canvasSize.height
  val previewWidth = if (aspect >= 1f) maxPreviewDim else maxPreviewDim * aspect
  val previewHeight = if (aspect >= 1f) maxPreviewDim / aspect else maxPreviewDim

  LaunchedEffect(isPlaying) {
    if (isPlaying) {
      if (playbackProgress >= 1f) playbackProgress = 0f
      while (isPlaying && playbackProgress < 1f) {
        delay(35)
        playbackProgress = (playbackProgress + 0.02f).coerceAtMost(1f)
      }
      if (playbackProgress >= 1f) isPlaying = false
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .width(340.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(Color.White)
        .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(16.dp))
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "🎬 রেকর্ড প্রিভিউ ও এক্সপোর্ট",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
          )
          Text(
            text = "ক্যানভাস: ${canvasSize.name} • ${totalStrokes} স্ট্রোক",
            fontSize = 11.sp,
            color = Color(0xFF64748B)
          )
        }
        Text(
          text = "✕",
          fontSize = 16.sp,
          color = Color(0xFF94A3B8),
          modifier = Modifier.clickable(onClick = onDismiss).padding(4.dp)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Video Preview Canvas (Dynamically matches canvas size aspect ratio: A4, A5, Square, Story, etc.)
      Box(
        modifier = Modifier
          .width(previewWidth)
          .height(previewHeight)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFFF8FAFC))
          .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
      ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
          // 1. Draw canvas background color
          drawRect(color = canvasColor)

          // 2. Draw canvas background image
          canvasBgImage?.let { bgBmp ->
            drawImage(
              image = bgBmp,
              dstOffset = IntOffset.Zero,
              dstSize = IntSize(size.width.toInt(), size.height.toInt())
            )
          }

          val scale = size.width / canvasSize.width
          val strokesToDraw = recordedStrokes.take(visibleStrokeCount)

          for (stroke in strokesToDraw) {
            if (stroke.points.size > 1) {
              val path = Path()
              path.moveTo(stroke.points[0].offset.x * scale, stroke.points[0].offset.y * scale)
              for (p in stroke.points) {
                path.lineTo(p.offset.x * scale, p.offset.y * scale)
              }
              drawPath(
                path = path,
                color = if (stroke.isEraser) canvasColor else stroke.color,
                style = Stroke(width = stroke.width * scale, cap = StrokeCap.Round)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Playback scrubber
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF007ACC))
            .clickable { isPlaying = !isPlaying },
          contentAlignment = Alignment.Center
        ) {
          Text(if (isPlaying) "⏸" else "▶", color = Color.White, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.width(6.dp))

        Slider(
          value = playbackProgress,
          onValueChange = {
            isPlaying = false
            playbackProgress = it
          },
          valueRange = 0f..1f,
          modifier = Modifier.weight(1f).height(24.dp),
          colors = SliderDefaults.colors(
            thumbColor = Color(0xFF007ACC),
            activeTrackColor = Color(0xFF007ACC)
          )
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // PRIMARY ACTION 1: RESUME DRAWING & RECORDING
      Button(
        onClick = onResumeRecording,
        modifier = Modifier.fillMaxWidth().height(40.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
        shape = RoundedCornerShape(8.dp)
      ) {
        Icon(Icons.Default.Edit, contentDescription = "Edit More", tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("▶ আরও ড্রয়িং চালিয়ে যান (Resume)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
      }

      Spacer(modifier = Modifier.height(6.dp))

      // PRIMARY ACTION 2: SAVE VIDEO TO GALLERY (Matches exact canvas size, color & background)
      Button(
        onClick = {
          if (!isSavingToGallery) {
            isSavingToGallery = true
            coroutineScope.launch {
              GalleryExporter.saveLayerRecordingToGallery(
                context = context,
                layerName = layerName,
                strokes = recordedStrokes,
                paperWidth = canvasSize.width.toInt(),
                paperHeight = canvasSize.height.toInt(),
                layers = layers,
                canvasColor = canvasColor,
                canvasBgImage = canvasBgImage
              )
              isSavingToGallery = false
              isSavedSuccess = true
            }
          }
        },
        modifier = Modifier.fillMaxWidth().height(40.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isSavedSuccess) Color(0xFF059669) else Color(0xFF007ACC)
        ),
        shape = RoundedCornerShape(8.dp)
      ) {
        if (isSavingToGallery) {
          CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
          Spacer(modifier = Modifier.width(6.dp))
          Text("গ্যালারিতে সেভ হচ্ছে...", fontSize = 12.sp, color = Color.White)
        } else {
          Icon(Icons.Default.Download, contentDescription = "Save to Gallery", tint = Color.White, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            if (isSavedSuccess) "ভিডিও গ্যালারিতে সেভ হয়েছে ✓" else "গ্যালারিতে সেভ ও এক্সপোর্ট করুন",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // SECONDARY ACTIONS: Restart recording (with warning modal), Share, Finish
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        OutlinedButton(
          onClick = {
            if (recordedStrokes.isNotEmpty()) {
              showRestartConfirm = true
            } else {
              onRestartRecording()
            }
          },
          modifier = Modifier.weight(1f).height(34.dp),
          shape = RoundedCornerShape(6.dp)
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Restart", modifier = Modifier.size(12.dp))
          Spacer(modifier = Modifier.width(2.dp))
          Text("নতুন রেকর্ড", fontSize = 10.sp)
        }

        OutlinedButton(
          onClick = onShareClick,
          modifier = Modifier.weight(1f).height(34.dp),
          shape = RoundedCornerShape(6.dp)
        ) {
          Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(12.dp))
          Spacer(modifier = Modifier.width(2.dp))
          Text("শেয়ার", fontSize = 10.sp)
        }

        Button(
          onClick = onFinishRecording,
          modifier = Modifier.weight(1f).height(34.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
          shape = RoundedCornerShape(6.dp)
        ) {
          Icon(Icons.Default.Stop, contentDescription = "Finish", tint = Color.White, modifier = Modifier.size(12.dp))
          Spacer(modifier = Modifier.width(2.dp))
          Text("শেষ", fontSize = 10.sp, color = Color.White)
        }
      }
    }

    // Warning confirmation modal to prevent accidental reset of recording
    if (showRestartConfirm) {
      androidx.compose.material3.AlertDialog(
        onDismissRequest = { showRestartConfirm = false },
        title = {
          Text("⚠️ রেকর্ড রিসেট সতর্কবার্তা", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        },
        text = {
          Text("বর্তমান রেকর্ডিংয়ের ${recordedStrokes.size}টি স্ট্রোক মুছে নতুন রেকর্ড শুরু হবে। আপনি কি নিশ্চিত?", fontSize = 12.sp)
        },
        confirmButton = {
          Button(
            onClick = {
              showRestartConfirm = false
              onRestartRecording()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
          ) {
            Text("হ্যাঁ, রিসেট করুন", fontSize = 11.sp)
          }
        },
        dismissButton = {
          OutlinedButton(onClick = { showRestartConfirm = false }) {
            Text("বাতিল", fontSize = 11.sp)
          }
        }
      )
    }
  }
}
