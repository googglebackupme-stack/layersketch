package com.example.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
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

data class PaperColorPreset(
  val name: String,
  val color: Color,
  val description: String
)

object PaperColorPresets {
  val PURE_WHITE = PaperColorPreset("সাদা (White)", Color(0xFFFFFFFF), "ক্রিস্প আর্ট পেপার")
  val WARM_IVORY = PaperColorPreset("আইভরি পেপার (Ivory)", Color(0xFFFDFBF7), "ন্যাচারাল ড্রয়িং পেপার")
  val SKETCH_CREAM = PaperColorPreset("স্কেচ ক্রিম (Cream)", Color(0xFFF9F6EE), "ড্রয়িং পেপার টোন")
  val KRAFT_PAPER = PaperColorPreset("ক্রাফট পেপার (Kraft)", Color(0xFFE8D8C3), "ভিন্টেজ খাকি আর্ট শিট")
  val WARM_PARCHMENT = PaperColorPreset("পার্চমেন্ট (Parchment)", Color(0xFFF4EBD9), "অ্যান্টিক পার্চমেন্ট")
  val COOL_GRAY = PaperColorPreset("ড্রাফটিং গ্রে (Cool Gray)", Color(0xFFE2E8F0), "প্রফেশনাল ড্রাফটিং শিট")
  val SLATE_DARK = PaperColorPreset("ডার্ক স্লেট (Slate Dark)", Color(0xFF1E293B), "ডার্ক স্কেচিং বোর্ড")
  val CHARCOAL_BLACK = PaperColorPreset("চারকোল (Charcoal)", Color(0xFF121212), "ব্ল্যাক আর্ট বোর্ড")

  val ALL = listOf(
    PURE_WHITE,
    WARM_IVORY,
    SKETCH_CREAM,
    KRAFT_PAPER,
    WARM_PARCHMENT,
    COOL_GRAY,
    SLATE_DARK,
    CHARCOAL_BLACK
  )
}

enum class DrawingToolType {
  PENCIL,
  TECH_PEN,
  HAIR_PEN,
  BRUSH_PEN,
  CHARCOAL,
  BLENDER,
  WATERCOLOR,
  HIGHLIGHT_PEN,
  AIRBRUSH,
  DIP_PEN,
  HATCHING,
  MARKER,
  PEN,
  ERASER
}

data class PencilGrade(
  val code: String,
  val name: String,
  val hardness: String,
  val baseWidth: Float,
  val baseOpacity: Float,
  val graphiteColor: Color,
  val grain: Float,
  val shadowWidthMultiplier: Float = 1.35f,
  val shadowAlphaMultiplier: Float = 0.22f,
  val coreAlpha: Float = 0.95f,
  val isEraser: Boolean = false,
  val toolType: DrawingToolType = DrawingToolType.PENCIL
)

object PencilPalette {
  val ERASER = PencilGrade(
    code = "ERASER",
    name = "Eraser (মুছনি)",
    hardness = "Eraser",
    baseWidth = 22.0f,
    baseOpacity = 1.0f,
    graphiteColor = Color.White,
    grain = 0f,
    shadowWidthMultiplier = 1.0f,
    shadowAlphaMultiplier = 0f,
    isEraser = true,
    toolType = DrawingToolType.ERASER
  )

  val KNEADED_ERASER = PencilGrade(
    code = "KNEADED",
    name = "Kneaded Eraser (হাইলাইট ইরেজার)",
    hardness = "Precision Highlight",
    baseWidth = 8.0f,
    baseOpacity = 0.75f,
    graphiteColor = Color.White,
    grain = 0f,
    shadowWidthMultiplier = 1.0f,
    shadowAlphaMultiplier = 0f,
    isEraser = true,
    toolType = DrawingToolType.ERASER
  )

