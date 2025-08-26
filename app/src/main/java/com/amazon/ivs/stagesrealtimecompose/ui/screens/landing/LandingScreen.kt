package com.amazon.ivs.stagesrealtimecompose.ui.screens.landing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amazon.ivs.stagesrealtimecompose.R
import com.amazon.ivs.stagesrealtimecompose.core.handlers.Destination
import com.amazon.ivs.stagesrealtimecompose.core.handlers.DialogDestination
import com.amazon.ivs.stagesrealtimecompose.core.handlers.NavigationHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.StageDestinationType
import com.amazon.ivs.stagesrealtimecompose.core.handlers.User
import com.amazon.ivs.stagesrealtimecompose.core.handlers.UserHandler
import com.amazon.ivs.stagesrealtimecompose.ui.components.ButtonImage
import com.amazon.ivs.stagesrealtimecompose.ui.components.DesktopPreview
import com.amazon.ivs.stagesrealtimecompose.ui.components.LandscapePreview
import com.amazon.ivs.stagesrealtimecompose.ui.components.PortraitPreview
import com.amazon.ivs.stagesrealtimecompose.ui.components.PreviewSurface
import com.amazon.ivs.stagesrealtimecompose.ui.components.SquarePreview
import com.amazon.ivs.stagesrealtimecompose.ui.components.fillMaxPortraitWidth
import com.amazon.ivs.stagesrealtimecompose.ui.theme.BlackSecondary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.GrayPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.GraySecondary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.InterPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.OrangePrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.RobotoMonoPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.RobotoPrimary

@Composable
fun LandingScreen(
    innerPadding: PaddingValues
) {
    val user by UserHandler.selfUser.collectAsStateWithLifecycle()

    LandingScreenContent(
        user = user,
        innerPadding = innerPadding,
        onRefreshStageId = UserHandler::refreshUserName
    )
}

@Composable
private fun LandingScreenContent(
    innerPadding: PaddingValues,
    user: User,
    onRefreshStageId: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GrayPrimary)
            .padding(top = innerPadding.calculateTopPadding()),
    ) {
        Row(
            modifier = Modifier.padding(
                top = 20.dp,
                start = 20.dp,
                end = 12.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ButtonImage(
                modifier = Modifier.size(42.dp),
                image = R.drawable.ic_refresh,
                description = stringResource(R.string.dsc_refresh_button),
                onClick = onRefreshStageId
            )
            Text(
                modifier = Modifier
                    .padding(horizontal = 10.dp)
                    .weight(1f),
                text = user.username,
                style = RobotoMonoPrimary
            )
            ButtonImage(
                modifier = Modifier.size(42.dp),
                image = R.drawable.ic_settings,
                background = Color.Transparent,
                description = stringResource(R.string.dsc_settings_button),
                onClick = {
                    NavigationHandler.showDialog(DialogDestination.Settings)
                }
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxPortraitWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 30.dp)
        ) {
            Row {
                Text(
                    text = stringResource(R.string.ivs),
                    style = InterPrimary.copy(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.W900
                    )
                )
                Text(
                    text = stringResource(R.string.real_time),
                    style = InterPrimary.copy(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.W900
                    ),
                    color = OrangePrimary
                )
            }
            LandingButton(
                modifier = Modifier.padding(top = 40.dp),
                text = stringResource(R.string.create_new_stage),
                onClick = {
                    NavigationHandler.showDialog(DialogDestination.Experience)
                }
            )
            LandingButton(
                modifier = Modifier.padding(top = 10.dp),
                text = stringResource(R.string.join_stage_feed_view),
                color = GraySecondary,
                onClick = {
                    NavigationHandler.goTo(Destination.Stage(StageDestinationType.None))
                }
            )
        }
    }
}

@Composable
private fun LandingButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = OrangePrimary,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = color,
                shape = shape
            )
            .clickable {
                onClick()
            }
            .clip(shape)
    ) {
        Text(
            modifier = Modifier.padding(
                start = 30.dp,
                top = 64.dp,
                bottom = 28.dp
            ),
            text = text,
            style = RobotoPrimary,
            color = BlackSecondary,
        )
    }
}

@PortraitPreview
@Composable
private fun LandingPortrait() {
    LandingScreenContentPreview()
}

@SquarePreview
@Composable
private fun LandingSquare() {
    LandingScreenContentPreview()
}

@LandscapePreview
@Composable
private fun LandingLandscape() {
    LandingScreenContentPreview()
}

@DesktopPreview
@Composable
private fun LandingDesktop() {
    LandingScreenContentPreview()
}

@Composable
private fun LandingScreenContentPreview() {
    PreviewSurface {
        LandingScreenContent(
            user = User(),
            innerPadding = PaddingValues(),
            onRefreshStageId = {}
        )
    }
}
