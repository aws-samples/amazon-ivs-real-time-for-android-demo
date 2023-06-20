package com.amazon.ivs.stagesrealtime.ui.stage

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import com.amazon.ivs.stagesrealtime.R
import com.amazon.ivs.stagesrealtime.common.viewBinding
import com.amazon.ivs.stagesrealtime.databinding.BottomSheetJoinStageBinding
import com.amazon.ivs.stagesrealtime.repository.networking.models.StageMode
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class JoinStageBottomSheet : BottomSheetDialogFragment(R.layout.bottom_sheet_join_stage) {
    private val binding by viewBinding(BottomSheetJoinStageBinding::bind)
    private val viewModel by activityViewModels<StageViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            cancelJoinButton.setOnClickListener { dismissNow() }
            guestButton.setOnClickListener {
                viewModel.startPublishing(StageMode.GUEST_SPOT)
                dismissNow()
            }
            pkButton.setOnClickListener {
                viewModel.startPublishing(StageMode.PK)
                dismissNow()
            }
        }
    }
}
