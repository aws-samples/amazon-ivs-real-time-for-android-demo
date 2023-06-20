package com.amazon.ivs.stagesrealtime.ui.stage

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.amazon.ivs.stagesrealtime.R
import com.amazon.ivs.stagesrealtime.common.viewBinding
import com.amazon.ivs.stagesrealtime.databinding.BottomSheetLeaveDeleteStageBinding
import com.amazon.ivs.stagesrealtime.ui.stage.models.LeaveDeleteStageMode
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LeaveDeleteStageBottomSheet : BottomSheetDialogFragment(R.layout.bottom_sheet_leave_delete_stage) {
    private val binding by viewBinding(BottomSheetLeaveDeleteStageBinding::bind)
    private val navArgs by navArgs<LeaveDeleteStageBottomSheetArgs>()
    private val viewModel by activityViewModels<StageViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            when (navArgs.mode) {
                LeaveDeleteStageMode.LEAVE -> {
                    leaveDeleteTitle.text = getString(R.string.leave_current_stage)
                    leaveDeleteButton.text = getString(R.string.leave_stage)
                    leaveDeleteButton.setOnClickListener {
                        viewModel.stopPublishing()
                        if (navArgs.shouldDisconnectAndClearResources) {
                            viewModel.disconnectFromCurrentStage()
                            viewModel.clearResources()
                        }
                        viewModel.shouldCloseFeed(navArgs.shouldCloseFeed)
                        dismissNow()
                    }
                }
                LeaveDeleteStageMode.KICK_USER -> {
                    leaveDeleteTitle.text = getString(R.string.remove_participant)
                    leaveDeleteButton.text = getString(R.string.remove)
                    leaveDeleteButton.setOnClickListener {
                        viewModel.kickParticipant()
                        dismissNow()
                    }
                }
                LeaveDeleteStageMode.DELETE -> {
                    leaveDeleteTitle.text = getString(R.string.end_stage)
                    leaveDeleteButton.text = getString(R.string.end_stage_for_everyone)
                    leaveDeleteButton.setOnClickListener {
                        viewModel.deleteStage()
                        viewModel.shouldCloseFeed(navArgs.shouldCloseFeed)
                        dismissNow()
                    }
                }
            }

            cancelButton.setOnClickListener {
                dismissNow()
            }
        }
    }
}
