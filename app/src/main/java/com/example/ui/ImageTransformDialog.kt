package com.example.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Layer

@Composable
fun ImageTransformDialog(
  layer: Layer,
  paperWidth: Float,
  paperHeight: Float,
  onUpdateTransform: (offsetX: Float, offsetY: Float, scale: Float, rotation: Float) -> Unit,
  onDismiss: () -> Unit
) {
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Default.Transform, contentDescription = "Transform", tint = Color(0xFF007ACC))
          Text(
            text = "ছবি ট্রান্সফর্ম ও পজিশন",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
          )
        }
        Text(
          text = "✕",
          fontSize = 16.sp,
          color = Color(0xFF94A3B8),
          modifier = Modifier.clickable(onClick = onDismiss).padding(4.dp)
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // 1. Scale / সাইজ ছোট-বড় করুন
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("সাইজ (Scale):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
        Text("${(layer.imageScale * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007ACC))
      }
      Slider(
        value = layer.imageScale,
        onValueChange = {
          onUpdateTransform(layer.imageOffsetX, layer.imageOffsetY, it, layer.imageRotation)
        },
        valueRange = 0.15f..3.0f,
        colors = SliderDefaults.colors(thumbColor = Color(0xFF007ACC), activeTrackColor = Color(0xFF007ACC)),
        modifier = Modifier.fillMaxWidth().height(26.dp)
      )

      Spacer(modifier = Modifier.height(8.dp))

      // 2. Position X (বামে - ডানে সরান)
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("বামে - ডানে পজিশন (X):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
        Text("${layer.imageOffsetX.toInt()} px", fontSize = 11.sp, color = Color(0xFF64748B))
      }
      Slider(
        value = layer.imageOffsetX,
        onValueChange = {
          onUpdateTransform(it, layer.imageOffsetY, layer.imageScale, layer.imageRotation)
        },
        valueRange = -paperWidth / 1.5f..paperWidth / 1.5f,
        colors = SliderDefaults.colors(thumbColor = Color(0xFF007ACC), activeTrackColor = Color(0xFF007ACC)),
        modifier = Modifier.fillMaxWidth().height(26.dp)
      )

      Spacer(modifier = Modifier.height(8.dp))

      // 3. Position Y (উপরে - নিচে সরান)
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("উপরে - নিচে পজিশন (Y):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
        Text("${layer.imageOffsetY.toInt()} px", fontSize = 11.sp, color = Color(0xFF64748B))
      }
      Slider(
        value = layer.imageOffsetY,
        onValueChange = {
          onUpdateTransform(layer.imageOffsetX, it, layer.imageScale, layer.imageRotation)
        },
        valueRange = -paperHeight / 1.5f..paperHeight / 1.5f,
        colors = SliderDefaults.colors(thumbColor = Color(0xFF007ACC), activeTrackColor = Color(0xFF007ACC)),
        modifier = Modifier.fillMaxWidth().height(26.dp)
      )

      Spacer(modifier = Modifier.height(8.dp))

      // 4. Rotation / ঘোরান
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("ঘোরান (Rotation):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
        Text("${layer.imageRotation.toInt()}°", fontSize = 11.sp, color = Color(0xFF64748B))
      }
      Slider(
        value = layer.imageRotation,
        onValueChange = {
          onUpdateTransform(layer.imageOffsetX, layer.imageOffsetY, layer.imageScale, it)
        },
        valueRange = -180f..180f,
        colors = SliderDefaults.colors(thumbColor = Color(0xFF007ACC), activeTrackColor = Color(0xFF007ACC)),
        modifier = Modifier.fillMaxWidth().height(26.dp)
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Preset Quick Actions: Center / Fit / Reset
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        OutlinedButton(
          onClick = {
            // Reset to Center
            onUpdateTransform(0f, 0f, 1.0f, 0f)
          },
          modifier = Modifier.weight(1f).height(36.dp),
          shape = RoundedCornerShape(6.dp)
        ) {
          Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(12.dp))
          Spacer(modifier = Modifier.width(2.dp))
          Text("রিসেট", fontSize = 11.sp)
        }

        OutlinedButton(
          onClick = {
            // Fit to canvas
            val scale = (paperWidth / (layer.imageOriginalWidth.takeIf { it > 0 } ?: paperWidth)).coerceAtMost(
              paperHeight / (layer.imageOriginalHeight.takeIf { it > 0 } ?: paperHeight)
            )
            onUpdateTransform(0f, 0f, scale, 0f)
          },
          modifier = Modifier.weight(1.2f).height(36.dp),
          shape = RoundedCornerShape(6.dp)
        ) {
          Icon(Icons.Default.CropFree, contentDescription = "Fit", modifier = Modifier.size(12.dp))
          Spacer(modifier = Modifier.width(2.dp))
          Text("ক্যানভাসে ফিট", fontSize = 11.sp)
        }

        Button(
          onClick = onDismiss,
          modifier = Modifier.weight(1f).height(36.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007ACC)),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text("সম্পন্ন", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
