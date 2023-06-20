package com.amazon.ivs.stagesrealtime.ui.stage

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import com.amazon.ivs.stagesrealtime.common.DEFAULT_LOADING_DELAY
import com.amazon.ivs.stagesrealtime.common.SHAKE_FORCE_THRESHOLD
import com.amazon.ivs.stagesrealtime.common.VOTE_SESSION_TIME_SECONDS
import com.amazon.ivs.stagesrealtime.common.extensions.launch
import com.amazon.ivs.stagesrealtime.common.extensions.launchIO
import com.amazon.ivs.stagesrealtime.repository.StageRepository
import com.amazon.ivs.stagesrealtime.repository.models.PKModeScore
import com.amazon.ivs.stagesrealtime.repository.networking.models.StageMode
import com.amazon.ivs.stagesrealtime.repository.networking.models.asRTCUIDataList
import com.amazon.ivs.stagesrealtime.repository.networking.models.requests.Error
import com.amazon.ivs.stagesrealtime.ui.stage.models.PKVotingEnd
import com.amazon.ivs.stagesrealtime.ui.stage.models.RTCDataUIItemModel
import com.amazon.ivs.stagesrealtime.ui.stage.models.ScrollDirection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject

private const val STREAM_REFRESH_DELTA = 1000 * 5L // 5 seconds
private const val RTC_REFRESH_DELTA = 1000L // 1 second

