package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Layer
import com.example.model.LayerType

@Composable
fun LayerDrawer(
  modifier: Modifier = Modifier,
  layers: List<Layer>,
  activeLayerId: String,
  recTargetLayerId: String,
  soloMode: Boolean,
  onSelectLayer: (String) -> Unit,
  onSetRecTarget: (String) -> Unit,
  onToggleVisibility: (String) -> Unit,
  onToggleLock: (String) -> Unit,
  onUpdateOpacity: (String, Float) -> Unit,
  onMoveUp: (Int) -> Unit,
  onMoveDown: (Int) -> Unit,
  onAddLayer: (LayerType) -> Unit,
  onDeleteLayer: (String) -> Unit,
  onToggleSoloMode: () -> Unit,
  onClose: () -> Unit
) {
  Column(
    modifier = modifier
      .fillMaxHeight()
      .width(320.dp)
      .background(Color(0xFF161B26))
      .border(1.dp, Color(0xFF2A3449))
      .padding(12.dp)
  ) {
    // Drawer Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "LAYERS (${layers.size})",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFF0F6FC)
      )

      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        // Solo Mode Toggle
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (soloMode) Color(0xFF0284C7) else Color(0xFF202738))
            .clickable(onClick = onToggleSoloMode)
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = if (soloMode) "Solo ON" else "Solo",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (soloMode) Color.White else Color(0xFF94A3B8)
          )
        }

        IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
          Text("✕", color = Color(0xFF94A3B8), fontSize = 16.sp)
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Add Layer Quick Actions
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Button(
        onClick = { onAddLayer(LayerType.DRAWING) },
        modifier = Modifier.weight(1f).height(32.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202738)),
        shape = RoundedCornerShape(6.dp)
      ) {
        Text("+ Sketch", fontSize = 11.sp, color = Color(0xFF38BDF8))
      }
      Button(
        onClick = { onAddLayer(LayerType.TEXT) },
        modifier = Modifier.weight(1f).height(32.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202738)),
        shape = RoundedCornerShape(6.dp)
      ) {
        Text("+ Text", fontSize = 11.sp, color = Color(0xFF38BDF8))
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Layers List
    LazyColumn(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      itemsIndexed(layers) { index, layer ->
        val isSelected = layer.id == activeLayerId
        val isRecTarget = layer.id == recTargetLayerId

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF1E293B) else Color(0xFF1A202C))
            .border(
              width = if (isRecTarget) 1.5.dp else if (isSelected) 1.dp else 0.5.dp,
              color = if (isRecTarget) Color(0xFFEF4444) else if (isSelected) Color(0xFF38BDF8) else Color(0xFF333D52),
              shape = RoundedCornerShape(8.dp)
            )
            .clickable { onSelectLayer(layer.id) }
            .padding(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Visibility
            IconButton(
              onClick = { onToggleVisibility(layer.id) },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(
                imageVector = if (layer.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = "Toggle Visibility",
                tint = if (layer.visible) Color(0xFFF0F6FC) else Color(0xFF64748B),
                modifier = Modifier.size(16.dp)
              )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Name
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = layer.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF0F6FC),
                maxLines = 1
              )
              Text(
                text = "${layer.type.name} • ${layer.strokes.size} strokes",
                fontSize = 10.sp,
                color = Color(0xFF64748B)
              )
            }

            // REC TARGET Badge / Button
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(if (isRecTarget) Color(0xFFEF4444) else Color(0x22EF4444))
                .clickable { onSetRecTarget(layer.id) }
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = if (isRecTarget) "● REC TARGET" else "Set REC",
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isRecTarget) Color.White else Color(0xFFFCA5A5)
              )
            }
          }

          // Opacity Slider for active layer
          if (isSelected) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Op: ${(layer.opacity * 100).toInt()}%", fontSize = 10.sp, color = Color(0xFF94A3B8))
              Slider(
                value = layer.opacity,
                onValueChange = { onUpdateOpacity(layer.id, it) },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f).height(24.dp).padding(horizontal = 6.dp),
                colors = SliderDefaults.colors(
                  thumbColor = Color(0xFF38BDF8),
                  activeTrackColor = Color(0xFF38BDF8),
                  inactiveTrackColor = Color(0xFF333D52)
                )
              )
              // Up / Down / Delete
              IconButton(onClick = { onMoveUp(index) }, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.ArrowUpward, "Move Up", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
              }
              IconButton(onClick = { onMoveDown(index) }, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.ArrowDownward, "Move Down", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
              }
              IconButton(onClick = { onDeleteLayer(layer.id) }, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
              }
            }
          }
        }
      }
    }
  }
}
