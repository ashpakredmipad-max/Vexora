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
    private var duration = 3080L
    private var hasMedia = false
    private var playing = false
    private var selected = -1

    private val bg = Color.rgb(20, 20, 24)
    private val panel = Color.rgb(34, 35, 42)
    private val panel2 = Color.rgb(39, 40, 48)
    private val white = Color.rgb(245, 245, 247)
    private val muted = Color.rgb(160, 161, 168)
    private val dim = Color.rgb(108, 109, 117)
    private val blue = Color.rgb(20, 122, 232)

    private val tools = arrayOf("Filter","Trim","FX","Split","Flow","Cutout","Crop","Rotate","Mirror","Flip","Fit","BG","Border","Blur","Opacity","Zoom","TTS","Mosaic","Magnifier","Stories","Overlay\nTrack")

    fun setMedia(ms: Long) { duration = max(1L, ms); hasMedia = true; invalidate() }
    fun togglePlaying() { playing = !playing; invalidate() }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        c.drawColor(bg)
        val sx = width / 1536f
        val sy = height / 824f
        c.save(); c.scale(sx, sy)

        // Top toolbar (reference coordinates are relative to the app content, below status bar).
        line(c, 58, 27, 47, 38, white, 2.2f); line(c, 47, 38, 58, 49, white, 2.2f)
        circleStroke(c, 115, 37, 10, white, 1.7f); text(c, "?", 115, 42, 14, white, true, true)
        // small monitor icon
        strokeRect(c, 157, 29, 178, 46, white, 1.5f); line(c, 158, 41, 177, 41, white, 1.5f)

        // Original selector
        strokeRect(c, 723, 27, 739, 43, white, 1.7f); text(c, "Original", 751, 42, 14, white); triangle(c, 808, 35, 814, 35, 811, 41, white)
        text(c, "•••", 1357, 43, 20, white, true, true)
        round(c, 1396, 12, 1439, 55, 5, Color.rgb(55,56,63))
        drawSave(c, 1417, 33)
        round(c, 1448, 10, 1490, 57, 6, blue); drawShare(c, 1469, 34)

        // Preview placeholder / frame. Real media is placed underneath this view by MainActivity.
        if (!hasMedia) {
            p.color = Color.rgb(238,238,238); c.drawRect(467, 71, 1068, 431, p)
        }
        // Fullscreen control at preview lower-right.
        line(c, 1499, 397, 1499, 407, white, 2); line(c, 1499, 397, 1509, 397, white, 2)
        line(c, 1514, 397, 1514, 407, white, 2); line(c, 1504, 397, 1514, 397, white, 2)
        line(c, 1499, 421, 1499, 411, white, 2); line(c, 1499, 421, 1509, 421, white, 2)
        line(c, 1514, 421, 1514, 411, white, 2); line(c, 1504, 421, 1514, 421, white, 2)

        // Thin progress separator.
        p.color = Color.rgb(91,92,99); c.drawRect(0, 443, 1536, 445, p)
        text(c, format(duration), 9, 486, 13, muted)

        // Transport controls.
        drawPrev(c, 717, 463); text(c, if (playing) "❚❚" else "▶", 768, 472, 25, white, true, true); drawNext(c, 818, 463)
        drawSliders(c, 1444, 463); drawUndo(c, 1478, 463, false); drawUndo(c, 1514, 463, true)

        // Timeline track controls.
        drawMusicRow(c, 211, 491, "Tap to add music")
        drawTextRow(c, 211, 535, "Tap to add subtitle")
        drawOverlayRow(c, 211, 579, "Tap to add sticker / Overlay")
        drawCover(c, 78, 630)
        drawFilmPlus(c, 174, 632)
        drawSpeaker(c, 174, 682)

        // Main timeline lane.
        round(c, 210, 623, 840, 715, 3, panel)
        if (hasMedia) drawClip(c, 210, 623, 215)
        else drawPlaceholderClip(c, 210, 623)
        // Add clip button at end of lane.
        round(c, 788, 623, 840, 715, 2, panel2)
        line(c, 815, 646, 815, 691, white, 2); line(c, 793, 668, 837, 668, white, 2)
        // time ruler
        for (i in 0..8) text(c, String.format("00:%02d", i), 210 + i * 72, 737, 10, muted)
        // vertical playhead
        p.color = white; c.drawRect(767, 486, 770, 740, p)

        // Bottom tools.
        p.color = bg; c.drawRect(0, 747, 1536, 824, p)
        val start = 110f; val gap = 70f
        for (i in tools.indices) {
            val x = start + i * gap
            if (x > 1515) break
            drawTool(c, x, 774, tools[i], i == selected)
        }

        c.restore()
    }

    private fun drawMusicRow(c: Canvas, x: Int, y: Int, label: String) {
        drawMusicIcon(c, x-44, y+18); round(c,x,y,x+213,y+36,3,panel); text(c,label,x+12,y+24,13,Color.rgb(128,129,137))
    }
    private fun drawTextRow(c: Canvas, x: Int, y: Int, label: String) {
        drawTextIcon(c,x-44,y+18); round(c,x,y,x+213,y+36,3,panel); text(c,label,x+12,y+24,13,Color.rgb(128,129,137))
    }
    private fun drawOverlayRow(c: Canvas, x: Int, y: Int, label: String) {
        drawImageIcon(c,x-44,y+18); round(c,x,y,x+213,y+36,3,panel); text(c,label,x+12,y+24,13,Color.rgb(128,129,137))
    }
    private fun drawCover(c: Canvas,x:Int,y:Int){strokeRect(c,x,y,x+56,y+56,Color.rgb(67,68,74),1); line(c,x+15,y+45,x+25,y+29,muted,1.5f); line(c,x+25,y+29,x+34,y+39,muted,1.5f); line(c,x+34,y+39,x+43,y+25,muted,1.5f); text(c,"Cover",x+28,y+74,11,white,false,true)}
    private fun drawFilmPlus(c:Canvas,x:Int,y:Int){round(c,x-11,y-11,x+11,y+11,2,Color.TRANSPARENT,muted); for(i in -7..7 step 7) line(c,x+i,y-9,x+i,y+9,muted,1); text(c,"+",x+11,y+6,17,white,true,true)}
    private fun drawSpeaker(c:Canvas,x:Int,y:Int){p.color=muted;val path=Path();path.moveTo(x-10f,y-4f);path.lineTo(x-4f,y-4f);path.lineTo(x+4f,y-11f);path.lineTo(x+4f,y+11f);path.lineTo(x-4f,y+4f);path.lineTo(x-10f,y+4f);path.close();c.drawPath(path,p);line(c,x+8,y-8,x+14,y+8,muted,2)}
    private fun drawClip(c:Canvas,x:Int,y:Int,w:Int){round(c,x,y,x+w,y+62,2,Color.rgb(42,43,49));for(i in 0..3){val l=x+i*w/4f;r(l+2,y+3,l+w/4f-2,y+59,Color.rgb(205,205,208));line(c,l+8,y+14,l+w/4f-8,y+14,Color.rgb(145,145,150),1);line(c,l+8,y+24,l+w/4f-22,y+24,Color.rgb(110,110,116),1)} }
    private fun drawPlaceholderClip(c:Canvas,x:Int,y:Int){round(c,x,y,x+215,y+62,2,Color.rgb(55,56,62));text(c,"Tap + to add media",x+107,y+38,13,muted,false,true)}

    private fun drawTool(c:Canvas,x:Float,y:Float,label:String,active:Boolean){
        val iconColor=if(active)white else muted
        when(label.substringBefore('\n')){
            "Filter"->drawFilter(c,x,y,iconColor); "Trim"->drawTrim(c,x,y,iconColor); "FX"->text(c,"☆",x,y+8,31,iconColor,true,true); "Split"->drawScissors(c,x,y,iconColor); "Flow"->drawSquareF(c,x,y,iconColor); "Cutout"->drawCutout(c,x,y,iconColor); "Crop"->drawCrop(c,x,y,iconColor); "Rotate"->text(c,"↻",x,y+8,29,iconColor,true,true); "Mirror"->drawMirror(c,x,y,iconColor); "Flip"->drawFlip(c,x,y,iconColor); "Fit"->drawFit(c,x,y,iconColor); "BG"->drawBg(c,x,y,iconColor); "Border"->drawBorder(c,x,y,iconColor); "Blur"->drawBlur(c,x,y,iconColor); "Opacity"->drawOpacity(c,x,y,iconColor); "Zoom"->drawZoom(c,x,y,iconColor); "TTS"->drawTts(c,x,y,iconColor); "Mosaic"->drawMosaic(c,x,y,iconColor); "Magnifier"->drawMagnifier(c,x,y,iconColor); "Stories"->drawStories(c,x,y,iconColor); else->drawOverlayTrack(c,x,y,iconColor)
        }
        text(c,label,x,y+37,11,iconColor,false,true)
    }

    // Small line icons intentionally drawn rather than using platform fonts so they stay visually consistent.
    private fun drawFilter(c:Canvas,x:Float,y:Float,col:Int){circleStroke(c,x,y,12,col,2);circleStroke(c,x,y,6,col,2);circle(c,x,y,2,col)}
    private fun drawTrim(c:Canvas,x:Float,y:Float,col:Int){strokeRect(c,x-12,y-10,x+12,y+10,col,2);line(c,x-3,y-10,x-3,y+10,col,2);line(c,x+3,y-10,x+3,y+10,col,2)}
    private fun drawScissors(c:Canvas,x:Float,y:Float,col:Int){line(c,x-11,y-9,x+11,y+10,col,2);line(c,x-11,y+9,x+11,y-10,col,2);circleStroke(c,x-11,y-9,3,col,2);circleStroke(c,x-11,y+9,3,col,2)}
    private fun drawSquareF(c:Canvas,x:Float,y:Float,col:Int){strokeRect(c,x-12,y-12,x+12,y+12,col,2);text(c,"F",x,y+8,18,col,true,true)}
    private fun drawCutout(c:Canvas,x:Float,y:Float,col:Int){circleStroke(c,x,y,12,col,2);circleStroke(c,x,y,5,col,2);line(c,x+8,y-9,x+13,y-14,col,2)}
    private fun drawCrop(c:Canvas,x:Float,y:Float,col:Int){line(c,x-11,y-2,x-2,y-2,col,2);line(c,x-2,y-2,x-2,y-11,col,2);line(c,x+11,y+2,x+2,y+2,col,2);line(c,x+2,y+2,x+2,y+11,col,2)}
    private fun drawMirror(c:Canvas,x:Float,y:Float,col:Int){line(c,x,y-13,x,y+13,col,2);strokeRect(c,x-12,y-9,x-3,y+9,col,1.5f);strokeRect(c,x+3,y-9,x+12,y+9,col,1.5f)}
    private fun drawFlip(c:Canvas,x:Float,y:Float,col:Int){strokeRect(c,x-12,y-8,x+12,y+8,col,1.5f);line(c,x,y-10,x,y+10,col,2)}
    private fun drawFit(c:Canvas,x:Float,y:Float,col:Int){strokeRect(c,x-10,y-10,x+10,y+10,col,2);line(c,x-5,y,x+5,y,col,2);line(c,x,y-5,x,y+5,col,2)}
    private fun drawBg(c:Canvas,x:Float,y:Float,col:Int){round(c,x-11,y-11,x+11,y+11,3,Color.TRANSPARENT,col);line(c,x-6,y-6,x+6,y+6,col,2);line(c,x+6,y-6,x-6,y+6,col,2)}
    private fun drawBorder(c:Canvas,x:Float,y:Float,col:Int){strokeRect(c,x-11,y-11,x+11,y+11,col,2);strokeRect(c,x-6,y-6,x+6,y+6,col,1.3f)}
    private fun drawBlur(c:Canvas,x:Float,y:Float,col:Int){for(i in -1..1) for(j in -1..1) c.drawCircle(x+i*7,y+j*7,3,p.apply{color=col})}
    private fun drawOpacity(c:Canvas,x:Float,y:Float,col:Int){circleStroke(c,x,y,11,col,2);text(c,"½",x,y+7,16,col,true,true)}
    private fun drawZoom(c:Canvas,x:Float,y:Float,col:Int){circleStroke(c,x-3,y-3,8,col,2);line(c,x+3,y+3,x+12,y+12,col,2)}
    private fun drawTts(c:Canvas,x:Float,y:Float,col:Int){strokeRect(c,x-12,y-10,x+12,y+10,col,2);text(c,"A",x,y+6,13,col,true,true)}
    private fun drawMosaic(c:Canvas,x:Float,y:Float,col:Int){for(i in -1..1)for(j in -1..1)round(c,x+i*8-3,y+j*8-3,x+i*8+3,y+j*8+3,1,Color.TRANSPARENT,col)}
    private fun drawMagnifier(c:Canvas,x:Float,y:Float,col:Int){circleStroke(c,x-2,y-2,8,col,2);line(c,x+4,y+4,x+12,y+12,col,2);text(c,"+",x-2,y+3,10,col,true,true)}
    private fun drawStories(c:Canvas,x:Float,y:Float,col:Int){strokeRect(c,x-10,y-10,x+10,y+10,col,2);line(c,x-6,y-3,x+6,y-3,col,1.5f);line(c,x-6,y+2,x+6,y+2,col,1.5f)}
    private fun drawOverlayTrack(c:Canvas,x:Float,y:Float,col:Int){for(i in -1..1)for(j in -1..1)c.drawRect(x+i*8-3,y+j*8-3,x+i*8+3,y+j*8+3,p.apply{color=col})}

    private fun drawMusicIcon(c:Canvas,x:Float,y:Float){text(c,"♫",x,y+8,28,muted,true,true);text(c,"+",x+12,y+8,18,white,true,true)}
    private fun drawTextIcon(c:Canvas,x:Float,y:Float){round(c,x-10,y-11,x+10,y+10,2,Color.TRANSPARENT,muted);text(c,"T",x,y+7,18,muted,true,true);text(c,"+",x+13,y+9,18,white,true,true)}
    private fun drawImageIcon(c:Canvas,x:Float,y:Float){strokeRect(c,x-11,y-9,x+11,y+8,muted,1.8f);circle(c,x-4,y-3,2,muted);line(c,x-9,y+6,x-2,y,x+4,y+5,x+10,y-1,muted,1.7f);text(c,"+",x+14,y+9,18,white,true,true)}

    private fun drawPrev(c:Canvas,x:Float,y:Float){line(c,x-6,y,x-6,y+13,muted,2);Path().also{it.moveTo(x+6f,y);it.lineTo(x-5f,y+6f);it.lineTo(x+6f,y+13);it.close();p.color=muted;c.drawPath(it,p)}}
    private fun drawNext(c:Canvas,x:Float,y:Float){line(c,x+6,y,x+6,y+13,muted,2);Path().also{it.moveTo(x-6f,y);it.lineTo(x+5f,y+6f);it.lineTo(x-6f,y+13);it.close();p.color=muted;c.drawPath(it,p)}}
    private fun drawSliders(c:Canvas,x:Float,y:Float){line(c,x-9,y,x+9,y,white,1.7f);line(c,x-9,y+8,x+9,y+8,white,1.7f);circle(c,x+2,y,3,white);circle(c,x-3,y+8,3,white)}
    private fun drawUndo(c:Canvas,x:Float,y:Float,redo:Boolean){text(c,if(redo)"↷" else "↶",x,y+12,29,if(redo)dim:white,true,true)}
    private fun drawSave(c:Canvas,x:Float,y:Float){strokeRect(c,x-9,y-9,x+9,y+9,white,1.8f);c.drawRect(x-5,y-7,x+5,y-2,p.apply{color=white});strokeRect(c,x-4,y+2,x+4,y+7,white,1)}
    private fun drawShare(c:Canvas,x:Float,y:Float){line(c,x,y+8,x,y-8,white,2);line(c,x,y-8,x-6,y-2,white,2);line(c,x,y-8,x+6,y-2,white,2);line(c,x-9,y+2,x-9,y+9,white,2);line(c,x-9,y+9,x+9,y+9,white,2);line(c,x+9,y+9,x+9,y+2,white,2)}

    private fun round(c:Canvas,l:Float,t:Float,r:Float,b:Float,rad:Float,col:Int,border:Int?=null){p.style=Paint.Style.FILL;p.color=col;c.drawRoundRect(RectF(l,t,r,b),rad,rad,p);if(border!=null){p.style=Paint.Style.STROKE;p.strokeWidth=1f;p.color=border;c.drawRoundRect(RectF(l,t,r,b),rad,rad,p);p.style=Paint.Style.FILL}}
    private fun strokeRect(c:Canvas,l:Float,t:Float,r:Float,b:Float,col:Int,sw:Float){p.style=Paint.Style.STROKE;p.strokeWidth=sw;p.color=col;c.drawRect(l,t,r,b,p);p.style=Paint.Style.FILL}
    private fun circle(c:Canvas,x:Float,y:Float,r:Float,col:Int){p.color=col;p.style=Paint.Style.FILL;c.drawCircle(x,y,r,p)}
    private fun circleStroke(c:Canvas,x:Float,y:Float,r:Float,col:Int,sw:Float){p.color=col;p.style=Paint.Style.STROKE;p.strokeWidth=sw;c.drawCircle(x,y,r,p);p.style=Paint.Style.FILL}
    private fun line(c:Canvas,vararg a:Any){val x1=a[0] as Float;val y1=a[1] as Float;val x2=a[2] as Float;val y2=a[3] as Float;val col=a[4] as Int;val sw=a[5] as Float;p.color=col;p.strokeWidth=sw;p.style=Paint.Style.STROKE;c.drawLine(x1,y1,x2,y2,p);p.style=Paint.Style.FILL}
    private fun line(c:Canvas,x1:Int,y1:Int,x2:Int,y2:Int,col:Int,sw:Float){p.color=col;p.strokeWidth=sw;p.style=Paint.Style.STROKE;c.drawLine(x1.toFloat(),y1.toFloat(),x2.toFloat(),y2.toFloat(),p);p.style=Paint.Style.FILL}
    private fun line(c:Canvas,x1:Float,y1:Float,x2:Float,y2:Float,x3:Float,y3:Float,x4:Float,y4:Float,col:Int,sw:Float){line(c,x1,y1,x2,y2,col,sw);line(c,x2,y2,x3,y3,col,sw);line(c,x3,y3,x4,y4,col,sw)}
    private fun text(c:Canvas,s:String,x:Float,y:Float,size:Float,col:Int,bold:Boolean=false,center:Boolean=false){p.style=Paint.Style.FILL;p.color=col;p.textSize=size;p.typeface=if(bold)android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT;p.textAlign=if(center)Paint.Align.CENTER else Paint.Align.LEFT;c.drawText(s,x,y,p)}
    private fun triangle(c:Canvas,x1:Float,y1:Float,x2:Float,y2:Float,x3:Float,y3:Float,col:Int){p.color=col;p.style=Paint.Style.FILL;Path().also{it.moveTo(x1,y1);it.lineTo(x2,y2);it.lineTo(x3,y3);it.close();c.drawPath(it,p)}}
    private fun format(ms:Long):String=String.format("%d.%02d / %d.%02d",ms/1000,(ms%1000)/10,ms/1000,(ms%1000)/10)

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if(e.action != MotionEvent.ACTION_UP) return true
        val sx = 1536f / width; val sy = 824f / height
        val x=e.x*sx; val y=e.y*sy
        if(y<65 && x>1435){action("export");return true}
        if(y in 445f..500f && x in 735f..800f){action("play");return true}
        if(y in 615f..725f && x in 195f..855f){action("add");return true}
        if(y in 485f..625f && x<450f){
            when { y<530 -> action("music"); y<575 -> action("subtitle"); else -> action("overlay") }
            return true
        }
        if(y>745){
            val idx=((x-110)/70).toInt(); if(idx in tools.indices){selected=idx;action(tools[idx].replace("\n"," "));invalidate()};return true
        }
        return true
    }
}