  // 1. Technical Inking Pen (০.৩ মিমি ফাইন কলম) - Perfect for eyes, lips, and fine portrait contours
  val TECH_PEN = PencilGrade(
    code = "TECH_PEN",
    name = "Technical Pen (০.৩ মিমি ফাইন কলম)",
    hardness = "0.3mm Fine Nib",
    baseWidth = 2.0f,
    baseOpacity = 1.0f,
    graphiteColor = Color(0xFF0F172A),
    grain = 0f,
    shadowWidthMultiplier = 1.0f,
    shadowAlphaMultiplier = 0f,
    coreAlpha = 1.0f,
    toolType = DrawingToolType.TECH_PEN
  )

  // 2. Hair & Fine Lash Pen (০.১৫ মিমি হেয়ার ও ল্যাশ পেন) - For single hair strands and lashes in portrait
  val HAIR_PEN = PencilGrade(
    code = "HAIR_PEN",
    name = "Hair & Lash Pen (হেয়ার ও ল্যাশ পেন ০.১৫ মিমি)",
    hardness = "0.15mm Micro Hairline",
    baseWidth = 1.2f,
    baseOpacity = 1.0f,
    graphiteColor = Color(0xFF090D14),
    grain = 0f,
    shadowWidthMultiplier = 1.05f,
    shadowAlphaMultiplier = 0.08f,
    coreAlpha = 1.0f,
    toolType = DrawingToolType.HAIR_PEN
  )

  // 3. Calligraphy / Hair Brush Pen (ব্রাশ পেন) - Dynamic tapered flowing lines for hair locks
  val BRUSH_PEN = PencilGrade(
    code = "BRUSH_PEN",
    name = "Hair Brush Pen (ব্রাশ পেন)",
    hardness = "Flexible Tapered",
    baseWidth = 5.5f,
    baseOpacity = 0.95f,
    graphiteColor = Color(0xFF111827),
    grain = 0.05f,
    shadowWidthMultiplier = 1.15f,
    shadowAlphaMultiplier = 0.15f,
    coreAlpha = 0.98f,
    toolType = DrawingToolType.BRUSH_PEN
  )

  // 4. Charcoal Pencil (চারকোল পেন্সিল) - Deep velvety dark carbon with rich tooth for hair shading
  val CHARCOAL = PencilGrade(
    code = "CHARCOAL",
    name = "Charcoal Pencil (চারকোল পেন্সিল)",
    hardness = "Matte Carbon Black",
    baseWidth = 6.2f,
    baseOpacity = 0.98f,
    graphiteColor = Color(0xFF05070A),
    grain = 0.55f,
    shadowWidthMultiplier = 1.45f,
    shadowAlphaMultiplier = 0.32f,
    coreAlpha = 1.0f,
    toolType = DrawingToolType.CHARCOAL
  )

  // 5. Blending Stump (ব্লেন্ডিং স্টাম্প / শ্যাডো) - Soft graphite blending for face and neck shadows
  val BLENDER = PencilGrade(
    code = "BLENDER",
    name = "Blending Stump (ব্লেন্ডিং স্টাম্প)",
    hardness = "Soft Paper Tortillon",
    baseWidth = 14.0f,
    baseOpacity = 0.35f,
    graphiteColor = Color(0xFF475569),
    grain = 0.12f,
    shadowWidthMultiplier = 1.6f,
    shadowAlphaMultiplier = 0.45f,
    coreAlpha = 0.5f,
    toolType = DrawingToolType.BLENDER
  )

  // 6. Watercolor Wash Brush (ওয়াটারকালার সফট ব্রাশ) - For skin blush, warm tones, and soft washes
  val WATERCOLOR = PencilGrade(
    code = "WATERCOLOR",
    name = "Watercolor Brush (ওয়াটারকালার ব্রাশ)",
    hardness = "Soft Translucent Wash",
    baseWidth = 24.0f,
    baseOpacity = 0.25f,
    graphiteColor = Color(0xFFE11D48),
    grain = 0.08f,
    shadowWidthMultiplier = 1.6f,
    shadowAlphaMultiplier = 0.30f,
    coreAlpha = 0.55f,
    toolType = DrawingToolType.WATERCOLOR
  )

