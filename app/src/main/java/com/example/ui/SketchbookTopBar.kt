package com.example.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

@Composable
fun SketchbookTopBar(
  modifier: Modifier = Modifier,
  canUndo: Boolean,
  canRedo: Boolean,
  currentColor: Color,
  isLayerPanelOpen: Boolean,
  layersCount: Int = 1,
  onMenuClick: () -> Unit,
  onUndoClick: () -> Unit,
  onRedoClick: () -> Unit,
  onUndoLongClick: () -> Unit = {},
  onRedoLongClick: () -> Unit = {},
  onToolsClick: () -> Unit,
  onBrushLibraryClick: () -> Unit,
  onColorWheelClick: () -> Unit,
  onSavePhotoClick: () -> Unit,
  onLayersClick: () -> Unit,
  onFitClick: () -> Unit
) {
  val scrollState = rememberScrollState()

  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(52.dp)
      .zIndex(30f)
      .shadow(elevation = 6.dp)
      .background(Color.White)
      .border(0.5.dp, Color(0xFFD1D5DB))
      .horizontalScroll(scrollState)
      .padding(horizontal = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // 1. Menu (৩ লাইনের মেনু - সতর্কতামূলক মোডাল সহ)
    IconButton(onClick = onMenuClick, modifier = Modifier.size(38.dp)) {
      Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color(0xFF1E293B))
    }

    // 2. PROMINENT LAYER BUTTON (মোবাইলে স্ক্রিনের শুরুতেই স্পষ্ট খুঁজে পাওয়ার জন্য হাইলাইটেড বাটন)
    Box(
      modifier = Modifier
        .height(36.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(if (isLayerPanelOpen) Color(0xFF007ACC) else Color(0xFFEFF6FF))
        .border(
          width = 1.5.dp,
          color = if (isLayerPanelOpen) Color(0xFF005999) else Color(0xFF0284C7),
          shape = RoundedCornerShape(8.dp)
        )
        .clickable(onClick = onLayersClick)
        .padding(horizontal = 8.dp),
      contentAlignment = Alignment.Center
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Icon(
          Icons.Default.Layers,
          contentDescription = "Layers",
          tint = if (isLayerPanelOpen) Color.White else Color(0xFF007ACC),
          modifier = Modifier.size(18.dp)
        )
        Text(
          text = "লেয়ার ($layersCount)",
          fontSize = 11.5.sp,
          fontWeight = FontWeight.ExtraBold,
          color = if (isLayerPanelOpen) Color.White else Color(0xFF007ACC)
        )
      }
    }

    // App Branding Badge (মহফুজ ড্রয়িং)
    Row(
      modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .background(Color(0xFF0F172A))
        .padding(horizontal = 8.dp, vertical = 5.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text("🎨", fontSize = 12.sp)
      Text(
        text = "mahfuj",
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFF38BDF8)
      )
    }

    // 3. Undo (আগের অবস্থায় ফিরুন - দ্রুত ট্যাপ: এক এক করে আগের সব স্ট্রোক মুছবে, লং-প্রেস: হিস্টোরি মেনু)
    @OptIn(ExperimentalFoundationApi::class)
    Box(
      modifier = Modifier
        .size(36.dp)
        .clip(CircleShape)
        .combinedClickable(
          enabled = canUndo,
          onClick = { onUndoClick() },
          onLongClick = { onUndoLongClick() }
        ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        Icons.AutoMirrored.Filled.Undo,
        contentDescription = "Undo",
        tint = if (canUndo) Color(0xFF1E293B) else Color(0xFFCBD5E1),
        modifier = Modifier.size(20.dp)
      )
    }

    // 4. Redo (পরের অবস্থায় যান)
    @OptIn(ExperimentalFoundationApi::class)
    Box(
      modifier = Modifier
        .size(36.dp)
        .clip(CircleShape)
        .combinedClickable(
          enabled = canRedo,
          onClick = { onRedoClick() },
          onLongClick = { onRedoLongClick() }
        ),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        Icons.AutoMirrored.Filled.Redo,
        contentDescription = "Redo",
        tint = if (canRedo) Color(0xFF1E293B) else Color(0xFFCBD5E1),
        modifier = Modifier.size(20.dp)
      )
    }

    // 4.1 Dedicated History / Multi-Undo Button (আগের একাধিক স্ট্রোক একসাথে মুছার মেনু)
    IconButton(
      onClick = onUndoLongClick,
      modifier = Modifier.size(36.dp),
      enabled = canUndo
    ) {
      Icon(
        Icons.Default.History,
        contentDescription = "Undo History",
        tint = if (canUndo) Color(0xFF0284C7) else Color(0xFFCBD5E1),
        modifier = Modifier.size(20.dp)
      )
    }

    // 4. Quick Save to Gallery (গ্যালারিতে সেভ)
    IconButton(onClick = onSavePhotoClick, modifier = Modifier.size(36.dp)) {
      Icon(Icons.Default.SaveAlt, contentDescription = "Save Photo", tint = Color(0xFFEA580C))
    }

    // 5. Tools Grid (৩x৩ টুলস গ্রিড: ক্যানভাস সেটআপ, ব্যাকগ্রাউন্ড ছবি, টেক্সট, শেপ ইত্যাদি)
    IconButton(onClick = onToolsClick, modifier = Modifier.size(36.dp)) {
      Icon(Icons.Default.GridView, contentDescription = "Tools", tint = Color(0xFF475569))
    }

    // 6. Brush & Pen Library (পেন্সিল ও কলম লাইব্রেরি)
    IconButton(onClick = onBrushLibraryClick, modifier = Modifier.size(36.dp)) {
      Icon(Icons.Default.Create, contentDescription = "Brush Library", tint = Color(0xFF0284C7))
    }

    // 7. Color Wheel (রেইনবো সার্কেল রিং)
    Box(
      modifier = Modifier
        .size(28.dp)
        .clip(CircleShape)
        .background(
          Brush.sweepGradient(
            listOf(
              Color.Red,
              Color.Yellow,
              Color.Green,
              Color.Cyan,
              Color.Blue,
              Color.Magenta,
              Color.Red
            )
          )
        )
        .clickable(onClick = onColorWheelClick),
      contentAlignment = Alignment.Center
    ) {
      Box(
        modifier = Modifier
          .size(13.dp)
          .clip(CircleShape)
          .background(currentColor)
          .border(1.5.dp, Color.White, CircleShape)
      )
    }

    // 8. Fit Canvas to Screen (ক্যানভাস ফিট)
    IconButton(onClick = onFitClick, modifier = Modifier.size(36.dp)) {
      Icon(Icons.Default.CropFree, contentDescription = "Fit Canvas", tint = Color(0xFF475569))
    }
  }
}
