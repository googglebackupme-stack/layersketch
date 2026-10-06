package com.example.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.CanvasPresets
import com.example.model.CanvasSize
import com.example.model.PaperColorPresets

@Composable
fun CanvasSizeDialog(
  currentCanvasSize: CanvasSize,
  currentCanvasColor: Color,
  currentBackgroundImage: ImageBitmap?,
  onApplyCanvasConfig: (size: CanvasSize, color: Color, clearBgImage: Boolean) -> Unit,
  onRequestPickBackgroundImage: () -> Unit,
  onDismiss: () -> Unit
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  var selectedSize by remember { mutableStateOf(currentCanvasSize) }
  var selectedColor by remember { mutableStateOf(currentCanvasColor) }
  var isCustom by remember { mutableStateOf(false) }
  var customWidth by remember { mutableStateOf(currentCanvasSize.width.toInt().toString()) }
  var customHeight by remember { mutableStateOf(currentCanvasSize.height.toInt().toString()) }
  var removeBgImage by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .width(360.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(Color.White)
        .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(16.dp))
        .padding(16.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.AspectRatio, contentDescription = "Canvas Setup", tint = Color(0xFF007ACC))
          Text(
            text = "ক্যানভাস সেটআপ ও ব্যাকগ্রাউন্ড",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
          )
        }
        Text(
          text = "✕",
          fontSize = 16.sp,
          color = Color(0xFF94A3B8),
          modifier = Modifier.clickable(onClick = onDismiss).padding(4.dp)
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Tab Navigation: 0 = সাইজ (Size), 1 = রঙ ও ব্যাকগ্রাউন্ড (Color & Image)
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color(0xFFF1F5F9),
        contentColor = Color(0xFF007ACC),
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).height(38.dp)
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = { Text("১. ক্যানভাস সাইজ", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = { Text("২. রঙ ও ছবি", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      if (selectedTab == 0) {
        // Tab 1: Canvas Size Presets & Custom
        LazyColumn(
          modifier = Modifier.fillMaxWidth().height(220.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          items(CanvasPresets.ALL) { preset ->
            val isSelected = !isCustom &&
              preset.width == selectedSize.width &&
              preset.height == selectedSize.height

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
                .clickable {
                  isCustom = false
                  selectedSize = preset
                }
                .padding(horizontal = 10.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = preset.name,
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                  color = if (isSelected) Color(0xFF007ACC) else Color(0xFF1E293B)
                )
                Text(
                  text = preset.description,
                  fontSize = 10.sp,
                  color = Color(0xFF64748B)
                )
              }

              if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFF007ACC), modifier = Modifier.size(16.dp))
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Custom Size Input
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedTextField(
            value = customWidth,
            onValueChange = {
              customWidth = it.filter { ch -> ch.isDigit() }
              isCustom = true
            },
            label = { Text("প্রস্থ (W px)", fontSize = 10.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).height(50.dp),
            singleLine = true
          )
          OutlinedTextField(
            value = customHeight,
            onValueChange = {
              customHeight = it.filter { ch -> ch.isDigit() }
              isCustom = true
            },
            label = { Text("উচ্চতা (H px)", fontSize = 10.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).height(50.dp),
            singleLine = true
          )
        }
      } else {
        // Tab 2: Canvas Background Color & Background Image
        Column(
          modifier = Modifier.fillMaxWidth().height(280.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Color Header
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.ColorLens, contentDescription = null, tint = Color(0xFF007ACC), modifier = Modifier.size(16.dp))
            Text("ক্যানভাস পেপারের রঙ নির্বাচন করুন:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
          }

          // Paper Color Chips
          LazyColumn(
            modifier = Modifier.fillMaxWidth().height(130.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            items(PaperColorPresets.ALL) { paperPreset ->
              val isChosen = selectedColor == paperPreset.color
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isChosen) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                  .border(
                    width = if (isChosen) 1.5.dp else 1.dp,
                    color = if (isChosen) Color(0xFF007ACC) else Color(0xFFE2E8F0),
                    shape = RoundedCornerShape(8.dp)
                  )
                  .clickable { selectedColor = paperPreset.color }
                  .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  Box(
                    modifier = Modifier
                      .size(24.dp)
                      .clip(CircleShape)
                      .background(paperPreset.color)
                      .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                  )
                  Column {
                    Text(paperPreset.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text(paperPreset.description, fontSize = 9.sp, color = Color(0xFF64748B))
                  }
                }
                if (isChosen) {
                  Icon(Icons.Default.Check, contentDescription = "Active Color", tint = Color(0xFF007ACC), modifier = Modifier.size(16.dp))
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(2.dp))

          // Background Image Section
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
            Text("ক্যানভাস ব্যাকগ্রাউন্ড ছবি:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
          }

          if (currentBackgroundImage != null && !removeBgImage) {
            // Preview & Remove
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF0FDF4))
                .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(8.dp))
                .padding(8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(
                  bitmap = currentBackgroundImage,
                  contentDescription = "Background Preview",
                  modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)),
                  contentScale = ContentScale.Crop
                )
                Text("ব্যাকগ্রাউন্ড ছবি সক্রিয় আছে", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF166534))
              }
              OutlinedButton(
                onClick = { removeBgImage = true },
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                modifier = Modifier.height(32.dp)
              ) {
                Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("সরান", fontSize = 10.sp)
              }
            }
          } else {
            // Upload button
            OutlinedButton(
              onClick = {
                removeBgImage = false
                onRequestPickBackgroundImage()
              },
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.fillMaxWidth().height(40.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF16A34A))
            ) {
              Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("গ্যালারি থেকে ব্যাকগ্রাউন্ড ছবি যুক্ত করুন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Bottom Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = onDismiss,
          modifier = Modifier.weight(1f).height(40.dp),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text("বাতিল", fontSize = 12.sp, color = Color(0xFF64748B))
        }

        Button(
          onClick = {
            val finalSize = if (isCustom) {
              val w = customWidth.toFloatOrNull()?.coerceIn(200f, 6000f) ?: 1080f
              val h = customHeight.toFloatOrNull()?.coerceIn(200f, 6000f) ?: 1080f
              CanvasSize("Custom (${w.toInt()}×${h.toInt()})", w, h, "${w.toInt()} × ${h.toInt()} px")
            } else {
              selectedSize
            }
            onApplyCanvasConfig(finalSize, selectedColor, removeBgImage)
            onDismiss()
          },
          modifier = Modifier.weight(1.5f).height(40.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007ACC)),
          shape = RoundedCornerShape(8.dp)
        ) {
          Icon(Icons.Default.Check, contentDescription = "Apply", tint = Color.White, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("ক্যানভাস তৈরি ও প্রয়োগ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
      }
    }
  }
}
