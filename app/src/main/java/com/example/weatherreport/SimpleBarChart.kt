package com.example.weatherreport

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class SimpleBarChart : View {
    constructor(ctx:Context) : super(ctx)
    constructor(ctx:Context,attr: AttributeSet) : super(ctx,attr)
    var input: List<Pair<Float,Long>> = listOf()
    private var paint = Paint()
    override fun onDraw(canvas: Canvas?) {
        //background
        val spacing = height/10f
        paint.setColor(Color.DKGRAY)
        paint.alpha = 180
        canvas?.drawRoundRect(0f,0f,width.toFloat(),height.toFloat(),spacing,spacing,paint)

        paint.setColor(Color.BLUE)
        paint.alpha = 255
        val columnwidth = ((width-spacing)/input.size)-spacing
        for(i in input.indices) {
            val factor = 1f-input[i].first
            val maxheight = height-(spacing*2)
            val adjustedheight= (factor)*(maxheight)
            canvas?.drawRect(spacing+(i*(spacing+columnwidth)),spacing+adjustedheight,(i+1)*(spacing+columnwidth),height-(spacing),paint)
        }

        paint.setColor(Color.WHITE)
        paint.textSize = spacing
        paint.alpha = 255

        var lasthour = "0"
        for(i in input.indices) {
            val hour = input[i].second.toDate("HH")
            canvas?.drawText(hour,spacing+(i*(spacing+columnwidth)),height-spacing,paint)
            if(hour.toInt() < lasthour.toInt()) {
                canvas?.drawRect((spacing/2)+(i*(spacing+columnwidth))-1,spacing,(spacing/2)+(i*(spacing+columnwidth))+1,height-spacing,paint)
            }
            lasthour = hour
        }
    }
}