package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun AddTextDialog(
  initialText: String = "LayerSketch",
  initialSize: Float = 48f,
  onAddText: (text: String, size: Float) -> Unit,
  onDismiss: () -> Unit
) {
  var text by remember { mutableStateOf(initialText) }
  var textSize by remember { mutableFloatStateOf(initialSize) }

  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .width(320.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(Color.White)
        .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(14.dp))
        .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(Icons.Default.TextFields, contentDescription = "Text", tint = Color(0xFF007ACC))
          Text("টেক্সট লেয়ার যোগ করুন", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        }
        Text("✕", fontSize = 16.sp, color = Color(0xFF94A3B8), modifier = Modifier.clickable(onClick = onDismiss).padding(4.dp))
      }

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("আপনার টেক্সট লিখুন") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = false,
        maxLines = 3
      )

      Spacer(modifier = Modifier.height(10.dp))

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("ফন্ট সাইজ:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
        Text("${textSize.toInt()} pt", fontSize = 11.sp, color = Color(0xFF007ACC), fontWeight = FontWeight.Bold)
      }
      Slider(
        value = textSize,
        onValueChange = { textSize = it },
        valueRange = 20f..120f,
        colors = SliderDefaults.colors(thumbColor = Color(0xFF007ACC), activeTrackColor = Color(0xFF007ACC)),
        modifier = Modifier.fillMaxWidth().height(26.dp)
      )

      Spacer(modifier = Modifier.height(14.dp))

      Button(
        onClick = {
          if (text.isNotBlank()) {
            onAddText(text, textSize)
            onDismiss()
          }
        },
        modifier = Modifier.fillMaxWidth().height(40.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007ACC)),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("ক্যানভাসে যোগ করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
      }
    }
  }
}
