package com.amazon.ivs.stagesrealtime.ui.lobby

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.amazon.ivs.stagesrealtime.R
import com.amazon.ivs.stagesrealtime.common.*
import com.amazon.ivs.stagesrealtime.common.extensions.collectLatestWithLifecycle
import com.amazon.ivs.stagesrealtime.common.extensions.fadeAlpha
import com.amazon.ivs.stagesrealtime.common.extensions.navigate
import com.amazon.ivs.stagesrealtime.common.extensions.showErrorBar
import com.amazon.ivs.stagesrealtime.databinding.FragmentLobbyBinding
import com.amazon.ivs.stagesrealtime.ui.lobby.dialog.LobbyDialogMode
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class LobbyFragment : Fragment(R.layout.fragment_lobby) {
    private val binding by viewBinding(FragmentLobbyBinding::bind)
    private val viewModel by activityViewModels<LobbyViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        collectLatestWithLifecycle(viewModel.appSettings) { settings ->
            binding.username.text = settings.stageId
        }

        collectLatestWithLifecycle(viewModel.onSignedOut) {
            navigate(LobbyFragmentDirections.toWelcome(isSignedOut = true))
        }

        collectLatestWithLifecycle(viewModel.onNavigateToStage) { stageMode ->
            navigate(
                LobbyFragmentDirections.toStage(
                    mode = stageMode.first,
                    createMode = stageMode.second
                )
            )
        }

        collectLatestWithLifecycle(viewModel.onError) { error ->
            Timber.d("Create stage failed: $error")
            showErrorBar(error.errorResource)
        }

        collectLatestWithLifecycle(viewModel.isLoading) { isLoading ->
            binding.loadingView.root.fadeAlpha(isLoading)
        }

        with(binding) {
            settingsButton.setOnClickListener {
                navigate(LobbyFragmentDirections.toBottomSheet(mode = LobbyDialogMode.SETTINGS))
            }
            createStageButton.setOnClickListener {
                navigate(LobbyFragmentDirections.toBottomSheet(mode = LobbyDialogMode.PICK_MODE))
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
