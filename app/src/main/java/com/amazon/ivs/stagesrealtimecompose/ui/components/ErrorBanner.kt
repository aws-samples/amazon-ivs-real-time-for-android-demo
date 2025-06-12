package com.amazon.ivs.stagesrealtimecompose.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amazon.ivs.stagesrealtimecompose.core.handlers.ErrorDestination
import com.amazon.ivs.stagesrealtimecompose.core.handlers.NavigationHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.networking.Error
import com.amazon.ivs.stagesrealtimecompose.ui.theme.InterTertiary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.RedPrimary

@Composable
fun ErrorBanner(
    innerPadding: PaddingValues
) {
    val errorDestination by NavigationHandler.errorDestination.collectAsStateWithLifecycle()

    AnimatedVisibility(
        modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
        visible = errorDestination != ErrorDestination.None,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        ErrorBannerContent(errorDestination)
    }
}

@Composable
private fun ErrorBannerContent(
    errorDestination: ErrorDestination
) {
    val resource = when (errorDestination) {
        is ErrorDestination.SnackBar -> stringResource(errorDestination.error.errorResource)
        else -> ""
    }
    var text by remember { mutableStateOf(resource) }
    val shape = RoundedCornerShape(18.dp)

    LaunchedEffect(key1 = errorDestination) {
        if (errorDestination != ErrorDestination.None) {
            text = resource
        }
    }

    Box(
        modifier = Modifier
            .padding(top = 30.dp, bottom = 6.dp)
            .padding(horizontal = 6.dp)
            .fillMaxWidth()
            .height(64.dp)
            .shadow(elevation = 4.dp, shape = shape)
            .background(color = RedPrimary, shape = shape)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = InterTertiary
        )
    }
}

@Preview
@Composable
fun ErrorBannerPreviewCustomerCode() {
    ErrorBannerPreview(ErrorDestination.SnackBar(Error.CustomerCodeError))
}

@Preview
@Composable
fun ErrorBannerPreviewJoinStage() {
    ErrorBannerPreview(ErrorDestination.SnackBar(Error.JoinStageError))
}

@Preview
@Composable
fun ErrorBannerPreviewCreateStage() {
    ErrorBannerPreview(ErrorDestination.SnackBar(Error.CreateStageError))
}

@Composable
fun ErrorBannerPreview(
    errorDestination: ErrorDestination
) {
    PreviewSurface {
        ErrorBannerContent(
            errorDestination = errorDestination
        )
    }
}
