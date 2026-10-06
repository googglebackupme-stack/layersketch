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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChangeHistory
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun ToolsMenuDialog(
  onDismiss: () -> Unit,
  onRecordClick: () -> Unit,
  onClearClick: () -> Unit,
  onAddTextClick: () -> Unit,
  onAddShapeClick: () -> Unit,
  onAddPhotoClick: () -> Unit,
  onTransformPhotoClick: () -> Unit,
  onOpenCanvasSetup: () -> Unit = {},
  onOpenLayers: () -> Unit = {}
) {
  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .width(320.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(Color.White)
        .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(12.dp))
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("টুলস ও ফিচার মেনু", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
        Text("✕", fontSize = 16.sp, color = Color(0xFF94A3B8), modifier = Modifier.clickable(onClick = onDismiss).padding(4.dp))
      }

      // Row 1: Selection, Transform, Canvas Setup
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ToolGridItem("সিলেকশন", Icons.Default.Crop) { onDismiss() }
        ToolGridItem("ছবি ট্রান্সফর্ম", Icons.Default.OpenWith) {
          onTransformPhotoClick()
          onDismiss()
        }
        ToolGridItem("ক্যানভাস রঙ ও ব্যাকগ্রাউন্ড", Icons.Default.FormatColorFill, isHighlight = true) {
          onOpenCanvasSetup()
          onDismiss()
        }
      }

      // Row 2: Guides, Symmetry, Draw Styles (Shapes)
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ToolGridItem("গাইড রুলার", Icons.Default.Straighten) { onDismiss() }
        ToolGridItem("সিমেট্রি", Icons.Default.AutoAwesome) { onDismiss() }
        ToolGridItem("শেপ আর্ট", Icons.Default.ChangeHistory) {
          onAddShapeClick()
          onDismiss()
        }
      }

      // Row 3: Steady Stroke, Import Image, Text
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ToolGridItem("স্মুথ স্ট্রোক", Icons.Default.Straighten) { onDismiss() }
        ToolGridItem("ছবি যুক্ত করুন", Icons.Default.AddPhotoAlternate) {
          onAddPhotoClick()
          onDismiss()
        }
        ToolGridItem("টেক্সট যোগ", Icons.Default.TextFields) {
          onAddTextClick()
          onDismiss()
        }
      }

      // Row 4: Clear Canvas, Time-lapse / Record, Layers
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ToolGridItem("ক্লিয়ার লেয়ার", Icons.Default.Crop) {
          onClearClick()
          onDismiss()
        }
        ToolGridItem("টাইম-ল্যাপস\nরেকর্ড", Icons.Default.Videocam, isHighlight = true) {
          onRecordClick()
          onDismiss()
        }
        ToolGridItem("লেয়ারসমূহ\n(Layers)", Icons.Default.Layers, isHighlight = true) {
          onOpenLayers()
          onDismiss()
        }
      }
    }
  }
}

@Composable
fun ToolGridItem(
  title: String,
  icon: ImageVector,
  isHighlight: Boolean = false,
  onClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .size(86.dp)
      .clip(RoundedCornerShape(6.dp))
      .background(if (isHighlight) Color(0xFFEFF6FF) else Color(0xFFFAFAFA))
      .border(
        width = if (isHighlight) 1.5.dp else 0.5.dp,
        color = if (isHighlight) Color(0xFF007ACC) else Color(0xFFE2E8F0),
        shape = RoundedCornerShape(6.dp)
      )
      .clickable(onClick = onClick)
      .padding(6.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = title,
      tint = if (isHighlight) Color(0xFF007ACC) else Color(0xFF333333),
      modifier = Modifier.size(24.dp)
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = title,
      fontSize = 9.sp,
      fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
      color = if (isHighlight) Color(0xFF007ACC) else Color(0xFF475569),
      textAlign = androidx.compose.ui.text.style.TextAlign.Center,
      lineHeight = 11.sp
    )
  }
}
