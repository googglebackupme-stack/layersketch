package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CanvasSize
import com.example.model.Layer
import com.example.model.LayerType
import com.example.model.PencilGrade
import com.example.model.PencilStroke
import com.example.model.ShapeType
import com.example.model.StrokePoint
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class AnimatedTransform(
  val offsetX: Float,
  val offsetY: Float,
  val scale: Float,
  val rotation: Float,
  val opacity: Float
)

fun calculateLayerAnimatedTransform(layer: Layer, timeSeconds: Float): AnimatedTransform {
  if (layer.keyframes.isEmpty()) {
    return AnimatedTransform(layer.offsetX, layer.offsetY, layer.scale, layer.rotation, layer.opacity)
  }

  val sorted = layer.keyframes.sortedBy { it.time }
  if (timeSeconds <= sorted.first().time) {
    val kf = sorted.first()
    return AnimatedTransform(kf.offsetX, kf.offsetY, kf.scale, kf.rotation, kf.opacity * layer.opacity)
  }
  if (timeSeconds >= sorted.last().time) {
    val kf = sorted.last()
    return AnimatedTransform(kf.offsetX, kf.offsetY, kf.scale, kf.rotation, kf.opacity * layer.opacity)
  }

  for (i in 0 until sorted.size - 1) {
    val k1 = sorted[i]
    val k2 = sorted[i + 1]
    if (timeSeconds in k1.time..k2.time) {
      val duration = k2.time - k1.time
      val progress = if (duration > 0f) (timeSeconds - k1.time) / duration else 0f
      val t = (0.5f * (1f - cos(progress * PI))).toFloat()

      return AnimatedTransform(
        offsetX = k1.offsetX + (k2.offsetX - k1.offsetX) * t,
        offsetY = k1.offsetY + (k2.offsetY - k1.offsetY) * t,
        scale = k1.scale + (k2.scale - k1.scale) * t,
        rotation = k1.rotation + (k2.rotation - k1.rotation) * t,
        opacity = (k1.opacity + (k2.opacity - k1.opacity) * t) * layer.opacity
      )
    }
  }

  return AnimatedTransform(layer.offsetX, layer.offsetY, layer.scale, layer.rotation, layer.opacity)
}

/**
 * Transforms screen coordinate to canvas coordinate with 100% mathematical precision.
 * Guarantees zero pixel/millimeter offset at any zoom level or canvas rotation!
 */
fun screenToCanvas(
  screenPos: Offset,
  panOffset: Offset,
  zoomScale: Float,
  rotation: Float,
  paperWidth: Float,
  paperHeight: Float
): Offset {
  // 1. Invert translation and zoom scale (pivot = 0, 0)
  val pRot = (screenPos - panOffset) / zoomScale

  if (rotation == 0f) {
    return pRot
  }

  // 2. Invert rotation around canvas center
  val cx = paperWidth / 2f
  val cy = paperHeight / 2f
  val dx = pRot.x - cx
  val dy = pRot.y - cy
  val rad = -Math.toRadians(rotation.toDouble())
  val cosR = cos(rad).toFloat()
  val sinR = sin(rad).toFloat()
  val rx = dx * cosR - dy * sinR + cx
  val ry = dx * sinR + dy * cosR + cy
  return Offset(rx, ry)
}

/**
 * True Stroke Eraser: Erases/removes any stroke passing within eraser radius.
 * No white paint is drawn!
 */
fun eraseStrokesUnderPoint(
  layer: Layer,
  point: Offset,
  eraserRadius: Float,
  onStrokeErased: (PencilStroke) -> Unit
) {
  val r2 = eraserRadius * eraserRadius
  val iterator = layer.strokes.iterator()
  while (iterator.hasNext()) {
    val stroke = iterator.next()
    var hit = false
    for (pt in stroke.points) {
      val dx = pt.offset.x - point.x
      val dy = pt.offset.y - point.y
      if ((dx * dx + dy * dy) <= r2) {
        hit = true
        break
      }
    }
    if (hit) {
      onStrokeErased(stroke)
      iterator.remove()
    }
  }
}

/**
 * Renders EXACTLY ONE layer in complete isolation.
 */
