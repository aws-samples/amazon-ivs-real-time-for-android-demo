package com.amazon.ivs.stagesrealtimecompose.ui

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amazon.ivs.stagesrealtimecompose.core.handlers.Destination
import com.amazon.ivs.stagesrealtimecompose.core.handlers.DialogDestination
import com.amazon.ivs.stagesrealtimecompose.core.handlers.ErrorDestination
import com.amazon.ivs.stagesrealtimecompose.core.handlers.NavigationHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.ShakeHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.networking.Error
import com.amazon.ivs.stagesrealtimecompose.ui.components.ErrorBanner
import com.amazon.ivs.stagesrealtimecompose.ui.components.LoadingOverlay
import com.amazon.ivs.stagesrealtimecompose.ui.components.PermissionRequester
import com.amazon.ivs.stagesrealtimecompose.ui.dialogs.DebugDialog
import com.amazon.ivs.stagesrealtimecompose.ui.dialogs.DialogContainer
import com.amazon.ivs.stagesrealtimecompose.ui.dialogs.DialogOverlay
import com.amazon.ivs.stagesrealtimecompose.ui.dialogs.EndStageDialog
import com.amazon.ivs.stagesrealtimecompose.ui.dialogs.EnterCodeDialog
import com.amazon.ivs.stagesrealtimecompose.ui.dialogs.JoinStageDialog
import com.amazon.ivs.stagesrealtimecompose.ui.dialogs.KickParticipantDialog
import com.amazon.ivs.stagesrealtimecompose.ui.dialogs.LeaveAudioRoomDialog
import com.amazon.ivs.stagesrealtimecompose.ui.dialogs.LeaveStageDialog
import com.amazon.ivs.stagesrealtimecompose.ui.dialogs.SelectExperienceDialog
import com.amazon.ivs.stagesrealtimecompose.ui.dialogs.settings.SettingsDialog
import com.amazon.ivs.stagesrealtimecompose.ui.screens.landing.LandingScreen
import com.amazon.ivs.stagesrealtimecompose.ui.screens.splash.QRScreen
import com.amazon.ivs.stagesrealtimecompose.ui.screens.splash.SplashScreen
import com.amazon.ivs.stagesrealtimecompose.ui.screens.stage.StageScreen
import com.amazon.ivs.stagesrealtimecompose.ui.theme.GrayPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.StagesRealtimeComposeTheme
import com.amazon.ivs.stagesrealtimecompose.ui.theme.WhitePrimary
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb())
        )
        super.onCreate(savedInstanceState)
        ShakeHandler.setup(this)
        setContent {
            StagesRealtimeComposeTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) { innerPadding ->
                    val destination by viewModel.destination.collectAsStateWithLifecycle()
                    val dialogDestination by viewModel.dialogDestination.collectAsStateWithLifecycle()
                    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

                    BackHandler(onBack = NavigationHandler::goBack)
                    ChangeSystemBarIcons(useDarkIcons = destination.useDarkIcons)
                    PermissionRequester()
                    MainActivityContent(
                        innerPadding = innerPadding,
                        destination = destination,
                        isLoading = isLoading,
                        dialogDestination = dialogDestination,
                        onFinish = {
                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ShakeHandler.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        ShakeHandler.onPause(this)
    }
}

@Composable
private fun MainActivityContent(
    destination: Destination,
    dialogDestination: DialogDestination,
    isLoading: Boolean,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onFinish: () -> Unit
) {
    Box(
        modifier = modifier.padding(bottom = innerPadding.calculateBottomPadding())
    ) {
        Crossfade(
            modifier = Modifier.fillMaxSize(),
            targetState = destination,
        ) { target ->
            when (target) {
                Destination.Finish -> onFinish()
                Destination.None -> return@Crossfade
                Destination.QR -> QRScreen()
                Destination.Splash -> SplashScreen()
                is Destination.Landing -> LandingScreen(innerPadding)
                is Destination.Stage -> StageScreen(
                    stageType = target.type,
                    innerPadding = innerPadding
                )
            }
        }

        Crossfade(
            modifier = Modifier.fillMaxSize(),
            targetState = dialogDestination
        ) { target ->
            if (target == DialogDestination.None) return@Crossfade
            val background = if (target is DialogDestination.Debug) WhitePrimary else GrayPrimary

            DialogOverlay {
                DialogContainer(
                    background = background
                ) {
                    when (target) {
                        DialogDestination.Debug -> DebugDialog()
                        DialogDestination.EndStage -> EndStageDialog()
                        DialogDestination.EnterCode -> EnterCodeDialog()
                        DialogDestination.Experience -> SelectExperienceDialog()
                        DialogDestination.JoinStage -> JoinStageDialog()
                        DialogDestination.KickParticipant -> KickParticipantDialog()
                        DialogDestination.LeaveStage -> LeaveStageDialog()
                        DialogDestination.LeaveAudioRoom -> LeaveAudioRoomDialog()
                        DialogDestination.None -> return@DialogContainer
                        DialogDestination.Settings -> SettingsDialog()
                    }
                }
            }
        }

        LoadingOverlay(isLoading = isLoading)
        ErrorBanner(innerPadding = innerPadding)
    }
}

@Composable
private fun ChangeSystemBarIcons(useDarkIcons: Boolean) {
    val context = LocalContext.current
    val view = LocalView.current

    LaunchedEffect(useDarkIcons) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val insetsController = WindowInsetsControllerCompat(window, view)

        Timber.d("Use dark icons: $useDarkIcons")
        insetsController.isAppearanceLightStatusBars = useDarkIcons
        insetsController.isAppearanceLightNavigationBars = useDarkIcons
    }
}

@Preview(showBackground = true)
@Composable
private fun MainActivityContentPreview() {
    StagesRealtimeComposeTheme {
        NavigationHandler.showError(ErrorDestination.SnackBar(Error.CustomerCodeError))
        MainActivityContent(
            destination = Destination.Splash,
            dialogDestination = DialogDestination.EnterCode,
            innerPadding = PaddingValues(),
            isLoading = true,
            onFinish = {}
        )
    }
}
