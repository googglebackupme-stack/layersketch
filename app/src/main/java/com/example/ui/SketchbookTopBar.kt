package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SketchbookTopBar(
  modifier: Modifier = Modifier,
  canUndo: Boolean,
  canRedo: Boolean,
  currentColor: Color,
  isLayerPanelOpen: Boolean,
  onMenuClick: () -> Unit,
  onUndoClick: () -> Unit,
  onRedoClick: () -> Unit,
  onToolsClick: () -> Unit,
  onBrushLibraryClick: () -> Unit,
  onColorWheelClick: () -> Unit,
  onAddImageClick: () -> Unit,
  onSavePhotoClick: () -> Unit,
  onCanvasSizeClick: () -> Unit,
  onLayersClick: () -> Unit,
  onFitClick: () -> Unit
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(52.dp)
      .background(Color.White)
      .border(0.5.dp, Color(0xFFD1D5DB))
      .padding(horizontal = 3.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // 1. Menu
    IconButton(onClick = onMenuClick, modifier = Modifier.size(36.dp)) {
      Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color(0xFF555555))
    }

    // 2. Undo
    IconButton(
      onClick = onUndoClick,
      enabled = canUndo,
      modifier = Modifier.size(36.dp)
    ) {
      Icon(
        Icons.Default.Undo,
        contentDescription = "Undo",
        tint = if (canUndo) Color(0xFF333333) else Color(0xFFCCCCCC)
      )
    }

    // 3. Redo
    IconButton(
      onClick = onRedoClick,
      enabled = canRedo,
      modifier = Modifier.size(36.dp)
    ) {
      Icon(
        Icons.Default.Redo,
        contentDescription = "Redo",
        tint = if (canRedo) Color(0xFF333333) else Color(0xFFCCCCCC)
      )
    }

    // 4. Save as Photo (ফটোগ্রাফ / ছবি গ্যালারিতে সেভ করুন)
    IconButton(onClick = onSavePhotoClick, modifier = Modifier.size(36.dp)) {
      Icon(Icons.Default.CameraAlt, contentDescription = "Save Photo", tint = Color(0xFFEA580C))
    }

    // 5. Canvas Size (A4, A5, Custom)
    IconButton(onClick = onCanvasSizeClick, modifier = Modifier.size(36.dp)) {
      Icon(Icons.Default.AspectRatio, contentDescription = "Canvas Size", tint = Color(0xFF007ACC))
    }

    // 6. Add Photo / Image to Layer (ছবি যোগ করুন)
    IconButton(onClick = onAddImageClick, modifier = Modifier.size(36.dp)) {
      Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Image", tint = Color(0xFF16A34A))
    }

    // 7. Tools Grid (3x3 grid)
    IconButton(onClick = onToolsClick, modifier = Modifier.size(36.dp)) {
      Icon(Icons.Default.GridView, contentDescription = "Tools", tint = Color(0xFF555555))
    }

    // 8. Brush Library (Pen & Pencil)
    IconButton(onClick = onBrushLibraryClick, modifier = Modifier.size(36.dp)) {
      Icon(Icons.Default.Create, contentDescription = "Brush Library", tint = Color(0xFF555555))
    }

    // 9. Color Wheel (Iconic Rainbow Circle Ring)
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
          .border(1.dp, Color.White, CircleShape)
      )
    }

    // 10. Layers (Stacked Cards)
    IconButton(onClick = onLayersClick, modifier = Modifier.size(36.dp)) {
      Icon(
        Icons.Default.Layers,
        contentDescription = "Layers",
        tint = if (isLayerPanelOpen) Color(0xFF007ACC) else Color(0xFF555555)
      )
    }

    // 11. Fit Canvas
    IconButton(onClick = onFitClick, modifier = Modifier.size(36.dp)) {
      Icon(Icons.Default.CropFree, contentDescription = "Fit Canvas", tint = Color(0xFF555555))
    }
  }
}
