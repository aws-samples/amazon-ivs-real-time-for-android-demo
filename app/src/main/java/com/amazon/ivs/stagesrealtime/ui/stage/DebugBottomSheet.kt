package com.amazon.ivs.stagesrealtime.ui.stage

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.amazon.ivs.stagesrealtime.R
import com.amazon.ivs.stagesrealtime.common.extensions.collect
import com.amazon.ivs.stagesrealtime.common.extensions.firstCharUpperCase
import com.amazon.ivs.stagesrealtime.common.extensions.formatStringOrEmpty
import com.amazon.ivs.stagesrealtime.common.extensions.setVisibleOr
import com.amazon.ivs.stagesrealtime.common.viewBinding
import com.amazon.ivs.stagesrealtime.databinding.BottomSheetDebugBinding
import com.amazon.ivs.stagesrealtime.ui.stage.adapters.RTCDataAdapter
import com.amazon.ivs.stagesrealtime.ui.stage.models.RTCDataUIItemModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class DebugBottomSheet : BottomSheetDialogFragment(R.layout.bottom_sheet_debug) {
    private val binding by viewBinding(BottomSheetDebugBinding::bind)
    private val viewModel by activityViewModels<StageViewModel>()
    private val clipboard by lazy {
        ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)!!
    }
    private val adapter by lazy {
        RTCDataAdapter()
    }
    private lateinit var lastRawRTSStats: String

    override fun getTheme() = R.style.BottomSheetDialogTheme_Debug

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.requestRTCStats()

        with(binding) {
            qualityValues.setVisibleOr(viewModel.isCurrentStageVideo())
            fps.root.setVisibleOr(viewModel.isCurrentStageVideo())

            participantLayout.setVisibleOr(viewModel.isParticipating())
            guestLayout.setVisibleOr(!viewModel.isParticipating())

            streamsData.adapter = adapter
            streamsData.itemAnimator = null
            dismissButton.setOnClickListener { dismissNow() }
            copyButton.setOnClickListener {
                clipboard.setPrimaryClip(ClipData.newPlainText("RTC stats", lastRawRTSStats))
            }

            topIndent.setOnClickListener {
                dismiss()
            }
        }

        collect(viewModel.rtcData) { data ->
            Timber.d("RTC data received: \n$data")
            lastRawRTSStats = data.rawRTCStats ?: ""
            with(binding) {
                streamQuality.dataValue = data.streamQuality?.name?.lowercase()?.firstCharUpperCase() ?: "-"
                cpuTime.dataValue = data.cpuLimitedTime?.let { getString(R.string.s_template, it) } ?: "-"
                networkTime.dataValue = data.networkLimitedTime?.let { getString(R.string.s_template, it) } ?: "-"
                latency.dataValue = data.latency?.let { getString(R.string.ms_template, it) } ?: "-"
                fps.dataValue = data.fps ?: "-"
                packetLoss.dataValue = data.packetLoss?.let {
                    String.format(getString(R.string.percentage_template), it)
                } ?: "-"
            }
        }

        collect(viewModel.rtcDataList) { dataList ->
            Timber.d("RTC data list received: \n$dataList")
            adapter.submitList(dataList)
            lastRawRTSStats = dataList.asStringData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopRequestingRTCStats()
    }

    private fun List<RTCDataUIItemModel>.asStringData(): String {
        var combinedString = ""
        this.forEach { data ->
            combinedString += "${data.username}:" +
                    data.latency.formatStringOrEmpty { "\n${getString(R.string.latency)}:$it" }.prependIndent() +
                    data.fps.formatStringOrEmpty { "\n${getString(R.string.fps)}:$it" }.prependIndent() +
                    data.packetsLost.formatStringOrEmpty {
                        "\n${getString(R.string.packet_loss)}:$it"
                    }.prependIndent() + "\n"
        }
        return combinedString
    }
}
