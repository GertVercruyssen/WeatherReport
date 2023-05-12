package com.example.weatherreport

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class SimpleGraph: View {
    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attr: AttributeSet) : super(ctx,attr)
    var input: List<Float> = listOf()
    var firsttimestamp: Long = 0 //amount of records before a new day starts
    private var paint = Paint()
    override fun onDraw(canvas: Canvas?) {
        //background
        val spacing = height/10f
        paint.setColor(Color.DKGRAY)
        paint.alpha = 180
        canvas?.drawRoundRect(0f,0f,width.toFloat(),height.toFloat(),spacing,spacing,paint)

        paint.setColor(Color.LTGRAY)
        paint.alpha = 255
        paint.strokeWidth = 1.5f
        var offset: Int = (firsttimestamp.toDate("HH").toInt())/4
        val distance = ((width-(spacing*2))/(input.size-1))
        for(i in 0 until input.size-1) {
            if((i-offset)%8 == 0)
            canvas?.drawLine(spacing+((i+0.5f)*distance),0f,spacing+((i+0.5f)*distance),height.toFloat(),paint)
        }

        val maxtemp = input.max()
        val lowtemp = input.min()

        paint.color = Color.YELLOW
        paint.alpha = 255
        paint.strokeWidth = 2f
        val heightmultiplier = ((height)/(maxtemp-lowtemp))

        for(i in 0 until input.size-1) {
            canvas?.drawLine(spacing+(i*distance),height-((input[i]-lowtemp)*heightmultiplier),spacing+((i+1)*distance),height-((input[(i+1)]-lowtemp)*heightmultiplier),paint)
        }

        paint.setColor(Color.WHITE)
        paint.textSize = spacing
        paint.alpha = 255
        canvas?.drawText(String.format("%.1f", maxtemp),spacing,spacing*2, paint)
        canvas?.drawText(String.format("%.1f", lowtemp),spacing,height-spacing, paint)
    }
}