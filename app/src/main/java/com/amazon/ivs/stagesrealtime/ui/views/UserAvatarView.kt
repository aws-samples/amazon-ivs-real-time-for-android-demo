package com.amazon.ivs.stagesrealtime.ui.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.graphics.toColorInt
import com.amazon.ivs.stagesrealtime.R
import com.amazon.ivs.stagesrealtime.common.extensions.setVisibleOr
import com.amazon.ivs.stagesrealtime.databinding.ViewUserAvatarBinding
import com.amazon.ivs.stagesrealtime.repository.models.UserAvatar

class UserAvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val binding = ViewUserAvatarBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.UserAvatarView, 0, 0).run {
            try {
                val hasBorder = getBoolean(R.styleable.UserAvatarView_showBorder, false)
                binding.border.setVisibleOr(hasBorder)
                updateMargins(hasBorder)
            } finally {
                recycle()
            }
        }
    }

    fun setAvatar(userAvatar: UserAvatar?) {
        if (userAvatar == null) {
            binding.avatarRoot.setVisibleOr(false)
            return
        }
        updateMargins(userAvatar.hasBorder)
        binding.avatarRoot.setVisibleOr(true)
        binding.border.setVisibleOr(userAvatar.hasBorder)
        binding.colorLeft.setBackgroundTint(userAvatar.colorLeft)
        binding.colorRight.setBackgroundTint(userAvatar.colorRight)
        binding.colorBottom.setBackgroundTint(userAvatar.colorBottom)
        binding.root.requestLayout()
    }

    private fun updateMargins(hasBorder: Boolean) {
        val borderWidth = resources.getDimensionPixelSize(R.dimen.size_avatar_border)
        val marginAvatar = if (hasBorder) borderWidth else 0
        binding.border.setMargins(marginAvatar)
        binding.colorLeft.setMargins(marginAvatar)
        binding.colorRight.setMargins(marginAvatar)
        binding.colorBottom.setMargins(marginAvatar)
    }

    private fun View.setBackgroundTint(color: String) {
        backgroundTintList = ColorStateList.valueOf(color.toColorInt())
    }

    private fun View.setMargins(margin: Int) {
        (layoutParams as MarginLayoutParams).setMargins(margin, margin, margin, margin)
    }
}
