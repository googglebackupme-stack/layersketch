package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PencilGrade
import com.example.model.PencilPalette

@Composable
fun StudioToolDock(
  modifier: Modifier = Modifier,
  activePencil: PencilGrade,
  onSelectPencil: (PencilGrade) -> Unit,
  brushColor: Color,
  onSelectColor: (Color) -> Unit,
  brushSize: Float,
  onUpdateSize: (Float) -> Unit,
  brushOpacity: Float,
  onUpdateOpacity: (Float) -> Unit,
  isEraser: Boolean,
  onToggleEraser: (Boolean) -> Unit,
  isPanMode: Boolean,
  onTogglePanMode: (Boolean) -> Unit,
  onOpenColorWheel: () -> Unit,
  isRecording: Boolean,
  targetLayerName: String,
  onToggleRecord: () -> Unit,
  isLayerPanelOpen: Boolean = false,
  layersCount: Int = 1,
  onToggleLayers: () -> Unit = {},
  onUndo: () -> Unit = {},
  onUndoLongClick: () -> Unit = {}
) {
  val pencilScrollState = rememberScrollState()

  // Scroll to active pencil on start
  LaunchedEffect(activePencil.code) {
    val index = PencilPalette.PENCILS.indexOfFirst { it.code == activePencil.code }
    if (index >= 0) {
      pencilScrollState.animateScrollTo(index * 60)
    }
  }

  val swatches = listOf(
    Color(0xFF222833), // 2B Graphite
    Color(0xFF0C0F15), // Deep Black
    Color(0xFF64748B), // Slate Grey
    Color(0xFFDC2626), // Crimson Red
    Color(0xFFF59E0B), // Warm Yellow
    Color(0xFF16A34A), // Forest Green
    Color(0xFF0284C7), // Sky Blue
    Color(0xFF7C3AED)  // Violet
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(Color.White)
      .border(0.5.dp, Color(0xFFCBD5E1))
      .padding(horizontal = 6.dp, vertical = 5.dp)
  ) {
    // ROW 1: Main Tool Selector Tabs (লেয়ার, পেন্সিল, টেক পেন, ব্রাশ পেন, চারকোল, ব্লেন্ডার, মার্কার, মুছনি, নাড়ানো, কালার, রেকর্ড)
    val toolTabScroll = rememberScrollState()
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(toolTabScroll),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // 0. Dedicated Layer Tool Button (লেয়ার অপশন - মোবাইলে সবসময় এক ক্লিকেই লেয়ার বের করার জন্য)
      ToolTabButton(
        title = "লেয়ার ($layersCount)",
        icon = "📑",
        isSelected = isLayerPanelOpen,
        activeColor = Color(0xFF007ACC),
        onClick = onToggleLayers
      )

      // 1. Pencil Tool (পেন্সিল)
      ToolTabButton(
        title = "পেন্সিল (${activePencil.code})",
        icon = "✏️",
        isSelected = !isEraser && !isPanMode && activePencil.toolType == com.example.model.DrawingToolType.PENCIL,
        onClick = {
          onTogglePanMode(false)
          onToggleEraser(false)
          if (activePencil.toolType != com.example.model.DrawingToolType.PENCIL) {
            onSelectPencil(PencilPalette.getGrade("2B"))
          }
        }
      )

      // 2. Technical Inking Pen (০.৩মিমি ফাইন পেন - চোখ, ঠোঁট ও আউটলাইনের জন্য)
      ToolTabButton(
        title = "টেক পেন (০.৩মিমি)",
        icon = "🖋️",
        isSelected = !isEraser && !isPanMode && activePencil.code == "TECH_PEN",
        activeColor = Color(0xFF0284C7),
        onClick = {
          onTogglePanMode(false)
          onToggleEraser(false)
          onSelectPencil(PencilPalette.TECH_PEN)
        }
      )

      // 3. Hair Brush Pen (ব্রাশ কলম - চুলের বাঁক ও ফ্লোয়িং স্ট্রোক)
      ToolTabButton(
        title = "ব্রাশ কলম",
        icon = "🖌️",
        isSelected = !isEraser && !isPanMode && activePencil.code == "BRUSH_PEN",
        activeColor = Color(0xFF16A34A),
        onClick = {
          onTogglePanMode(false)
          onToggleEraser(false)
          onSelectPencil(PencilPalette.BRUSH_PEN)
        }
      )

      // 4. Charcoal Pencil (চারকোল পেন্সিল - গাঢ় চুলের শেডিং ও কালো ডার্কনেস)
      ToolTabButton(
        title = "চারকোল",
        icon = "🖤",
        isSelected = !isEraser && !isPanMode && activePencil.code == "CHARCOAL",
        activeColor = Color(0xFF1E293B),
        onClick = {
          onTogglePanMode(false)
          onToggleEraser(false)
          onSelectPencil(PencilPalette.CHARCOAL)
        }
      )

      // 5. Blending Stump (ব্লেন্ডিং স্টাম্প - স্মুথ স্কিন ও ফেস শ্যাডো)
      ToolTabButton(
        title = "ব্লেন্ডার",
        icon = "🌫️",
        isSelected = !isEraser && !isPanMode && activePencil.code == "BLENDER",
        activeColor = Color(0xFF64748B),
        onClick = {
          onTogglePanMode(false)
          onToggleEraser(false)
          onSelectPencil(PencilPalette.BLENDER)
        }
      )

      // 5b. Hair & Lash Pen (হেয়ার ও ল্যাশ পেন - ০.১৫মিমি)
      ToolTabButton(
        title = "হেয়ার পেন",
        icon = "🪮",
        isSelected = !isEraser && !isPanMode && activePencil.code == "HAIR_PEN",
        activeColor = Color(0xFF0F172A),
        onClick = {
          onTogglePanMode(false)
          onToggleEraser(false)
          onSelectPencil(PencilPalette.HAIR_PEN)
        }
      )

      // 5c. White Gel Highlight Pen (হোয়াইট জেল পেন - চোখ ও ঠোঁটের হাইলাইট)
      ToolTabButton(
        title = "হোয়াইট জেল",
        icon = "🌟",
        isSelected = !isEraser && !isPanMode && activePencil.code == "HIGHLIGHT_PEN",
        activeColor = Color(0xFF0284C7),
        onClick = {
          onTogglePanMode(false)
          onToggleEraser(false)
          onSelectPencil(PencilPalette.HIGHLIGHT_PEN)
        }
      )

      // 5d. Watercolor Wash Brush (ওয়াটারকালার ব্রাশ)
      ToolTabButton(
        title = "ওয়াটারকালার",
        icon = "🎨",
        isSelected = !isEraser && !isPanMode && activePencil.code == "WATERCOLOR",
        activeColor = Color(0xFFE11D48),
        onClick = {
          onTogglePanMode(false)
          onToggleEraser(false)
          onSelectPencil(PencilPalette.WATERCOLOR)
        }
      )

      // 5e. Soft Airbrush (সফট এয়ারব্রাশ - স্কিন শেডিং)
      ToolTabButton(
        title = "এয়ারব্রাশ",
        icon = "💨",
        isSelected = !isEraser && !isPanMode && activePencil.code == "AIRBRUSH",
        activeColor = Color(0xFF8B5CF6),
        onClick = {
          onTogglePanMode(false)
          onToggleEraser(false)
          onSelectPencil(PencilPalette.AIRBRUSH)
        }
      )

      // 5f. G-Pen / Dip Pen (জি-পেন / ডিপ কলম)
      ToolTabButton(
        title = "জি-পেন",
        icon = "✒️",
        isSelected = !isEraser && !isPanMode && activePencil.code == "DIP_PEN",
        activeColor = Color(0xFF1E293B),
        onClick = {
          onTogglePanMode(false)
          onToggleEraser(false)
          onSelectPencil(PencilPalette.DIP_PEN)
        }
      )

      // 5g. Hatching Pencil (হ্যাচিং শেডিং পেন্সিল)
      ToolTabButton(
        title = "হ্যাচিং",
        icon = "📐",
        isSelected = !isEraser && !isPanMode && activePencil.code == "HATCHING",
        activeColor = Color(0xFF475569),
        onClick = {
          onTogglePanMode(false)
          onToggleEraser(false)
          onSelectPencil(PencilPalette.HATCHING)
        }
      )

      // 6. Dedicated Eraser Tool (মুছনি)
      ToolTabButton(
        title = "মুছনি",
        icon = "🧼",
        isSelected = isEraser && !isPanMode,
        activeColor = Color(0xFFF97316),
        onClick = {
          onTogglePanMode(false)
          onToggleEraser(true)
        }
      )

      // 6.1 Quick Undo Button (আনডু - পূর্ববর্তী স্ট্রোক মোছা)
      ToolTabButton(
        title = "আনডু",
        icon = "↩️",
        isSelected = false,
        activeColor = Color(0xFF0284C7),
        onClick = onUndo
      )

      // 7. Move / Pan Canvas Tool (ক্যানভাস নাড়ানো)
      ToolTabButton(
        title = "নাড়ানো",
        icon = "✋",
        isSelected = isPanMode,
        activeColor = Color(0xFF475569),
        onClick = { onTogglePanMode(!isPanMode) }
      )

      // 8. Chisel Marker (মার্কার)
      ToolTabButton(
        title = "মার্কার",
        icon = "🖍️",
        isSelected = !isEraser && !isPanMode && activePencil.code == "MARKER",
        onClick = {
          onTogglePanMode(false)
          onToggleEraser(false)
          onSelectPencil(PencilPalette.MARKER)
        }
      )

      // 9. Hand Color Picker
      Box(
        modifier = Modifier
          .height(34.dp)
          .clip(RoundedCornerShape(17.dp))
          .background(Color(0xFFF1F5F9))
          .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(17.dp))
          .clickable(onClick = onOpenColorWheel)
          .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          Box(
            modifier = Modifier
              .size(18.dp)
              .clip(CircleShape)
              .background(
                Brush.sweepGradient(
                  listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                )
              ),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(if (isEraser) Color.White else brushColor)
                .border(1.dp, Color.White, CircleShape)
            )
          }
          Text("কালার", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
        }
      }

      // 10. Record Layer Button
      Box(
        modifier = Modifier
          .height(34.dp)
          .clip(RoundedCornerShape(17.dp))
          .background(if (isRecording) Color(0xFFDC2626) else Color(0xFFFEE2E2))
          .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(17.dp))
          .clickable(onClick = onToggleRecord)
          .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
          Icon(
            Icons.Default.FiberManualRecord,
            contentDescription = "Record",
            tint = if (isRecording) Color.White else Color(0xFFDC2626),
            modifier = Modifier.size(13.dp)
          )
          Text(
            text = if (isRecording) "চলছে" else "রেকর্ড",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (isRecording) Color.White else Color(0xFFDC2626)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // ROW 2: Pencils rack / Eraser Presets / Pan hint
    if (isPanMode) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        Text(
          "✋ নাড়ানো মোড অন: আঙুল দিয়ে ক্যানভাস ডানে-বামে উপরে-নিচে সহজে সরান",
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = Color(0xFF475569)
        )
      }
    } else if (isEraser) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text("মুছনি ব্যাসার্ধ:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))

        listOf(
          "সূক্ষ্ম (6)" to 6f,
          "মাঝারি (14)" to 14f,
          "বড় (26)" to 26f,
          "ওয়াইড (44)" to 44f
        ).forEach { (label, sz) ->
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(if (brushSize == sz) Color(0xFFF97316) else Color(0xFFF1F5F9))
              .clickable { onUpdateSize(sz) }
              .padding(horizontal = 8.dp, vertical = 3.dp)
          ) {
            Text(
              label,
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold,
              color = if (brushSize == sz) Color.White else Color(0xFF334155)
            )
          }
        }
      }
    } else {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Horizontal scrollable pencil grade list
        Row(
          modifier = Modifier
            .weight(1f)
            .horizontalScroll(pencilScrollState),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          PencilPalette.PENCILS.forEach { pencil ->
            val isSelected = !isEraser && !isPanMode && activePencil.code == pencil.code

            Box(
              modifier = Modifier
                .height(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (isSelected) Color(0xFF007ACC) else Color(0xFFF1F5F9))
                .border(
                  width = if (isSelected) 1.5.dp else 1.dp,
                  color = if (isSelected) Color(0xFF007ACC) else Color(0xFFE2E8F0),
                  shape = RoundedCornerShape(13.dp)
                )
                .clickable {
                  onTogglePanMode(false)
                  onToggleEraser(false)
                  onSelectPencil(pencil)
                }
                .padding(horizontal = 6.dp),
              contentAlignment = Alignment.Center
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                  modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(pencil.graphiteColor)
                )
                Text(
                  text = pencil.code,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = if (isSelected) Color.White else Color(0xFF1E293B)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Direct Touch Swatches
        Row(
          horizontalArrangement = Arrangement.spacedBy(3.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          swatches.take(4).forEach { color ->
            val isCurrent = !isEraser && brushColor == color
            Box(
              modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                  width = if (isCurrent) 2.dp else 1.dp,
                  color = if (isCurrent) Color(0xFF007ACC) else Color(0xFFCBD5E1),
                  shape = CircleShape
                )
                .clickable {
                  onTogglePanMode(false)
                  onToggleEraser(false)
                  onSelectColor(color)
                }
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(3.dp))

    // ROW 3: Dedicated Size & Opacity Sliders right in the bottom menu!
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
        .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
        .padding(horizontal = 8.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // 1. Brush Size Slider
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.weight(1f)
      ) {
        Text("📏 সাইজ:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
        Slider(
          value = brushSize,
          onValueChange = onUpdateSize,
          valueRange = 1f..60f,
          colors = SliderDefaults.colors(thumbColor = Color(0xFF007ACC), activeTrackColor = Color(0xFF007ACC)),
          modifier = Modifier.weight(1f).height(20.dp)
        )
        Text("${brushSize.toInt()}px", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007ACC))
      }

      Spacer(modifier = Modifier.width(12.dp))

      // 2. Brush Opacity Slider
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.weight(1f)
      ) {
        Text("💧 অপাসিটি:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
        Slider(
          value = brushOpacity,
          onValueChange = onUpdateOpacity,
          valueRange = 0.05f..1.0f,
          colors = SliderDefaults.colors(thumbColor = Color(0xFF007ACC), activeTrackColor = Color(0xFF007ACC)),
          modifier = Modifier.weight(1f).height(20.dp)
        )
        Text("${(brushOpacity * 100).toInt()}%", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007ACC))
      }
    }
  }
}

@Composable
private fun ToolTabButton(
  title: String,
  icon: String,
  isSelected: Boolean,
  activeColor: Color = Color(0xFF007ACC),
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .height(34.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(if (isSelected) activeColor else Color(0xFFF1F5F9))
      .border(
        width = 1.dp,
        color = if (isSelected) activeColor else Color(0xFFE2E8F0),
        shape = RoundedCornerShape(8.dp)
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 6.dp),
    contentAlignment = Alignment.Center
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(icon, fontSize = 12.sp)
      Text(
        text = title,
        fontSize = 10.5.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = if (isSelected) Color.White else Color(0xFF334155)
      )
    }
  }
}
