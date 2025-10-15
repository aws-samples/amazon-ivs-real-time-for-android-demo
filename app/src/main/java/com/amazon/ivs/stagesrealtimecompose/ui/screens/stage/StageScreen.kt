package com.amazon.ivs.stagesrealtimecompose.ui.screens.stage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.amazon.ivs.stagesrealtimecompose.R
import com.amazon.ivs.stagesrealtimecompose.core.common.getNewUserAvatar
import com.amazon.ivs.stagesrealtimecompose.core.common.mockEmptyStage
import com.amazon.ivs.stagesrealtimecompose.core.common.mockStages
import com.amazon.ivs.stagesrealtimecompose.core.handlers.NavigationHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.StageDestinationType
import com.amazon.ivs.stagesrealtimecompose.core.handlers.VSHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.VSResult
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.Stage
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.StageHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.StageManager
import com.amazon.ivs.stagesrealtimecompose.ui.components.AvatarImage
import com.amazon.ivs.stagesrealtimecompose.ui.components.ButtonCircleImage
import com.amazon.ivs.stagesrealtimecompose.ui.components.PreviewSurface
import com.amazon.ivs.stagesrealtimecompose.ui.components.unScrollable
import com.amazon.ivs.stagesrealtimecompose.ui.screens.stage.common.StageOverlay
import com.amazon.ivs.stagesrealtimecompose.ui.screens.stage.page.StagePage
import com.amazon.ivs.stagesrealtimecompose.ui.theme.BlackPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.InterPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.WhitePrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.ldp
import kotlinx.coroutines.delay
import timber.log.Timber

@Composable
fun StageScreen(
    stageType: StageDestinationType,
    innerPadding: PaddingValues,
) {
    val stages by StageHandler.stages.collectAsStateWithLifecycle()
    val winner by VSHandler.winner.collectAsStateWithLifecycle()
    val topPadding by remember {
        derivedStateOf {
            innerPadding.calculateTopPadding()
        }
    }
    val nonEmptyStages = stages.takeIf { it.isNotEmpty() } ?: listOf(mockEmptyStage)

    LaunchedEffect(key1 = stageType) {
        Timber.d("Stage screen started")
        StageHandler.onStageStarted(stageType)
    }

    StageScreenContent(
        topPadding = topPadding,
        stages = nonEmptyStages,
        winner = winner,
    )
}

@Composable
private fun StageScreenContent(
    topPadding: Dp,
    stages: List<Stage>,
    winner: VSResult,
) {
    val bottomOffset = 30.dp
    val maxPages = Int.MAX_VALUE
    val middlePage = maxPages / 2
    val initialPage = middlePage - (middlePage % stages.size)
    val pagerState = if (stages.size == 1 && stages.first().stageId.isBlank()) {
        rememberPagerState { 1 }
    } else {
        rememberPagerState(
            initialPage = initialPage,
            pageCount = { maxPages }
        )
    }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var currentStageIndex by remember { mutableIntStateOf(0) }
    var showOverlay by remember { mutableStateOf(false) }

    val height = LocalDensity.current.run { size.height.ldp - bottomOffset }
    val isPreview = LocalInspectionMode.current
    val showPager = if (isPreview) true else size != IntSize.Zero
    val pageSize = if (isPreview) PageSize.Fill else PageSize.Fixed(height)
    val currentStage = stages[pagerState.targetPage % stages.size]
    val isAudioRoom = currentStage.isAudioRoom
    val isJoined = currentStage.isJoined

    fun getSettledStage() = pagerState.settledPage % stages.size

    LaunchedEffect(key1 = stages.size) {
        currentStageIndex = getSettledStage()
        val currentStage = stages[currentStageIndex]
        Timber.d("Stage size changed: $currentStageIndex, ${currentStage.stageId}, ${currentStage.isLoading}")
        StageHandler.loadPage(currentStageIndex)
    }

    LaunchedEffect(key1 = pagerState.settledPage) {
        currentStageIndex = getSettledStage()
        val currentStage = stages[currentStageIndex]
        if (currentStage.stageId.isBlank()) return@LaunchedEffect
        Timber.d("Stage settled: $currentStageIndex, ${currentStage.stageId}, ${currentStage.isLoading}")
        StageHandler.loadPage(currentStageIndex)
    }

    LaunchedEffect(key1 = pagerState.isScrollInProgress, key2 = currentStage) {
        val currentStage = stages[currentStageIndex]
        showOverlay = !currentStage.isLoading && !pagerState.isScrollInProgress
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .background(BlackPrimary)
    ) {
        AnimatedVisibility(
            visible = showPager,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val scrollDisabled = stages.size <= 1 || isJoined

            VerticalPager(
                modifier = Modifier
                    .fillMaxSize()
                    .unScrollable(enabled = scrollDisabled),
                state = pagerState,
                beyondViewportPageCount = 1,
                userScrollEnabled = !scrollDisabled,
                pageSize = pageSize
            ) { page ->
                val pageIndex = page % stages.size
                val stage = stages[pageIndex]

                StagePage(
                    stage = stage,
                    isActiveStage = pagerState.settledPage == page,
                    topPadding = topPadding,
                )
            }
        }
        StageHeader(
            topPadding = topPadding,
            isAudioRoom = isAudioRoom
        )
        AnimatedVisibility(
            modifier = Modifier.fillMaxSize(),
            visible = showOverlay,
            enter = fadeIn(animationSpec = tween(durationMillis = 500)),
            exit = fadeOut(animationSpec = tween(durationMillis = 300))
        ) {
            if (currentStageIndex >= stages.size) return@AnimatedVisibility

            StageOverlay(
                modifier = Modifier.padding(bottom = bottomOffset),
                stage = stages[currentStageIndex],
            )
        }
        stages.getOrNull(currentStageIndex)?.run {
            VSWinner(
                winner = winner,
                stage = this,
            )
        }
    }
}

