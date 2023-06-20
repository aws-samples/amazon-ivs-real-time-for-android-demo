package com.amazon.ivs.stagesrealtime.ui.welcome

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.amazon.ivs.stagesrealtime.R
import com.amazon.ivs.stagesrealtime.common.extensions.collect
import com.amazon.ivs.stagesrealtime.common.extensions.navigate
import com.amazon.ivs.stagesrealtime.common.extensions.setVisibleOr
import com.amazon.ivs.stagesrealtime.common.extensions.showErrorBar
import com.amazon.ivs.stagesrealtime.common.extensions.toStringThousandOrZero
import com.amazon.ivs.stagesrealtime.common.viewBinding
import com.amazon.ivs.stagesrealtime.databinding.BottomSheetWelcomeBinding
import com.amazon.ivs.stagesrealtime.repository.models.CreateStageMode
import com.amazon.ivs.stagesrealtime.ui.welcome.models.WelcomeSheetMode
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class WelcomeBottomSheet : BottomSheetDialogFragment(R.layout.bottom_sheet_welcome) {
    private val binding by viewBinding(BottomSheetWelcomeBinding::bind)
    private val navArgs by navArgs<WelcomeBottomSheetArgs>()
    private val viewModel by activityViewModels<WelcomeViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        collect(viewModel.onCustomerCodeSet) { isCodeValid ->
            Timber.d("Signed in: $isCodeValid")
            val drawableResource = if (isCodeValid) R.drawable.bg_square_input_white else R.drawable.bg_square_input_red
            showErrorBar(R.string.error_customer_code)
            val inputBackground = ContextCompat.getDrawable(requireActivity(), drawableResource)
            binding.codeInput.background = inputBackground
            if (isCodeValid) dismissNow()
        }

        with(binding) {
            when (navArgs.mode) {
                WelcomeSheetMode.ENTER_CODE -> enterCodeLayout.setVisibleOr(true)
                WelcomeSheetMode.SETTINGS -> settingsLayout.setVisibleOr(true)
                WelcomeSheetMode.PICK_MODE -> pickViewModeLayout.setVisibleOr(true)
            }
        }

        setupListeners()
    }

    private fun setupListeners() = with(binding) {
        continueButton.setOnClickListener {
            viewModel.signIn(codeInput.text.toString())
        }

        changeCodeButton.setOnClickListener {
            dismissNow()
            viewModel.onSignOut()
        }

        cancelButton.setOnClickListener {
            dismissNow()
        }

        modeAudio.setOnClickListener {
            dismissNow()
            viewModel.onCreateStage(CreateStageMode.AUDIO)
        }

        modeVideo.setOnClickListener {
            dismissNow()
            viewModel.onCreateStage(CreateStageMode.VIDEO)
        }

        cancelModeButton.setOnClickListener {
            dismissNow()
        }

        bitrate.dataValue = viewModel.bitrate.toStringThousandOrZero()
        bitrateSlider.value = viewModel.bitrate.toFloat()

        bitrateSlider.addOnChangeListener { _, value, _ ->
            viewModel.bitrate = value.toInt()
            bitrate.value.text = value.toInt().toStringThousandOrZero()
        }

        scanQrCodeButton.setOnClickListener {
            navigate(WelcomeBottomSheetDirections.toScanner())
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        if (dialog is BottomSheetDialog) {
            dialog.behavior.skipCollapsed = true
            dialog.behavior.state = STATE_EXPANDED
        }
        return dialog
    }
}