@HiltViewModel
class StageViewModel @Inject constructor(
    private val repository: StageRepository
) : ViewModel() {
    private var timerPKVoteJob: Job? = null
    private var timerEnabled = false
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = Runnable {
        launch {
            if (!isCreator()) {
                Timber.d("Getting stages from runnable")
                repository.getStages()
                triggerStageTimer()
            }
        }
    }
    private var rtcTimerEnabled = false
    private val rtcTimerHandler = Handler(Looper.getMainLooper())
    private val rtcTimerRunnable = Runnable {
        repository.requestRTCStats()
        triggerRTCStatsTimer()
    }
    private val _onLoading = Channel<Boolean>()
    private val _onError = Channel<Error>()
    private val _onStageDeleted = Channel<Boolean>()
    private val _onCloseFeed = Channel<Boolean>()
    private val _onPKVotingEnd = Channel<PKVotingEnd>()
    private val _pkModeScore = MutableStateFlow(PKModeScore())
    private val _pkVoteTimer = MutableStateFlow(VOTE_SESSION_TIME_SECONDS)
    private val _rtcDataUIList = MutableStateFlow(emptyList<RTCDataUIItemModel>())

    val onLoading = _onLoading.receiveAsFlow()
    val onError = _onError.receiveAsFlow()
    val onStageDeleted = _onStageDeleted.receiveAsFlow()
    val onCloseFeed = _onCloseFeed.receiveAsFlow()
    val pkVoteTimer = _pkVoteTimer.asStateFlow()
    val stages = repository.stages
    val messages = repository.messages
    val onStageLike = repository.onStageLike
    val rtcData = repository.stageRTCData

    val onPKVotingEnd by lazy {
        handleVoteStart()
        _onPKVotingEnd.receiveAsFlow()
    }

    val pkModeScore by lazy {
        collectPKScoreAsStateFlow()
        _pkModeScore.asStateFlow()
    }

    val rtcDataList by lazy {
        collectRTCData()
        _rtcDataUIList.asStateFlow()
    }

    fun stopPkVoteJob() {
        timerPKVoteJob?.cancel()
        timerPKVoteJob = null
    }

    fun startCollectingStages() = launch {
        timerEnabled = true
        timerHandler.post(timerRunnable)
    }

    fun stopCollectingStages() {
        timerEnabled = false
        timerHandler.removeCallbacks(timerRunnable)
    }

    fun scrollStages(direction: ScrollDirection) = repository.scrollStages(direction)

    fun seatClicked(index: Int) = launch {
        Timber.d("Seat clicked: $index")
        _onLoading.send(true)
        val response = repository.onSeatClicked(index)
        response.onFailure {
            delay(DEFAULT_LOADING_DELAY)
            _onLoading.send(false)
            _onError.send(it)
        }
        response.onSuccess {
            delay(DEFAULT_LOADING_DELAY)
            _onLoading.send(false)
        }
    }

    fun requestRTCStats() {
        Timber.d("Started collecting RTC stats")
        rtcTimerEnabled = true
        rtcTimerHandler.post(rtcTimerRunnable)
    }

    fun stopRequestingRTCStats() {
        Timber.d("Stopped collecting RTC stats")
        rtcTimerEnabled = false
        rtcTimerHandler.removeCallbacks(rtcTimerRunnable)
    }

    fun switchVideo() = repository.switchVideo()

    fun switchAudio() = repository.switchAudio()

    fun switchFacing() = repository.switchFacing()

    fun isCurrentStageVideo() = repository.isCurrentStageVideo()

    fun isCreator() = repository.isStageCreator()

    fun isParticipating() = repository.isParticipating()

    fun canScroll() = repository.canScroll()

    fun likeStage() {
        repository.likeStage()
    }

    fun sendMessage(message: String) {
        repository.sendMessage(message)
    }

    fun castVote(voteHost: Boolean) = launch {
        repository.castVote(voteHost)
    }

    fun shouldShowDebugData(force: Float) =
        force > SHAKE_FORCE_THRESHOLD && stages.value.stageCount != 0 && stages.value.stageCenter != null

    fun deleteStage() = launch {
        _onLoading.send(true)
        val response = repository.deleteStage()
        response.onFailure {
            delay(DEFAULT_LOADING_DELAY)
            _onLoading.send(false)
            _onError.send(it)
            _onStageDeleted.send(false)
        }
        response.onSuccess {
            delay(DEFAULT_LOADING_DELAY)
            _onLoading.send(false)
            _onStageDeleted.send(true)
            stopPkVoteJob()
            startCollectingStages()
        }
    }

    fun kickParticipant() = launch {
        _onLoading.send(true)
        val response = repository.kickParticipant()
        response.onFailure {
            delay(DEFAULT_LOADING_DELAY)
            _onLoading.send(false)
            _onError.send(it)
        }
        response.onSuccess {
            delay(DEFAULT_LOADING_DELAY)
            _onLoading.send(false)
        }
    }

    fun startPublishing(mode: StageMode) = launch {
        _onLoading.send(true)
        val response = repository.startPublishing(mode)
        response.onFailure {
            delay(DEFAULT_LOADING_DELAY)
            _onLoading.send(false)
            _onError.send(it)
        }
        response.onSuccess {
            Timber.d("Publishing started")
            delay(DEFAULT_LOADING_DELAY)
            _onLoading.send(false)
        }
    }

    fun stopPublishing() = launch {
        _onLoading.send(true)
        val response = repository.stopPublishing()
        response.onFailure {
            delay(DEFAULT_LOADING_DELAY)
            _onLoading.send(false)
            _onError.send(it)
        }
        response.onSuccess {
            Timber.d("Publishing stopped")
            delay(DEFAULT_LOADING_DELAY)
            _onLoading.send(false)
        }
    }

    fun disconnectFromCurrentStage() = launch {
        repository.disconnectFromCurrentStage()
    }

    fun clearResources() = repository.clearResources()

    fun shouldCloseFeed(closeFeed: Boolean) = launch {
        // Workaround for waiting of dismissal of the bottom sheet
        delay(500)
        _onCloseFeed.send(closeFeed)
    }

    fun getNextStageByDirection(direction: ScrollDirection) = when (direction) {
        ScrollDirection.DOWN -> stages.value.stageBottom
        ScrollDirection.UP -> stages.value.stageTop
        ScrollDirection.NONE -> null
    }

    private fun triggerStageTimer() {
        if (timerEnabled) {
            timerHandler.postDelayed(timerRunnable, STREAM_REFRESH_DELTA)
        }
    }

    private fun triggerRTCStatsTimer() {
        if (rtcTimerEnabled) {
            rtcTimerHandler.postDelayed(rtcTimerRunnable, RTC_REFRESH_DELTA)
        }
    }

    private fun collectPKScoreAsStateFlow() = launch {
        repository.onPKModeScore.collect { pkModeScore ->
            Timber.d("PK mode score updated: $pkModeScore")
            _pkModeScore.update { pkModeScore.copy() }
        }
    }

    private fun handleVoteStart() = launch {
        repository.onVoteStart.collect { onVoteStart ->
            Timber.d("Starting vote: $onVoteStart")
            timerPKVoteJob?.cancel()
            if (onVoteStart.secondsRemaining < 0) {
                _onPKVotingEnd.send(PKVotingEnd.Nothing)
                return@collect
            }
            timerPKVoteJob = launchIO {
                var remainedTime = onVoteStart.secondsRemaining
                while (true) {
                    _pkVoteTimer.update { remainedTime }
                    remainedTime -= 1
                    if (remainedTime < 0) {
                        val endResult = _pkModeScore.value
                        val guestScore = endResult.guestScore
                        val hostScore = endResult.hostScore
                        when {
                            guestScore > hostScore -> _onPKVotingEnd.send(
                                PKVotingEnd.GuestWon(repository.getPKModeWinnerAvatar(hostWin = false))
                            )

                            guestScore < hostScore -> _onPKVotingEnd.send(
                                PKVotingEnd.HostWon(repository.getPKModeWinnerAvatar(hostWin = true))
                            )

                            else -> _onPKVotingEnd.send(PKVotingEnd.Draw)
                        }
                        this.cancel()
                    }
                    delay(1000)
                }
            }
        }
    }

    private fun collectRTCData() = launch {
        repository.stageRTCDataList.collect { dataList ->
            _rtcDataUIList.update {
                dataList.asRTCUIDataList(!isCurrentStageVideo())
            }
        }
    }
}