fun drawSingleLayerIsolated(
  drawScope: DrawScope,
  layer: Layer,
  timeSeconds: Float,
  paperWidth: Float,
  paperHeight: Float
) {
  val anim = calculateLayerAnimatedTransform(layer, timeSeconds)

  drawScope.scale(anim.scale, Offset(paperWidth / 2f, paperHeight / 2f)) {
    drawScope.rotate(anim.rotation, Offset(paperWidth / 2f, paperHeight / 2f)) {
      drawScope.translate(anim.offsetX, anim.offsetY) {
        val layerAlpha = anim.opacity.coerceIn(0f, 1f)

        when (layer.type) {
          LayerType.DRAWING -> {
            for (stroke in layer.strokes) {
              drawSharpPencilStroke(drawScope, stroke, layerAlpha)
            }
          }
          LayerType.IMAGE -> {
            layer.imageBitmap?.let { bmp ->
              val origW = if (layer.imageOriginalWidth > 0f) layer.imageOriginalWidth else bmp.width.toFloat()
              val origH = if (layer.imageOriginalHeight > 0f) layer.imageOriginalHeight else bmp.height.toFloat()

              val baseFit = (paperWidth / origW).coerceAtMost(paperHeight / origH)
              val targetW = (origW * baseFit * layer.imageScale).coerceAtLeast(10f)
              val targetH = (origH * baseFit * layer.imageScale).coerceAtLeast(10f)

              val centerX = paperWidth / 2f + layer.imageOffsetX
              val centerY = paperHeight / 2f + layer.imageOffsetY

              drawScope.rotate(layer.imageRotation, Offset(centerX, centerY)) {
                drawScope.drawImage(
                  image = bmp,
                  dstOffset = IntOffset((centerX - targetW / 2f).toInt(), (centerY - targetH / 2f).toInt()),
                  dstSize = IntSize(targetW.toInt(), targetH.toInt()),
                  alpha = layerAlpha
                )
              }
            }
          }
          LayerType.SHAPE -> {
            drawShapeLayer(drawScope, layer, layerAlpha)
          }
          LayerType.TEXT -> {
            drawTextLayer(drawScope, layer, layerAlpha)
          }
          LayerType.BACKGROUND -> {
            drawScope.drawRect(
              color = layer.backgroundColor,
              size = Size(paperWidth, paperHeight),
              alpha = layerAlpha
            )
          }
        }
      }
    }
  }
}

/**
 * Sharp, authentic artist graphite pencil stroke rendering.
 */
fun drawSharpPencilStroke(drawScope: DrawScope, stroke: PencilStroke, parentAlpha: Float) {
  if (stroke.points.isEmpty()) return

  val effectiveAlpha = (stroke.opacity * parentAlpha).coerceIn(0f, 1f)

  if (stroke.points.size == 1) {
    val pt = stroke.points[0]
    val r = (stroke.width * (0.6f + pt.pressure * 0.5f)) / 2f
    drawScope.drawCircle(
      color = stroke.color,
      radius = r.coerceAtLeast(0.8f),
      center = pt.offset,
      alpha = effectiveAlpha
    )
    return
  }

  val path = Path()
  path.moveTo(stroke.points[0].offset.x, stroke.points[0].offset.y)

  for (i in 1 until stroke.points.size) {
    val p0 = stroke.points[i - 1].offset
    val p1 = stroke.points[i].offset
    val mid = Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
    path.quadraticTo(p0.x, p0.y, mid.x, mid.y)
  }

  drawScope.drawPath(
    path = path,
    color = stroke.color,
    alpha = effectiveAlpha,
    style = Stroke(
      width = stroke.width.coerceAtLeast(1.0f),
      cap = StrokeCap.Round,
      join = StrokeJoin.Round
    )
  )
}

fun drawShapeLayer(drawScope: DrawScope, layer: Layer, alpha: Float) {
  val rectSize = Size(layer.width, layer.height)
  when (layer.shapeType) {
    ShapeType.ROUNDED_RECT -> {
      drawScope.drawRoundRect(
        color = layer.shapeFill,
        size = rectSize,
        cornerRadius = CornerRadius(24f, 24f),
        alpha = alpha
      )
      drawScope.drawRoundRect(
        color = layer.shapeStroke,
        size = rectSize,
        cornerRadius = CornerRadius(24f, 24f),
        style = Stroke(width = 3f),
        alpha = alpha
      )
    }
    ShapeType.CIRCLE -> {
      drawScope.drawOval(
        color = layer.shapeFill,
        size = rectSize,
        alpha = alpha
      )
      drawScope.drawOval(
        color = layer.shapeStroke,
        size = rectSize,
        style = Stroke(width = 3f),
        alpha = alpha
      )
    }
    ShapeType.RECTANGLE, ShapeType.STAR, ShapeType.LINE -> {
      drawScope.drawRect(
        color = layer.shapeFill,
        size = rectSize,
        alpha = alpha
      )
      drawScope.drawRect(
        color = layer.shapeStroke,
        size = rectSize,
        style = Stroke(width = 3f),
        alpha = alpha
      )
    }
  }
}

