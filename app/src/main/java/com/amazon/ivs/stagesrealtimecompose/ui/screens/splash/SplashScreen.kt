package com.amazon.ivs.stagesrealtimecompose.ui.screens.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amazon.ivs.stagesrealtimecompose.R
import com.amazon.ivs.stagesrealtimecompose.core.handlers.DialogDestination
import com.amazon.ivs.stagesrealtimecompose.core.handlers.NavigationHandler
import com.amazon.ivs.stagesrealtimecompose.ui.components.ButtonPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.components.PreviewSurface
import com.amazon.ivs.stagesrealtimecompose.ui.theme.InterPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.OrangePrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.WhitePrimary

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(color = OrangePrimary)
            },
        contentAlignment = Alignment.BottomStart
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(R.drawable.bg_splash),
            contentDescription = null,
            contentScale = ContentScale.FillBounds
        )

        Column(
            modifier = Modifier.padding(horizontal = 22.dp)
        ) {
            Text(
                text = stringResource(R.string.welcome_to_ivs_real_time),
                style = InterPrimary.copy(
                    fontSize = 42.sp,
                    fontWeight = FontWeight.W900
                )
            )
            ButtonPrimary(
                modifier = Modifier.padding(top = 56.dp, bottom = 30.dp),
                text = stringResource(R.string.get_started),
                background = WhitePrimary,
                onClick = {
                    NavigationHandler.showDialog(DialogDestination.EnterCode)
                }
            )
        }
    }
}

@Preview
@Composable
private fun SplashScreenPreview() {
    PreviewSurface {
        SplashScreen()
    }
}
