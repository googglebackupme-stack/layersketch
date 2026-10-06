package com.example

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.CanvasPresets
import com.example.model.CanvasSize
import com.example.model.Layer
import com.example.model.LayerType
import com.example.model.PencilPalette
import com.example.model.PencilStroke
import com.example.model.StrokePoint
import com.example.model.UndoActionItem
import com.example.ui.AddShapeDialog
import com.example.ui.AddTextDialog
import com.example.ui.BrushLibraryDialog
import com.example.ui.CanvasSizeDialog
import com.example.ui.ColorWheelDialog
import com.example.ui.GalleryExporter
import com.example.ui.ImageTransformDialog
import com.example.ui.RecordingOverlay
import com.example.ui.RecordingPlaybackDialog
import com.example.ui.SketchbookCanvas
import com.example.ui.SketchbookLayerPanel
import com.example.ui.SketchbookTopBar
import com.example.ui.StudioToolDock
import com.example.ui.ToolsMenuDialog
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        SketchbookAppScreen()
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    // Wipe history & temp cache completely when exiting app
    try {
      cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}

@Composable
fun SketchbookAppScreen() {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  // Canvas Dimensions State (A4, A5, Square, Story, YouTube, Custom)
  var currentCanvasSize by remember { mutableStateOf(CanvasPresets.SQUARE) }
  var canvasBackgroundColor by remember { mutableStateOf(Color.White) }
  var canvasBackgroundImage by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
  var isCanvasSizeDialogOpen by remember { mutableStateOf(false) }

  // Layers Stack
  val layers = remember { mutableStateListOf<Layer>() }
  var activeLayerId by remember { mutableStateOf("") }
  var recTargetLayerId by remember { mutableStateOf("") }
  val activeLayer = layers.find { it.id == activeLayerId }

  // Tool & Pencil State (15 Sharp Pencils: 4H to 9B, Inking Pen, Chisel Marker, Eraser)
  var activePencil by remember { mutableStateOf(PencilPalette.getGrade("2B")) }
  var brushColor by remember { mutableStateOf(Color(0xFF1B222C)) }
  var brushSize by remember { mutableFloatStateOf(4.0f) }
  var brushOpacity by remember { mutableFloatStateOf(1.0f) }
  var isEraser by remember { mutableStateOf(false) }

  // Pan / Move Canvas Mode
  var isPanMode by remember { mutableStateOf(false) }

  // Hand Touch Image Resize/Move Mode
  var isImageEditMode by remember { mutableStateOf(false) }

  // Canvas Viewport State (Zoom, Pan, and Hand Rotation)
  var zoomScale by remember { mutableFloatStateOf(0.68f) }
  var panOffset by remember { mutableStateOf(Offset(24f, 32f)) }
  var canvasRotation by remember { mutableFloatStateOf(0f) }
  var soloMode by remember { mutableStateOf(false) }

  // Modals & Panels State
  var isLayerPanelOpen by remember { mutableStateOf(false) }
  var isBrushLibraryOpen by remember { mutableStateOf(false) }
  var isColorWheelOpen by remember { mutableStateOf(false) }
  var isToolsMenuOpen by remember { mutableStateOf(false) }
  var isPlaybackDialogOpen by remember { mutableStateOf(false) }
  var isImageTransformDialogOpen by remember { mutableStateOf(false) }
  var isAddTextDialogOpen by remember { mutableStateOf(false) }
  var isAddShapeDialogOpen by remember { mutableStateOf(false) }

  // Live Layer Recording State (records ONLY the target layer while drawing)
  var isLiveRecording by remember { mutableStateOf(false) }
  var recordingElapsedSeconds by remember { mutableIntStateOf(0) }
  val recordedStrokesForTarget = remember { mutableStateListOf<PencilStroke>() }

  // Safety & Warning Modals (to prevent accidental recording stop or canvas reset)
  var showRecordStopConfirmDialog by remember { mutableStateOf(false) }
  var showCanvasResetConfirmDialog by remember { mutableStateOf(false) }
  var showClearLayerConfirmDialog by remember { mutableStateOf(false) }
  var showUndoHistoryDialog by remember { mutableStateOf(false) }

  // Stroke Undo / Redo History & Invalidation Counter
  var canvasRevision by remember { mutableLongStateOf(0L) }
  val undoStack = remember { mutableStateListOf<UndoActionItem>() }
  val redoStack = remember { mutableStateListOf<UndoActionItem>() }

  // Wipe history on disposal/exit
  DisposableEffect(Unit) {
    onDispose {
      undoStack.clear()
      redoStack.clear()
      recordedStrokesForTarget.clear()
      try {
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
      } catch (e: Exception) {}
    }
  }

  // Common Undo Handler (আগের সমস্ত হিস্টোরি স্ট্রোক এক এক করে সম্পূর্ণ মুছে ফেলার নিখুঁত সিস্টেম)
  val performUndo: () -> Unit = {
    var undone = false
    if (undoStack.isNotEmpty()) {
      val item = undoStack.removeLast()
      val targetL = layers.find { it.id == item.layerId }
      if (targetL != null) {
        if (!item.isErased) {
          // ড্রয়িং স্ট্রোক মুছে ফেলা (Undo Draw)
          val removed = targetL.strokes.removeAll { it.id == item.stroke.id }
          if (!removed && targetL.strokes.isNotEmpty()) {
            targetL.strokes.removeLast()
          }
          redoStack.add(UndoActionItem(item.layerId, item.stroke, isErased = false))
          if (isLiveRecording && item.layerId == recTargetLayerId) {
            recordedStrokesForTarget.removeAll { it.id == item.stroke.id }
          }
          undone = true
        } else {
          // ইরেজ করা স্ট্রোক পুনরায় ফিরিয়ে আনা (Undo Erase)
          targetL.strokes.add(item.stroke)
          redoStack.add(UndoActionItem(item.layerId, item.stroke, isErased = true))
          if (isLiveRecording && item.layerId == recTargetLayerId) {
            recordedStrokesForTarget.add(item.stroke)
          }
          undone = true
        }
      }
    }

    if (!undone) {
      // Fallback: undoStack শেষ হয়ে গেলেও একটিভ লেয়ার বা পূর্বের ড্রয়িং লেয়ার থেকে আগের স্ট্রোক মুছুন
      val targetL = activeLayer?.takeIf { it.strokes.isNotEmpty() }
        ?: layers.firstOrNull { it.type == LayerType.DRAWING && it.strokes.isNotEmpty() }
      if (targetL != null && targetL.strokes.isNotEmpty()) {
        val stroke = targetL.strokes.removeLast()
        redoStack.add(UndoActionItem(targetL.id, stroke, isErased = false))
        if (isLiveRecording && targetL.id == recTargetLayerId) {
          recordedStrokesForTarget.removeAll { it.id == stroke.id }
        }
        undone = true
      }
    }

    if (undone) {
      canvasRevision++
    }
  }

  // Multi-step Undo Handler (আগের একাধিক স্ট্রোক একসাথে মোছা)
  val performUndoMultiple: (Int) -> Unit = { count ->
    var executed = 0
    repeat(count) {
      if (undoStack.isNotEmpty() || layers.any { it.type == LayerType.DRAWING && it.strokes.isNotEmpty() }) {
        performUndo()
        executed++
      }
    }
    if (executed > 0) {
      Toast.makeText(context, "${executed}টি পূর্ববর্তী স্ট্রোক মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
    }
  }

  // Clear all strokes on active layer and register in redo stack
  val performUndoAllOnActiveLayer: () -> Unit = {
    activeLayer?.let { l ->
      val count = l.strokes.size
      if (count > 0) {
        val strokesToUndo = l.strokes.toList()
        l.strokes.clear()
        strokesToUndo.reversed().forEach { s ->
          redoStack.add(UndoActionItem(l.id, s, isErased = false))
        }
        undoStack.removeAll { it.layerId == l.id }
        if (isLiveRecording && l.id == recTargetLayerId) {
          recordedStrokesForTarget.clear()
        }
        canvasRevision++
        Toast.makeText(context, "${count}টি স্ট্রোক মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
      }
    }
  }

  // Common Redo Handler
  val performRedo: () -> Unit = {
    if (redoStack.isNotEmpty()) {
      val item = redoStack.removeLast()
      val targetL = layers.find { it.id == item.layerId }
      if (targetL != null) {
        if (!item.isErased) {
          targetL.strokes.add(item.stroke)
          undoStack.add(UndoActionItem(item.layerId, item.stroke, isErased = false))
          if (isLiveRecording && item.layerId == recTargetLayerId) {
            recordedStrokesForTarget.add(item.stroke)
          }
        } else {
          targetL.strokes.removeAll { it.id == item.stroke.id }
          undoStack.add(UndoActionItem(item.layerId, item.stroke, isErased = true))
          if (isLiveRecording && item.layerId == recTargetLayerId) {
            recordedStrokesForTarget.removeAll { it.id == item.stroke.id }
          }
        }
        canvasRevision++
      }
    }
  }

  val performRedoMultiple: (Int) -> Unit = { count ->
    repeat(count) {
      if (redoStack.isNotEmpty()) {
        performRedo()
      }
    }
  }

  // Photo Picker Launcher (Preserves original dimensions & aspect ratio)
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
          val bitmap = BitmapFactory.decodeStream(inputStream)
          if (bitmap != null) {
            val imageBitmap = bitmap.asImageBitmap()
            val origW = bitmap.width.toFloat()
            val origH = bitmap.height.toFloat()

            val newImageLayer = Layer(
              id = UUID.randomUUID().toString(),
              name = "Photo ${layers.size + 1}",
              type = LayerType.IMAGE,
              imageBitmap = imageBitmap,
              imageOriginalWidth = origW,
              imageOriginalHeight = origH,
              imageOffsetX = 0f,
              imageOffsetY = 0f,
              imageScale = 1.0f,
              imageRotation = 0f,
              width = currentCanvasSize.width,
              height = currentCanvasSize.height
            )
            val insertIndex = (layers.size - 1).coerceAtLeast(0)
            layers.add(insertIndex, newImageLayer)
            activeLayerId = newImageLayer.id
            isImageEditMode = true // Enable direct hand resizing/positioning on canvas!
            Toast.makeText(context, "ছবি যুক্ত হয়েছে! হাত দিয়ে ড্র্যাগ করে বসান ও পিঞ্চ করে রিসাইজ করুন", Toast.LENGTH_LONG).show()
          }
        }
      } catch (e: Exception) {
        Toast.makeText(context, "ছবি লোড করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
      }
    }
  }

  // Background Photo Picker Launcher
  val bgPhotoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
          val bitmap = BitmapFactory.decodeStream(inputStream)
          if (bitmap != null) {
            val bmp = bitmap.asImageBitmap()
            canvasBackgroundImage = bmp
            val bgLayer = layers.find { it.type == LayerType.BACKGROUND }
            bgLayer?.backgroundImageBitmap = bmp
            bgLayer?.backgroundImageUri = uri.toString()
            Toast.makeText(context, "ক্যানভাস ব্যাকগ্রাউন্ড ছবি সফলভাবে সেট হয়েছে!", Toast.LENGTH_SHORT).show()
          }
        }
      } catch (e: Exception) {
        Toast.makeText(context, "ব্যাকগ্রাউন্ড ছবি লোড করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
      }
    }
  }

  // Timer for Live Recording
  LaunchedEffect(isLiveRecording) {
    if (isLiveRecording) {
      while (isLiveRecording) {
        delay(1000)
        recordingElapsedSeconds++
      }
    }
  }

  // Preload initial portrait sketch matching Autodesk Sketchbook showcase
  LaunchedEffect(Unit) {
    if (layers.isEmpty()) {
      val lPaper = Layer(
        id = UUID.randomUUID().toString(),
        name = "Layer 1: Paper (ব্যাকগ্রাউন্ড)",
        type = LayerType.BACKGROUND,
        backgroundColor = Color.White
      )

      val lCaricature = Layer(
        id = UUID.randomUUID().toString(),
        name = "Layer 2: Sketch Art",
        type = LayerType.DRAWING
      )

      fun makeStroke(points: List<Offset>, width: Float = 3.6f, opacity: Float = 0.85f): PencilStroke {
        return PencilStroke(
          points = points.map { StrokePoint(it, 0.75f) },
          color = Color(0xFF1B222C),
          width = width,
          opacity = opacity,
          pencilGrade = "2B"
        )
      }

      // Cap outline
      lCaricature.strokes.add(
        makeStroke(listOf(
          Offset(360f, 260f), Offset(420f, 210f), Offset(540f, 200f),
          Offset(660f, 220f), Offset(740f, 310f), Offset(710f, 340f),
          Offset(480f, 330f), Offset(360f, 310f), Offset(360f, 260f)
        ), 4.2f)
      )

      // Face contour
      lCaricature.strokes.add(
        makeStroke(listOf(
          Offset(340f, 400f), Offset(320f, 460f), Offset(340f, 520f),
          Offset(420f, 580f), Offset(520f, 600f), Offset(630f, 570f),
          Offset(720f, 490f), Offset(740f, 410f)
        ), 4.2f)
      )

      // Eyes & Eyebrows
      lCaricature.strokes.add(
        makeStroke(listOf(
          Offset(390f, 400f), Offset(430f, 385f), Offset(480f, 400f),
          Offset(460f, 420f), Offset(410f, 420f), Offset(390f, 400f)
        ), 3.2f)
      )
      lCaricature.strokes.add(
        makeStroke(listOf(
          Offset(580f, 400f), Offset(630f, 385f), Offset(680f, 405f),
          Offset(660f, 425f), Offset(610f, 425f), Offset(580f, 400f)
        ), 3.2f)
      )

      // Smile & Beard
      lCaricature.strokes.add(
        makeStroke(listOf(
          Offset(440f, 480f), Offset(490f, 520f), Offset(540f, 525f),
          Offset(620f, 515f), Offset(660f, 475f)
        ), 4.0f)
      )
      lCaricature.strokes.add(
        makeStroke(listOf(
          Offset(320f, 520f), Offset(340f, 590f), Offset(420f, 640f),
          Offset(540f, 660f), Offset(640f, 630f), Offset(720f, 560f), Offset(740f, 500f)
        ), 3.8f)
      )

      // Shirt & Shoulders
      lCaricature.strokes.add(
        makeStroke(listOf(
          Offset(280f, 750f), Offset(360f, 650f), Offset(450f, 620f),
          Offset(540f, 630f), Offset(650f, 650f), Offset(750f, 750f)
        ), 4.2f)
      )

      // Layer 3: Detail & Live Recording Target
      val lDetail = Layer(
        id = UUID.randomUUID().toString(),
        name = "Layer 3: Live Drawing (রেকর্ড টার্গেট)",
        type = LayerType.DRAWING,
        isRecTarget = true
      )

      layers.addAll(listOf(lDetail, lCaricature, lPaper))
      activeLayerId = lDetail.id
      recTargetLayerId = lDetail.id

      // Register initial caricature sketch strokes into undo history so they can also be undone!
      lCaricature.strokes.forEach { stroke ->
        undoStack.add(UndoActionItem(lCaricature.id, stroke, isErased = false))
      }
    }
  }

  val recTargetLayer = layers.find { it.id == recTargetLayerId } ?: layers.firstOrNull()

  // Handle back button to dismiss panels
  BackHandler(
    enabled = isLayerPanelOpen || isBrushLibraryOpen || isColorWheelOpen || isToolsMenuOpen ||
      isPlaybackDialogOpen || isCanvasSizeDialogOpen || isImageTransformDialogOpen ||
      isAddTextDialogOpen || isAddShapeDialogOpen || isImageEditMode || showUndoHistoryDialog
  ) {
    if (showUndoHistoryDialog) showUndoHistoryDialog = false
    else if (isImageEditMode) isImageEditMode = false
    else if (isLayerPanelOpen) isLayerPanelOpen = false
    else if (isCanvasSizeDialogOpen) isCanvasSizeDialogOpen = false
    else if (isImageTransformDialogOpen) isImageTransformDialogOpen = false
    else if (isAddTextDialogOpen) isAddTextDialogOpen = false
    else if (isAddShapeDialogOpen) isAddShapeDialogOpen = false
    else if (isBrushLibraryOpen) isBrushLibraryOpen = false
    else if (isColorWheelOpen) isColorWheelOpen = false
    else if (isToolsMenuOpen) isToolsMenuOpen = false
    else if (isPlaybackDialogOpen) isPlaybackDialogOpen = false
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0xFF888E99)) // Clean studio desk
      .safeDrawingPadding()
      .testTag("sketchbook_studio_root")
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // 1. Sketchbook Top Toolbar (Fixed at top with high zIndex so zoom never covers it!)
      SketchbookTopBar(
        modifier = Modifier.fillMaxWidth().zIndex(30f),
        canUndo = undoStack.isNotEmpty() || layers.any { it.type == LayerType.DRAWING && it.strokes.isNotEmpty() },
        canRedo = redoStack.isNotEmpty(),
        currentColor = brushColor,
        isLayerPanelOpen = isLayerPanelOpen,
        layersCount = layers.size,
        onMenuClick = {
          showCanvasResetConfirmDialog = true
        },
        onUndoClick = performUndo,
        onRedoClick = performRedo,
        onUndoLongClick = { showUndoHistoryDialog = true },
        onRedoLongClick = { performRedoMultiple(3) },
        onSavePhotoClick = {
          // SAVE FULL ARTWORK AS PHOTO TO GALLERY (ফটোগ্রাফ সেভ - ক্যানভাস কালার ও ব্যাকগ্রাউন্ড সহ)
          coroutineScope.launch {
            GalleryExporter.saveCanvasAsPhoto(
              context = context,
              layers = layers,
              paperWidth = currentCanvasSize.width.toInt(),
              paperHeight = currentCanvasSize.height.toInt(),
              canvasColor = canvasBackgroundColor,
              canvasBgImage = canvasBackgroundImage
            )
          }
        },
        onToolsClick = { isToolsMenuOpen = true },
        onBrushLibraryClick = { isBrushLibraryOpen = true },
        onColorWheelClick = { isColorWheelOpen = true },
        onLayersClick = { isLayerPanelOpen = !isLayerPanelOpen },
        onFitClick = {
          zoomScale = 0.65f
          panOffset = Offset(24f, 32f)
          canvasRotation = 0f
        }
      )

      // 2. Main Studio Canvas with Right-Side Layer Panel (Strictly clipped so zoom never bleeds over topbar)
      Box(modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
        SketchbookCanvas(
          modifier = Modifier.fillMaxSize().clipToBounds(),
          canvasSize = currentCanvasSize,
          layers = layers,
          activeLayer = activeLayer,
          activePencil = activePencil,
          brushColor = brushColor,
          brushSize = brushSize,
          onUpdateBrushSize = { brushSize = it },
          brushOpacity = brushOpacity,
          onUpdateBrushOpacity = { brushOpacity = it },
          isEraser = isEraser,
          canUndo = undoStack.isNotEmpty() || layers.any { it.type == LayerType.DRAWING && it.strokes.isNotEmpty() },
          canRedo = redoStack.isNotEmpty(),
          onUndoClick = performUndo,
          onRedoClick = performRedo,
          onUndoLongClick = { showUndoHistoryDialog = true },
          onRedoLongClick = { performRedoMultiple(3) },
          isPanMode = isPanMode,
          onTogglePanMode = { isPanMode = it },
          isImageEditMode = isImageEditMode,
          onExitImageEditMode = { isImageEditMode = false },
          onUpdateImageTransform = { x, y, s, r ->
            activeLayer?.let {
              it.imageOffsetX = x
              it.imageOffsetY = y
              it.imageScale = s
              it.imageRotation = r
            }
          },
          onOpenImageTransformDialog = { isImageTransformDialogOpen = true },
          soloMode = soloMode,
          currentTime = 0f,
          canvasRevision = canvasRevision,
          zoomScale = zoomScale,
          panOffset = panOffset,
          canvasRotation = canvasRotation,
          onTransformChange = { newZoom, newPan, newRot ->
            zoomScale = newZoom
            panOffset = newPan
            canvasRotation = newRot
          },
          onStrokeCompleted = { stroke ->
            if (activeLayer != null) {
              activeLayer.strokes.add(stroke)
              undoStack.add(UndoActionItem(activeLayer.id, stroke, isErased = false))
              redoStack.clear()
              canvasRevision++

              if (isLiveRecording && activeLayer.id == recTargetLayerId) {
                recordedStrokesForTarget.add(stroke)
              }
            }
          },
          onStrokeErased = { erasedStroke ->
            if (activeLayer != null) {
              undoStack.add(UndoActionItem(activeLayer.id, erasedStroke, isErased = true))
              redoStack.clear()
              canvasRevision++
              if (isLiveRecording && activeLayer.id == recTargetLayerId) {
                recordedStrokesForTarget.removeAll { it.id == erasedStroke.id }
              }
            }
          }
        )

        // Ultra-Compact Live Recording Active Banner
        if (isLiveRecording) {
          Box(modifier = Modifier.align(Alignment.TopCenter)) {
            RecordingOverlay(
              layerName = recTargetLayer?.name ?: "Layer",
              recordedSeconds = recordingElapsedSeconds,
              recordedStrokeCount = recordedStrokesForTarget.size,
              onPausePreviewClick = {
                showRecordStopConfirmDialog = true
              }
            )
          }
        }

        // 1. Right-Edge Floating Layer Pull-Tab (মোবাইলে সবসময় ডান প্রান্তে সহজে লেয়ার বের করার জন্য হ্যান্ডেল)
        if (!isLayerPanelOpen) {
          Box(
            modifier = Modifier
              .align(Alignment.CenterEnd)
              .zIndex(25f)
              .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
              .shadow(elevation = 8.dp)
              .background(Color(0xFF0F172A))
              .border(
                width = 1.5.dp,
                color = Color(0xFF38BDF8),
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
              )
              .clickable { isLayerPanelOpen = true }
              .padding(horizontal = 8.dp, vertical = 12.dp)
              .testTag("layer_pull_tab"),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = "Open Layers",
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(20.dp)
              )
              Text(
                text = "লেয়ার",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Box(
                modifier = Modifier
                  .clip(CircleShape)
                  .background(Color(0xFF007ACC))
                  .padding(horizontal = 5.dp, vertical = 1.dp)
              ) {
                Text(
                  text = "${layers.size}",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = Color.White
                )
              }
              Text(
                text = "◀",
                fontSize = 12.sp,
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        // 2. Right-Side Layer Drawer with Backdrop Scrim and Collapse Handle
        if (isLayerPanelOpen) {
          Box(modifier = Modifier.fillMaxSize().zIndex(35f)) {
            // Touch-outside Scrim to close layers
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
                .clickable { isLayerPanelOpen = false }
            )

            // Panel with attached collapse handle aligned to CenterEnd
            Row(
              modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Attached Collapse Tab on the left edge of panel (ট্যাপ করলেই সহজে ভেতরে ঢুকে যাবে)
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                  .shadow(elevation = 6.dp)
                  .background(Color(0xFF0F172A))
                  .border(
                    width = 1.5.dp,
                    color = Color(0xFF38BDF8),
                    shape = RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp)
                  )
                  .clickable { isLayerPanelOpen = false }
                  .padding(horizontal = 6.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
              ) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Text("▶", fontSize = 14.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                  Text("বন্ধ", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
              }

              // The Layer Panel
              SketchbookLayerPanel(
                layers = layers,
                activeLayerId = activeLayerId,
                recTargetLayerId = recTargetLayerId,
                onSelectLayer = { activeLayerId = it },
                onSetRecTarget = { targetId ->
                  recTargetLayerId = targetId
                  layers.forEach { it.isRecTarget = (it.id == targetId) }
                  recordedStrokesForTarget.clear()
                  isLiveRecording = true
                  recordingElapsedSeconds = 0
                  Toast.makeText(
                    context,
                    "রেকর্ডিং শুরু: শুধুমাত্র '${layers.find { it.id == targetId }?.name}' রেকর্ড হবে!",
                    Toast.LENGTH_SHORT
                  ).show()
                },
                onToggleVisibility = { id ->
                  layers.find { it.id == id }?.let { it.visible = !it.visible }
                },
                onUpdateOpacity = { id, newOpacity ->
                  layers.find { it.id == id }?.let { it.opacity = newOpacity }
                },
                onSelectBackgroundColor = { color ->
                  layers.find { it.type == LayerType.BACKGROUND }?.let {
                    it.backgroundColor = color
                  }
                },
                onAddLayer = {
                  val newL = Layer(name = "Layer ${layers.size + 1}", type = LayerType.DRAWING)
                  layers.add(0, newL)
                  activeLayerId = newL.id
                },
                onAddImageLayer = {
                  photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
                onTransformImageLayer = {
                  isImageEditMode = true
                },
                onDeleteLayer = { id ->
                  if (layers.size > 1) {
                    layers.removeAll { it.id == id }
                    if (activeLayerId == id) activeLayerId = layers.first().id
                    if (recTargetLayerId == id) recTargetLayerId = layers.first().id
                  }
                },
                onColorWheelClick = { isColorWheelOpen = true },
                onClose = { isLayerPanelOpen = false }
              )
            }
          }
        }
      }

      // 3. Dedicated Bottom Studio Tool Dock (Pencil, Eraser, Move/Pan, Pen, Marker, Hand Color, Record)
      StudioToolDock(
        modifier = Modifier.fillMaxWidth().zIndex(30f),
        activePencil = activePencil,
        onSelectPencil = { p ->
          activePencil = p
          isEraser = false
          isPanMode = false
          isImageEditMode = false
          brushColor = p.graphiteColor
          brushSize = p.baseWidth
        },
        brushColor = brushColor,
        onSelectColor = { c ->
          brushColor = c
          isEraser = false
          isPanMode = false
        },
        brushSize = brushSize,
        onUpdateSize = { brushSize = it },
        brushOpacity = brushOpacity,
        onUpdateOpacity = { brushOpacity = it },
        isEraser = isEraser,
        onToggleEraser = { eraserOn ->
          isEraser = eraserOn
          isPanMode = false
          isImageEditMode = false
          if (eraserOn) {
            brushSize = 18.0f
          } else {
            brushSize = activePencil.baseWidth
          }
        },
        isPanMode = isPanMode,
        onTogglePanMode = { isPanMode = it },
        onOpenColorWheel = { isColorWheelOpen = true },
        isRecording = isLiveRecording,
        targetLayerName = recTargetLayer?.name ?: "Layer",
        onToggleRecord = {
          if (isLiveRecording) {
            showRecordStopConfirmDialog = true
          } else {
            recTargetLayerId = activeLayerId
            layers.forEach { it.isRecTarget = (it.id == activeLayerId) }
            recordedStrokesForTarget.clear()
            recordingElapsedSeconds = 0
            isLiveRecording = true
            Toast.makeText(
              context,
              "রেকর্ড শুরু: শুধুমাত্র '${activeLayer?.name}' রেকর্ড হচ্ছে। এখন এর উপর ড্র করুন!",
              Toast.LENGTH_LONG
            ).show()
          }
        },
        isLayerPanelOpen = isLayerPanelOpen,
        layersCount = layers.size,
        onToggleLayers = { isLayerPanelOpen = !isLayerPanelOpen },
        onUndo = performUndo,
        onUndoLongClick = { showUndoHistoryDialog = true }
      )
    }

    // 4. Modals & Dialogs

    // Canvas Size & Background Dialog (A4, A5, Square, Custom, Color, Image)
    if (isCanvasSizeDialogOpen) {
      CanvasSizeDialog(
        currentCanvasSize = currentCanvasSize,
        currentCanvasColor = canvasBackgroundColor,
        currentBackgroundImage = canvasBackgroundImage,
        onApplyCanvasConfig = { newSize, newColor, clearBgImage ->
          currentCanvasSize = newSize
          canvasBackgroundColor = newColor
          zoomScale = (720f / newSize.height.coerceAtLeast(newSize.width)).coerceIn(0.2f, 1.2f)
          panOffset = Offset(24f, 32f)
          canvasRotation = 0f

          val bgLayer = layers.find { it.type == LayerType.BACKGROUND }
          bgLayer?.backgroundColor = newColor
          if (clearBgImage) {
            canvasBackgroundImage = null
            bgLayer?.backgroundImageBitmap = null
            bgLayer?.backgroundImageUri = null
          }
          Toast.makeText(context, "ক্যানভাস সেটআপ ও ব্যাকগ্রাউন্ড সম্পন্ন হয়েছে!", Toast.LENGTH_SHORT).show()
        },
        onRequestPickBackgroundImage = {
          bgPhotoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        },
        onDismiss = { isCanvasSizeDialogOpen = false }
      )
    }

    // Image Transform Dialog (Numeric Pos X, Pos Y, Scale, Rotation, Fit)
    if (isImageTransformDialogOpen && activeLayer != null && activeLayer.type == LayerType.IMAGE) {
      ImageTransformDialog(
        layer = activeLayer,
        paperWidth = currentCanvasSize.width,
        paperHeight = currentCanvasSize.height,
        onUpdateTransform = { x, y, s, r ->
          activeLayer.imageOffsetX = x
          activeLayer.imageOffsetY = y
          activeLayer.imageScale = s
          activeLayer.imageRotation = r
        },
        onDismiss = { isImageTransformDialogOpen = false }
      )
    }

    // Add Text Dialog
    if (isAddTextDialogOpen) {
      AddTextDialog(
        onAddText = { txt, sz ->
          val lText = Layer(
            name = "Text: ${txt.take(10)}",
            type = LayerType.TEXT,
            text = txt,
            textSize = sz,
            textColor = brushColor,
            width = currentCanvasSize.width,
            height = currentCanvasSize.height
          )
          layers.add(0, lText)
          activeLayerId = lText.id
          Toast.makeText(context, "টেক্সট লেয়ার যোগ করা হয়েছে", Toast.LENGTH_SHORT).show()
        },
        onDismiss = { isAddTextDialogOpen = false }
      )
    }

    // Add Shape Dialog
    if (isAddShapeDialogOpen) {
      AddShapeDialog(
        onSelectShape = { shapeType ->
          val lShape = Layer(
            name = "Shape: ${shapeType.name}",
            type = LayerType.SHAPE,
            shapeType = shapeType,
            shapeFill = brushColor.copy(alpha = 0.4f),
            shapeStroke = brushColor,
            width = currentCanvasSize.width * 0.6f,
            height = currentCanvasSize.height * 0.6f
          )
          layers.add(0, lShape)
          activeLayerId = lShape.id
          Toast.makeText(context, "শেপ লেয়ার যোগ করা হয়েছে", Toast.LENGTH_SHORT).show()
        },
        onDismiss = { isAddShapeDialogOpen = false }
      )
    }

    // Tools Menu (3x3 grid)
    if (isToolsMenuOpen) {
      ToolsMenuDialog(
        onDismiss = { isToolsMenuOpen = false },
        onRecordClick = {
          if (isLiveRecording) {
            showRecordStopConfirmDialog = true
          } else {
            recTargetLayerId = activeLayerId
            layers.forEach { it.isRecTarget = (it.id == activeLayerId) }
            recordedStrokesForTarget.clear()
            recordingElapsedSeconds = 0
            isLiveRecording = true
            Toast.makeText(
              context,
              "নির্দিষ্ট লেয়ার রেকর্ড শুরু হয়েছে: '${activeLayer?.name}'. এখন এর ওপর ড্র করুন!",
              Toast.LENGTH_LONG
            ).show()
          }
        },
        onClearClick = {
          showUndoHistoryDialog = true
        },
        onAddTextClick = { isAddTextDialogOpen = true },
        onAddShapeClick = { isAddShapeDialogOpen = true },
        onAddPhotoClick = {
          photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        },
        onTransformPhotoClick = {
          if (activeLayer?.type == LayerType.IMAGE) {
            isImageEditMode = true
          } else {
            Toast.makeText(context, "প্রথমে একটি ছবি লেয়ার নির্বাচন করুন", Toast.LENGTH_SHORT).show()
          }
        },
        onOpenCanvasSetup = { isCanvasSizeDialogOpen = true },
        onOpenLayers = { isLayerPanelOpen = true }
      )
    }

    // Brush Library Modal
    if (isBrushLibraryOpen) {
      BrushLibraryDialog(
        activePencil = activePencil,
        brushSize = brushSize,
        brushOpacity = brushOpacity * activePencil.baseOpacity,
        brushColor = brushColor,
        isEraser = isEraser,
        onSelectPencil = { p ->
          if (p.isEraser) {
            isEraser = true
            brushSize = 18f
          } else {
            isEraser = false
            activePencil = p
            brushColor = p.graphiteColor
            brushSize = p.baseWidth
          }
        },
        onUpdateSize = { brushSize = it },
        onUpdateOpacity = { brushOpacity = it },
        onColorWheelClick = {
          isBrushLibraryOpen = false
          isColorWheelOpen = true
        },
        onDismiss = { isBrushLibraryOpen = false }
      )
    }

    // Hand/Touch Interactive Color Wheel
    if (isColorWheelOpen) {
      ColorWheelDialog(
        initialColor = brushColor,
        onColorSelected = {
          brushColor = it
          isEraser = false
        },
        onDismiss = { isColorWheelOpen = false }
      )
    }

    // Isolated Layer Recording Playback / Pause & Preview / Save to Gallery Dialog
    if (isPlaybackDialogOpen) {
      RecordingPlaybackDialog(
        canvasSize = currentCanvasSize,
        layerName = recTargetLayer?.name ?: "Target Layer",
        recordedStrokes = recordedStrokesForTarget.toList(),
        canvasColor = canvasBackgroundColor,
        canvasBgImage = canvasBackgroundImage,
        layers = layers.toList(),
        onResumeRecording = {
          isPlaybackDialogOpen = false
          isLiveRecording = true
          Toast.makeText(context, "রেকর্ডিং চালু রয়েছে: ড্র করতে থাকুন!", Toast.LENGTH_SHORT).show()
        },
        onRestartRecording = {
          recordedStrokesForTarget.clear()
          recordingElapsedSeconds = 0
          isPlaybackDialogOpen = false
          isLiveRecording = true
          Toast.makeText(context, "নতুন রেকর্ডিং শুরু হয়েছে!", Toast.LENGTH_SHORT).show()
        },
        onFinishRecording = {
          isLiveRecording = false
          isPlaybackDialogOpen = false
          Toast.makeText(context, "রেকর্ডিং সম্পূর্ণ হয়েছে", Toast.LENGTH_SHORT).show()
        },
        onShareClick = {
          val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Layer Recording: ${recTargetLayer?.name}")
            putExtra(Intent.EXTRA_TEXT, "Exported isolated layer recording of '${recTargetLayer?.name}' (${recordedStrokesForTarget.size} strokes) from mahfujdrawing")
          }
          context.startActivity(Intent.createChooser(shareIntent, "Share Isolated Layer Drawing"))
        },
        onDismiss = {
          isPlaybackDialogOpen = false
        }
      )
    }

    // 1. Safety Modal: Stop / Discard Recording Warning
    if (showRecordStopConfirmDialog) {
      AlertDialog(
        onDismissRequest = { showRecordStopConfirmDialog = false },
        title = {
          Text("🎬 রেকর্ডিং নিয়ন্ত্রণ ও সতর্কতা", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        },
        text = {
          Text(
            "আপনার '${recTargetLayer?.name}' লেয়ারের ${recordedStrokesForTarget.size}টি স্ট্রোক রেকর্ড করা হয়েছে (${recordingElapsedSeconds} সেকেন্ড)।\n\nআপনি কি রেকর্ডিং সেভ করে প্রিভিউ দেখতে চান, ড্র চালিয়ে যেতে চান, নাকি বাতিল করে রিসেট করতে চান?",
            fontSize = 13.sp
          )
        },
        confirmButton = {
          Button(
            onClick = {
              showRecordStopConfirmDialog = false
              isPlaybackDialogOpen = true
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007ACC))
          ) {
            Text("💾 সেভ ও প্রিভিউ দেখুন", fontSize = 12.sp)
          }
        },
        dismissButton = {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
              onClick = {
                showRecordStopConfirmDialog = false
                isLiveRecording = false
                recordedStrokesForTarget.clear()
                recordingElapsedSeconds = 0
                Toast.makeText(context, "রেকর্ডিং বাতিল ও রিসেট করা হয়েছে", Toast.LENGTH_SHORT).show()
              }
            ) {
              Text("⚠️ বাতিল ও রিসেট", fontSize = 11.sp, color = Color(0xFFDC2626))
            }
            Button(
              onClick = { showRecordStopConfirmDialog = false },
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
            ) {
              Text("▶ ড্র চালিয়ে যান", fontSize = 11.sp)
            }
          }
        }
      )
    }

    // 2. Safety Modal: Canvas & History Reset Warning
    if (showCanvasResetConfirmDialog) {
      AlertDialog(
        onDismissRequest = { showCanvasResetConfirmDialog = false },
        title = {
          Text("⚠️ ক্যানভাস ও হিস্ট্রি রিসেট", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        },
        text = {
          Text(
            "সতর্কতা: ক্যানভাস রিসেট করলে সমস্ত আনডু/রিডু হিস্ট্রি এবং রেকর্ড করা স্ট্রোকগুলো স্থায়ীভাবে মুছে যাবে। আপনি কি নিশ্চিত?",
            fontSize = 13.sp
          )
        },
        confirmButton = {
          Button(
            onClick = {
              showCanvasResetConfirmDialog = false
              undoStack.clear()
              redoStack.clear()
              recordedStrokesForTarget.clear()
              recordingElapsedSeconds = 0
              isLiveRecording = false
              Toast.makeText(context, "ক্যানভাস হিস্ট্রি ও রেকর্ডিং রিসেট করা হয়েছে", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
          ) {
            Text("হ্যাঁ, রিসেট করুন", fontSize = 12.sp)
          }
        },
        dismissButton = {
          OutlinedButton(onClick = { showCanvasResetConfirmDialog = false }) {
            Text("বাতিল", fontSize = 12.sp)
          }
        }
      )
    }

    // 3. Safety Modal: Clear Active Layer Warning
    if (showClearLayerConfirmDialog) {
      AlertDialog(
        onDismissRequest = { showClearLayerConfirmDialog = false },
        title = {
          Text("⚠️ লেয়ার ড্রয়িং মুছুন", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        },
        text = {
          Text(
            "সতর্কতা: '${activeLayer?.name}' লেয়ারের সমস্ত অঙ্কিত দাগ মুছে যাবে! আপনি কি মুছে ফেলতে চান?",
            fontSize = 13.sp
          )
        },
        confirmButton = {
          Button(
            onClick = {
              showClearLayerConfirmDialog = false
              activeLayer?.strokes?.clear()
              Toast.makeText(context, "লেয়ার ক্লিয়ার করা হয়েছে", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
          ) {
            Text("হ্যাঁ, মুছুন", fontSize = 12.sp)
          }
        },
        dismissButton = {
          OutlinedButton(onClick = { showClearLayerConfirmDialog = false }) {
            Text("বাতিল", fontSize = 12.sp)
          }
        }
      )
    }

    // 4. Undo History Dialog: Multi-step & Continuous Undo (আগের একাধিক স্ট্রোক একসাথে মোছার অপশন)
    if (showUndoHistoryDialog) {
      AlertDialog(
        onDismissRequest = { showUndoHistoryDialog = false },
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Undo,
              contentDescription = "Undo History",
              tint = Color(0xFF007ACC)
            )
            Text(
              text = "হিস্টোরি আনডু (একাধিক স্ট্রোক মুছুন)",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF0F172A)
            )
          }
        },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = "আপনি একসাথে পূর্ববর্তী কতটি ড্রয়িং স্ট্রোক মুছতে চান? নিচের অপশনগুলো থেকে বেছে নিন:",
              fontSize = 12.5.sp,
              color = Color(0xFF475569)
            )

            Button(
              onClick = {
                performUndo()
                showUndoHistoryDialog = false
              },
              modifier = Modifier.fillMaxWidth(),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = Color(0xFF1E293B))
            ) {
              Text("↩️ ১টি আগের স্ট্রোক মুছুন (Undo 1)")
            }

            Button(
              onClick = {
                performUndoMultiple(3)
                showUndoHistoryDialog = false
              },
              modifier = Modifier.fillMaxWidth(),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF007ACC))
            ) {
              Text("⏪ আগের ৩টি স্ট্রোক মুছুন (Undo 3)")
            }

            Button(
              onClick = {
                performUndoMultiple(5)
                showUndoHistoryDialog = false
              },
              modifier = Modifier.fillMaxWidth(),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF007ACC))
            ) {
              Text("⏪ আগের ৫টি স্ট্রোক মুছুন (Undo 5)")
            }

            Button(
              onClick = {
                performUndoMultiple(10)
                showUndoHistoryDialog = false
              },
              modifier = Modifier.fillMaxWidth(),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF007ACC))
            ) {
              Text("⏮️ আগের ১০টি স্ট্রোক মুছুন (Undo 10)")
            }

            OutlinedButton(
              onClick = {
                performUndoAllOnActiveLayer()
                showUndoHistoryDialog = false
              },
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "🗑️ বর্তমান লেয়ারের সব ড্রয়িং মুছুন (Clear Layer)",
                color = Color(0xFFDC2626),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
            }
          }
        },
        confirmButton = {},
        dismissButton = {
          TextButton(onClick = { showUndoHistoryDialog = false }) {
            Text("বাতিল", color = Color(0xFF64748B))
          }
        }
      )
    }
  }
}
