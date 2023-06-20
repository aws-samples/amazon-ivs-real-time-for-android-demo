package com.amazon.ivs.stagesrealtime.common.heartview

import android.graphics.Path
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import com.amazon.ivs.stagesrealtime.common.HEART_ANIMATION_FADE_DURATION
import com.amazon.ivs.stagesrealtime.common.extensions.launchMain
import java.util.Random
import kotlin.math.abs

// Second variation of new heart animation, that has more flexibility
// TODO code could be improved
class PathOutSlowAnimator(private val configuration: HeartConfig) {

    private val xPathValuesBeforeSlow = mutableMapOf<View, Float>()

    fun start(child: ImageView, parent: ViewGroup) {
        parent.addView(child, ViewGroup.LayoutParams(configuration.heartWidth, configuration.heartHeight))
        var slowFloatFadeAnimation: Animation? = null
        val floatUpAnimation = FloatFadedAnimation(createPath(parent, child), randomAnimation(), parent, child).apply {
            duration = configuration.animDuration.toLong()
            interpolator = LinearInterpolator()
            setAnimationListener(createAnimationListener {
                child.startAnimation(slowFloatFadeAnimation)
            })
        }
        slowFloatFadeAnimation =
            FloatFadedAnimation(createShortPath(child), randomAnimation(), parent, child, true).apply {
                duration = HEART_ANIMATION_FADE_DURATION
                interpolator = LinearInterpolator()
                setAnimationListener(createAnimationListener {
                    launchMain {
                        parent.removeView(child)
                        xPathValuesBeforeSlow.remove(child)
                    }
                })
            }
        child.startAnimation(floatUpAnimation)
    }

    private fun randomAnimation() = Random().nextFloat()

    private fun createPath(view: View, childView: View): Path {
        val rand = Random()
        val factor = 2
        var x = abs(configuration.x - rand.nextInt(configuration.rand))
        val y = view.height - configuration.y
        var x2 = abs(x - rand.nextInt(configuration.rand))
        val y2 = abs(0f - configuration.yOffset)
        val value = y2 / configuration.factor

        x += configuration.point * (1 + rand.nextInt(2))
        x2 += configuration.point * (1 + rand.nextInt(2))
        xPathValuesBeforeSlow[childView] = x
        val y3 = y - y2
        return Path().apply {
            moveTo(configuration.x, y)
            cubicTo(configuration.x, (y - value), x, y2 + value, x, y2)
            moveTo(x, y2)
            cubicTo(x, (y2 - value), x2, (y3 + factor), x2, y3)
        }
    }

    private fun createShortPath(childView: View): Path {
        val factor = 2
        val y = abs(0f - configuration.yOffset)
        val y2 = 0f
        val value = y2 / configuration.factor
        val x = xPathValuesBeforeSlow[childView] ?: 0f
        val y3 = y - y2
        return Path().apply {
            moveTo(x, y)
            cubicTo(x, (y - value), x, y2 + value, x, y2)
            moveTo(x, y2)
            cubicTo(x, (y2 - value), x, (y3 + factor), x, y3)
        }
    }

    private fun createAnimationListener(onAnimationEnd: () -> Unit) = object : Animation.AnimationListener {
        override fun onAnimationRepeat(animation: Animation?) {
            /* Ignored */
        }

        override fun onAnimationEnd(animation: Animation?) {
            onAnimationEnd()
        }

        override fun onAnimationStart(animation: Animation?) {
            /* Ignored */
        }
    }
}