fun drawTextLayer(drawScope: DrawScope, layer: Layer, alpha: Float) {
  drawScope.drawRoundRect(
    color = layer.textColor.copy(alpha = 0.08f * alpha),
    topLeft = Offset(0f, 0f),
    size = Size(layer.width, layer.height),
    cornerRadius = CornerRadius(12f, 12f)
  )
  drawScope.drawRoundRect(
    color = layer.textColor,
    topLeft = Offset(20f, 20f),
    size = Size(layer.width - 40f, layer.height - 40f),
    style = Stroke(width = 2f),
    cornerRadius = CornerRadius(8f, 8f),
    alpha = alpha * 0.4f
  )
}

@Composable
fun SketchbookCanvas(
  modifier: Modifier = Modifier,
  canvasSize: CanvasSize,
  layers: List<Layer>,
  activeLayer: Layer?,
  activePencil: PencilGrade,
  brushColor: Color,
  brushSize: Float,
  onUpdateBrushSize: (Float) -> Unit,
  brushOpacity: Float,
  onUpdateBrushOpacity: (Float) -> Unit,
  isEraser: Boolean,
  canUndo: Boolean,
  canRedo: Boolean,
  onUndoClick: () -> Unit,
  onRedoClick: () -> Unit,
  isPanMode: Boolean,
  onTogglePanMode: (Boolean) -> Unit,
  isImageEditMode: Boolean,
  onExitImageEditMode: () -> Unit,
  onUpdateImageTransform: (offsetX: Float, offsetY: Float, scale: Float, rotation: Float) -> Unit,
  soloMode: Boolean,
  currentTime: Float,
  onStrokeCompleted: (PencilStroke) -> Unit,
  onStrokeErased: (PencilStroke) -> Unit,
  zoomScale: Float,
  panOffset: Offset,
  canvasRotation: Float,
  onTransformChange: (zoom: Float, pan: Offset, rotation: Float) -> Unit
) {
  val activeStrokePoints = remember { mutableStateListOf<StrokePoint>() }
  val paperWidth = canvasSize.width
  val paperHeight = canvasSize.height

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF888E99)) // Studio sketchbook desk
      .pointerInput(
        activeLayer?.id,
        activePencil.code,
        brushColor,
        brushSize,
        brushOpacity,
        isEraser,
        isPanMode,
        isImageEditMode,
        zoomScale,
        panOffset,
        canvasRotation,
        paperWidth,
        paperHeight
      ) {
        awaitPointerEventScope {
          var prevDist = 0f
          var prevCentroid = Offset.Zero
          var prevAngle = 0f
          var prevSinglePos = Offset.Zero
          var isMultiTouch = false

          while (true) {
            val event = awaitPointerEvent()
            val changes = event.changes

            if (changes.size >= 2) {
              isMultiTouch = true
              val p1 = changes[0].position
              val p2 = changes[1].position
              val dx = p1.x - p2.x
              val dy = p1.y - p2.y
              val currentDist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
              val currentCentroid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
              val currentAngle = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI).toFloat()

              if (isImageEditMode && activeLayer != null && activeLayer.type == LayerType.IMAGE) {
                // HAND RESIZE IMAGE: 2 fingers scale & rotate photo
                if (prevDist > 10f) {
                  val scaleRatio = currentDist / prevDist
                  val newScale = (activeLayer.imageScale * scaleRatio).coerceIn(0.1f, 5.0f)
                  val angleDelta = currentAngle - prevAngle
                  onUpdateImageTransform(
                    activeLayer.imageOffsetX,
                    activeLayer.imageOffsetY,
                    newScale,
                    activeLayer.imageRotation + angleDelta
                  )
                }
              } else {
                // PINPOINT FOCAL ZOOM & SMOOTH HAND ROTATION
                if (prevDist > 10f && prevCentroid != Offset.Zero) {
                  val zoomRatio = currentDist / prevDist
                  val newZoom = (zoomScale * zoomRatio).coerceIn(0.10f, 15.0f)
                  val effectiveRatio = newZoom / zoomScale
                  val newPan = currentCentroid - (prevCentroid - panOffset) * effectiveRatio

                  val angleDelta = currentAngle - prevAngle
                  val newRot = canvasRotation + angleDelta

                  onTransformChange(newZoom, newPan, newRot)
                }
              }

              prevDist = currentDist
              prevCentroid = currentCentroid
              prevAngle = currentAngle

              activeStrokePoints.clear()
              changes.forEach { it.consume() }
            } else if (changes.size == 1) {
              val change = changes[0]

              if (isImageEditMode && activeLayer != null && activeLayer.type == LayerType.IMAGE) {
                // HAND MOVE IMAGE: 1 finger moves photo
                if (change.pressed) {
                  if (prevSinglePos != Offset.Zero) {
                    val deltaX = (change.position.x - prevSinglePos.x) / zoomScale
                    val deltaY = (change.position.y - prevSinglePos.y) / zoomScale
                    onUpdateImageTransform(
                      activeLayer.imageOffsetX + deltaX,
                      activeLayer.imageOffsetY + deltaY,
                      activeLayer.imageScale,
                      activeLayer.imageRotation
                    )
                  }
                  prevSinglePos = change.position
                } else {
                  prevSinglePos = Offset.Zero
                }
                change.consume()
              } else if (isPanMode) {
                // PAN CANVAS MODE
                if (change.pressed) {
                  if (prevSinglePos != Offset.Zero) {
                    val delta = change.position - prevSinglePos
                    onTransformChange(zoomScale, panOffset + delta, canvasRotation)
                  }
                  prevSinglePos = change.position
                } else {
                  prevSinglePos = Offset.Zero
                }
                change.consume()
              } else if (!isMultiTouch && activeLayer != null && !activeLayer.locked && activeLayer.visible) {
                // DRAWING OR ERASING MODE WITH 100% ACCURATE TOUCH ALIGNMENT!
                val canvasPos = screenToCanvas(
                  change.position,
                  panOffset,
                  zoomScale,
                  canvasRotation,
                  paperWidth,
                  paperHeight
                )

                if (isEraser) {
                  // TRUE STROKE ERASER (NO WHITE PAINT!)
                  if (change.pressed) {
                    eraseStrokesUnderPoint(
                      layer = activeLayer,
                      point = canvasPos,
                      eraserRadius = (brushSize * 2.2f).coerceAtLeast(10f),
                      onStrokeErased = onStrokeErased
                    )
                  }
                  change.consume()
                } else {
                  // DRAWING STROKE
                  val pressure = if (change.type == PointerType.Stylus) {
                    change.pressure.coerceIn(0.1f, 1.0f)
                  } else {
                    0.5f
                  }

                  if (change.pressed) {
                    activeStrokePoints.add(StrokePoint(canvasPos, pressure, System.currentTimeMillis()))
                    change.consume()
                  } else {
                    if (activeStrokePoints.isNotEmpty()) {
                      val newStroke = PencilStroke(
                        points = activeStrokePoints.toList(),
                        color = brushColor,
                        width = brushSize * (activePencil.baseWidth / 4.0f),
                        opacity = brushOpacity * activePencil.baseOpacity,
                        pencilGrade = activePencil.code,
                        isEraser = false,
                        startTimeMs = System.currentTimeMillis()
                      )
                      onStrokeCompleted(newStroke)
                      activeStrokePoints.clear()
                    }
                    change.consume()
                  }
                }
              }

              if (!change.pressed) {
                isMultiTouch = false
                prevDist = 0f
                prevCentroid = Offset.Zero
                prevSinglePos = Offset.Zero
              }
            } else {
              isMultiTouch = false
              prevDist = 0f
              prevCentroid = Offset.Zero
              prevSinglePos = Offset.Zero
            }
          }
        }
      }
  ) {
    // 1. Canvas Paper Drawing Area
    Canvas(modifier = Modifier.fillMaxSize()) {
      translate(panOffset.x, panOffset.y) {
        scale(zoomScale, pivot = Offset.Zero) {
          rotate(canvasRotation, pivot = Offset(paperWidth / 2f, paperHeight / 2f)) {
            // Paper Drop Shadow
            drawRoundRect(
              color = Color(0x33000000),
              topLeft = Offset(8f, 16f),
              size = Size(paperWidth, paperHeight),
              cornerRadius = CornerRadius(8f, 8f)
            )
            // Paper Base
            val bgLayer = layers.find { it.type == LayerType.BACKGROUND }
            val paperColor = bgLayer?.backgroundColor ?: Color.White
            drawRect(
              color = paperColor,
              topLeft = Offset(0f, 0f),
              size = Size(paperWidth, paperHeight)
            )
            // Paper Border Outline
            drawRect(
              color = Color(0xFFD1D5DB),
              topLeft = Offset(0f, 0f),
              size = Size(paperWidth, paperHeight),
              style = Stroke(width = 1.5f)
            )

            // STRICT CANVAS BOUNDS CLIPPING:
            // "আর ছবি ইনসার্ট করার সময় যেই ছবিটা ক্যানভাসে থাকবে, ক্যানভাসে বাকি অংশগুলো কেটে দিও ফাইনাল করার পর।"
            // Any image, drawing, text, or shape extending outside the canvas is cleanly clipped/cut off!
            clipRect(left = 0f, top = 0f, right = paperWidth, bottom = paperHeight) {
              if (soloMode && activeLayer != null) {
                drawSingleLayerIsolated(this, activeLayer, currentTime, paperWidth, paperHeight)
              } else {
                for (layer in layers.reversed()) {
                  if (!layer.visible) continue
                  drawSingleLayerIsolated(this, layer, currentTime, paperWidth, paperHeight)
                }
              }

              // Live drawing stroke (same transform space!)
              if (activeStrokePoints.isNotEmpty() && activeLayer != null && !isEraser) {
                val liveStroke = PencilStroke(
                  points = activeStrokePoints.toList(),
                  color = brushColor,
                  width = brushSize * (activePencil.baseWidth / 4.0f),
                  opacity = brushOpacity * activePencil.baseOpacity,
                  pencilGrade = activePencil.code,
                  isEraser = false
                )
                drawSharpPencilStroke(this, liveStroke, activeLayer.opacity)
              }
            }

            // Visual Bounding Box if Image Edit Mode is active
            if (isImageEditMode && activeLayer != null && activeLayer.type == LayerType.IMAGE && activeLayer.imageBitmap != null) {
              val bmp = activeLayer.imageBitmap!!
              val origW = if (activeLayer.imageOriginalWidth > 0f) activeLayer.imageOriginalWidth else bmp.width.toFloat()
              val origH = if (activeLayer.imageOriginalHeight > 0f) activeLayer.imageOriginalHeight else bmp.height.toFloat()
              val baseFit = (paperWidth / origW).coerceAtMost(paperHeight / origH)
              val targetW = origW * baseFit * activeLayer.imageScale
              val targetH = origH * baseFit * activeLayer.imageScale
              val centerX = paperWidth / 2f + activeLayer.imageOffsetX
              val centerY = paperHeight / 2f + activeLayer.imageOffsetY

              rotate(activeLayer.imageRotation, Offset(centerX, centerY)) {
                drawRect(
                  color = Color(0xFF007ACC),
                  topLeft = Offset(centerX - targetW / 2f, centerY - targetH / 2f),
                  size = Size(targetW, targetH),
                  style = Stroke(width = 2.5f / zoomScale)
                )
                listOf(
                  Offset(centerX - targetW / 2f, centerY - targetH / 2f),
                  Offset(centerX + targetW / 2f, centerY - targetH / 2f),
                  Offset(centerX - targetW / 2f, centerY + targetH / 2f),
                  Offset(centerX + targetW / 2f, centerY + targetH / 2f)
                ).forEach { corner ->
                  drawCircle(color = Color(0xFF007ACC), radius = 6f / zoomScale, center = corner)
                  drawCircle(color = Color.White, radius = 4f / zoomScale, center = corner)
                }
              }
            }
          }
        }
      }
    }

    // 2. Persistent Floating Quick Undo / Redo on Canvas (Available at ANY zoom level!)
    Row(
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(start = 12.dp, bottom = 12.dp)
        .clip(RoundedCornerShape(20.dp))
        .background(Color(0xF0FFFFFF))
        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(20.dp))
        .padding(horizontal = 4.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onUndoClick,
        enabled = canUndo,
        modifier = Modifier.size(36.dp)
      ) {
        Icon(
          Icons.AutoMirrored.Filled.Undo,
          contentDescription = "Quick Undo",
          tint = if (canUndo) Color(0xFF0F172A) else Color(0xFFCBD5E1),
          modifier = Modifier.size(20.dp)
        )
      }

      Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color(0xFFE2E8F0)))

      IconButton(
        onClick = onRedoClick,
        enabled = canRedo,
        modifier = Modifier.size(36.dp)
      ) {
        Icon(
          Icons.AutoMirrored.Filled.Redo,
          contentDescription = "Quick Redo",
          tint = if (canRedo) Color(0xFF0F172A) else Color(0xFFCBD5E1),
          modifier = Modifier.size(20.dp)
        )
      }
    }

    // 3. Hand Image Resize Active Banner
    if (isImageEditMode && activeLayer != null && activeLayer.type == LayerType.IMAGE) {
      Row(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 10.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(Color(0xF00F172A))
          .border(1.dp, Color(0xFF007ACC), RoundedCornerShape(20.dp))
          .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(Icons.Default.Transform, contentDescription = "Resize", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
        Text(
          "হাত দিয়ে ছবি সরান ও পিঞ্চ করে রিসাইজ করুন",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF16A34A))
            .clickable(onClick = onExitImageEditMode)
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(12.dp))
            Text("সম্পন্ন", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
          }
        }
      }
    }

    // 4. Clean Floating Zoom & Rotation Controls (Far Top-Right Corner)
    Row(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(top = 8.dp, end = 10.dp)
        .clip(RoundedCornerShape(20.dp))
        .background(Color(0xE6FFFFFF))
        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(20.dp))
        .padding(horizontal = 6.dp, vertical = 3.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      // Rotation Reset Button (Shown if rotated)
      if (kotlin.math.abs(canvasRotation) > 1f) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFEF3C7))
            .clickable { onTransformChange(zoomScale, panOffset, 0f) }
            .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(Icons.Default.Refresh, contentDescription = "Reset Rotation", tint = Color(0xFFB45309), modifier = Modifier.size(12.dp))
            Text("${canvasRotation.toInt()}°", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
          }
        }
      }

      // Zoom Out (-)
      Box(
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
          .clickable {
            val newZoom = (zoomScale / 1.35f).coerceIn(0.10f, 15.0f)
            onTransformChange(newZoom, panOffset, canvasRotation)
          },
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color(0xFF334155), modifier = Modifier.size(16.dp))
      }

      // Zoom Percentage
      Text(
        text = "${(zoomScale * 100).toInt()}%",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1E293B),
        modifier = Modifier
          .clickable {
            onTransformChange(1.0f, Offset(24f, 32f), 0f)
          }
          .padding(horizontal = 4.dp)
      )

      // Zoom In (+)
      Box(
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
          .clickable {
            val newZoom = (zoomScale * 1.35f).coerceIn(0.10f, 15.0f)
            onTransformChange(newZoom, panOffset, canvasRotation)
          },
        contentAlignment = Alignment.Center
      ) {
        Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color(0xFF334155), modifier = Modifier.size(16.dp))
      }

      // Fit Button (📐 ফিট)
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFFE2E8F0))
          .clickable {
            onTransformChange(0.65f, Offset(24f, 32f), 0f)
          }
          .padding(horizontal = 6.dp, vertical = 3.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
          Icon(Icons.Default.CropFree, contentDescription = "Fit", tint = Color(0xFF0F172A), modifier = Modifier.size(12.dp))
          Text("ফিট", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        }
      }

      // Move / Pan Hand Toggle Button (✋ মুভ)
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(if (isPanMode) Color(0xFF007ACC) else Color(0xFFF1F5F9))
          .clickable { onTogglePanMode(!isPanMode) }
          .padding(horizontal = 6.dp, vertical = 3.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
          Icon(
            Icons.Default.PanTool,
            contentDescription = "Pan Mode",
            tint = if (isPanMode) Color.White else Color(0xFF0F172A),
            modifier = Modifier.size(12.dp)
          )
          Text(
            if (isPanMode) "মুভিং" else "মুভ",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPanMode) Color.White else Color(0xFF0F172A)
          )
        }
      }
    }
  }
}
