package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Layer
import kotlinx.coroutines.delay

@Composable
fun RecordingStudioDialog(
  layers: List<Layer>,
  recTargetLayerId: String,
  onDismiss: () -> Unit,
  onRecordCompleted: (layerName: String, frameCount: Int) -> Unit
) {
  var selectedTargetId by remember { mutableStateOf(recTargetLayerId) }
  var isRecordingActive by remember { mutableStateOf(false) }
  var recordedSeconds by remember { mutableFloatStateOf(0f) }
  var frameCount by remember { mutableIntStateOf(0) }

  val targetLayer = layers.find { it.id == selectedTargetId } ?: layers.firstOrNull()

  // Live preview animation timer
  val infiniteTransition = rememberInfiniteTransition(label = "preview")
  val previewTime by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 5.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(5000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "time"
  )

  // Recording timer simulation
  LaunchedEffect(isRecordingActive) {
    if (isRecordingActive) {
      recordedSeconds = 0f
      frameCount = 0
      while (recordedSeconds < 5.0f) {
        delay(100)
        recordedSeconds += 0.1f
        frameCount += 3 // 30 fps
      }
      isRecordingActive = false
      onRecordCompleted(targetLayer?.name ?: "Layer", frameCount)
    }
  }

  Dialog(onDismissRequest = { if (!isRecordingActive) onDismiss() }) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(Color(0xFF161B26))
        .border(1.dp, Color(0xFF2A3449), RoundedCornerShape(16.dp))
        .padding(16.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .clip(CircleShape)
              .background(Color(0xFFEF4444))
          )
          Text(
            text = "Layer Recording Studio",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF0F6FC)
          )
        }

        if (!isRecordingActive) {
          Text(
            text = "✕",
            fontSize = 16.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier
              .clickable(onClick = onDismiss)
              .padding(4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Isolated Recording Architecture Banner
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0x22EF4444))
          .border(1.dp, Color(0x55EF4444), RoundedCornerShape(8.dp))
          .padding(10.dp)
      ) {
        Column {
          Text(
            text = "⚡ Isolated Single-Layer Renderer",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF87171)
          )
          Text(
            text = "Records ONLY target layer '${targetLayer?.name}'. All other background, sketch, and text layers are completely excluded from the exported video.",
            fontSize = 11.sp,
            color = Color(0xFFCBD5E1),
            lineHeight = 15.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Layer Selector Chips
      Text(
        text = "Select Recording Target:",
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF94A3B8)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        layers.take(3).forEach { l ->
          val isTarget = l.id == selectedTargetId
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(6.dp))
              .background(if (isTarget) Color(0xFFEF4444) else Color(0xFF202738))
              .border(
                1.dp,
                if (isTarget) Color(0xFFF87171) else Color(0xFF333D52),
                RoundedCornerShape(6.dp)
              )
              .clickable { if (!isRecordingActive) selectedTargetId = l.id }
              .padding(vertical = 6.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = l.name.take(12),
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = if (isTarget) Color.White else Color(0xFF94A3B8),
              maxLines = 1
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Live Isolated Preview Stage
      Text(
        text = "LIVE ISOLATED PREVIEW (Target Only):",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF64748B)
      )
      Spacer(modifier = Modifier.height(4.dp))

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(160.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF0F1218))
          .border(1.dp, Color(0xFF2A3449), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
      ) {
        // Render isolated layer directly onto preview canvas
        if (targetLayer != null) {
          Canvas(modifier = Modifier.size(150.dp)) {
            // Draw subtle paper bounds
            drawRoundRect(
              color = Color.White,
              size = Size(size.width, size.height),
              cornerRadius = CornerRadius(4f, 4f)
            )
            // Scale and draw only target layer
            val scaleFactor = size.width / 1080f
            scale(scale = scaleFactor, pivot = Offset.Zero) {
              drawSingleLayerIsolated(this, targetLayer, previewTime, 1080f, 1080f)
            }
          }
        }

        // Recording in progress overlay badge
        if (isRecordingActive) {
          Box(
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(8.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(Color(0xDDDC2626))
              .padding(horizontal = 8.dp, vertical = 3.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
              Text(
                text = "REC ${String.format("%.1f", recordedSeconds)}s / 5.0s",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (!isRecordingActive) {
          Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202738)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(end = 8.dp)
          ) {
            Text("Cancel", fontSize = 12.sp, color = Color(0xFF94A3B8))
          }

          Button(
            onClick = { isRecordingActive = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("● Record Isolated Layer", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
          }
        } else {
          Button(
            onClick = { isRecordingActive = false },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text("Stop Recording", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
          }
        }
      }
    }
  }
}
