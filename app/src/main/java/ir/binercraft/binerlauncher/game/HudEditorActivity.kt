package ir.binercraft.binerlauncher.game

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import org.json.JSONArray
import org.json.JSONObject

class HudEditorActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(HudEditorView(this))
    }
}

private data class HudKey(var code: Int, var label: String, var x: Float, var y: Float, var w: Float = 72f, var h: Float = 58f, var opacity: Float = .78f)

private class HudEditorView(activity: Activity) : View(activity) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val prefs = activity.getSharedPreferences("biner_hud", 0)
    private val keys = mutableListOf<HudKey>()
    private var selected = -1
    private var lastX = 0f
    private var lastY = 0f
    private var resizing = false

    private val allKeys = buildList {
        addAll(('A'..'Z').map { it.code to it.toString() })
        addAll(('0'..'9').map { it.code to it.toString() })
        addAll((1..12).map { 111 + it to "F$it" })
        addAll(listOf(
            66 to "ENTER", 67 to "BACK", 61 to "TAB", 62 to "SPACE", 111 to "ESC",
            59 to "SHIFT", 113 to "CTRL", 57 to "ALT", 115 to "CAPS",
            19 to "↑", 20 to "↓", 21 to "←", 22 to "→", 122 to "HOME", 123 to "END",
            92 to "PGUP", 93 to "PGDN", 124 to "INS", 112 to "DEL", 82 to "MENU",
            144 to "NUM0", 145 to "NUM1", 146 to "NUM2", 147 to "NUM3", 148 to "NUM4", 149 to "NUM5", 150 to "NUM6", 151 to "NUM7", 152 to "NUM8", 153 to "NUM9",
            -1 to "LMB", -2 to "RMB", -3 to "MMB", -4 to "SCROLL↑", -5 to "SCROLL↓"
        ))
    }

    init { load(); isFocusable = true }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(8, 10, 16))
        paint.color = Color.WHITE; paint.textSize = 34f; paint.isFakeBoldText = true
        canvas.drawText("Biner HUD Editor", 28f, 48f, paint)
        paint.textSize = 18f; paint.isFakeBoldText = false
        canvas.drawText("Drag = move   •   bottom-right handle = resize   •   double tap = remove", 28f, 78f, paint)
        keys.forEachIndexed { index, key ->
            val r = RectF(key.x, key.y, key.x + key.w, key.y + key.h)
            paint.color = Color.argb((key.opacity * 255).toInt(), 35, 45, 65)
            canvas.drawRoundRect(r, 14f, 14f, paint)
            paint.color = if (index == selected) Color.CYAN else Color.WHITE
            paint.style = Paint.Style.STROKE; paint.strokeWidth = if (index == selected) 4f else 2f
            canvas.drawRoundRect(r, 14f, 14f, paint); paint.style = Paint.Style.FILL
            paint.textSize = if (key.label.length > 6) 13f else 18f
            canvas.drawText(key.label, key.x + 10f, key.y + key.h / 2f + 6f, paint)
            if (index == selected) { paint.color = Color.CYAN; canvas.drawCircle(key.x + key.w, key.y + key.h, 9f, paint) }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                selected = keys.indexOfLast { event.x in it.x..(it.x + it.w) && event.y in it.y..(it.y + it.h) }
                lastX = event.x; lastY = event.y
                resizing = selected >= 0 && event.x > keys[selected].x + keys[selected].w - 20 && event.y > keys[selected].y + keys[selected].h - 20
                invalidate(); return true
            }
            MotionEvent.ACTION_MOVE -> if (selected >= 0) {
                val key = keys[selected]
                val dx = event.x - lastX; val dy = event.y - lastY
                if (resizing) { key.w = (key.w + dx).coerceIn(42f, 220f); key.h = (key.h + dy).coerceIn(40f, 140f) }
                else { key.x += dx; key.y += dy }
                lastX = event.x; lastY = event.y; invalidate(); return true
            }
            MotionEvent.ACTION_UP -> { save(); return true }
        }
        return true
    }

    fun addKey(code: Int, label: String) { keys += HudKey(code, label, 80f + keys.size * 8f, 110f); save(); invalidate() }

    private fun load() {
        val raw = prefs.getString("layout", null) ?: return
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) { val o = arr.getJSONObject(i); keys += HudKey(o.getInt("code"), o.getString("label"), o.getDouble("x").toFloat(), o.getDouble("y").toFloat(), o.getDouble("w").toFloat(), o.getDouble("h").toFloat(), o.getDouble("opacity").toFloat()) }
        }
    }

    private fun save() {
        val arr = JSONArray()
        keys.forEach { k -> arr.put(JSONObject().apply { put("code", k.code); put("label", k.label); put("x", k.x); put("y", k.y); put("w", k.w); put("h", k.h); put("opacity", k.opacity) }) }
        prefs.edit().putString("layout", arr.toString()).apply()
    }
}
