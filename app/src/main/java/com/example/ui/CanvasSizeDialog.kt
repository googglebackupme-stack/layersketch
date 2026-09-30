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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.CanvasPresets
import com.example.model.CanvasSize

@Composable
fun CanvasSizeDialog(
  currentCanvasSize: CanvasSize,
  onSelectSize: (CanvasSize) -> Unit,
  onDismiss: () -> Unit
) {
  var isCustom by remember { mutableStateOf(false) }
  var customWidth by remember { mutableStateOf(currentCanvasSize.width.toInt().toString()) }
  var customHeight by remember { mutableStateOf(currentCanvasSize.height.toInt().toString()) }

  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .width(340.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(Color.White)
        .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(16.dp))
        .padding(18.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.AspectRatio, contentDescription = "Canvas Size", tint = Color(0xFF007ACC))
          Text(
            text = "ক্যানভাস সাইজ নির্ধারণ করুন",
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

      Spacer(modifier = Modifier.height(14.dp))

      // Presets List
      LazyColumn(
        modifier = Modifier.fillMaxWidth().height(260.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(CanvasPresets.ALL) { preset ->
          val isSelected = !isCustom &&
            preset.width == currentCanvasSize.width &&
            preset.height == currentCanvasSize.height

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
              .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) Color(0xFF007ACC) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(8.dp)
              )
              .clickable {
                isCustom = false
                onSelectSize(preset)
                onDismiss()
              }
              .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = preset.name,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) Color(0xFF007ACC) else Color(0xFF1E293B)
              )
              Text(
                text = preset.description,
                fontSize = 11.sp,
                color = Color(0xFF64748B)
              )
            }

            if (isSelected) {
              Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFF007ACC), modifier = Modifier.size(18.dp))
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Custom Size Section
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFFF8FAFC))
          .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
          .padding(10.dp)
      ) {
        Text(
          text = "কাস্টম সাইজ (Custom Width & Height)",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFF334155)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = customWidth,
            onValueChange = { customWidth = it.filter { ch -> ch.isDigit() } },
            label = { Text("প্রস্থ (Width px)", fontSize = 10.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).height(54.dp),
            singleLine = true
          )
          OutlinedTextField(
            value = customHeight,
            onValueChange = { customHeight = it.filter { ch -> ch.isDigit() } },
            label = { Text("উচ্চতা (Height px)", fontSize = 10.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).height(54.dp),
            singleLine = true
          )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
          onClick = {
            val w = customWidth.toFloatOrNull()?.coerceIn(200f, 6000f) ?: 1080f
            val h = customHeight.toFloatOrNull()?.coerceIn(200f, 6000f) ?: 1080f
            onSelectSize(CanvasSize("Custom (${w.toInt()}×${h.toInt()})", w, h, "${w.toInt()} × ${h.toInt()} px"))
            onDismiss()
          },
          modifier = Modifier.fillMaxWidth().height(38.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007ACC)),
          shape = RoundedCornerShape(6.dp)
        ) {
          Text("কাস্টম সাইজ সেট করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
      }
    }
  }
}
