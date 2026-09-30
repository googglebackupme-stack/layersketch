package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Layer
import com.example.model.LayerType

@Composable
fun SketchbookLayerPanel(
  modifier: Modifier = Modifier,
  layers: List<Layer>,
  activeLayerId: String,
  recTargetLayerId: String,
  onSelectLayer: (String) -> Unit,
  onSetRecTarget: (String) -> Unit,
  onToggleVisibility: (String) -> Unit,
  onUpdateOpacity: (String, Float) -> Unit,
  onSelectBackgroundColor: (Color) -> Unit,
  onAddLayer: () -> Unit,
  onAddImageLayer: () -> Unit,
  onTransformImageLayer: () -> Unit,
  onDeleteLayer: (String) -> Unit,
  onColorWheelClick: () -> Unit
) {
  val activeLayer = layers.find { it.id == activeLayerId }

  val bgColors = listOf(
    Color.White,
    Color(0xFFFFFDF0), // Warm Cream
    Color(0xFFF5EBE1), // Parchment / Kraft
    Color(0xFFE2E8F0), // Muted Grey
    Color(0xFF1E293B), // Charcoal Dark
    Color(0xFF0F172A)  // Deep Midnight
  )

  Column(
    modifier = modifier
      .width(112.dp)
      .fillMaxHeight()
      .background(Color.White)
      .border(0.5.dp, Color(0xFFD1D5DB))
      .padding(vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // 1. Top Add Actions (+ Layer, + Photo)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(44.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onAddLayer, modifier = Modifier.size(34.dp)) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Add Layer",
          tint = Color(0xFF334155),
          modifier = Modifier.size(22.dp)
        )
      }

      IconButton(onClick = onAddImageLayer, modifier = Modifier.size(34.dp)) {
        Icon(
          imageVector = Icons.Default.AddPhotoAlternate,
          contentDescription = "Add Image Layer",
          tint = Color(0xFF16A34A),
          modifier = Modifier.size(20.dp)
        )
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(Color(0xFFE5E7EB))
    )

    // 2. Active Layer Opacity Slider (লেয়ার অপাসিটি কমানো বাড়ানোর অপশন)
    if (activeLayer != null) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFFF8FAFC))
          .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("অপাসিটি", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
          Text("${(activeLayer.opacity * 100).toInt()}%", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007ACC))
        }
        Slider(
          value = activeLayer.opacity,
          onValueChange = { onUpdateOpacity(activeLayer.id, it) },
          valueRange = 0.05f..1.0f,
          colors = SliderDefaults.colors(thumbColor = Color(0xFF007ACC), activeTrackColor = Color(0xFF007ACC)),
          modifier = Modifier.fillMaxWidth().height(20.dp)
        )
      }

      // If active layer is BACKGROUND, show Background Color Picker!
      if (activeLayer.type == LayerType.BACKGROUND) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F5F9))
            .padding(horizontal = 4.dp, vertical = 4.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text("ব্যাকগ্রাউন্ড কালার", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
          Spacer(modifier = Modifier.height(3.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            bgColors.take(4).forEach { color ->
              Box(
                modifier = Modifier
                  .size(16.dp)
                  .clip(CircleShape)
                  .background(color)
                  .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                  .clickable { onSelectBackgroundColor(color) }
              )
            }
          }
          Spacer(modifier = Modifier.height(3.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            bgColors.drop(4).forEach { color ->
              Box(
                modifier = Modifier
                  .size(16.dp)
                  .clip(CircleShape)
                  .background(color)
                  .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                  .clickable { onSelectBackgroundColor(color) }
              )
            }
          }
        }
      }

      // If active layer is IMAGE, show Transform Tool Button
      if (activeLayer.type == LayerType.IMAGE) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFEFF6FF))
            .clickable(onClick = onTransformImageLayer)
            .padding(vertical = 3.dp),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(Icons.Default.Transform, contentDescription = "Transform", tint = Color(0xFF007ACC), modifier = Modifier.size(12.dp))
            Text("ছবি সাইজ ও মুভ", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007ACC))
          }
        }
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(Color(0xFFE5E7EB))
    )

    // 3. Layer Cards Stack
    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 4.dp, vertical = 4.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      itemsIndexed(layers) { _, layer ->
        val isSelected = layer.id == activeLayerId
        val isRecTarget = layer.id == recTargetLayerId

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(94.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (layer.type == LayerType.BACKGROUND) layer.backgroundColor else Color.White)
            .border(
              width = if (isSelected) 2.5.dp else 1.dp,
              color = if (isSelected) Color(0xFF007ACC) else Color(0xFFE5E7EB)
            )
            .clickable { onSelectLayer(layer.id) }
        ) {
          // Layer Drawings Mini-Thumbnail Canvas OR Image Preview
          if (layer.type == LayerType.IMAGE && layer.imageBitmap != null) {
            Image(
              bitmap = layer.imageBitmap!!,
              contentDescription = "Layer Image",
              modifier = Modifier.fillMaxSize().padding(3.dp).clip(RoundedCornerShape(2.dp)),
              contentScale = ContentScale.Crop
            )
          } else {
            Canvas(
              modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
            ) {
              val scale = size.width / 1080f
              for (stroke in layer.strokes) {
                if (stroke.points.size > 1) {
                  val path = Path()
                  path.moveTo(stroke.points[0].offset.x * scale, stroke.points[0].offset.y * scale)
                  for (p in stroke.points) {
                    path.lineTo(p.offset.x * scale, p.offset.y * scale)
                  }
                  drawPath(
                    path = path,
                    color = if (stroke.isEraser) Color.White else stroke.color,
                    style = Stroke(width = stroke.width * scale * 1.5f, cap = StrokeCap.Round)
                  )
                }
              }
            }
          }

          // Top-Left Eye Icon (Visibility)
          IconButton(
            onClick = { onToggleVisibility(layer.id) },
            modifier = Modifier
              .align(Alignment.TopStart)
              .size(22.dp)
              .padding(1.dp)
          ) {
            Icon(
              imageVector = if (layer.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
              contentDescription = "Toggle Visibility",
              tint = if (layer.visible) Color(0xFF333333) else Color(0xFFAAAAAA),
              modifier = Modifier.size(15.dp)
            )
          }

          // Delete icon on active layer
          if (isSelected && layers.size > 1) {
            IconButton(
              onClick = { onDeleteLayer(layer.id) },
              modifier = Modifier
                .align(Alignment.TopEnd)
                .size(20.dp)
                .padding(1.dp)
            ) {
              Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(13.dp)
              )
            }
          }

          // Bottom Record Target Badge Button
          Box(
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .padding(bottom = 2.dp)
              .clip(RoundedCornerShape(3.dp))
              .background(if (isRecTarget) Color(0xFFDC2626) else Color(0x22DC2626))
              .clickable { onSetRecTarget(layer.id) }
              .padding(horizontal = 4.dp, vertical = 1.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(5.dp)
                  .clip(CircleShape)
                  .background(if (isRecTarget) Color.White else Color(0xFFDC2626))
              )
              Text(
                text = if (isRecTarget) "REC" else "Set REC",
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isRecTarget) Color.White else Color(0xFFDC2626)
              )
            }
          }
        }
      }
    }

    // 4. Bottom Color Wheel Button
    Box(
      modifier = Modifier
        .padding(bottom = 6.dp)
        .size(32.dp)
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
          .size(15.dp)
          .clip(CircleShape)
          .background(Color.White)
      )
    }
  }
}
