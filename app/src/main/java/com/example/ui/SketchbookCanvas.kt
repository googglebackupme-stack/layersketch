package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import com.example.model.DrawingToolType
import com.example.model.Layer
import com.example.model.LayerType
import com.example.model.PencilGrade
import com.example.model.PencilPalette
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
  val pRot = (screenPos - panOffset) / zoomScale

  if (rotation == 0f) {
    return pRot
  }

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
            layer.backgroundImageBitmap?.let { bgBmp ->
              drawScope.drawImage(
                image = bgBmp,
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(paperWidth.toInt(), paperHeight.toInt()),
                alpha = layerAlpha
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Realistic Artist Graphite Pencil Stroke Rendering matching Image 2 & Image 1:
 * 1. Soft graphite paper shadow / bevel under-layer (শ্যাডো ভাব)
 * 2. Medium graphite textured body
 * 3. Sharp dense core
 * 4. Natural grain tooth speckles along the path
 * 5. Special rendering for Technical Pen (0.3mm), Hair Brush Pen, Charcoal & Blending Stump
 */
fun drawSharpPencilStroke(drawScope: DrawScope, stroke: PencilStroke, parentAlpha: Float) {
  if (stroke.points.isEmpty()) return

  val effectiveAlpha = (stroke.opacity * parentAlpha).coerceIn(0f, 1f)
  val grade = PencilPalette.getGrade(stroke.pencilGrade)
  val ptCount = stroke.points.size

  if (ptCount == 1) {
    val pt = stroke.points[0]
    val r = (stroke.width * (0.6f + pt.pressure * 0.5f)) / 2f

    if (stroke.isEraser) {
      drawScope.drawCircle(
        color = stroke.color,
        radius = r.coerceAtLeast(0.8f),
        center = pt.offset,
        alpha = effectiveAlpha
      )
      return
    }

    when (grade.toolType) {
      DrawingToolType.TECH_PEN, DrawingToolType.HAIR_PEN, DrawingToolType.DIP_PEN -> {
        drawScope.drawCircle(
          color = stroke.color,
          radius = (stroke.width / 2f).coerceAtLeast(0.5f),
          center = pt.offset,
          alpha = effectiveAlpha
        )
      }
      DrawingToolType.HIGHLIGHT_PEN -> {
        // Bright luminous highlight dot with outer soft glow
        drawScope.drawCircle(
          color = Color.White.copy(alpha = (effectiveAlpha * 0.45f).coerceIn(0f, 1f)),
          radius = (stroke.width * 1.35f).coerceAtLeast(1.5f),
          center = pt.offset
        )
        drawScope.drawCircle(
          color = Color.White,
          radius = (stroke.width / 2f).coerceAtLeast(0.8f),
          center = pt.offset,
          alpha = effectiveAlpha
        )
      }
      DrawingToolType.WATERCOLOR -> {
        drawScope.drawCircle(
          color = stroke.color,
          radius = (stroke.width * 1.2f).coerceAtLeast(3f),
          center = pt.offset,
          alpha = (effectiveAlpha * 0.22f).coerceIn(0f, 1f)
        )
      }
      DrawingToolType.AIRBRUSH -> {
        drawScope.drawCircle(
          color = stroke.color,
          radius = (stroke.width * 1.5f).coerceAtLeast(4f),
          center = pt.offset,
          alpha = (effectiveAlpha * 0.15f).coerceIn(0f, 1f)
        )
      }
      DrawingToolType.BLENDER -> {
        drawScope.drawCircle(
          color = stroke.color.copy(alpha = 0.25f),
          radius = (stroke.width * 1.5f),
          center = pt.offset,
          alpha = (effectiveAlpha * 0.4f).coerceIn(0f, 1f)
        )
      }
      else -> {
        // 1. Soft Graphite Shadow under-dot
        drawScope.drawCircle(
          color = stroke.color,
          radius = (r * grade.shadowWidthMultiplier).coerceAtLeast(1.2f),
          center = pt.offset,
          alpha = (effectiveAlpha * grade.shadowAlphaMultiplier).coerceIn(0f, 1f)
        )
        // 2. Main Dot Body
        drawScope.drawCircle(
          color = stroke.color,
          radius = r.coerceAtLeast(0.8f),
          center = pt.offset,
          alpha = (effectiveAlpha * 0.85f).coerceIn(0f, 1f)
        )
        // 3. Dense Core Dot
        drawScope.drawCircle(
          color = stroke.color,
          radius = (r * 0.52f).coerceAtLeast(0.5f),
          center = pt.offset,
          alpha = (effectiveAlpha * grade.coreAlpha).coerceIn(0f, 1f)
        )
      }
    }
    return
  }

  val path = Path()
  val pts = stroke.points
  if (ptCount == 2) {
    path.moveTo(pts[0].offset.x, pts[0].offset.y)
    path.lineTo(pts[1].offset.x, pts[1].offset.y)
  } else {
    path.moveTo(pts[0].offset.x, pts[0].offset.y)
    for (i in 1 until ptCount - 1) {
      val p0 = pts[i].offset
      val p1 = pts[i + 1].offset
      val mid = Offset((p0.x + p1.x) / 2f, (p0.y + p1.y) / 2f)
      path.quadraticTo(p0.x, p0.y, mid.x, mid.y)
    }
    path.lineTo(pts.last().offset.x, pts.last().offset.y)
  }

  if (stroke.isEraser) {
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
    return
  }

  when (grade.toolType) {
    DrawingToolType.TECH_PEN -> {
      // 0.3mm Fine Technical Pen for clean crisp portrait contours (eyes, lips, signature)
      drawScope.drawPath(
        path = path,
        color = stroke.color,
        alpha = effectiveAlpha,
        style = Stroke(
          width = stroke.width.coerceAtLeast(0.8f),
          cap = StrokeCap.Round,
          join = StrokeJoin.Round
        )
      )
    }

    DrawingToolType.HAIR_PEN -> {
      // Ultra-fine 0.15mm hairline pen for individual hair strands and delicate eyelashes
      drawScope.drawPath(
        path = path,
        color = stroke.color,
        alpha = effectiveAlpha,
        style = Stroke(
          width = (stroke.width * 0.75f).coerceIn(0.5f, 2.5f),
          cap = StrokeCap.Round,
          join = StrokeJoin.Round
        )
      )
    }

    DrawingToolType.HIGHLIGHT_PEN -> {
      // White Gel Highlight Pen: Luminous white core with subtle outer glow for eyes and lips
      drawScope.drawPath(
        path = path,
        color = Color.White,
        alpha = (effectiveAlpha * 0.40f).coerceIn(0f, 1f),
        style = Stroke(
          width = (stroke.width * 1.55f).coerceAtLeast(2.0f),
          cap = StrokeCap.Round,
          join = StrokeJoin.Round
        )
      )
      drawScope.drawPath(
        path = path,
        color = Color.White,
        alpha = effectiveAlpha,
        style = Stroke(
          width = stroke.width.coerceAtLeast(1.0f),
          cap = StrokeCap.Round,
          join = StrokeJoin.Round
        )
      )
    }

    DrawingToolType.WATERCOLOR -> {
      // Soft translucent watercolor wash for blush and delicate skin shading
      drawScope.drawPath(
        path = path,
        color = stroke.color,
        alpha = (effectiveAlpha * 0.20f).coerceIn(0f, 1f),
        style = Stroke(
          width = (stroke.width * 1.6f).coerceAtLeast(6.0f),
          cap = StrokeCap.Round,
          join = StrokeJoin.Round
        )
      )
      drawScope.drawPath(
        path = path,
        color = stroke.color,
        alpha = (effectiveAlpha * 0.35f).coerceIn(0f, 1f),
        style = Stroke(
          width = stroke.width.coerceAtLeast(3.0f),
          cap = StrokeCap.Round,
          join = StrokeJoin.Round
        )
      )
    }

    DrawingToolType.AIRBRUSH -> {
      // Soft diffuse airbrush for flawless skin gradients
      drawScope.drawPath(
        path = path,
        color = stroke.color,
        alpha = (effectiveAlpha * 0.12f).coerceIn(0f, 1f),
        style = Stroke(
          width = (stroke.width * 2.0f).coerceAtLeast(10.0f),
          cap = StrokeCap.Round,
          join = StrokeJoin.Round
        )
      )
      drawScope.drawPath(
        path = path,
        color = stroke.color,
        alpha = (effectiveAlpha * 0.22f).coerceIn(0f, 1f),
        style = Stroke(
          width = (stroke.width * 1.2f).coerceAtLeast(5.0f),
          cap = StrokeCap.Round,
          join = StrokeJoin.Round
        )
      )
    }

    DrawingToolType.DIP_PEN -> {
      // Dynamic Spring G-Pen for expressive comic & portrait inking
      drawScope.drawPath(
        path = path,
        color = stroke.color,
        alpha = (effectiveAlpha * 0.25f).coerceIn(0f, 1f),
        style = Stroke(
          width = (stroke.width * 1.25f).coerceAtLeast(1.2f),
          cap = StrokeCap.Round,
          join = StrokeJoin.Round
        )
      )
      drawScope.drawPath(
        path = path,
        color = stroke.color,
        alpha = effectiveAlpha,
        style = Stroke(
          width = stroke.width.coerceAtLeast(0.9f),
          cap = StrokeCap.Round,
          join = StrokeJoin.Round
        )
      )
    }

    DrawingToolType.HATCHING -> {
      // Academic cross-hatch shading pencil
      drawScope.drawPath(
        path = path,
        color = stroke.color,
        alpha = (effectiveAlpha * 0.85f).coerceIn(0f, 1f),
        style = Stroke(
          width = stroke.width.coerceAtLeast(1.0f),
          cap = StrokeCap.Square,
          join = StrokeJoin.Miter
        )
      )
    }

    DrawingToolType.BLENDER -> {
      // Soft Blending Stump for smooth skin/shadow shading without harsh lines
      drawScope.drawPath(
        path = path,
        color = stroke.color,
        alpha = (effectiveAlpha * 0.28f).coerceIn(0f, 1f),
        style = Stroke(
          width = (stroke.width * 1.8f).coerceAtLeast(4.0f),
          cap = StrokeCap.Round,
          join = StrokeJoin.Round
        )
      )
      drawScope.drawPath(
        path = path,
        color = stroke.color,
        alpha = (effectiveAlpha * 0.45f).coerceIn(0f, 1f),
        style = Stroke(
          width = (stroke.width * 1.1f).coerceAtLeast(2.0f),
          cap = StrokeCap.Round,
          join = StrokeJoin.Round
        )
      )
    }

    DrawingToolType.BRUSH_PEN -> {
      // Dynamic calligraphy / hair brush pen
      drawScope.drawPath(
        path = path,
        color = stroke.color,
        alpha = (effectiveAlpha * 0.35f).coerceIn(0f, 1f),
        style = Stroke(
          width = (stroke.width * 1.35f).coerceAtLeast(1.5f),
          cap = StrokeCap.Round,
          join = StrokeJoin.Round
        )
      )
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

    DrawingToolType.MARKER -> {
      // Chisel Marker with broad flat edge
      drawScope.drawPath(
        path = path,
        color = stroke.color,
        alpha = (effectiveAlpha * 0.65f).coerceIn(0f, 1f),
        style = Stroke(
          width = stroke.width.coerceAtLeast(4.0f),
          cap = StrokeCap.Square,
          join = StrokeJoin.Bevel
        )
      )
    }

    DrawingToolType.PEN -> {
      // Solid clean inking pen
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

    DrawingToolType.CHARCOAL -> {
      // Deep matte velvety carbon charcoal with rich tooth & grain for hair shading
      val shadowWidth = (stroke.width * 1.55f).coerceAtLeast(2.0f)
      drawScope.drawPath(
        path = path,
        color = stroke.color,
        alpha = (effectiveAlpha * 0.35f).coerceIn(0f, 1f),
        style = Stroke(width = shadowWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
      )
      drawScope.drawPath(
        path = path,
        color = stroke.color,
        alpha = (effectiveAlpha * 0.88f).coerceIn(0f, 1f),
        style = Stroke(width = stroke.width.coerceAtLeast(1.2f), cap = StrokeCap.Round, join = StrokeJoin.Round)
      )
      drawScope.drawPath(
        path = path,
        color = stroke.color,
        alpha = (effectiveAlpha * 0.98f).coerceIn(0f, 1f),
        style = Stroke(width = (stroke.width * 0.65f).coerceAtLeast(0.9f), cap = StrokeCap.Round, join = StrokeJoin.Round)
      )
      // Charcoal grain dots along the stroke
      val step = (ptCount / 12).coerceAtLeast(2)
      for (i in 0 until ptCount step step) {
        val pt = stroke.points[i].offset
        val seed = ((pt.x * 37 + pt.y * 19).toInt() and 0x7FFFFFFF)
        val jx = ((seed % 100) / 100f - 0.5f) * stroke.width * 0.8f
        val jy = (((seed / 100) % 100) / 100f - 0.5f) * stroke.width * 0.8f
        drawScope.drawCircle(
          color = stroke.color,
          radius = (stroke.width * 0.18f).coerceIn(0.6f, 2.5f),
          center = Offset(pt.x + jx, pt.y + jy),
          alpha = (effectiveAlpha * 0.55f).coerceIn(0f, 1f)
        )
      }
    }

    else -> {
      // 1. Realistic Graphite Shadow Layer (বাস্তব পেন্সিলের মতো নরম শ্যাডো ভাব)
      val shadowWidth = (stroke.width * grade.shadowWidthMultiplier).coerceAtLeast(1.5f)
      drawScope.drawPath(
        path = path,
        color = stroke.color,
        alpha = (effectiveAlpha * grade.shadowAlphaMultiplier).coerceIn(0f, 1f),
        style = Stroke(
          width = shadowWidth,
          cap = StrokeCap.Round,
          join = StrokeJoin.Round
        )
      )

      // 2. Main Graphite Body (পেন্সিলের আসল বডি ও টেক্সচার)
      drawScope.drawPath(
        path = path,
        color = stroke.color,
        alpha = (effectiveAlpha * 0.82f).coerceIn(0f, 1f),
        style = Stroke(
          width = stroke.width.coerceAtLeast(1.0f),
          cap = StrokeCap.Round,
          join = StrokeJoin.Round
        )
      )

      // 3. Dense Sharp Core (শার্প গ্রাফাইট কোর)
      drawScope.drawPath(
        path = path,
        color = stroke.color,
        alpha = (effectiveAlpha * grade.coreAlpha).coerceIn(0f, 1f),
        style = Stroke(
          width = (stroke.width * 0.52f).coerceAtLeast(0.8f),
          cap = StrokeCap.Round,
          join = StrokeJoin.Round
        )
      )

      // 4. Graphite Paper Tooth / Grain Speckles along the stroke (দ্বিতীয় ছবির মতো পেন্সিলের টেক্সচার দাগ)
      val step = (ptCount / 14).coerceAtLeast(2)
      for (i in 0 until ptCount step step) {
        val pt = stroke.points[i].offset
        val seed = ((pt.x * 31 + pt.y * 17).toInt() and 0x7FFFFFFF)
        val jx = ((seed % 100) / 100f - 0.5f) * stroke.width * 0.65f
        val jy = (((seed / 100) % 100) / 100f - 0.5f) * stroke.width * 0.65f
        drawScope.drawCircle(
          color = stroke.color,
          radius = (stroke.width * 0.16f).coerceIn(0.5f, 1.8f),
          center = Offset(pt.x + jx, pt.y + jy),
          alpha = (effectiveAlpha * (0.35f + grade.grain * 0.35f)).coerceIn(0f, 1f)
        )
      }
    }
  }
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

enum class ImageDragMode {
  NONE,
  MOVE,
  RESIZE_CORNER,
  ROTATE
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
  onUndoLongClick: () -> Unit = {},
  onRedoLongClick: () -> Unit = {},
  isPanMode: Boolean,
  onTogglePanMode: (Boolean) -> Unit,
  isImageEditMode: Boolean,
  onExitImageEditMode: () -> Unit,
  onUpdateImageTransform: (offsetX: Float, offsetY: Float, scale: Float, rotation: Float) -> Unit,
  onOpenImageTransformDialog: () -> Unit,
  soloMode: Boolean,
  currentTime: Float,
  canvasRevision: Long = 0L,
  onStrokeCompleted: (PencilStroke) -> Unit,
  onStrokeErased: (PencilStroke) -> Unit,
  zoomScale: Float,
  panOffset: Offset,
  canvasRotation: Float,
  onTransformChange: (zoom: Float, pan: Offset, rotation: Float) -> Unit
) {
  val activeStrokePoints = remember { mutableStateListOf<StrokePoint>() }
  var liveStrokeStartTime by remember { mutableLongStateOf(0L) }
  val paperWidth = canvasSize.width
  val paperHeight = canvasSize.height

  Box(
    modifier = modifier
      .fillMaxSize()
      .clipToBounds()
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
          var prevCanvasPos = Offset.Zero
          var isMultiTouch = false
          var imageDragMode = ImageDragMode.NONE
          var strokeStartTimeMs = 0L
          var strokeStartPos = Offset.Zero
          var strokeMaxDist = 0f
          var lastRecordedPos = Offset.Zero

          while (true) {
            val event = awaitPointerEvent()
            val changes = event.changes

            if (changes.size >= 2) {
              isMultiTouch = true
              activeStrokePoints.clear()
              liveStrokeStartTime = 0L
              strokeStartTimeMs = 0L
              strokeStartPos = Offset.Zero
              strokeMaxDist = 0f
              lastRecordedPos = Offset.Zero

              val p1 = changes[0].position
              val p2 = changes[1].position
              val dx = p1.x - p2.x
              val dy = p1.y - p2.y
              val currentDist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
              val currentCentroid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)
              val currentAngle = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI).toFloat()

              if (isImageEditMode && activeLayer != null && activeLayer.type == LayerType.IMAGE) {
                // Two-finger pinch: Smooth scale & rotate photo
                if (prevDist > 10f) {
                  val scaleRatio = currentDist / prevDist
                  val newScale = (activeLayer.imageScale * scaleRatio).coerceIn(0.1f, 6.0f)
                  val angleDelta = currentAngle - prevAngle

                  // Center pan
                  val panDelta = (currentCentroid - prevCentroid) / zoomScale

                  onUpdateImageTransform(
                    activeLayer.imageOffsetX + panDelta.x,
                    activeLayer.imageOffsetY + panDelta.y,
                    newScale,
                    activeLayer.imageRotation + angleDelta
                  )
                }
              } else {
                // Pinpoint focal zoom & smooth rotation for canvas
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

              if (isImageEditMode && activeLayer != null && activeLayer.type == LayerType.IMAGE && activeLayer.imageBitmap != null) {
                // Single-touch photo manipulation: Corner drag resize, Rotation stick, or Move
                val canvasPos = screenToCanvas(
                  change.position,
                  panOffset,
                  zoomScale,
                  canvasRotation,
                  paperWidth,
                  paperHeight
                )

                val bmp = activeLayer.imageBitmap!!
                val origW = if (activeLayer.imageOriginalWidth > 0f) activeLayer.imageOriginalWidth else bmp.width.toFloat()
                val origH = if (activeLayer.imageOriginalHeight > 0f) activeLayer.imageOriginalHeight else bmp.height.toFloat()
                val baseFit = (paperWidth / origW).coerceAtMost(paperHeight / origH)
                val targetW = origW * baseFit * activeLayer.imageScale
                val targetH = origH * baseFit * activeLayer.imageScale
                val centerX = paperWidth / 2f + activeLayer.imageOffsetX
                val centerY = paperHeight / 2f + activeLayer.imageOffsetY

                if (change.pressed) {
                  if (prevSinglePos == Offset.Zero) {
                    // Decide drag mode based on touch start location
                    val dx = canvasPos.x - centerX
                    val dy = canvasPos.y - centerY
                    val rad = -Math.toRadians(activeLayer.imageRotation.toDouble())
                    val unrotX = (dx * cos(rad) - dy * sin(rad)).toFloat()
                    val unrotY = (dx * sin(rad) + dy * cos(rad)).toFloat()

                    val halfW = targetW / 2f
                    val halfH = targetH / 2f
                    val tolerance = (44f / zoomScale).coerceAtLeast(30f)

                    val isNearRotationHandle = kotlin.math.abs(unrotX) <= tolerance &&
                      kotlin.math.abs(unrotY - (-halfH - 45f / zoomScale)) <= tolerance

                    val isNearCorner = (kotlin.math.abs(kotlin.math.abs(unrotX) - halfW) <= tolerance &&
                      kotlin.math.abs(kotlin.math.abs(unrotY) - halfH) <= tolerance)

                    imageDragMode = if (isNearRotationHandle) {
                      ImageDragMode.ROTATE
                    } else if (isNearCorner) {
                      ImageDragMode.RESIZE_CORNER
                    } else {
                      ImageDragMode.MOVE
                    }
                  } else {
                    when (imageDragMode) {
                      ImageDragMode.RESIZE_CORNER -> {
                        // Smooth corner drag resizing!
                        val distFromCenter = sqrt(((canvasPos.x - centerX) * (canvasPos.x - centerX) + (canvasPos.y - centerY) * (canvasPos.y - centerY)).toDouble()).toFloat()
                        val baseHalfDiag = sqrt(((origW * baseFit / 2f) * (origW * baseFit / 2f) + (origH * baseFit / 2f) * (origH * baseFit / 2f)).toDouble()).toFloat()
                        if (baseHalfDiag > 1f) {
                          val newScale = (distFromCenter / baseHalfDiag).coerceIn(0.1f, 6.0f)
                          onUpdateImageTransform(activeLayer.imageOffsetX, activeLayer.imageOffsetY, newScale, activeLayer.imageRotation)
                        }
                      }
                      ImageDragMode.ROTATE -> {
                        // Smooth rotation handle dragging
                        val angleRad = atan2((canvasPos.y - centerY).toDouble(), (canvasPos.x - centerX).toDouble())
                        val deg = (angleRad * 180.0 / PI).toFloat() + 90f
                        onUpdateImageTransform(activeLayer.imageOffsetX, activeLayer.imageOffsetY, activeLayer.imageScale, deg)
                      }
                      ImageDragMode.MOVE -> {
                        // Smooth translation in canvas coordinates
                        val deltaX = canvasPos.x - prevCanvasPos.x
                        val deltaY = canvasPos.y - prevCanvasPos.y
                        onUpdateImageTransform(
                          activeLayer.imageOffsetX + deltaX,
                          activeLayer.imageOffsetY + deltaY,
                          activeLayer.imageScale,
                          activeLayer.imageRotation
                        )
                      }
                      ImageDragMode.NONE -> {}
                    }
                  }
                  prevSinglePos = change.position
                  prevCanvasPos = canvasPos
                } else {
                  prevSinglePos = Offset.Zero
                  prevCanvasPos = Offset.Zero
                  imageDragMode = ImageDragMode.NONE
                }
                change.consume()
              } else if (isPanMode) {
                // Pan Canvas Mode
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
                // Drawing / Erasing Mode with 100% accurate coordinate alignment
                val canvasPos = screenToCanvas(
                  change.position,
                  panOffset,
                  zoomScale,
                  canvasRotation,
                  paperWidth,
                  paperHeight
                )

                if (isEraser) {
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
                  val isStylus = change.type == PointerType.Stylus
                  val pressure = if (isStylus) {
                    change.pressure.coerceIn(0.1f, 1.0f)
                  } else {
                    0.5f
                  }

                  if (change.pressed) {
                    if (activeStrokePoints.isEmpty()) {
                      strokeStartTimeMs = System.currentTimeMillis()
                      liveStrokeStartTime = strokeStartTimeMs
                      strokeStartPos = canvasPos
                      strokeMaxDist = 0f
                      lastRecordedPos = canvasPos
                      activeStrokePoints.add(StrokePoint(canvasPos, pressure, strokeStartTimeMs))
                    } else {
                      val dx = canvasPos.x - lastRecordedPos.x
                      val dy = canvasPos.y - lastRecordedPos.y
                      val distFromLast = kotlin.math.hypot(dx, dy)
                      val distFromStart = kotlin.math.hypot(canvasPos.x - strokeStartPos.x, canvasPos.y - strokeStartPos.y)
                      if (distFromStart > strokeMaxDist) {
                        strokeMaxDist = distFromStart
                      }

                      // De-jitter threshold: only add point if finger moved sufficiently (>= 2.0px)
                      // Apply weighted moving average smoothing so curves are silky smooth without micro-jitter
                      if (distFromLast >= 2.0f) {
                        val smoothX = lastRecordedPos.x * 0.30f + canvasPos.x * 0.70f
                        val smoothY = lastRecordedPos.y * 0.30f + canvasPos.y * 0.70f
                        val smoothedPos = Offset(smoothX, smoothY)
                        activeStrokePoints.add(StrokePoint(smoothedPos, pressure, System.currentTimeMillis()))
                        lastRecordedPos = smoothedPos
                      }
                    }
                    change.consume()
                  } else {
                    // Finger lifted: evaluate if this was an accidental contact or a deliberate stroke
                    val durationMs = if (strokeStartTimeMs > 0L) System.currentTimeMillis() - strokeStartTimeMs else 0L

                    // অনাকাঙ্ক্ষিত ফোটা ও ছোট ছোট ডট পড়া সম্পূর্ণ বন্ধ করার ইন্টেলিজেন্ট ফিল্টার:
                    // ১. সাধারণ আঙুলের ক্ষণিক স্পর্শ (< 300ms) যাতে কোনো দাগ/ফোটা না পড়ে
                    // ২. ইচ্ছাকৃত ডট আঁকতে চাইলে আঙুল ধরে রাখতে হবে (>= 320ms) অথবা স্টাইলাস ব্যবহার করতে হবে
                    val isAccidentalTap = if (isStylus) {
                      activeStrokePoints.isEmpty()
                    } else {
                      (strokeMaxDist < 7.5f && durationMs < 300L) ||
                      (activeStrokePoints.size <= 2 && strokeMaxDist < 4.5f && durationMs < 450L) ||
                      (strokeMaxDist < 2.5f && durationMs < 600L)
                    }

                    if (!isAccidentalTap && activeStrokePoints.isNotEmpty()) {
                      val newStroke = PencilStroke(
                        points = activeStrokePoints.toList(),
                        color = brushColor,
                        width = brushSize * (activePencil.baseWidth / 4.0f),
                        opacity = brushOpacity * activePencil.baseOpacity,
                        pencilGrade = activePencil.code,
                        isEraser = false,
                        startTimeMs = strokeStartTimeMs
                      )
                      onStrokeCompleted(newStroke)
                    }
                    activeStrokePoints.clear()
                    liveStrokeStartTime = 0L
                    strokeStartTimeMs = 0L
                    strokeStartPos = Offset.Zero
                    strokeMaxDist = 0f
                    lastRecordedPos = Offset.Zero
                    change.consume()
                  }
                }
              }

              if (!change.pressed) {
                isMultiTouch = false
                prevDist = 0f
                prevCentroid = Offset.Zero
                prevSinglePos = Offset.Zero
                prevCanvasPos = Offset.Zero
                imageDragMode = ImageDragMode.NONE
                strokeStartTimeMs = 0L
                strokeStartPos = Offset.Zero
                strokeMaxDist = 0f
                lastRecordedPos = Offset.Zero
              }
            } else {
              isMultiTouch = false
              prevDist = 0f
              prevCentroid = Offset.Zero
              prevSinglePos = Offset.Zero
              prevCanvasPos = Offset.Zero
              imageDragMode = ImageDragMode.NONE
            }
          }
        }
      }
  ) {
    // 1. Canvas Paper Drawing Area
    Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
      // Observe revision counter so any undo/redo/stroke change invalidates canvas immediately
      val _revisionInvalidator = canvasRevision

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

            // Paper Base Color (রঙ)
            val bgLayer = layers.find { it.type == LayerType.BACKGROUND }
            val paperColor = bgLayer?.backgroundColor ?: Color.White
            drawRect(
              color = paperColor,
              topLeft = Offset(0f, 0f),
              size = Size(paperWidth, paperHeight)
            )

            // Paper Background Image (যদি ব্যাকগ্রাউন্ড ছবি নির্বাচন করা থাকে)
            bgLayer?.backgroundImageBitmap?.let { bgBmp ->
              drawImage(
                image = bgBmp,
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(paperWidth.toInt(), paperHeight.toInt())
              )
            }

            // Paper Border Outline
            drawRect(
              color = Color(0xFFD1D5DB),
              topLeft = Offset(0f, 0f),
              size = Size(paperWidth, paperHeight),
              style = Stroke(width = 1.5f)
            )

            // Canvas Bounds Clipping: Drawings and images outside canvas are cleanly clipped
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
                val liveDuration = if (liveStrokeStartTime > 0L) System.currentTimeMillis() - liveStrokeStartTime else 0L
                val shouldRenderLive = activeStrokePoints.size >= 2 || liveDuration >= 280L
                if (shouldRenderLive) {
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
            }

            // Visual Bounding Box with Corner Drag Handles and Rotation Stick
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
                // Bounding rect
                drawRect(
                  color = Color(0xFF007ACC),
                  topLeft = Offset(centerX - targetW / 2f, centerY - targetH / 2f),
                  size = Size(targetW, targetH),
                  style = Stroke(width = 2.5f / zoomScale)
                )

                // Rotation Stick & Handle at top
                val rotStickY = centerY - targetH / 2f - 40f / zoomScale
                drawLine(
                  color = Color(0xFF007ACC),
                  start = Offset(centerX, centerY - targetH / 2f),
                  end = Offset(centerX, rotStickY),
                  strokeWidth = 2f / zoomScale
                )
                drawCircle(color = Color(0xFF007ACC), radius = 10f / zoomScale, center = Offset(centerX, rotStickY))
                drawCircle(color = Color.White, radius = 6f / zoomScale, center = Offset(centerX, rotStickY))

                // 4 Interactive Corner Handles
                val corners = listOf(
                  Offset(centerX - targetW / 2f, centerY - targetH / 2f),
                  Offset(centerX + targetW / 2f, centerY - targetH / 2f),
                  Offset(centerX - targetW / 2f, centerY + targetH / 2f),
                  Offset(centerX + targetW / 2f, centerY + targetH / 2f)
                )
                corners.forEach { corner ->
                  drawCircle(color = Color(0xFF007ACC), radius = 10f / zoomScale, center = corner)
                  drawCircle(color = Color.White, radius = 7f / zoomScale, center = corner)
                  drawCircle(color = Color(0xFF007ACC), radius = 3.5f / zoomScale, center = corner)
                }

                // Center crosshair
                drawLine(
                  color = Color(0x88007ACC),
                  start = Offset(centerX - 10f / zoomScale, centerY),
                  end = Offset(centerX + 10f / zoomScale, centerY),
                  strokeWidth = 1.5f / zoomScale
                )
                drawLine(
                  color = Color(0x88007ACC),
                  start = Offset(centerX, centerY - 10f / zoomScale),
                  end = Offset(centerX, centerY + 10f / zoomScale),
                  strokeWidth = 1.5f / zoomScale
                )
              }
            }
          }
        }
      }
    }

    // 2. Persistent Floating Quick Undo / Redo / History on Canvas
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
          contentDescription = "Quick Undo",
          tint = if (canUndo) Color(0xFF0F172A) else Color(0xFFCBD5E1),
          modifier = Modifier.size(20.dp)
        )
      }

      Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color(0xFFE2E8F0)))

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
          contentDescription = "Quick Redo",
          tint = if (canRedo) Color(0xFF0F172A) else Color(0xFFCBD5E1),
          modifier = Modifier.size(20.dp)
        )
      }

      Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color(0xFFE2E8F0)))

      // Dedicated Multi-Undo History Menu
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .clickable(enabled = canUndo, onClick = onUndoLongClick),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Default.History,
          contentDescription = "Undo History",
          tint = if (canUndo) Color(0xFF0284C7) else Color(0xFFCBD5E1),
          modifier = Modifier.size(19.dp)
        )
      }
    }

    // 3. Hand Image Resize & Move Quick Action Bar (Floating at Top Center)
    if (isImageEditMode && activeLayer != null && activeLayer.type == LayerType.IMAGE) {
      Column(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 8.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(Color(0xF20F172A))
          .border(1.dp, Color(0xFF007ACC), RoundedCornerShape(16.dp))
          .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        // Status & Hint Row
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            "ছবি রিসাইজ: কোণ ধরে টানুন বা বাটনে চাপুন",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8)
          )
          Text(
            "${(activeLayer.imageScale * 100).toInt()}%",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            modifier = Modifier
              .background(Color(0xFF007ACC), RoundedCornerShape(4.dp))
              .padding(horizontal = 6.dp, vertical = 1.dp)
          )
        }

        // Quick Touch Buttons: Zoom In/Out, Rotate, Fit, Slider, Done
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Scale Down (- 10%)
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF334155))
              .clickable {
                val newS = (activeLayer.imageScale * 0.90f).coerceIn(0.1f, 6.0f)
                onUpdateImageTransform(activeLayer.imageOffsetX, activeLayer.imageOffsetY, newS, activeLayer.imageRotation)
              }
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
              Icon(Icons.Default.ZoomOut, contentDescription = "Smaller", tint = Color.White, modifier = Modifier.size(13.dp))
              Text("ছোট", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
          }

          // Scale Up (+ 10%)
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF334155))
              .clickable {
                val newS = (activeLayer.imageScale * 1.10f).coerceIn(0.1f, 6.0f)
                onUpdateImageTransform(activeLayer.imageOffsetX, activeLayer.imageOffsetY, newS, activeLayer.imageRotation)
              }
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
              Icon(Icons.Default.ZoomIn, contentDescription = "Bigger", tint = Color.White, modifier = Modifier.size(13.dp))
              Text("বড়", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
          }

          // Rotate 90°
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF334155))
              .clickable {
                val newRot = (activeLayer.imageRotation + 90f) % 360f
                onUpdateImageTransform(activeLayer.imageOffsetX, activeLayer.imageOffsetY, activeLayer.imageScale, newRot)
              }
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
              Icon(Icons.Default.RotateRight, contentDescription = "Rotate", tint = Color.White, modifier = Modifier.size(13.dp))
              Text("ঘোরান", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
          }

          // Center Image
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF334155))
              .clickable {
                onUpdateImageTransform(0f, 0f, activeLayer.imageScale, activeLayer.imageRotation)
              }
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
              Icon(Icons.Default.CenterFocusStrong, contentDescription = "Center", tint = Color.White, modifier = Modifier.size(13.dp))
              Text("মাঝে", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
          }

          // Detailed Sliders Dialog button
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF0284C7))
              .clickable(onClick = onOpenImageTransformDialog)
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
              Icon(Icons.Default.Tune, contentDescription = "Sliders", tint = Color.White, modifier = Modifier.size(13.dp))
              Text("স্লাইডার", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
          }

          // Done (সম্পন্ন)
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF16A34A))
              .clickable(onClick = onExitImageEditMode)
              .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
              Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(13.dp))
              Text("সম্পন্ন", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
          }
        }
      }
    }

    // 4. Floating Zoom & Rotation Controls (Far Top-Right Corner)
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
