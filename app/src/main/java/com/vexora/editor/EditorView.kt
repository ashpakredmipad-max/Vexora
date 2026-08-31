package com.vexora.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class EditorView(context: Context, private val action: (String) -> Unit) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = resources.displayMetrics.density
    private var duration = 3080L
    private var fileName = ""
    private var playing = false
    private var hasMedia = false
    private var selectedTool = "Fit"
    private var startX = 0f

    private val bg = Color.rgb(22,22,26)
    private val panel = Color.rgb(31,32,39)
    private val muted = Color.rgb(165,166,173)
    private val white = Color.rgb(245,245,247)
    private val blue = Color.rgb(30,136,229)

    fun setMedia(ms: Long, name: String) { duration = max(1L, ms); fileName = name; hasMedia = true; invalidate() }
    fun togglePlaying() { playing = !playing; invalidate() }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        c.drawColor(bg)
        val w = width.toFloat(); val h = height.toFloat()
        val top = 0f
        // top bar
        p.color = bg; c.drawRect(0f, top, w, d(62), p)
        text(c, "‹", d(50), d(44), 38f, white, true)
        circle(c, d(116), d(31), d(11), muted); text(c, "?", d(113), d(36), 13f, white, true)
        drawSmallScreenIcon(c, d(168), d(31))
        drawOriginal(c, w/2f, d(31))
        text(c, "•••", w-d(176), d(38), 20f, white, true)
        roundRect(c, w-d(137), d(16), w-d(94), d(51), 5f, Color.rgb(53,54,60))
        drawSave(c, w-d(115), d(33))
        roundRect(c, w-d(82), d(12), w-d(39), d(55), 6f, blue); drawShare(c,w-d(61),d(33))

        // preview frame; transparent if a real VideoView/ImageView is underneath
        if (!hasMedia) {
            roundRect(c, w*0.305f, d(70), w*0.695f, d(390), 2f, Color.rgb(238,238,238))
            text(c, "Add a video or image to start", w/2f, d(235), 18f, Color.DKGRAY, false, true)
        }
        text(c, format(duration), d(10), d(470), 13f, muted)
        // playback row
        drawTransport(c, w/2f, d(487))
        drawAdjust(c, w-d(95), d(487)); drawUndo(c,w-d(55),d(487)); drawUndo(c,w-d(18),d(487),true)

        val timelineTop = d(510)
        // timeline left add rows
        drawAddRow(c, d(210), timelineTop+d(6), "♪+", "Tap to add music")
        drawAddRow(c, d(210), timelineTop+d(50), "T+", "Tap to add subtitle")
        drawAddRow(c, d(210), timelineTop+d(94), "⌁+", "Tap to add sticker / Overlay")
        text(c, "▣+", d(166), timelineTop+d(154), 23f, muted, true)
        text(c, "◖", d(178), timelineTop+d(202), 22f, muted, true)
        roundRect(c, d(78), timelineTop+d(144), d(134), timelineTop+d(200), 2f, Color.rgb(28,29,34), Color.rgb(67,68,74))
        text(c, "⌁", d(106), timelineTop+d(180), 20f, muted, true)
        text(c, "Cover", d(105), timelineTop+d(193), 11f, white, false, true)

        // main timeline
        val trackX = d(210); val trackY = timelineTop+d(136); val trackW = min(w-d(520), d(630))
        roundRect(c, trackX, trackY, trackX+trackW, trackY+d(92), 3f, panel)
        if (hasMedia) drawClip(c, trackX, trackY, min(trackW-d(14), max(d(210), trackW*0.35f)))
        else drawPlaceholderClip(c, trackX, trackY)
        roundRect(c, trackX+trackW-d(2), trackY+d(16), trackX+trackW+d(12), trackY+d(44), 2f, Color.WHITE)
        text(c, "+", trackX+trackW+d(5), trackY+d(37), 22f, Color.DKGRAY, false, true)
        text(c, "00:00", trackX, trackY+d(104), 10f, muted)
        for (i in 1..8) { val x=trackX+d(72*i); text(c, String.format("00:%02d",i), x, trackY+d(104), 10f, muted) }
        // playhead
        val ph = if (hasMedia) trackX + trackW*0.89f else trackX + trackW*0.89f
        p.color = white; c.drawRect(ph, timelineTop+d(1), ph+d(2), trackY+d(110), p)

        // tool strip
        val toolsY = h-d(64)
        p.color = bg; c.drawRect(0f, toolsY-d(12), w, h, p)
        val labels = arrayOf("Filter","Trim","FX","Split","Flow","Cutout","Crop","Rotate","Mirror","Flip","Fit","BG","Border","Blur","Opacity","Zoom","TTS","Mosaic","Magnifier","Stories","Overlay\nTrack")
        val glyphs = arrayOf("◉","<>","☆","✂","F","◌","⌗","↻","◫","▱","⊞","▨","□","▒","◉","↗","A","▦","⌕","☷","⌁")
        val start = d(110); val gap = d(70)
        labels.forEachIndexed { i, label ->
            val x = start + i*gap
            if (x > w-d(20)) return@forEachIndexed
            text(c, glyphs[i], x, toolsY+d(17), 25f, if (label==selectedTool) white else muted, true, true)
            text(c, label, x, toolsY+d(43), 11f, if (label==selectedTool) white else muted, false, true)
        }
        // current selection marker
        p.color = white; c.drawRect(start + labels.indexOf(selectedTool)*gap-d(18), h-d(9), start + labels.indexOf(selectedTool)*gap+d(18), h-d(7), p)
    }

    private fun drawAddRow(c: Canvas, x: Float, y: Float, icon: String, label: String) {
        text(c, icon, x-d(45), y+d(24), 23f, muted, true, true)
        roundRect(c, x, y, x+d(214), y+d(36), 3f, panel)
        text(c, label, x+d(12), y+d(23), 13f, Color.rgb(130,131,139))
    }

    private fun drawClip(c: Canvas, x: Float, y: Float, width: Float) {
        roundRect(c,x,y,x+width,y+d(62),2f,Color.rgb(41,42,48))
        val cells = 4
        for (i in 0 until cells) {
            val xx=x+i*width/cells
            p.color=Color.rgb(205,205,208); c.drawRect(xx+d(2),y+d(3),xx+width/cells-d(2),y+d(59),p)
            // thumbnail-like lines
            p.color=Color.rgb(160,160,165); c.drawRect(xx+d(7),y+d(10),xx+width/cells-d(7),y+d(12),p)
            p.color=Color.rgb(100,100,105); c.drawRect(xx+d(7),y+d(18),xx+width/cells-d(18),y+d(20),p)
        }
        p.color=Color.WHITE; c.drawRect(x+width-d(3),y-d(2),x+width+d(3),y+d(64),p)
    }
    private fun drawPlaceholderClip(c: Canvas, x: Float, y: Float) {
        roundRect(c,x,y,x+d(210),y+d(62),2f,Color.rgb(55,56,62)); text(c,"Tap + to add media",x+d(105),y+d(38),13f,muted,false,true)
    }

    private fun drawTransport(c: Canvas,x:Float,y:Float){ text(c,"|◀",x-d(54),y+d(8),20f,muted,true,true); text(c,if(playing)"❚❚" else "▶",x,y+d(9),25f,white,true,true); text(c,"▶|",x+d(45),y+d(8),20f,muted,true,true) }
    private fun drawAdjust(c:Canvas,x:Float,y:Float){text(c,"☷",x,y+d(8),23f,white,true,true)}
    private fun drawUndo(c:Canvas,x:Float,y:Float,redo:Boolean=false){text(c,if(redo)"↷" else "↶",x,y+d(7),28f,if(redo)muted else white,true,true)}
    private fun drawSmallScreenIcon(c:Canvas,x:Float,y:Float){roundRect(c,x-d(11),y-d(7),x+d(11),y+d(8),1f,Color.TRANSPARENT,muted);c.drawLine(x-d(8),y+d(4),x+d(8),y+d(4),p)}
    private fun drawOriginal(c:Canvas,x:Float,y:Float){roundRect(c,x-d(8),y-d(8),x+d(8),y+d(8),2f,Color.TRANSPARENT,white);text(c,"Original⌄",x+d(22),y+d(5),14f,white,false,true)}
    private fun drawSave(c:Canvas,x:Float,y:Float){roundRect(c,x-d(8),y-d(9),x+d(8),y+d(9),2f,Color.TRANSPARENT,white);c.drawRect(x-d(5),y-d(7),x+d(5),y-d(2),p);c.drawRect(x-d(4),y+d(3),x+d(4),y+d(7),p)}
    private fun drawShare(c:Canvas,x:Float,y:Float){p.color=white;c.drawLine(x,y+d(7),x,y-d(8),p);c.drawLine(x,y-d(8),x-d(6),y-d(2),p);c.drawLine(x,y-d(8),x+d(6),y-d(2),p);c.drawLine(x-d(8),y+d(3),x-d(8),y+d(9),p);c.drawLine(x-d(8),y+d(9),x+d(8),y+d(9),p);c.drawLine(x+d(8),y+d(9),x+d(8),y+d(3),p)}
    private fun circle(c:Canvas,x:Float,y:Float,r:Float,color:Int){p.color=color;p.style=Paint.Style.STROKE;p.strokeWidth=d(2);c.drawCircle(x,y,r,p);p.style=Paint.Style.FILL}
    private fun roundRect(c:Canvas,l:Float,t:Float,r:Float,b:Float,rad:Float,color:Int,border:Int?=null){p.color=color;p.style=Paint.Style.FILL;c.drawRoundRect(RectF(l,t,r,b),d(rad),d(rad),p);if(border!=null){p.color=border;p.style=Paint.Style.STROKE;p.strokeWidth=d(1);c.drawRoundRect(RectF(l,t,r,b),d(rad),d(rad),p);p.style=Paint.Style.FILL}}
    private fun text(c:Canvas,s:String,x:Float,y:Float,size:Float,color:Int,bold:Boolean=false,center:Boolean=false){p.color=color;p.textSize=d(size);p.typeface=if(bold)android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT;p.textAlign=if(center)Paint.Align.CENTER else Paint.Align.LEFT;c.drawText(s,x,y,p)}
    private fun d(v:Int)=v*density
    private fun d(v:Float)=v*density
    private fun format(ms:Long):String=String.format("%d.%02d / %d.%02d",ms/1000,(ms%1000)/10,ms/1000,(ms%1000)/10)

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if(e.action==MotionEvent.ACTION_DOWN){startX=e.x;return true}
        if(e.action!=MotionEvent.ACTION_UP)return true
        val y=e.y; val x=e.x; val h=height.toFloat(); val w=width.toFloat()
        if(y<d(70) && x>w-d(95)){ action("export"); return true }
        if(y in d(455)..d(515) && x in w/2-d(45)..w/2+d(45)){action("play");return true}
        if(y in d(510)..d(720) && x in d(390)..d(880)){action("add");return true}
        if(y>h-d(82)){
            val idx=((x-d(110))/d(70)).toInt()
            val names=arrayOf("Filter","Trim","FX","Split","Flow","Cutout","Crop","Rotate","Mirror","Flip","Fit","BG","Border","Blur","Opacity","Zoom","TTS","Mosaic","Magnifier","Stories","Overlay Track")
            if(idx in names.indices){selectedTool=names[idx];action(names[idx]) ;invalidate()}
            return true
        }
        if(y in d(510)..d(660) && x<d(450)){ action(if(y<d(575))"music" else if(y<d(620))"subtitle" else "overlay");return true }
        return true
    }
}
