package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ColorWheelDialog(
  initialColor: Color,
  onColorSelected: (Color) -> Unit,
  onDismiss: () -> Unit
) {
  var hue by remember { mutableFloatStateOf(0f) }
  var saturation by remember { mutableFloatStateOf(1f) }
  var value by remember { mutableFloatStateOf(1f) }

  val currentColor = Color.hsv(hue, saturation, value)

  val quickSwatches = listOf(
    Color(0xFF222833), // 2B Graphite Black
    Color(0xFF0C0F15), // Deep Black
    Color(0xFF5E6774), // Graphite Grey
    Color(0xFFFFFFFF), // White
    Color(0xFFEF4444), // Red
    Color(0xFFF97316), // Orange
    Color(0xFFEAB308), // Yellow
    Color(0xFF22C55E), // Green
    Color(0xFF06B6D4), // Cyan
    Color(0xFF3B82F6), // Blue
    Color(0xFFA855F7), // Purple
    Color(0xFF78350F)  // Brown
  )

  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .width(320.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(Color.White)
        .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(16.dp))
        .padding(18.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "Color Wheel (কালার সিলেক্টর)",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1E293B)
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Interactive Finger-Draggable Rainbow Color Wheel
      Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
      ) {
        Canvas(
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .pointerInput(Unit) {
              detectDragGestures { change, _ ->
                val center = Offset(size.width / 2f, size.height / 2f)
                val pos = change.position
                val angle = Math.toDegrees(atan2((pos.y - center.y).toDouble(), (pos.x - center.x).toDouble())).toFloat()
                hue = (angle + 360f) % 360f
                change.consume()
              }
            }
            .pointerInput(Unit) {
              detectTapGestures { pos ->
                val center = Offset(size.width / 2f, size.height / 2f)
                val angle = Math.toDegrees(atan2((pos.y - center.y).toDouble(), (pos.x - center.x).toDouble())).toFloat()
                hue = (angle + 360f) % 360f
              }
            }
        ) {
          val radius = size.minDimension / 2f - 16f
          val center = Offset(size.width / 2f, size.height / 2f)

          // Rainbow Sweep Ring
          drawCircle(
            brush = Brush.sweepGradient(
              listOf(
                Color.Red,
                Color.Yellow,
                Color.Green,
                Color.Cyan,
                Color.Blue,
                Color.Magenta,
                Color.Red
              ),
              center = center
            ),
            radius = radius,
            center = center,
            style = Stroke(width = 28f)
          )

          // Pointer Handle on Wheel
          val rad = Math.toRadians(hue.toDouble())
          val handleX = center.x + cos(rad).toFloat() * radius
          val handleY = center.y + sin(rad).toFloat() * radius
          drawCircle(
            color = Color.White,
            radius = 12f,
            center = Offset(handleX, handleY)
          )
          drawCircle(
            color = Color.Black,
            radius = 12f,
            center = Offset(handleX, handleY),
            style = Stroke(width = 2f)
          )
        }

        // Center Live Color Circle
        Box(
          modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(currentColor)
            .border(3.dp, Color.White, CircleShape)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Brightness / Value Slider
      Text(
        text = "Shade / Brightness: ${(value * 100).toInt()}%",
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF64748B)
      )
      Slider(
        value = value,
        onValueChange = { value = it },
        valueRange = 0.05f..1.0f,
        colors = SliderDefaults.colors(
          thumbColor = Color(0xFF007ACC),
          activeTrackColor = Color(0xFF007ACC)
        ),
        modifier = Modifier.fillMaxWidth().height(24.dp)
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Quick Artist Swatches (Touch with Finger)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        quickSwatches.take(6).forEach { color ->
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(color)
              .border(1.dp, Color(0xFFD1D5DB), CircleShape)
              .clickable {
                onColorSelected(color)
                onDismiss()
              }
          )
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        quickSwatches.drop(6).forEach { color ->
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(color)
              .border(1.dp, Color(0xFFD1D5DB), CircleShape)
              .clickable {
                onColorSelected(color)
                onDismiss()
              }
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Apply Button
      Button(
        onClick = {
          onColorSelected(currentColor)
          onDismiss()
        },
        modifier = Modifier.fillMaxWidth().height(42.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007ACC)),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Apply Color (নির্বাচন করুন)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
      }
    }
  }
}
