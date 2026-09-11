package ir.binercraft.binerlauncher.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.KeyEvent
import ir.binercraft.binerlauncher.nativebridge.NativeBridge
import org.json.JSONArray

private data class RuntimeHudKey(
    val code: Int,
    val label: String,
    var x: Float,
    var y: Float,
    var w: Float,
    var h: Float,
    var opacity: Float
)

/**
 * Runtime touch HUD. It uses the exact Android KeyEvent codes stored by the HUD editor,
 * so the same layout can be used by the game screen and the editor.
 */
class TouchHudView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val prefs = context.getSharedPreferences("biner_hud", Context.MODE_PRIVATE)
    private val keys = mutableListOf<RuntimeHudKey>()
    private val held = HashSet<Int>()

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        load()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (key in keys) {
            val down = held.contains(key.code)
            paint.color = Color.argb(
                ((if (down) key.opacity + .12f else key.opacity).coerceIn(.15f, 1f) * 255).toInt(),
                28, 35, 52
            )
            canvas.drawRoundRect(RectF(key.x, key.y, key.x + key.w, key.y + key.h), 14f, 14f, paint)
            paint.color = Color.argb(210, 255, 255, 255)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = if (down) 3f else 1.5f
            canvas.drawRoundRect(RectF(key.x, key.y, key.x + key.w, key.y + key.h), 14f, 14f, paint)
            paint.style = Paint.Style.FILL
            paint.textSize = if (key.label.length > 6) 12f else 18f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(key.label, key.x + key.w / 2f, key.y + key.h / 2f + 6f, paint)
        }
        paint.textAlign = Paint.Align.LEFT
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointer = event.actionIndex
                pressAt(event.getX(pointer), event.getY(pointer))
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val pointer = event.actionIndex.coerceAtMost(event.pointerCount - 1)
                releaseAt(event.getX(pointer), event.getY(pointer))
                return true
            }
        }
        return true
    }

    private fun pressAt(x: Float, y: Float) {
        val key = keys.lastOrNull { x in it.x..(it.x + it.w) && y in it.y..(it.y + it.h) } ?: return
        if (!held.add(key.code)) return
        when (key.code) {
            MOUSE_LEFT -> NativeBridge.mouseDown(0)
            MOUSE_RIGHT -> NativeBridge.mouseDown(1)
            MOUSE_MIDDLE -> NativeBridge.mouseDown(2)
            SCROLL_UP -> NativeBridge.scroll(1f)
            SCROLL_DOWN -> NativeBridge.scroll(-1f)
            else -> NativeBridge.keyDown(key.code)
        }
        invalidate()
    }

    private fun releaseAt(x: Float, y: Float) {
        val key = keys.lastOrNull { x in it.x..(it.x + it.w) && y in it.y..(it.y + it.h) } ?: return
        if (!held.remove(key.code)) return
        when (key.code) {
            MOUSE_LEFT -> NativeBridge.mouseUp(0)
            MOUSE_RIGHT -> NativeBridge.mouseUp(1)
            MOUSE_MIDDLE -> NativeBridge.mouseUp(2)
            SCROLL_UP, SCROLL_DOWN -> Unit
            else -> NativeBridge.keyUp(key.code)
        }
        invalidate()
    }

    private fun load() {
        val raw = prefs.getString("layout", null)
        if (!raw.isNullOrBlank()) runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                keys += RuntimeHudKey(
                    o.getInt("code"), o.getString("label"),
                    o.getDouble("x").toFloat(), o.getDouble("y").toFloat(),
                    o.getDouble("w").toFloat(), o.getDouble("h").toFloat(),
                    o.optDouble("opacity", .78).toFloat()
                )
            }
        }
        if (keys.isEmpty()) addDefaultLayout()
    }

    private fun addDefaultLayout() {
        val bottom = 0.72f * resources.displayMetrics.heightPixels
        keys += RuntimeHudKey(KeyEvent.KEYCODE_W, "W", 120f, bottom - 140f, 70f, 58f, .70f)
        keys += RuntimeHudKey(KeyEvent.KEYCODE_A, "A", 42f, bottom - 70f, 70f, 58f, .70f)
        keys += RuntimeHudKey(KeyEvent.KEYCODE_S, "S", 120f, bottom - 70f, 70f, 58f, .70f)
        keys += RuntimeHudKey(KeyEvent.KEYCODE_D, "D", 198f, bottom - 70f, 70f, 58f, .70f)
        keys += RuntimeHudKey(KeyEvent.KEYCODE_SPACE, "SPACE", width - 210f, bottom - 10f, 190f, 58f, .65f)
        keys += RuntimeHudKey(KeyEvent.KEYCODE_SHIFT_LEFT, "SHIFT", width - 210f, bottom - 75f, 90f, 52f, .60f)
        keys += RuntimeHudKey(MOUSE_LEFT, "LMB", width - 110f, bottom - 75f, 90f, 52f, .60f)
    }

    companion object {
        const val MOUSE_LEFT = -1
        const val MOUSE_RIGHT = -2
        const val MOUSE_MIDDLE = -3
        const val SCROLL_UP = -4
        const val SCROLL_DOWN = -5
    }
}
