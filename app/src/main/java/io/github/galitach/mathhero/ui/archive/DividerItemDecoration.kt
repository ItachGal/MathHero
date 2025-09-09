package io.github.galitach.mathhero.ui.archive

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.RecyclerView
import io.github.galitach.mathhero.R

class DividerItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

    private val divider: Drawable?

    init {
        val attrs = intArrayOf(android.R.attr.listDivider)
        val styledAttributes = context.obtainStyledAttributes(attrs)
        try {
            divider = styledAttributes.getDrawable(0)
        } finally {
            styledAttributes.recycle()
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        divider?.let {
            val left = parent.paddingLeft + (parent.context.resources.getDimension(R.dimen.archive_divider_margin))
            val right = parent.width - parent.paddingRight - (parent.context.resources.getDimension(R.dimen.archive_divider_margin))

            for (i in 0 until parent.childCount - 1) {
                val child = parent.getChildAt(i)
                val params = child.layoutParams as RecyclerView.LayoutParams
                val top = child.bottom + params.bottomMargin
                val bottom = top + it.intrinsicHeight
                it.setBounds(left.toInt(), top, right.toInt(), bottom)
                it.draw(c)
            }
        }
    }
}