package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PencilGrade
import com.example.model.PencilPalette

@Composable
fun PencilRack(
  modifier: Modifier = Modifier,
  activePencil: PencilGrade,
  onPencilSelected: (PencilGrade) -> Unit,
  brushColor: Color,
  onColorSelected: (Color) -> Unit,
  isEraser: Boolean,
  onToggleEraser: (Boolean) -> Unit
) {
  val scrollState = rememberScrollState()

  val colors = listOf(
    Color(0xFF222833), // 2B Graphite Black
    Color(0xFF0C0F15), // 6B Deep Black
    Color(0xFF5E6774), // 2H Light Grey
    Color(0xFF38BDF8), // Cyan
    Color(0xFF3B82F6), // Blue
    Color(0xFFEF4444), // Crimson Red
    Color(0xFFF59E0B), // Amber / Ochre
    Color(0xFF22C55E), // Forest Green
    Color(0xFFA855F7), // Purple
    Color(0xFFEC4899)  // Rose Pink
  )

  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(48.dp)
      .background(Color(0xFF161B26))
      .border(1.dp, Color(0xFF2A3449))
      .padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Label
    Text(
      text = "✏️ PENCILS",
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = Color(0xFF94A3B8),
      modifier = Modifier.padding(end = 8.dp)
    )

    // Horizontal Scrollable 15 Pencil Chips
    Row(
      modifier = Modifier
        .weight(1f)
        .horizontalScroll(scrollState),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      PencilPalette.PENCILS.forEach { pencil ->
        val isSelected = !isEraser && activePencil.code == pencil.code
        PencilChip(
          pencil = pencil,
          isSelected = isSelected,
          onClick = {
            onToggleEraser(false)
            onPencilSelected(pencil)
          }
        )
      }
    }

    Box(
      modifier = Modifier
        .padding(horizontal = 6.dp)
        .width(1.dp)
        .height(24.dp)
        .background(Color(0xFF333D52))
    )

    // Quick Color Dot Selector
    Row(
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      colors.take(4).forEach { color ->
        val isColorActive = !isEraser && brushColor == color
        Box(
          modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color)
            .border(
              width = if (isColorActive) 2.dp else 1.dp,
              color = if (isColorActive) Color(0xFF38BDF8) else Color(0x44FFFFFF),
              shape = CircleShape
            )
            .clickable {
              onToggleEraser(false)
              onColorSelected(color)
            }
        )
      }
    }
  }
}

@Composable
fun PencilChip(
  pencil: PencilGrade,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val borderColor = when {
    pencil.code.endsWith("H") -> Color(0xFF38BDF8)
    pencil.code == "HB" || pencil.code == "F" -> Color(0xFF22C55E)
    else -> Color(0xFFF59E0B) // B to 9B Soft Pencils
  }

  Row(
    modifier = Modifier
      .height(32.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(if (isSelected) Color(0xFF0284C7) else Color(0xFF202738))
      .border(
        width = if (isSelected) 1.5.dp else 1.dp,
        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF333D52),
        shape = RoundedCornerShape(16.dp)
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    // Pencil Lead Color Dot
    Box(
      modifier = Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(pencil.graphiteColor)
        .border(1.dp, Color(0x44FFFFFF), CircleShape)
    )

    // Grade Code (e.g. 4H, 2B, 9B)
    Text(
      text = pencil.code,
      fontSize = 12.sp,
      fontWeight = FontWeight.ExtraBold,
      color = if (isSelected) Color.White else Color(0xFFF0F6FC),
      letterSpacing = 0.5.sp
    )
  }
}
