package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PencilGrade

@Composable
fun SketchbookPuck(
  modifier: Modifier = Modifier,
  activePencil: PencilGrade,
  brushSize: Float,
  brushColor: Color,
  onClick: () -> Unit
) {
  Box(
    modifier = modifier
      .size(56.dp)
      .shadow(12.dp, CircleShape)
      .clip(CircleShape)
      .background(Color(0xE61A202C))
      .border(2.dp, Color(0xFF38BDF8), CircleShape)
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Dynamic Size Indicator Dot
      val dotSize = (brushSize * 1.5f).coerceIn(4f, 20f).dp
      Box(
        modifier = Modifier
          .size(dotSize)
          .clip(CircleShape)
          .background(brushColor)
      )

      // Grade Label (e.g. 2B)
      Text(
        text = activePencil.code,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFFF0F6FC),
        modifier = Modifier.padding(top = 2.dp)
      )
    }
  }
}
