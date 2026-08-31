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

class EditorView(context: Context, private val action: (String) -> Unit) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var duration = 3080L
    private var hasMedia = false
    private var playing = false
    private var selected = 10

    private val bg = Color.rgb(20, 20, 24)
    private val panel = Color.rgb(34, 35, 42)
    private val panel2 = Color.rgb(39, 40, 48)
    private val white = Color.rgb(245, 245, 247)
    private val muted = Color.rgb(160, 161, 168)
    private val blue = Color.rgb(20, 122, 232)

    private val tools = arrayOf("Filter", "Trim", "FX", "Split", "Flow", "Cutout", "Crop", "Rotate", "Mirror", "Flip", "Fit", "BG", "Border", "Blur", "Opacity", "Zoom", "TTS", "Mosaic", "Magnifier", "Stories", "Overlay\nTrack")

    fun setMedia(ms: Long) { duration = max(1L, ms); hasMedia = true; invalidate() }
    fun togglePlaying() { playing = !playing; invalidate() }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        c.drawColor(bg)
        val sx = width / 1536f
        val sy = height / 824f
        c.save(); c.scale(sx, sy)

        // Header
        line(c, 58f, 27f, 47f, 38f, white, 2.2f); line(c, 47f, 38f, 58f, 49f, white, 2.2f)
        circleStroke(c, 115f, 37f, 10f, white, 1.7f); text(c, "?", 115f, 42f, 14f, white, true, true)
        strokeRect(c, 157f, 29f, 178f, 46f, white, 1.5f); line(c, 158f, 41f, 177f, 41f, white, 1.5f)
        strokeRect(c, 723f, 27f, 739f, 43f, white, 1.7f); text(c, "Original", 751f, 42f, 14f, white); triangle(c, 808f, 35f, 814f, 35f, 811f, 41f, white)
        text(c, "•••", 1357f, 43f, 20f, white, true, true)
        round(c, 1396f, 12f, 1439f, 55f, 5f, Color.rgb(55,56,63)); drawSave(c, 1417f, 33f)
        round(c, 1448f, 10f, 1490f, 57f, 6f, blue); drawShare(c, 1469f, 34f)

        // Preview frame. MainActivity places the real media underneath this view.
        if (!hasMedia) { p.color = Color.rgb(238,238,238); c.drawRect(467f, 71f, 1068f, 431f, p) }
        drawFullscreen(c)
        p.color = Color.rgb(91,92,99); c.drawRect(0f, 443f, 1536f, 445f, p)
        text(c, format(duration), 9f, 486f, 13f, muted)

        // Playback
        drawPrev(c, 717f, 463f); text(c, if (playing) "❚❚" else "▶", 768f, 472f, 25f, white, true, true); drawNext(c, 818f, 463f)
        drawSliders(c, 1444f, 463f); drawUndo(c, 1478f, 463f, false); drawUndo(c, 1514f, 463f, true)

        // Timeline controls
        drawRow(c, 211f, 491f, "music", "Tap to add music")
        drawRow(c, 211f, 535f, "text", "Tap to add subtitle")
        drawRow(c, 211f, 579f, "image", "Tap to add sticker / Overlay")
        drawCover(c, 78f, 630f); drawFilmPlus(c, 174f, 632f); drawSpeaker(c, 174f, 682f)

        // Main lane
        round(c, 210f, 623f, 840f, 715f, 3f, panel)
        if (hasMedia) drawClip(c, 210f, 623f, 215f) else drawPlaceholderClip(c, 210f, 623f)
        
        for (i in 0..8) text(c, String.format("00:%02d", i), 210f + i * 72f, 737f, 10f, muted)
        p.color = white; c.drawRect(767f, 486f, 770f, 740f, p)

        // Bottom toolbar
        p.color = bg; c.drawRect(0f, 747f, 1536f, 824f, p)
        val start = 110f; val gap = 70f
        for (i in tools.indices) {
            val x = start + i * gap
            if (x > 1515f) break
            drawTool(c, x, 774f, tools[i], i == selected)
        }
        c.restore()
    }

    private fun drawRow(c: Canvas, x: Float, y: Float, kind: String, label: String) {
        when (kind) { "music" -> drawMusicIcon(c, x - 44f, y + 18f); "text" -> drawTextIcon(c, x - 44f, y + 18f); else -> drawImageIcon(c, x - 44f, y + 18f) }
        round(c, x, y, x + 213f, y + 36f, 3f, panel)
        text(c, label, x + 12f, y + 24f, 13f, Color.rgb(128,129,137))
    }

    private fun drawCover(c: Canvas, x: Float, y: Float) { strokeRect(c,x,y,x+56f,y+56f,Color.rgb(67,68,74),1f); line(c,x+15f,y+45f,x+25f,y+29f,muted,1.5f); line(c,x+25f,y+29f,x+34f,y+39f,muted,1.5f); line(c,x+34f,y+39f,x+43f,y+25f,muted,1.5f); text(c,"Cover",x+28f,y+74f,11f,white,false,true) }
    private fun drawFilmPlus(c: Canvas, x: Float, y: Float) { round(c,x-11f,y-11f,x+11f,y+11f,2f,Color.TRANSPARENT,muted); line(c,x-7f,y-9f,x-7f,y+9f,muted,1f); line(c,x,y-9f,x,y+9f,muted,1f); line(c,x+7f,y-9f,x+7f,y+9f,muted,1f); text(c,"+",x+13f,y+6f,17f,white,true,true) }
    private fun drawSpeaker(c: Canvas, x: Float, y: Float) { p.color=muted; val path=Path(); path.moveTo(x-10f,y-4f); path.lineTo(x-4f,y-4f); path.lineTo(x+4f,y-11f); path.lineTo(x+4f,y+11f); path.lineTo(x-4f,y+4f); path.lineTo(x-10f,y+4f); path.close(); c.drawPath(path,p); line(c,x+8f,y-8f,x+14f,y+8f,muted,2f) }
    private fun drawClip(c: Canvas, x: Float, y: Float, w: Float) { round(c,x,y,x+w,y+62f,2f,Color.rgb(42,43,49)); for(i in 0..3){ val l=x+i*w/4f; p.color=Color.rgb(205,205,208); c.drawRect(l+2f,y+3f,l+w/4f-2f,y+59f,p); line(c,l+8f,y+14f,l+w/4f-8f,y+14f,Color.rgb(145,145,150),1f); line(c,l+8f,y+24f,l+w/4f-22f,y+24f,Color.rgb(110,110,116),1f) } }
    private fun drawPlaceholderClip(c: Canvas,x: Float,y: Float){ round(c,x,y,x+215f,y+62f,2f,Color.rgb(55,56,62)); text(c,"Tap + to add media",x+107f,y+38f,13f,muted,false,true) }

    private fun drawTool(c: Canvas,x: Float,y: Float,label: String,active: Boolean){ val col=if(active) white else muted; when(label.substringBefore('\n')) { "Filter"->drawFilter(c,x,y,col); "Trim"->drawTrim(c,x,y,col); "FX"->text(c,"☆",x,y+8f,31f,col,true,true); "Split"->drawScissors(c,x,y,col); "Flow"->drawSquareF(c,x,y,col); "Cutout"->drawCutout(c,x,y,col); "Crop"->drawCrop(c,x,y,col); "Rotate"->text(c,"↻",x,y+8f,29f,col,true,true); "Mirror"->drawMirror(c,x,y,col); "Flip"->drawFlip(c,x,y,col); "Fit"->drawFit(c,x,y,col); "BG"->drawBg(c,x,y,col); "Border"->drawBorder(c,x,y,col); "Blur"->drawBlur(c,x,y,col); "Opacity"->drawOpacity(c,x,y,col); "Zoom"->drawZoom(c,x,y,col); "TTS"->drawTts(c,x,y,col); "Mosaic"->drawMosaic(c,x,y,col); "Magnifier"->drawMagnifier(c,x,y,col); "Stories"->drawStories(c,x,y,col); else->drawOverlayTrack(c,x,y,col) }; text(c,label,x,y+37f,11f,col,false,true) }

    private fun drawFilter(c:Canvas,x:Float,y:Float,col:Int){circleStroke(c,x,y,12f,col,2f);circleStroke(c,x,y,6f,col,2f);circle(c,x,y,2f,col)}
    private fun drawTrim(c:Canvas,x:Float,y:Float,col:Int){strokeRect(c,x-12f,y-10f,x+12f,y+10f,col,2f);line(c,x-3f,y-10f,x-3f,y+10f,col,2f);line(c,x+3f,y-10f,x+3f,y+10f,col,2f)}
    private fun drawScissors(c:Canvas,x:Float,y:Float,col:Int){line(c,x-11f,y-9f,x+11f,y+10f,col,2f);line(c,x-11f,y+9f,x+11f,y-10f,col,2f);circleStroke(c,x-11f,y-9f,3f,col,2f);circleStroke(c,x-11f,y+9f,3f,col,2f)}
    private fun drawSquareF(c:Canvas,x:Float,y:Float,col:Int){strokeRect(c,x-12f,y-12f,x+12f,y+12f,col,2f);text(c,"F",x,y+8f,18f,col,true,true)}
    private fun drawCutout(c:Canvas,x:Float,y:Float,col:Int){circleStroke(c,x,y,12f,col,2f);circleStroke(c,x,y,5f,col,2f);line(c,x+8f,y-9f,x+13f,y-14f,col,2f)}
    private fun drawCrop(c:Canvas,x:Float,y:Float,col:Int){line(c,x-11f,y-2f,x-2f,y-2f,col,2f);line(c,x-2f,y-2f,x-2f,y-11f,col,2f);line(c,x+11f,y+2f,x+2f,y+2f,col,2f);line(c,x+2f,y+2f,x+2f,y+11f,col,2f)}
    private fun drawMirror(c:Canvas,x:Float,y:Float,col:Int){line(c,x,y-13f,x,y+13f,col,2f);strokeRect(c,x-12f,y-9f,x-3f,y+9f,col,1.5f);strokeRect(c,x+3f,y-9f,x+12f,y+9f,col,1.5f)}
    private fun drawFlip(c:Canvas,x:Float,y:Float,col:Int){strokeRect(c,x-12f,y-8f,x+12f,y+8f,col,1.5f);line(c,x,y-10f,x,y+10f,col,2f)}
    private fun drawFit(c:Canvas,x:Float,y:Float,col:Int){strokeRect(c,x-10f,y-10f,x+10f,y+10f,col,2f);line(c,x-5f,y,x+5f,y,col,2f);line(c,x,y-5f,x,y+5f,col,2f)}
    private fun drawBg(c:Canvas,x:Float,y:Float,col:Int){round(c,x-11f,y-11f,x+11f,y+11f,3f,Color.TRANSPARENT,col);line(c,x-6f,y-6f,x+6f,y+6f,col,2f);line(c,x+6f,y-6f,x-6f,y+6f,col,2f)}
    private fun drawBorder(c:Canvas,x:Float,y:Float,col:Int){strokeRect(c,x-11f,y-11f,x+11f,y+11f,col,2f);strokeRect(c,x-6f,y-6f,x+6f,y+6f,col,1.3f)}
    private fun drawBlur(c:Canvas,x:Float,y:Float,col:Int){p.color=col;for(i in -1..1)for(j in -1..1)c.drawCircle(x+i*7f,y+j*7f,3f,p)}
    private fun drawOpacity(c:Canvas,x:Float,y:Float,col:Int){circleStroke(c,x,y,11f,col,2f);text(c,"½",x,y+7f,16f,col,true,true)}
    private fun drawZoom(c:Canvas,x:Float,y:Float,col:Int){circleStroke(c,x-3f,y-3f,8f,col,2f);line(c,x+3f,y+3f,x+12f,y+12f,col,2f)}
    private fun drawTts(c:Canvas,x:Float,y:Float,col:Int){strokeRect(c,x-12f,y-10f,x+12f,y+10f,col,2f);text(c,"A",x,y+6f,13f,col,true,true)}
    private fun drawMosaic(c:Canvas,x:Float,y:Float,col:Int){for(i in -1..1)for(j in -1..1)round(c,x+i*8f-3f,y+j*8f-3f,x+i*8f+3f,y+j*8f+3f,1f,Color.TRANSPARENT,col)}
    private fun drawMagnifier(c:Canvas,x:Float,y:Float,col:Int){circleStroke(c,x-2f,y-2f,8f,col,2f);line(c,x+4f,y+4f,x+12f,y+12f,col,2f);text(c,"+",x-2f,y+3f,10f,col,true,true)}
    private fun drawStories(c:Canvas,x:Float,y:Float,col:Int){strokeRect(c,x-10f,y-10f,x+10f,y+10f,col,2f);line(c,x-6f,y-3f,x+6f,y-3f,col,1.5f);line(c,x-6f,y+2f,x+6f,y+2f,col,1.5f)}
    private fun drawOverlayTrack(c:Canvas,x:Float,y:Float,col:Int){p.color=col;for(i in -1..1)for(j in -1..1)c.drawRect(x+i*8f-3f,y+j*8f-3f,x+i*8f+3f,y+j*8f+3f,p)}

    private fun drawMusicIcon(c:Canvas,x:Float,y:Float){ text(c,"♪+",x,y+8f,22f,muted,true,true) }
    private fun drawTextIcon(c:Canvas,x:Float,y:Float){ text(c,"T+",x,y+8f,21f,muted,true,true) }
    private fun drawImageIcon(c:Canvas,x:Float,y:Float){ text(c,"▧+",x,y+8f,21f,muted,true,true) }
    private fun drawFullscreen(c:Canvas){ line(c,1499f,397f,1499f,407f,white,2f);line(c,1499f,397f,1509f,397f,white,2f);line(c,1514f,397f,1514f,407f,white,2f);line(c,1504f,397f,1514f,397f,white,2f);line(c,1499f,421f,1499f,411f,white,2f);line(c,1499f,421f,1509f,421f,white,2f);line(c,1514f,421f,1514f,411f,white,2f);line(c,1504f,421f,1514f,421f,white,2f) }
    private fun drawPrev(c:Canvas,x:Float,y:Float){text(c,"|◀",x,y+9f,20f,muted,true,true)}
    private fun drawNext(c:Canvas,x:Float,y:Float){text(c,"▶|",x,y+9f,20f,muted,true,true)}
    private fun drawSliders(c:Canvas,x:Float,y:Float){line(c,x-8f,y-8f,x+8f,y-8f,white,1.7f);line(c,x-8f,y,x+8f,y,white,1.7f);line(c,x-8f,y+8f,x+8f,y+8f,white,1.7f);circle(c,x-2f,y-8f,2f,white);circle(c,x+3f,y,2f,white);circle(c,x-3f,y+8f,2f,white)}
    private fun drawUndo(c:Canvas,x:Float,y:Float,redo:Boolean){text(c,if(redo)"↷" else "↶",x,y+8f,28f,if(redo)muted else white,true,true)}
    private fun drawSave(c:Canvas,x:Float,y:Float){strokeRect(c,x-8f,y-9f,x+8f,y+9f,white,2f);p.color=white;c.drawRect(x-5f,y-7f,x+5f,y-2f,p);c.drawRect(x-4f,y+3f,x+4f,y+7f,p)}
    private fun drawShare(c:Canvas,x:Float,y:Float){line(c,x,y+8f,x,y-8f,white,2f);line(c,x,y-8f,x-6f,y-2f,white,2f);line(c,x,y-8f,x+6f,y-2f,white,2f);strokeRect(c,x-8f,y+3f,x+8f,y+10f,white,1.8f)}

    private fun circle(c:Canvas,x:Float,y:Float,r:Float,col:Int){p.style=Paint.Style.FILL;p.color=col;c.drawCircle(x,y,r,p)}
    private fun circleStroke(c:Canvas,x:Float,y:Float,r:Float,col:Int,sw:Float){p.style=Paint.Style.STROKE;p.strokeWidth=sw;p.color=col;c.drawCircle(x,y,r,p);p.style=Paint.Style.FILL}
    private fun strokeRect(c:Canvas,l:Float,t:Float,r:Float,b:Float,col:Int,sw:Float){p.style=Paint.Style.STROKE;p.strokeWidth=sw;p.color=col;c.drawRect(l,t,r,b,p);p.style=Paint.Style.FILL}
    private fun round(c:Canvas,l:Float,t:Float,r:Float,b:Float,rad:Float,col:Int,border:Int?=null){p.style=Paint.Style.FILL;p.color=col;c.drawRoundRect(RectF(l,t,r,b),rad,rad,p);if(border!=null){p.style=Paint.Style.STROKE;p.strokeWidth=1f;p.color=border;c.drawRoundRect(RectF(l,t,r,b),rad,rad,p);p.style=Paint.Style.FILL}}
    private fun line(c:Canvas,x1:Float,y1:Float,x2:Float,y2:Float,col:Int,sw:Float){p.style=Paint.Style.STROKE;p.strokeWidth=sw;p.color=col;c.drawLine(x1,y1,x2,y2,p);p.style=Paint.Style.FILL}
    private fun triangle(c:Canvas,x1:Float,y1:Float,x2:Float,y2:Float,x3:Float,y3:Float,col:Int){p.color=col;p.style=Paint.Style.FILL;val path=Path();path.moveTo(x1,y1);path.lineTo(x2,y2);path.lineTo(x3,y3);path.close();c.drawPath(path,p)}
    private fun text(c:Canvas,s:String,x:Float,y:Float,size:Float,col:Int,bold:Boolean=false,center:Boolean=false){p.color=col;p.textSize=size;p.typeface=if(bold)android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT;p.textAlign=if(center)Paint.Align.CENTER else Paint.Align.LEFT;c.drawText(s,x,y,p)}
    private fun format(ms:Long):String=String.format("%d.%02d / %d.%02d",ms/1000,(ms%1000)/10,ms/1000,(ms%1000)/10)

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action != MotionEvent.ACTION_UP) return true
        val sx = width / 1536f; val sy = height / 824f
        val x = e.x / sx; val y = e.y / sy
        if (y < 70f && x > 1440f) { action("export"); return true }
        if (y in 440f..500f && x in 730f..805f) { action("play"); return true }
        if (y in 610f..730f && x in 400f..445f) { action("add"); return true }
        if (y in 485f..525f && x < 450f) { action("music"); return true }
        if (y in 525f..570f && x < 450f) { action("subtitle"); return true }
        if (y in 570f..615f && x < 450f) { action("overlay"); return true }
        if (y > 747f) { val i=((x-75f)/70f).toInt(); if(i in tools.indices){selected=i; action(tools[i].replace("\n"," ")); invalidate()}; return true }
        return true
    }
}
