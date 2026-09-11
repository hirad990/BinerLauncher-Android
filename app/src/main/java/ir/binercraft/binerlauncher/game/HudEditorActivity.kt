package ir.binercraft.binerlauncher.game

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

private data class HudKey(var code:Int,var label:String,var x:Float,var y:Float,var w:Float=72f,var h:Float=58f,var opacity:Float=.78f)

class HudEditorActivity:Activity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContentView(HudEditorView(this))}}

private class HudEditorView(private val activity:Activity):View(activity){
 private val paint=Paint(Paint.ANTI_ALIAS_FLAG);private val prefs=activity.getSharedPreferences("biner_hud",0);private val keys=mutableListOf<HudKey>();private var selected=-1;private var lastX=0f;private var lastY=0f;private var resizing=false;private var lastTap=0L;private var lastTapIndex=-1
 init{load();isFocusable=true}
 override fun onDraw(c:Canvas){c.drawColor(Color.rgb(8,10,16));paint.color=Color.WHITE;paint.textSize=32f;paint.isFakeBoldText=true;c.drawText("Biner HUD Editor",24f,46f,paint);paint.textSize=15f;paint.isFakeBoldText=false;c.drawText("Drag: move  •  corner: resize  •  double tap: remove",24f,72f,paint);keys.forEachIndexed{i,k->val r=RectF(k.x,k.y,k.x+k.w,k.y+k.h);paint.color=Color.argb((k.opacity.coerceIn(.15f,1f)*255).toInt(),35,45,65);c.drawRoundRect(r,14f,14f,paint);paint.color=if(i==selected)Color.CYAN else Color.WHITE;paint.style=Paint.Style.STROKE;paint.strokeWidth=if(i==selected)4f else 2f;c.drawRoundRect(r,14f,14f,paint);paint.style=Paint.Style.FILL;paint.textSize=if(k.label.length>6)12f else 18f;paint.textAlign=Paint.Align.CENTER;c.drawText(k.label,k.x+k.w/2f,k.y+k.h/2f+6f,paint)};paint.textAlign=Paint.Align.LEFT}
 override fun onTouchEvent(e:MotionEvent):Boolean{when(e.actionMasked){MotionEvent.ACTION_DOWN->{val hit=keys.indexOfLast{x=e.x in it.x..it.x+it.w && e.y in it.y..it.y+it.h};val now=System.currentTimeMillis();if(hit>=0&&hit==lastTapIndex&&now-lastTap<320){keys.removeAt(hit);selected=-1;save();lastTap=0;lastTapIndex=-1;invalidate();return true};lastTap=now;lastTapIndex=hit;selected=hit;lastX=e.x;lastY=e.y;resizing=selected>=0&&e.x>keys[selected].x+keys[selected].w-20&&e.y>keys[selected].y+keys[selected].h-20;invalidate()};MotionEvent.ACTION_MOVE->if(selected>=0){val k=keys[selected];val dx=e.x-lastX;val dy=e.y-lastY;if(resizing){k.w=(k.w+dx).coerceIn(42f,220f);k.h=(k.h+dy).coerceIn(40f,140f)}else{k.x+=dx;k.y+=dy};lastX=e.x;lastY=e.y;invalidate()};MotionEvent.ACTION_UP->{save();Toast.makeText(activity,"HUD saved",Toast.LENGTH_SHORT).show()}};return true}
 private fun load(){val raw=prefs.getString("layout",null)?:return;runCatching{val a=JSONArray(raw);for(i in 0 until a.length()){val o=a.getJSONObject(i);keys+=HudKey(o.getInt("code"),o.getString("label"),o.getDouble("x").toFloat(),o.getDouble("y").toFloat(),o.getDouble("w").toFloat(),o.getDouble("h").toFloat(),o.optDouble("opacity",.78).toFloat())}}}
 private fun save(){val a=JSONArray();keys.forEach{k->a.put(JSONObject().apply{put("code",k.code);put("label",k.label);put("x",k.x);put("y",k.y);put("w",k.w);put("h",k.h);put("opacity",k.opacity)})};prefs.edit().putString("layout",a.toString()).apply()}
 companion object{const val MOUSE_LEFT=-1;const val MOUSE_RIGHT=-2;const val MOUSE_MIDDLE=-3;const val SCROLL_UP=-4;const val SCROLL_DOWN=-5}
}
