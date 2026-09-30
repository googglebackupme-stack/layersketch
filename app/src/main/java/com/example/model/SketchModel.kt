package com.example.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import java.util.UUID

enum class LayerType {
  DRAWING,
  IMAGE,
  BACKGROUND,
  TEXT,
  SHAPE
}

enum class ShapeType {
  RECTANGLE,
  ROUNDED_RECT,
  CIRCLE,
  STAR,
  LINE
}

data class CanvasSize(
  val name: String,
  val width: Float,
  val height: Float,
  val description: String = ""
)

object CanvasPresets {
  val SQUARE = CanvasSize("Square (১:১)", 1080f, 1080f, "1080 × 1080 px")
  val A4_PORTRAIT = CanvasSize("A4 লম্বালম্বি (Portrait)", 1240f, 1754f, "1240 × 1754 px")
  val A4_LANDSCAPE = CanvasSize("A4 আড়াআড়ি (Landscape)", 1754f, 1240f, "1754 × 1240 px")
  val A5_PORTRAIT = CanvasSize("A5 লম্বালম্বি (Portrait)", 874f, 1240f, "874 × 1240 px")
  val A5_LANDSCAPE = CanvasSize("A5 আড়াআড়ি (Landscape)", 1240f, 874f, "1240 × 874 px")
  val STORY_HD = CanvasSize("মোবাইল / রিল (Story)", 1080f, 1920f, "1080 × 1920 px (9:16)")
  val YOUTUBE_HD = CanvasSize("ইউটিউব / ফুল এইচডি", 1920f, 1080f, "1920 × 1080 px (16:9)")

  val ALL = listOf(
    SQUARE,
    A4_PORTRAIT,
    A4_LANDSCAPE,
    A5_PORTRAIT,
    A5_LANDSCAPE,
    STORY_HD,
    YOUTUBE_HD
  )
}

data class PencilGrade(
  val code: String,
  val name: String,
  val hardness: String,
  val baseWidth: Float,
  val baseOpacity: Float,
  val graphiteColor: Color,
  val grain: Float,
  val isEraser: Boolean = false
)

object PencilPalette {
  val ERASER = PencilGrade(
    code = "ERASER",
    name = "Eraser (মুছনি)",
    hardness = "Eraser",
    baseWidth = 24.0f,
    baseOpacity = 1.0f,
    graphiteColor = Color.White,
    grain = 0f,
    isEraser = true
  )

  val PEN = PencilGrade(
    code = "PEN",
    name = "Inking Pen (কলম)",
    hardness = "Razor Ink",
    baseWidth = 3.2f,
    baseOpacity = 1.0f,
    graphiteColor = Color(0xFF0F172A),
    grain = 0f
  )

  val MARKER = PencilGrade(
    code = "MARKER",
    name = "Chisel Marker (মার্কার)",
    hardness = "Broad Chisel",
    baseWidth = 14.0f,
    baseOpacity = 0.55f,
    graphiteColor = Color(0xFF334155),
    grain = 0.08f
  )

  // Razor-sharp authentic graphite pencils: fine, textured, and elegant
  val PENCILS = listOf(
    PencilGrade("4H", "4H Pencil", "Extra Fine Hard", 1.4f, 0.32f, Color(0xFF6E7682), 0.05f),
    PencilGrade("3H", "3H Pencil", "Very Hard & Sharp", 1.7f, 0.38f, Color(0xFF606875), 0.08f),
    PencilGrade("2H", "2H Pencil", "Technical Hard", 2.0f, 0.45f, Color(0xFF525A68), 0.12f),
    PencilGrade("H",  "H Pencil",  "Medium Hard", 2.4f, 0.52f, Color(0xFF454E5B), 0.16f),
    PencilGrade("F",  "F Pencil",  "Fine Point", 2.8f, 0.60f, Color(0xFF3B4452), 0.20f),
    PencilGrade("HB", "HB Pencil", "Classic Drafting", 3.2f, 0.70f, Color(0xFF2E3643), 0.25f),
    PencilGrade("B",  "B Pencil",  "Soft Sketching", 3.6f, 0.77f, Color(0xFF242C38), 0.32f),
    PencilGrade("2B", "2B Pencil", "Artist Standard", 4.0f, 0.83f, Color(0xFF1B222C), 0.40f),
    PencilGrade("3B", "3B Pencil", "Rich Soft", 4.6f, 0.88f, Color(0xFF151C25), 0.48f),
    PencilGrade("4B", "4B Pencil", "Deep Graphite", 5.2f, 0.92f, Color(0xFF10151E), 0.56f),
    PencilGrade("5B", "5B Pencil", "Intense Dark", 5.8f, 0.94f, Color(0xFF0C1017), 0.64f),
    PencilGrade("6B", "6B Pencil", "Velvety Dark", 6.4f, 0.96f, Color(0xFF080B10), 0.72f),
    PencilGrade("7B", "7B Pencil", "Heavy Soft", 7.0f, 0.98f, Color(0xFF05070B), 0.80f),
    PencilGrade("8B", "8B Pencil", "Ebony Graphite", 7.6f, 0.99f, Color(0xFF030406), 0.88f),
    PencilGrade("9B", "9B Pencil", "Maximum Black", 8.2f, 1.00f, Color(0xFF010203), 0.96f)
  )

  fun getGrade(code: String): PencilGrade {
    if (code == "ERASER") return ERASER
    if (code == "PEN") return PEN
    if (code == "MARKER") return MARKER
    return PENCILS.find { it.code == code } ?: PENCILS[7] // default 2B
  }
}

data class StrokePoint(
  val offset: Offset,
  val pressure: Float = 0.5f,
  val timestampMs: Long = 0L
)

data class PencilStroke(
  val points: List<StrokePoint>,
  val color: Color,
  val width: Float,
  val opacity: Float,
  val pencilGrade: String,
  val isEraser: Boolean = false,
  val startTimeMs: Long = 0L
)

data class Keyframe(
  val time: Float,
  val offsetX: Float,
  val offsetY: Float,
  val scale: Float = 1f,
  val rotation: Float = 0f,
  val opacity: Float = 1f
)

data class Layer(
  val id: String = UUID.randomUUID().toString(),
  var name: String,
  var type: LayerType = LayerType.DRAWING,
  var visible: Boolean = true,
  var locked: Boolean = false,
  var opacity: Float = 1.0f,
  var isRecTarget: Boolean = false,
  val strokes: MutableList<PencilStroke> = mutableListOf(),

  // Image Layer Specific: Original Dimensions & Interactive Transform
  var imageBitmap: ImageBitmap? = null,
  var imageOriginalWidth: Float = 0f,
  var imageOriginalHeight: Float = 0f,
  var imageOffsetX: Float = 0f,
  var imageOffsetY: Float = 0f,
  var imageScale: Float = 1.0f,
  var imageRotation: Float = 0f,

  // Text Layer Specific
  var text: String = "",
  var textColor: Color = Color(0xFF1E293B),
  var textSize: Float = 44f,

  // Shape Layer Specific
  var shapeType: ShapeType = ShapeType.RECTANGLE,
  var shapeFill: Color = Color(0xFF3B82F6),
  var shapeStroke: Color = Color(0xFF1E293B),

  // Background Layer Specific
  var backgroundColor: Color = Color.White,

  // Global Layer Transform
  var offsetX: Float = 0f,
  var offsetY: Float = 0f,
  var width: Float = 1080f,
  var height: Float = 1080f,
  var scale: Float = 1f,
  var rotation: Float = 0f,
  val keyframes: MutableList<Keyframe> = mutableListOf()
)
