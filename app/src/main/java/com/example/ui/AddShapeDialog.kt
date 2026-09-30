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
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.PanoramaFishEye
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ShapeType

@Composable
fun AddShapeDialog(
  onSelectShape: (ShapeType) -> Unit,
  onDismiss: () -> Unit
) {
  var selectedType by remember { mutableStateOf(ShapeType.RECTANGLE) }

  val shapes = listOf(
    Triple("আয়তক্ষেত্র (Rectangle)", Icons.Default.CropSquare, ShapeType.RECTANGLE),
    Triple("গোলক / বৃত্ত (Circle)", Icons.Default.PanoramaFishEye, ShapeType.CIRCLE),
    Triple("গোল কোণা (Rounded)", Icons.Default.CropSquare, ShapeType.ROUNDED_RECT),
    Triple("তারা (Star)", Icons.Default.StarOutline, ShapeType.STAR),
    Triple("সোজা রেখা (Line)", Icons.Default.Straighten, ShapeType.LINE)
  )

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
          Icon(Icons.Default.ChangeHistory, contentDescription = "Shapes", tint = Color(0xFF007ACC))
          Text("শেপ লেয়ার যোগ করুন", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        }
        Text("✕", fontSize = 16.sp, color = Color(0xFF94A3B8), modifier = Modifier.clickable(onClick = onDismiss).padding(4.dp))
      }

      Spacer(modifier = Modifier.height(12.dp))

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        shapes.forEach { (name, icon, type) ->
          val isSelected = selectedType == type
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
              .clickable { selectedType = type }
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(icon, contentDescription = name, tint = if (isSelected) Color(0xFF007ACC) else Color(0xFF475569))
            Text(
              text = name,
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) Color(0xFF007ACC) else Color(0xFF1E293B)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Button(
        onClick = {
          onSelectShape(selectedType)
          onDismiss()
        },
        modifier = Modifier.fillMaxWidth().height(40.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007ACC)),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("ক্যানভাসে শেপ যোগ করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
      }
    }
  }
}
