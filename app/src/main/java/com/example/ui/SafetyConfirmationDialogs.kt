package com.example.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Record Safety Warning Modal:
 * Prevents accidental stopping, loss, or resetting of recording.
 */
@Composable
fun RecordSafetyModal(
  targetLayerName: String,
  recordedSeconds: Int,
  strokeCount: Int,
  onSaveToGallery: () -> Unit,
  onPreviewPlayback: () -> Unit,
  onKeepRecording: () -> Unit,
  onDiscardAndReset: () -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .width(340.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(Color.White)
        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
        .padding(18.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Warning Icon
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(CircleShape)
          .background(Color(0xFFFEF2F2)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Default.WarningAmber,
          contentDescription = "Warning",
          tint = Color(0xFFDC2626),
          modifier = Modifier.size(26.dp)
        )
      }

      Text(
        text = "রেকর্ডিং ও ড্রয়িং সংরক্ষণ সতর্কতা",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1E293B),
        textAlign = TextAlign.Center
      )

      Text(
        text = "লেয়ার '${targetLayerName}' এর উপর ${recordedSeconds} সেকেন্ড ও ${strokeCount} টি স্ট্রোক রেকর্ড হয়েছে। অসাবধানতাবশত ড্রয়িং যাতে হারিয়ে বা রিসেট না হয়ে যায়, অনুগ্রহ করে নিচের অপশনগুলো থেকে বেছে নিন:",
        fontSize = 12.sp,
        color = Color(0xFF64748B),
        textAlign = TextAlign.Center,
        lineHeight = 16.sp
      )

      Spacer(modifier = Modifier.height(4.dp))

      // Action 1: Save Video & Photo to Gallery (Primary Safe Action)
      Button(
        onClick = onSaveToGallery,
        modifier = Modifier.fillMaxWidth().height(42.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
        shape = RoundedCornerShape(8.dp)
      ) {
        Icon(Icons.Default.SaveAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("ভিডিও ও ছবি গ্যালারিতে সেভ করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
      }

      // Action 2: View Preview Playback
      OutlinedButton(
        onClick = onPreviewPlayback,
        modifier = Modifier.fillMaxWidth().height(38.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0284C7)),
        shape = RoundedCornerShape(8.dp)
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("প্লেব্যাক প্রিভিউ দেখুন", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
      }

      // Action 3: Keep Recording (Cancel Stop)
      OutlinedButton(
        onClick = onKeepRecording,
        modifier = Modifier.fillMaxWidth().height(38.dp),
        shape = RoundedCornerShape(8.dp)
      ) {
        Icon(Icons.Default.FiberManualRecord, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("রেকর্ডিং চালু রাখুন (ড্র করতে থাকুন)", fontSize = 11.5.sp, color = Color(0xFF334155))
      }

      // Action 4: Discard / Reset
      Text(
        text = "রেকর্ডিং বাতিল ও রিসেট করুন",
        fontSize = 11.sp,
        color = Color(0xFFDC2626),
        fontWeight = FontWeight.Bold,
        modifier = Modifier
          .clickable(onClick = onDiscardAndReset)
          .padding(top = 4.dp, bottom = 2.dp)
      )
    }
  }
}

/**
 * Menu Reset Warning Dialog:
 * Prevents accidental clearing of canvas history.
 */
@Composable
fun MenuResetConfirmDialog(
  onSaveFirst: () -> Unit,
  onConfirmReset: () -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .width(330.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(Color.White)
        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
        .padding(18.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(Color(0xFFFEF3C7)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Default.Refresh,
          contentDescription = "Reset Warning",
          tint = Color(0xFFD97706),
          modifier = Modifier.size(24.dp)
        )
      }

      Text(
        text = "ক্যানভাস রিসেট ও সতর্কতা",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1E293B)
      )

      Text(
        text = "আপনি কি নতুন ক্যানভাস শুরু করতে চান? বর্তমান ড্রয়িং গ্যালারিতে সেভ করা না থাকলে তা মুছে যেতে পারে।",
        fontSize = 12.sp,
        color = Color(0xFF64748B),
        textAlign = TextAlign.Center,
        lineHeight = 16.sp
      )

      Spacer(modifier = Modifier.height(4.dp))

      Button(
        onClick = onSaveFirst,
        modifier = Modifier.fillMaxWidth().height(40.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
        shape = RoundedCornerShape(8.dp)
      ) {
        Icon(Icons.Default.SaveAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("আগে গ্যালারিতে সেভ করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        OutlinedButton(
          onClick = onDismiss,
          modifier = Modifier.weight(1f).height(38.dp),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text("বাতিল", fontSize = 11.sp, color = Color(0xFF64748B))
        }

        Button(
          onClick = onConfirmReset,
          modifier = Modifier.weight(1.2f).height(38.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text("হ্যাঁ, রিসেট করুন", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
      }
    }
  }
}
