package com.example.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RecordingOverlay(
  layerName: String,
  recordedSeconds: Int,
  recordedStrokeCount: Int,
  onPausePreviewClick: () -> Unit
) {
  val transition = rememberInfiniteTransition(label = "recPulse")
  val alpha by transition.animateFloat(
    initialValue = 1f,
    targetValue = 0.2f,
    animationSpec = infiniteRepeatable(
      animation = tween(500),
      repeatMode = RepeatMode.Reverse
    ),
    label = "recDot"
  )

  val mins = recordedSeconds / 60
  val secs = recordedSeconds % 60
  val timeText = String.format("%02d:%02d", mins, secs)

  // Ultra-compact floating pill at top
  Box(
    modifier = Modifier.padding(top = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Row(
      modifier = Modifier
        .clip(RoundedCornerShape(16.dp))
        .background(Color(0xF00F172A))
        .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(16.dp))
        .padding(horizontal = 10.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      // Blinking Red Dot
      Box(
        modifier = Modifier
          .size(8.dp)
          .clip(CircleShape)
          .background(Color(0xFFEF4444).copy(alpha = alpha))
      )

      // Time & Layer
      Text(
        text = "REC: $timeText ($recordedStrokeCount স্ট্রোক)",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )

      // Pause & Preview Button (পজ ও প্রিভিউ)
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .background(Color(0xFFDC2626))
          .clickable(onClick = onPausePreviewClick)
          .padding(horizontal = 8.dp, vertical = 3.dp)
      ) {
        Text(
          text = "⏸ পজ ও প্রিভিউ",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
      }
    }
  }
}
