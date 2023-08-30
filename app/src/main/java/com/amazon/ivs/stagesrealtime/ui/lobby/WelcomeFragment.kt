package com.amazon.ivs.stagesrealtime.ui.lobby

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.amazon.ivs.stagesrealtime.R
import com.amazon.ivs.stagesrealtime.common.*
import com.amazon.ivs.stagesrealtime.common.extensions.collectLatestWithLifecycle
import com.amazon.ivs.stagesrealtime.common.extensions.navigate
import com.amazon.ivs.stagesrealtime.databinding.FragmentWelcomeBinding
import com.amazon.ivs.stagesrealtime.ui.lobby.dialog.LobbyDialogMode
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class WelcomeFragment : Fragment(R.layout.fragment_welcome) {
    private val binding by viewBinding(FragmentWelcomeBinding::bind)
    private val navArgs by navArgs<WelcomeFragmentArgs>()
    private val viewModel by activityViewModels<LobbyViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (navArgs.isSignedOut) {
            navigate(WelcomeFragmentDirections.toBottomSheet(mode = LobbyDialogMode.ENTER_CODE))
        }

        collectLatestWithLifecycle(viewModel.onSignedIn) {
            Timber.d("Signed in")
            navigate(WelcomeFragmentDirections.toCreateJoinStage())
        }

        with(binding) {
            startButton.setOnClickListener {
                navigate(WelcomeFragmentDirections.toBottomSheet(mode = LobbyDialogMode.ENTER_CODE))
            }
        }

        viewModel.silentSignIn()
    }
}
