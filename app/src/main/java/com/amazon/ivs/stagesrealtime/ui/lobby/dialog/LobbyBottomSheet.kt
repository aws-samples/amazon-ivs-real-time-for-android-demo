package com.amazon.ivs.stagesrealtime.ui.lobby.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.amazon.ivs.stagesrealtime.R
import com.amazon.ivs.stagesrealtime.common.extensions.collectLatestWithLifecycle
import com.amazon.ivs.stagesrealtime.common.extensions.navigate
import com.amazon.ivs.stagesrealtime.common.extensions.setVisibleOr
import com.amazon.ivs.stagesrealtime.common.extensions.showErrorBar
import com.amazon.ivs.stagesrealtime.common.extensions.toStringThousandOrZero
import com.amazon.ivs.stagesrealtime.common.viewBinding
import com.amazon.ivs.stagesrealtime.databinding.BottomSheetWelcomeBinding
import com.amazon.ivs.stagesrealtime.repository.models.AppSettings
import com.amazon.ivs.stagesrealtime.repository.models.CreateStageMode
import com.amazon.ivs.stagesrealtime.ui.lobby.LobbyViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class LobbyBottomSheet : BottomSheetDialogFragment(R.layout.bottom_sheet_welcome) {
    private val binding by viewBinding(BottomSheetWelcomeBinding::bind)
    private val navArgs by navArgs<LobbyBottomSheetArgs>()
    private val viewModel by activityViewModels<LobbyViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            when (navArgs.mode) {
                LobbyDialogMode.ENTER_CODE -> enterCodeLayout.setVisibleOr(true)
                LobbyDialogMode.SETTINGS -> settingsLayout.setVisibleOr(true)
                LobbyDialogMode.PICK_MODE -> pickViewModeLayout.setVisibleOr(true)
            }

            collectLatestWithLifecycle(viewModel.onCustomerCodeSet) { isCodeValid ->
                Timber.d("Signed in: $isCodeValid")
                val drawableResource = if (isCodeValid) R.drawable.bg_square_input_white else R.drawable.bg_square_input_red
                showErrorBar(R.string.error_customer_code)
                val inputBackground = ContextCompat.getDrawable(requireActivity(), drawableResource)
                codeInput.background = inputBackground
                if (isCodeValid) dismissNow()
            }

            collectLatestWithLifecycle(viewModel.appSettings) { appSettings ->
                updateBitrateSliderState(appSettings)
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

        scanQrCodeButton.setOnClickListener {
            navigate(LobbyBottomSheetDirections.toScanner())
        }

        updateBitrateSliderState(viewModel.appSettings.value)
        simulcastSwitch.setOnCheckedChangeListener { _, isChecked ->
            Timber.d("Simulcast switch changed: $isChecked")
            viewModel.changeIsSimulcastEnabled(isChecked)
        }
        videoStatsSwitch.isChecked = viewModel.appSettings.value.isVideoStatsEnabled
        videoStatsSwitch.setOnCheckedChangeListener { _, isChecked ->
            Timber.d("Video stats switch changed: $isChecked")
            viewModel.changeIsVideoStatsEnabled(isChecked)
        }

        bitrateSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                Timber.d("Bitrate value changed: $value")
                viewModel.changeBitrate(value.toInt())
            }
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

    private fun BottomSheetWelcomeBinding.updateBitrateSliderState(appSettings: AppSettings) {
        val isSimulcastEnabled = appSettings.isSimulcastEnabled
        val bitrateValue = appSettings.bitrate
        bitrateSlider.isEnabled = !isSimulcastEnabled
        simulcastSwitch.isChecked = isSimulcastEnabled

        Timber.d("Bitrate slider updated: $appSettings")
        if (!isSimulcastEnabled) {
            bitrate.dataValueText.text = bitrateValue.toStringThousandOrZero()
            bitrate.dataValue = bitrateValue.toStringThousandOrZero()
            bitrateSlider.value = bitrateValue.toFloat()
        } else {
            bitrate.dataValue = resources.getString(R.string.auto)
        }
    }
}
