package com.example.medicinereminder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridDividerItemDecoration(
    context: Context,
    private val color: Int,
    private val thickness: Float
) : RecyclerView.ItemDecoration() {

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = thickness
        this.color = color
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams

            // Draw horizontal lines
            val left = child.left - params.leftMargin
            val right = child.right + params.rightMargin
            val top = child.bottom + params.bottomMargin
            val bottom = top + thickness.toInt()

            c.drawLine(left.toFloat(), top.toFloat(), right.toFloat(), top.toFloat(), paint)

            // Draw vertical lines
            val topLine = child.top - params.topMargin
            val bottomLine = child.bottom + params.bottomMargin
            val start = child.right + params.rightMargin
            val end = start + thickness.toInt()

            c.drawLine(start.toFloat(), topLine.toFloat(), start.toFloat(), bottomLine.toFloat(), paint)
        }
    }

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        outRect.set(1, 1, 1, 1)  // Offsets for top, left, right, bottom (thin borders)
    }
}