  // 7. White Gel Highlight Pen (হোয়াইট জেল পেন) - Catchlights in eyes, lip sheen, and hair shine
  val HIGHLIGHT_PEN = PencilGrade(
    code = "HIGHLIGHT_PEN",
    name = "White Gel Pen (চোখ ও ঠোঁটের হাইলাইট পেন)",
    hardness = "Opaque Gloss Highlight",
    baseWidth = 2.4f,
    baseOpacity = 1.0f,
    graphiteColor = Color(0xFFFFFFFF),
    grain = 0f,
    shadowWidthMultiplier = 1.35f,
    shadowAlphaMultiplier = 0.45f,
    coreAlpha = 1.0f,
    toolType = DrawingToolType.HIGHLIGHT_PEN
  )

  // 8. Soft Airbrush (সফট এয়ারব্রাশ) - Ultra-smooth skin tone gradient & portrait contouring
  val AIRBRUSH = PencilGrade(
    code = "AIRBRUSH",
    name = "Soft Airbrush (স্কিন এয়ারব্রাশ)",
    hardness = "Diffuse Gradient",
    baseWidth = 32.0f,
    baseOpacity = 0.20f,
    graphiteColor = Color(0xFF334155),
    grain = 0.05f,
    shadowWidthMultiplier = 1.8f,
    shadowAlphaMultiplier = 0.25f,
    coreAlpha = 0.45f,
    toolType = DrawingToolType.AIRBRUSH
  )

  // 9. Manga Dip Pen / G-Pen (জি-পেন / ডিপ কলম) - Expressive pressure lineart
  val DIP_PEN = PencilGrade(
    code = "DIP_PEN",
    name = "G-Pen / Dip Pen (জি-পেন / ডিপ কলম)",
    hardness = "Dynamic Spring Nib",
    baseWidth = 3.6f,
    baseOpacity = 1.0f,
    graphiteColor = Color(0xFF0F172A),
    grain = 0f,
    shadowWidthMultiplier = 1.1f,
    shadowAlphaMultiplier = 0.10f,
    coreAlpha = 1.0f,
    toolType = DrawingToolType.DIP_PEN
  )

  // 10. Cross-Hatching Shading Pencil (হ্যাচিং শেডিং পেন্সিল)
  val HATCHING = PencilGrade(
    code = "HATCHING",
    name = "Hatching Pencil (হ্যাচিং শেডিং পেন্সিল)",
    hardness = "Academic Cross-Hatch",
    baseWidth = 2.2f,
    baseOpacity = 0.70f,
    graphiteColor = Color(0xFF1E293B),
    grain = 0.25f,
    shadowWidthMultiplier = 1.25f,
    shadowAlphaMultiplier = 0.16f,
    coreAlpha = 0.95f,
    toolType = DrawingToolType.HATCHING
  )

  val PEN = PencilGrade(
    code = "PEN",
    name = "Inking Pen (কলম)",
    hardness = "Razor Ink",
    baseWidth = 3.2f,
    baseOpacity = 1.0f,
    graphiteColor = Color(0xFF0F172A),
    grain = 0f,
    shadowWidthMultiplier = 1.15f,
    shadowAlphaMultiplier = 0.12f,
    coreAlpha = 1.0f,
    toolType = DrawingToolType.PEN
  )

  val MARKER = PencilGrade(
    code = "MARKER",
    name = "Chisel Marker (মার্কার)",
    hardness = "Broad Chisel",
    baseWidth = 14.0f,
    baseOpacity = 0.55f,
    graphiteColor = Color(0xFF334155),
    grain = 0.08f,
    shadowWidthMultiplier = 1.2f,
    shadowAlphaMultiplier = 0.18f,
    coreAlpha = 0.85f,
    toolType = DrawingToolType.MARKER
  )

