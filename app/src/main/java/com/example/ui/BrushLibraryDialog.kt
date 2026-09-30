package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.PencilGrade
import com.example.model.PencilPalette

@Composable
fun BrushLibraryDialog(
  activePencil: PencilGrade,
  brushSize: Float,
  brushOpacity: Float,
  brushColor: Color,
  isEraser: Boolean,
  onSelectPencil: (PencilGrade) -> Unit,
  onUpdateSize: (Float) -> Unit,
  onUpdateOpacity: (Float) -> Unit,
  onColorWheelClick: () -> Unit,
  onDismiss: () -> Unit
) {
  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Library, 1 = Settings

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Column(
      modifier = Modifier
        .width(360.dp)
        .fillMaxHeight(0.85f)
        .clip(RoundedCornerShape(12.dp))
        .background(Color.White)
        .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(12.dp))
    ) {
      // 1. Top Blue/Grey Tabs matching Image 4
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .background(Color(0xFF888888))
      ) {
        // Tab 1: Library
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(if (selectedTab == 0) Color(0xFF007ACC) else Color(0xFF888888))
            .clickable { selectedTab = 0 },
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.GridView, contentDescription = "Library", tint = Color.White)
        }

        // Tab 2: Settings
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(if (selectedTab == 1) Color(0xFF007ACC) else Color(0xFF888888))
            .clickable { selectedTab = 1 },
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Tune, contentDescription = "Settings", tint = Color.White)
        }
      }

      // 2. Stroke Curve Preview
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(60.dp)
          .background(Color(0xFFFAFAFA))
          .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
      ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
          val path = Path()
          val w = size.width
          val h = size.height
          path.moveTo(20f, h * 0.7f)
          path.cubicTo(w * 0.35f, h * 0.1f, w * 0.65f, h * 0.9f, w - 20f, h * 0.3f)

          drawPath(
            path = path,
            color = if (isEraser) Color(0xFF94A3B8) else brushColor,
            alpha = brushOpacity,
            style = Stroke(
              width = brushSize.coerceIn(1f, 30f),
              cap = StrokeCap.Round
            )
          )
        }
      }

      // 3. Current Brush Title
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = if (isEraser) "Eraser (মুছনি / ইরেজার)" else "${activePencil.name}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
          )
          Text(
            text = if (isEraser) "Traditional Tools" else "Fine Art Pencils (${activePencil.hardness})",
            fontSize = 12.sp,
            color = Color(0xFF64748B)
          )
        }
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(Color(0xFFE2E8F0))
      )

      // 4. Tab Content: Library or Settings
      if (selectedTab == 0) {
        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          // --- TRADITIONAL SECTION (Eraser, Pen, Marker) ---
          item {
            Text(
              text = "Traditional (মুছনি ও কলম)",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF475569),
              modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              // Eraser (মুছনি) Tool Item
              BrushItemCard(
                name = "Eraser (মুছনি)",
                badge = "ERASE",
                iconText = "🧽",
                isSelected = isEraser,
                badgeColor = Color(0xFFEF4444),
                onClick = { onSelectPencil(PencilPalette.ERASER) }
              )

              // Inking Pen
              BrushItemCard(
                name = "Inking Pen",
                badge = "PEN",
                iconText = "✒️",
                isSelected = !isEraser && activePencil.code == "PEN",
                badgeColor = Color(0xFF0F172A),
                onClick = { onSelectPencil(PencilPalette.PEN) }
              )

              // Chisel Marker
              BrushItemCard(
                name = "Chisel Marker",
                badge = "MARK",
                iconText = "🖊️",
                isSelected = !isEraser && activePencil.code == "MARKER",
                badgeColor = Color(0xFF475569),
                onClick = { onSelectPencil(PencilPalette.MARKER) }
              )
            }
          }

          // --- FINE ART SECTION: ALL 15 PENCILS (4H to 9B) matching Image 4 ---
          item {
            Text(
              text = "Fine Art (১৫টি পেন্সিল)",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF475569),
              modifier = Modifier.padding(bottom = 8.dp)
            )

            // Pencils in 3 Rows of 5
            val rows = PencilPalette.PENCILS.chunked(5)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              rows.forEach { rowPencils ->
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  rowPencils.forEach { pencil ->
                    val isSelected = !isEraser && activePencil.code == pencil.code
                    PencilIconView(
                      pencil = pencil,
                      isSelected = isSelected,
                      onClick = { onSelectPencil(pencil) }
                    )
                  }
                }
              }
            }
          }
        }
      } else {
        // --- SETTINGS TAB (Size & Opacity Sliders) ---
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Size (সাইজ)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
              Text("${brushSize.toInt()} px", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007ACC))
            }
            Slider(
              value = brushSize,
              onValueChange = onUpdateSize,
              valueRange = 1f..80f,
              colors = SliderDefaults.colors(
                thumbColor = Color(0xFF007ACC),
                activeTrackColor = Color(0xFF007ACC)
              )
            )
          }

          Column {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Opacity (অপাসিটি)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
              Text("${(brushOpacity * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007ACC))
            }
            Slider(
              value = brushOpacity,
              onValueChange = onUpdateOpacity,
              valueRange = 0.05f..1.0f,
              colors = SliderDefaults.colors(
                thumbColor = Color(0xFF007ACC),
                activeTrackColor = Color(0xFF007ACC)
              )
            )
          }
        }
      }

      // 5. Bottom Controls Bar matching Image 4
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(Color(0xFFE2E8F0))
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(64.dp)
          .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Color Ring Button
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(
              Brush.sweepGradient(
                listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
              )
            )
            .clickable(onClick = onColorWheelClick),
          contentAlignment = Alignment.Center
        ) {
          Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.White))
        }

        // Close X Button
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0xFF1E293B))
            .clickable(onClick = onDismiss),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }

        // Active Tool Pin / Done
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color(0xFF007ACC))
            .clickable(onClick = onDismiss),
          contentAlignment = Alignment.Center
        ) {
          Text("✓", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun BrushItemCard(
  name: String,
  badge: String,
  iconText: String,
  isSelected: Boolean,
  badgeColor: Color,
  onClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .width(90.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(if (isSelected) Color(0xFFE0F2FE) else Color(0xFFF8FAFC))
      .border(
        width = if (isSelected) 2.dp else 1.dp,
        color = if (isSelected) Color(0xFF007ACC) else Color(0xFFCBD5E1),
        shape = RoundedCornerShape(8.dp)
      )
      .clickable(onClick = onClick)
      .padding(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(iconText, fontSize = 26.sp)
    Text(
      text = badge,
      fontSize = 11.sp,
      fontWeight = FontWeight.ExtraBold,
      color = badgeColor,
      modifier = Modifier.padding(top = 4.dp)
    )
    Text(
      text = name,
      fontSize = 9.sp,
      color = Color(0xFF64748B),
      maxLines = 1
    )
  }
}

@Composable
fun PencilIconView(
  pencil: PencilGrade,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .width(56.dp)
      .clip(RoundedCornerShape(6.dp))
      .background(if (isSelected) Color(0xFFE0F2FE) else Color.Transparent)
      .border(
        width = if (isSelected) 2.dp else 0.5.dp,
        color = if (isSelected) Color(0xFF007ACC) else Color(0xFFE2E8F0),
        shape = RoundedCornerShape(6.dp)
      )
      .clickable(onClick = onClick)
      .padding(vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Pencil Tip Illustration
    Canvas(modifier = Modifier.size(24.dp, 30.dp)) {
      val w = size.width
      val h = size.height

      // Wooden body cone
      val woodPath = Path()
      woodPath.moveTo(w * 0.15f, h)
      woodPath.lineTo(w * 0.5f, h * 0.3f)
      woodPath.lineTo(w * 0.85f, h)
      woodPath.close()
      drawPath(woodPath, Color(0xFFFDE68A))

      // Graphite Lead tip
      val leadPath = Path()
      leadPath.moveTo(w * 0.38f, h * 0.5f)
      leadPath.lineTo(w * 0.5f, h * 0.1f)
      leadPath.lineTo(w * 0.62f, h * 0.5f)
      leadPath.close()
      drawPath(leadPath, pencil.graphiteColor)
    }

    // Blue Grade Badge matching Image 4
    Box(
      modifier = Modifier
        .padding(top = 2.dp)
        .clip(RoundedCornerShape(3.dp))
        .background(Color(0xFF0284C7))
        .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
      Text(
        text = pencil.code,
        fontSize = 9.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color.White
      )
    }
  }
}