@Composable
private fun VSWinner(
    winner: VSResult,
    stage: Stage,
) {
    AnimatedVisibility(
        visible = winner != VSResult.None,
        enter = fadeIn() + scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        exit = fadeOut(),
    ) {
        LaunchedEffect(key1 = Unit) {
            delay(4000)
            VSHandler.reset()
        }

        val isPreview = LocalInspectionMode.current
        val isCreatorWinner = winner == VSResult.CreatorWins
        val flag = if (isCreatorWinner) R.drawable.ic_banner_red else R.drawable.ic_banner_blue
        val avatar = if (isPreview) getNewUserAvatar() else StageManager.getParticipantAvatar(isCreator = isCreatorWinner, stageId = stage.stageId)

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                modifier = Modifier.size(300.dp),
                model = R.drawable.bg_rays,
                contentDescription = null,
            )
            Image(
                modifier = Modifier
                    .padding(top = 150.dp)
                    .size(150.dp),
                painter = painterResource(flag),
                contentDescription = null,
            )
            AvatarImage(
                modifier = Modifier.padding(bottom = 32.dp),
                avatar = avatar,
                size = 62.dp
            )
            Image(
                modifier = Modifier.size(200.dp),
                painter = painterResource(R.drawable.bg_crest),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun StageHeader(
    topPadding: Dp,
    isAudioRoom: Boolean
) {
    Box(
        modifier = Modifier
            .padding(
                top = topPadding,
                start = 16.dp
            )
            .fillMaxWidth()
            .height(42.dp)
    ) {
        ButtonCircleImage(
            modifier = Modifier
                .size(42.dp),
            image = R.drawable.ic_back,
            padding = PaddingValues(8.dp),
            description = stringResource(R.string.back_button),
            background = Color.Transparent,
            tint = WhitePrimary,
            onClick = NavigationHandler::goBack
        )
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.Center),
            visible = isAudioRoom,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = stringResource(R.string.audio_room),
                style = InterPrimary.copy(
                    fontWeight = FontWeight.W700,
                    fontSize = 21.sp,
                    color = WhitePrimary,
                ),
            )
        }
    }
}

@Preview
@Composable
fun StageScreenContentStagePreview() {
    StageScreenContentPreview()
}

@Preview
@Composable
fun StageScreenContentStageWinnerPreview() {
    StageScreenContentPreview(
        winner = VSResult.CreatorWins
    )
}

@Composable
fun StageScreenContentPreview(
    stages: List<Stage> = mockStages,
    winner: VSResult = VSResult.None,
) {
    PreviewSurface {
        StageScreenContent(
            topPadding = 0.dp,
            stages = stages,
            winner = winner,
        )
    }
}