  // Razor-sharp authentic graphite pencils: with soft graphite shadow, dense core & real grain matching Image 2
  val PENCILS = listOf(
    PencilGrade("4H", "4H Pencil", "Extra Fine Hard", 1.4f, 0.32f, Color(0xFF6E7682), 0.05f, 1.22f, 0.12f, 0.85f),
    PencilGrade("3H", "3H Pencil", "Very Hard & Sharp", 1.7f, 0.38f, Color(0xFF606875), 0.08f, 1.25f, 0.14f, 0.88f),
    PencilGrade("2H", "2H Pencil", "Technical Hard", 2.0f, 0.45f, Color(0xFF525A68), 0.12f, 1.28f, 0.16f, 0.90f),
    PencilGrade("H",  "H Pencil",  "Medium Hard", 2.4f, 0.52f, Color(0xFF454E5B), 0.16f, 1.30f, 0.18f, 0.92f),
    PencilGrade("F",  "F Pencil",  "Fine Point", 2.8f, 0.60f, Color(0xFF3B4452), 0.20f, 1.32f, 0.20f, 0.94f),
    PencilGrade("HB", "HB Pencil", "Classic Drafting", 3.2f, 0.70f, Color(0xFF2E3643), 0.25f, 1.35f, 0.22f, 0.95f),
    PencilGrade("B",  "B Pencil",  "Soft Sketching", 3.6f, 0.77f, Color(0xFF242C38), 0.32f, 1.40f, 0.25f, 0.96f),
    PencilGrade("2B", "2B Pencil", "Artist Standard", 4.0f, 0.83f, Color(0xFF1B222C), 0.40f, 1.44f, 0.28f, 0.97f),
    PencilGrade("3B", "3B Pencil", "Rich Soft", 4.6f, 0.88f, Color(0xFF151C25), 0.48f, 1.48f, 0.30f, 0.98f),
    PencilGrade("4B", "4B Pencil", "Deep Graphite", 5.2f, 0.92f, Color(0xFF10151E), 0.56f, 1.52f, 0.32f, 0.99f),
    PencilGrade("5B", "5B Pencil", "Intense Dark", 5.8f, 0.94f, Color(0xFF0C1017), 0.64f, 1.56f, 0.34f, 1.00f),
    PencilGrade("6B", "6B Pencil", "Velvety Dark", 6.4f, 0.96f, Color(0xFF080B10), 0.72f, 1.60f, 0.36f, 1.00f),
    PencilGrade("7B", "7B Pencil", "Heavy Soft", 7.0f, 0.98f, Color(0xFF05070B), 0.80f, 1.64f, 0.38f, 1.00f),
    PencilGrade("8B", "8B Pencil", "Ebony Graphite", 7.6f, 0.99f, Color(0xFF030406), 0.88f, 1.68f, 0.40f, 1.00f),
    PencilGrade("9B", "9B Pencil", "Maximum Black", 8.2f, 1.00f, Color(0xFF010203), 0.96f, 1.72f, 0.42f, 1.00f)
  )

  fun getGrade(code: String): PencilGrade {
    if (code == "ERASER") return ERASER
    if (code == "KNEADED") return KNEADED_ERASER
    if (code == "TECH_PEN") return TECH_PEN
    if (code == "HAIR_PEN") return HAIR_PEN
    if (code == "BRUSH_PEN") return BRUSH_PEN
    if (code == "CHARCOAL") return CHARCOAL
    if (code == "BLENDER") return BLENDER
    if (code == "WATERCOLOR") return WATERCOLOR
    if (code == "HIGHLIGHT_PEN") return HIGHLIGHT_PEN
    if (code == "AIRBRUSH") return AIRBRUSH
    if (code == "DIP_PEN") return DIP_PEN
    if (code == "HATCHING") return HATCHING
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
  val id: String = UUID.randomUUID().toString(),
  val points: List<StrokePoint>,
  val color: Color,
  val width: Float,
  val opacity: Float,
  val pencilGrade: String,
  val isEraser: Boolean = false,
  val startTimeMs: Long = 0L
)

data class UndoActionItem(
  val layerId: String,
  val stroke: PencilStroke,
  val isErased: Boolean = false
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
  val strokes: SnapshotStateList<PencilStroke> = mutableStateListOf(),

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
  var backgroundImageBitmap: ImageBitmap? = null,
  var backgroundImageUri: String? = null,

  // Global Layer Transform
  var offsetX: Float = 0f,
  var offsetY: Float = 0f,
  var width: Float = 1080f,
  var height: Float = 1080f,
  var scale: Float = 1f,
  var rotation: Float = 0f,
  val keyframes: MutableList<Keyframe> = mutableListOf()
)
