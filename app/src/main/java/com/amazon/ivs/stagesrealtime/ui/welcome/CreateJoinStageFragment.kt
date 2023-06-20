package com.amazon.ivs.stagesrealtime.ui.welcome

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.amazon.ivs.stagesrealtime.R
import com.amazon.ivs.stagesrealtime.common.*
import com.amazon.ivs.stagesrealtime.common.extensions.collect
import com.amazon.ivs.stagesrealtime.common.extensions.fadeAlpha
import com.amazon.ivs.stagesrealtime.common.extensions.navigate
import com.amazon.ivs.stagesrealtime.common.extensions.showErrorBar
import com.amazon.ivs.stagesrealtime.databinding.FragmentCreateJoinStageBinding
import com.amazon.ivs.stagesrealtime.ui.welcome.models.WelcomeSheetMode
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class CreateJoinStageFragment : Fragment(R.layout.fragment_create_join_stage) {
    private val binding by viewBinding(FragmentCreateJoinStageBinding::bind)
    private val viewModel by activityViewModels<WelcomeViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        collect(viewModel.userName) { userName ->
            binding.username.text = userName
        }

        collect(viewModel.onSignedOut) {
            navigate(CreateJoinStageFragmentDirections.toWelcome(isSignedOut = true))
        }

        collect(viewModel.onNavigateToStage) { stageMode ->
            navigate(
                CreateJoinStageFragmentDirections.toStage(
                    mode = stageMode.first,
                    createMode = stageMode.second
                )
            )
        }

        collect(viewModel.onError) { error ->
            Timber.d("Create stage failed")
            showErrorBar(error.errorResource)
        }

        collect(viewModel.onLoading) { isLoading ->
            binding.loadingView.root.fadeAlpha(isLoading)
        }

        with(binding) {
            settingsButton.setOnClickListener {
                navigate(CreateJoinStageFragmentDirections.toBottomSheet(mode = WelcomeSheetMode.SETTINGS))
            }
            createStageButton.setOnClickListener {
                navigate(CreateJoinStageFragmentDirections.toBottomSheet(mode = WelcomeSheetMode.PICK_MODE))
            }
            joinStageButton.setOnClickListener {
                viewModel.onJoinStage()
            }
            refreshUsernameButton.setOnClickListener {
                viewModel.refreshUserName()
            }
        }
    }
}
