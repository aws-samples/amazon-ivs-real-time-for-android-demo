package com.amazon.ivs.stagesrealtimecompose.core.handlers

import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.launchDefault
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

private const val TIMER_SECONDS = 30

object VSHandler {
    private val _score = MutableStateFlow(VSScore())
    private val _winner = MutableStateFlow(VSResult.None)
    private val _scoreTimer = MutableStateFlow(TIMER_SECONDS)
    private val _showTimer = MutableStateFlow(false)
    private var _timerJob: Job? = null

    val score = _score.asStateFlow()
    val winner = _winner.asStateFlow()
    val scoreTimer = _scoreTimer.asStateFlow()
    val showTimer = _showTimer.asStateFlow()

    fun castVote(forCreator: Boolean) {
        val currentScore = _score.value
        val creatorScore = currentScore.creatorScore
        val participantScore = currentScore.participantScore
        val score = currentScore.copy(
            creatorScore = if (forCreator) creatorScore + 1 else creatorScore,
            participantScore = if (!forCreator) participantScore + 1 else participantScore
        )
        Timber.d("Score cast: $score, winner: ${score.winner}")
        _score.update { score }
    }

    fun setScore(score: VSScore) {
        Timber.d("Score received: $score, winner: ${score.winner}")
        _score.update { score }
    }

    fun startScoreTimer() {
        _score.update { VSScore() }
        _timerJob?.cancel()
        _scoreTimer.update { TIMER_SECONDS }
        _showTimer.update { true }
        _timerJob = launchDefault { timerJob() }
    }

    fun reset() {
        _winner.update { VSResult.None }
        _scoreTimer.update { TIMER_SECONDS }
        _showTimer.update { false }
        _timerJob?.cancel()
        _timerJob = null
    }

    private suspend fun timerJob() {
        Timber.d("Timer started")
        var seconds = _scoreTimer.value - 1
        while (seconds > 0) {
            delay(1000)
            _scoreTimer.update { seconds }
            seconds = _scoreTimer.value - 1
        }
        val winner = _score.value.winner
        Timber.d("Timer ended, winner: $winner")
        _winner.update { winner }
        delay(1000)
        _showTimer.update { false }
        _timerJob?.cancel()
        _timerJob = null
    }
}

data class VSScore(
    val creatorScore: Int = 0,
    val participantScore: Int = 0
) {
    val winner = when {
        creatorScore > participantScore -> VSResult.CreatorWins
        creatorScore < participantScore -> VSResult.ParticipantWins
        else -> VSResult.Draw
    }
}

enum class VSResult {
    None,
    CreatorWins,
    ParticipantWins,
    Draw
}
