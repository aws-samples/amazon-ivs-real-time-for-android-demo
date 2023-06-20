package com.amazon.ivs.stagesrealtime.common.extensions

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.amazon.ivs.stagesrealtime.R
import com.amazon.ivs.stagesrealtime.common.PK_WINNER_START_SCALE
import com.amazon.ivs.stagesrealtime.databinding.ViewErrorBarBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

fun View.setVisibleOr(show: Boolean, orWhat: Int = View.GONE) {
    this.visibility = if (show) View.VISIBLE else orWhat
}

fun Snackbar.showOnTop() {
    this.view.layoutParams = (this.view.layoutParams as FrameLayout.LayoutParams).apply {
        gravity = Gravity.TOP
    }
    this.show()
}

@SuppressLint("ShowToast")
fun Fragment.showErrorBar(@StringRes error: Int) {
    val parentView = activity!!.window!!.decorView
    val errorView = ViewErrorBarBinding.inflate(layoutInflater)
    errorView.message.text = parentView.context.getString(error)
    showErrorBar(parentView, errorView)
}

fun BottomSheetDialogFragment.showErrorBar(@StringRes error: Int) {
    val parentView = dialog!!.window!!.decorView
    val errorView = ViewErrorBarBinding.inflate(layoutInflater)
    errorView.message.text = parentView.context.getString(error)
    showErrorBar(parentView, errorView)
}

fun View.fadeAlpha(
    fadeIn: Boolean,
    outVisibility: Int = View.GONE
) {
    if (fadeIn) {
        fadeIn()
    } else {
        fadeOut(outVisibility)
    }
}

fun View.fadeIn() {
    visibility = View.VISIBLE
    animate()
        .alphaBy(1 - alpha)
        .setDuration(resources.getInteger(R.integer.fade_duration).toLong())
        .withEndAction {
            visibility = View.VISIBLE
            alpha = 1f
        }.start()
}

fun View.fadeOut(outVisibility: Int = View.GONE) {
    alpha = 1f
    animate()
        .alpha(0f)
        .setDuration(resources.getInteger(R.integer.fade_duration).toLong())
        .withEndAction {
            visibility = outVisibility
        }.start()
}

fun View.scaleInAndFade() {
    visibility = View.VISIBLE
    scaleX = PK_WINNER_START_SCALE
    scaleY = PK_WINNER_START_SCALE
    animate()
        .alphaBy(1 - alpha)
        .scaleX(1f)
        .scaleY(1f)
        .setDuration(resources.getInteger(R.integer.fade_duration).toLong())
        .withEndAction {
            visibility = View.VISIBLE
            alpha = 1f
        }.start()
}

fun View.scaleOutAndFade(outVisibility: Int = View.GONE) {
    alpha = 1f
    animate()
        .alpha(0f)
        .scaleX(PK_WINNER_START_SCALE)
        .scaleY(PK_WINNER_START_SCALE)
        .setDuration(resources.getInteger(R.integer.fade_duration).toLong())
        .withEndAction {
            visibility = outVisibility
        }.start()
}

fun View.isKeyboardVisible() = ViewCompat.getRootWindowInsets(this)?.isVisible(WindowInsetsCompat.Type.ime())

fun Fragment.hideKeyboard() = activity?.hideKeyboard()

fun FragmentActivity.hideKeyboard() {
    val view = currentFocus ?: window.decorView
    val token = view.windowToken
    view.clearFocus()
    ContextCompat.getSystemService(this, InputMethodManager::class.java)?.hideSoftInputFromWindow(token, 0)
}

fun Fragment.showKeyboard() = activity?.showKeyboard()

fun FragmentActivity.showKeyboard() {
    val view = currentFocus ?: window.decorView
    view.requestFocus()
    Timber.d("Show keyboard")
    ContextCompat.getSystemService(this, InputMethodManager::class.java)?.showSoftInput(view, 0)
}

@SuppressLint("ShowToast")
private fun showErrorBar(parentView: View, errorView: ViewErrorBarBinding) {
    val snackBar = Snackbar.make(parentView, "", Snackbar.LENGTH_LONG)
    val snackBarLayout = snackBar.view as Snackbar.SnackbarLayout
    snackBarLayout.setBackgroundColor(Color.TRANSPARENT)
    snackBarLayout.addView(errorView.root)
    snackBar.showOnTop()
}
