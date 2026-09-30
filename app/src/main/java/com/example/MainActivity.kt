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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.model.CanvasPresets
import com.example.model.CanvasSize
import com.example.model.Layer
import com.example.model.LayerType
import com.example.model.PencilPalette
import com.example.model.PencilStroke
import com.example.model.StrokePoint
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
  var isCanvasSizeDialogOpen by remember { mutableStateOf(false) }

  // Layers Stack
  val layers = remember { mutableStateListOf<Layer>() }
  var activeLayerId by remember { mutableStateOf("") }
  var recTargetLayerId by remember { mutableStateOf("") }

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

  // Stroke Undo / Redo History
  val undoStack = remember { mutableStateListOf<Pair<String, PencilStroke>>() }
  val redoStack = remember { mutableStateListOf<Pair<String, PencilStroke>>() }

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

  // Common Undo Handler
  val performUndo = {
    if (undoStack.isNotEmpty()) {
      val (layerId, stroke) = undoStack.removeLast()
      val targetL = layers.find { it.id == layerId }
      if (targetL != null) {
        if (targetL.strokes.contains(stroke)) {
          targetL.strokes.remove(stroke)
          redoStack.add(layerId to stroke)
          if (isLiveRecording && layerId == recTargetLayerId) {
            recordedStrokesForTarget.remove(stroke)
          }
        } else {
          // Erased stroke restored!
          targetL.strokes.add(stroke)
          redoStack.add(layerId to stroke)
          if (isLiveRecording && layerId == recTargetLayerId) {
            recordedStrokesForTarget.add(stroke)
          }
        }
      }
    }
  }

  // Common Redo Handler
  val performRedo = {
    if (redoStack.isNotEmpty()) {
      val (layerId, stroke) = redoStack.removeLast()
      val targetL = layers.find { it.id == layerId }
      if (targetL != null) {
        if (targetL.strokes.contains(stroke)) {
          targetL.strokes.remove(stroke)
          undoStack.add(layerId to stroke)
          if (isLiveRecording && layerId == recTargetLayerId) {
            recordedStrokesForTarget.remove(stroke)
          }
        } else {
          targetL.strokes.add(stroke)
          undoStack.add(layerId to stroke)
          if (isLiveRecording && layerId == recTargetLayerId) {
            recordedStrokesForTarget.add(stroke)
          }
        }
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
    }
  }

  val activeLayer = layers.find { it.id == activeLayerId }
  val recTargetLayer = layers.find { it.id == recTargetLayerId } ?: layers.firstOrNull()

  // Handle back button to dismiss panels
  BackHandler(
    enabled = isLayerPanelOpen || isBrushLibraryOpen || isColorWheelOpen || isToolsMenuOpen ||
      isPlaybackDialogOpen || isCanvasSizeDialogOpen || isImageTransformDialogOpen ||
      isAddTextDialogOpen || isAddShapeDialogOpen || isImageEditMode
  ) {
    if (isImageEditMode) isImageEditMode = false
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
      // 1. Sketchbook Top Toolbar
      SketchbookTopBar(
        canUndo = undoStack.isNotEmpty(),
        canRedo = redoStack.isNotEmpty(),
        currentColor = brushColor,
        isLayerPanelOpen = isLayerPanelOpen,
        onMenuClick = {
          undoStack.clear()
          redoStack.clear()
          recordedStrokesForTarget.clear()
          Toast.makeText(context, "হিস্ট্রি রিসেট করা হয়েছে", Toast.LENGTH_SHORT).show()
        },
        onUndoClick = performUndo,
        onRedoClick = performRedo,
        onSavePhotoClick = {
          // SAVE FULL ARTWORK AS PHOTO TO GALLERY (ফটোগ্রাফ সেভ)
          coroutineScope.launch {
            GalleryExporter.saveCanvasAsPhoto(
              context = context,
              layers = layers,
              paperWidth = currentCanvasSize.width.toInt(),
              paperHeight = currentCanvasSize.height.toInt()
            )
          }
        },
        onToolsClick = { isToolsMenuOpen = true },
        onBrushLibraryClick = { isBrushLibraryOpen = true },
        onColorWheelClick = { isColorWheelOpen = true },
        onAddImageClick = {
          photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        },
        onCanvasSizeClick = { isCanvasSizeDialogOpen = true },
        onLayersClick = { isLayerPanelOpen = !isLayerPanelOpen },
        onFitClick = {
          zoomScale = 0.65f
          panOffset = Offset(24f, 32f)
          canvasRotation = 0f
        }
      )

      // 2. Main Studio Canvas with Right-Side Layer Panel
      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        SketchbookCanvas(
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
          canUndo = undoStack.isNotEmpty(),
          canRedo = redoStack.isNotEmpty(),
          onUndoClick = performUndo,
          onRedoClick = performRedo,
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
          soloMode = soloMode,
          currentTime = 0f,
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
              undoStack.add(activeLayer.id to stroke)
              redoStack.clear()

              if (isLiveRecording && activeLayer.id == recTargetLayerId) {
                recordedStrokesForTarget.add(stroke)
              }
            }
          },
          onStrokeErased = { erasedStroke ->
            if (activeLayer != null) {
              undoStack.add(activeLayer.id to erasedStroke)
              redoStack.clear()
              if (isLiveRecording && activeLayer.id == recTargetLayerId) {
                recordedStrokesForTarget.remove(erasedStroke)
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
                isPlaybackDialogOpen = true
              }
            )
          }
        }

        // Right-Side Layer Panel
        if (isLayerPanelOpen) {
          Box(modifier = Modifier.align(Alignment.CenterEnd)) {
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
              onColorWheelClick = { isColorWheelOpen = true }
            )
          }
        }
      }

      // 3. Dedicated Bottom Studio Tool Dock (Pencil, Eraser, Move/Pan, Pen, Marker, Hand Color, Record)
      StudioToolDock(
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
            isPlaybackDialogOpen = true
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
        }
      )
    }

    // 4. Modals & Dialogs

    // Canvas Size Dialog (A4, A5, Square, Custom)
    if (isCanvasSizeDialogOpen) {
      CanvasSizeDialog(
        currentCanvasSize = currentCanvasSize,
        onSelectSize = { newSize ->
          currentCanvasSize = newSize
          zoomScale = (720f / newSize.height.coerceAtLeast(newSize.width)).coerceIn(0.2f, 1.2f)
          panOffset = Offset(24f, 32f)
          canvasRotation = 0f
          Toast.makeText(context, "ক্যানভাস সাইজ: ${newSize.name}", Toast.LENGTH_SHORT).show()
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
        },
        onClearClick = {
          activeLayer?.strokes?.clear()
          Toast.makeText(context, "অ্যাক্টিভ লেয়ার ক্লিয়ার করা হয়েছে", Toast.LENGTH_SHORT).show()
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
        }
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
            putExtra(Intent.EXTRA_TEXT, "Exported isolated layer recording of '${recTargetLayer?.name}' (${recordedStrokesForTarget.size} strokes) from LayerSketch Studio")
          }
          context.startActivity(Intent.createChooser(shareIntent, "Share Isolated Layer Drawing"))
        },
        onDismiss = {
          isPlaybackDialogOpen = false
        }
      )
    }
  }
}
