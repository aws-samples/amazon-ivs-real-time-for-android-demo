package com.amazon.ivs.stagesrealtime.common.heartview

import android.graphics.Path
import android.graphics.PathMeasure
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation

// Second variation of FloatAnimation that should be used together with new PathOutSlowAnimator
class FloatFadedAnimation(
    path: Path,
    private val rotation: Float,
    parent: View,
    private val child: View,
    private val shouldFade: Boolean = false
) : Animation() {

    private val pathMeasure: PathMeasure = PathMeasure(path, false)
    private val distance: Float = pathMeasure.length

    init {
        parent.setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    override fun applyTransformation(interpolatedTime: Float, transformation: Transformation?) {
        pathMeasure.getMatrix(distance * interpolatedTime, transformation?.matrix, PathMeasure.POSITION_MATRIX_FLAG)
        child.rotation = rotation * interpolatedTime

        if (shouldFade) {
            transformation?.alpha = 1f - interpolatedTime
        }
    }
}
