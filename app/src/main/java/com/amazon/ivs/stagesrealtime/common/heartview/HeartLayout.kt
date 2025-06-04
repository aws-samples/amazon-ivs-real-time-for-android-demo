package com.amazon.ivs.stagesrealtime.common.heartview

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.content.withStyledAttributes
import com.amazon.ivs.stagesrealtime.R

class HeartLayout : RelativeLayout {
    private lateinit var animator: PathOutSlowAnimator

    constructor(context: Context?) : super(context!!) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs, defStyleAttr)
    }

    private fun init(attrs: AttributeSet?, defStyleAttr: Int) {
        context.withStyledAttributes(attrs, R.styleable.HeartLayout, defStyleAttr, 0) {
            animator = PathOutSlowAnimator(toHeartConfig())
        }
    }

    fun clearAllViews() {
        (0 .. childCount).forEach { index ->
            getChildAt(index)?.clearAnimation()
        }
        removeAllViews()
    }

    fun addHeart() {
        animator.start(ImageView(context).createHeartView(), this)
    }
}
