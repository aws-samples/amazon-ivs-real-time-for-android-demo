package com.amazon.ivs.stagesrealtime.ui.stage

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.TransitionAdapter
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.view.marginTop
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.amazon.ivs.stagesrealtime.R
import com.amazon.ivs.stagesrealtime.common.*
import com.amazon.ivs.stagesrealtime.common.extensions.*
import com.amazon.ivs.stagesrealtime.databinding.FragmentStageBinding
import com.amazon.ivs.stagesrealtime.databinding.ViewStageCardBinding
import com.amazon.ivs.stagesrealtime.ui.BackHandler
import com.amazon.ivs.stagesrealtime.ui.stage.adapters.AudioSeatAdapter
import com.amazon.ivs.stagesrealtime.ui.stage.adapters.ChatItemAnimator
import com.amazon.ivs.stagesrealtime.ui.stage.adapters.ChatMessageAdapter
import com.amazon.ivs.stagesrealtime.ui.stage.models.LeaveDeleteStageMode
import com.amazon.ivs.stagesrealtime.ui.stage.models.PKVotingEnd
import com.amazon.ivs.stagesrealtime.ui.stage.models.ScrollDirection
import com.amazon.ivs.stagesrealtime.ui.stage.models.StageUIModel
import kotlinx.coroutines.delay
import timber.log.Timber
import java.util.*

private const val TRANSITION_THRESHOLD = 500
private const val CHAT_UPDATE_DELTA = 200L
private const val PK_WINNER_VIEW_VISIBILITY_TIME = 4000L

class StageFragment : Fragment(R.layout.fragment_stage), BackHandler {
    private val binding by viewBinding(FragmentStageBinding::bind)
    private val viewModel by activityViewModels<StageViewModel>()

    private lateinit var chatAdapter: ChatMessageAdapter
    private lateinit var seatAdapter: AudioSeatAdapter

    private var lastTransitionTime = 0L
    private var wasMessageSent = false
    private var isKeyboardVisible = false
    private var isPKMode = false
    private val pkAnimators = mutableListOf<Animator>()
    private val debugShakeSensor by lazy {
        DebugShakeSensor(
            isEnabled = { force -> viewModel.shouldShowDebugData(force) },
            onShaken = {
                Timber.d("Show debug menu")
                navigate(StageFragmentDirections.openDebugSheet())
            },
            context = requireActivity()
        )
    }
    private val onGlobalLayoutListener = OnGlobalLayoutListener {
        if (binding.root.isKeyboardVisible() == true && !isKeyboardVisible) {
            Timber.d("Keyboard appeared")
            isKeyboardVisible = true
            binding.stageButtons.root.updatePadding(bottom = resources.getDimensionPixelOffset(R.dimen.padding_null))
        } else if (binding.root.isKeyboardVisible() == false && isKeyboardVisible) {
            Timber.d("Keyboard disappeared")
            isKeyboardVisible = false
            binding.stageButtons.root.updatePadding(bottom = resources.getDimensionPixelOffset(R.dimen.padding_large))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        debugShakeSensor.setup(this)
        setupListeners()
        setupCollectors()
    }

    override fun onResume() {
        super.onResume()
        viewModel.startCollectingStages()
        viewModel.startCollectingRTCStats()
        binding.root.viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopCollectingStages()
        viewModel.stopCollectingRTCStats()
        binding.root.viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() = with(binding) {
        resetCenterCard()
        stageTop.setEmptyAdapter()
        stageBottom.setEmptyAdapter()
        stageBottomDummy.setEmptyAdapter()
        stageCenter.audioStage.audioSeats.itemAnimator = null

        with(stageButtons) {
            muteVideoButton.setOnClickListener { viewModel.switchVideo() }
            muteAudioButton.setOnClickListener { viewModel.switchAudio() }
            switchVideoButton.setOnClickListener { viewModel.switchFacing() }
            leaveButton.setOnClickListener {
                hideKeyboard()
                if (viewModel.isCreator()) {
                    navigate(StageFragmentDirections.toLeaveDeleteStage(LeaveDeleteStageMode.DELETE))
                } else {
                    navigate(StageFragmentDirections.toLeaveDeleteStage())
                }
            }
            heartButton.setOnClickListener {
                heartView.addHeart()
                viewModel.likeStage()
            }
            participantKickButton.setOnClickListener {
                hideKeyboard()
                if (viewModel.isCreator()) {
                    navigate(StageFragmentDirections.toLeaveDeleteStage(LeaveDeleteStageMode.KICK_USER))
                } else if (viewModel.isParticipating()) {
                    navigate(StageFragmentDirections.toLeaveDeleteStage())
                } else {
                    navigate(StageFragmentDirections.toJoinBottomSheet())
                }
            }
        }

        with(stageButtons.sendMessageField) {
            this.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    wasMessageSent = true
                    viewModel.sendMessage(this.text.toString())
                    this.text.clear()
                }
                true
            }
        }

        with(stageButtons) {
            voteLeft.setOnClickListener {
                Timber.d("Vote creator button clicked")
                voteScoreLeft.text = (voteScoreLeft.text.toString().toIntOrNull()?.plus(1) ?: 0).toString()
                viewModel.castVote(true)
            }

            voteRight.setOnClickListener {
                Timber.d("Vote guest button clicked")
                voteScoreRight.text = (voteScoreRight.text.toString().toIntOrNull()?.plus(1) ?: 0).toString()
                viewModel.castVote(false)
            }
        }

        backButton.setOnClickListener {
            hideKeyboard()
            handleBackPress()
        }

        motionLayout.doOnLayout {
            Timber.d("Motion layout do on layout called")
            updateViewHeight(binding.root.measuredHeight)
        }

        root.setOnTouchListener { _, event ->
            if (event.action != MotionEvent.ACTION_MOVE) {
                Timber.d("Event touch - $event")
            }
            if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN && event.pointerCount >= 2) {
                stageButtons.root.setVisibleOr(false)
                backButton.setVisibleOr(false)
            } else if (event.pointerCount <= 2
                && (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_POINTER_UP)
            ) {
                stageButtons.root.setVisibleOr(true)
                backButton.setVisibleOr(true)
            }
            true
        }

        motionLayout.setOnTouchListener { _, _ ->
            hideKeyboard()
            false
        }

        motionLayout.setTransitionListener(object : TransitionAdapter() {
            override fun onTransitionChange(motionLayout: MotionLayout?, startId: Int, endId: Int, progress: Float) {
                super.onTransitionChange(motionLayout, startId, endId, progress)
                val currentId = motionLayout?.currentState
                val delta = 1 - (progress * 5)
                val alpha = if (delta >= 0) delta else 0f
                if (binding.stageButtons.root.alpha != alpha && currentId == -1) {
                    binding.stageButtons.root.alpha = alpha
                }
            }

            override fun onTransitionStarted(motionLayout: MotionLayout?, startId: Int, endId: Int) {
                super.onTransitionStarted(motionLayout, startId, endId)
                hideKeyboard()
            }

            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                super.onTransitionCompleted(motionLayout, currentId)
                val currentTime = Date().time
                val transitionDelay = currentTime - lastTransitionTime
                lastTransitionTime = currentTime
                // Workaround for a bug in ConstraintLayout that causes multiple onTransitionCallbacks for no reason
                if (transitionDelay < TRANSITION_THRESHOLD) return
                Timber.d("Transition completed: $currentId, ${R.id.state_top}, ${R.id.state_center}, ${R.id.state_bottom}")
                when (currentId) {
                    R.id.state_top -> {
                        resetCenterCard(ScrollDirection.UP)
                        viewModel.scrollStages(ScrollDirection.UP)
                        motionLayout?.jumpToState(R.id.state_center)
                        temporaryBlockScrolling()
                    }

                    R.id.state_bottom -> {
                        resetCenterCard(ScrollDirection.DOWN)
                        viewModel.scrollStages(ScrollDirection.DOWN)
                        motionLayout?.jumpToState(R.id.state_center)
                        temporaryBlockScrolling()
                    }
                }
            }
        })
    }

    private fun temporaryBlockScrolling() {
        Timber.d("Scrolling locked")
        binding.motionLayout.getTransition(R.id.transition_to_top).isEnabled = false
        binding.motionLayout.getTransition(R.id.transition_to_bottom).isEnabled = false
        launchUI {
            delay(DEFAULT_SCROLLING_DELAY)
            binding.motionLayout.getTransition(R.id.transition_to_top).isEnabled = true
            binding.motionLayout.getTransition(R.id.transition_to_bottom).isEnabled = true
            Timber.d("Scrolling unlocked")
        }
    }

    private fun setupCollectors() {
        collectLatestWithLifecycle(viewModel.pkVoteTimer) { timeRemained ->
            binding.stageCenter.videoStage.pkModeTimer.fadeIn()
            binding.stageCenter.videoStage.pkModeTimer.text =
                getString(R.string.timer_template, timeRemained.toStringWithLeadingZero())
        }

        collectLatestWithLifecycle(viewModel.onPKVotingEnd) { onVotingEnd ->
            with(binding) {
                activity?.run {
                    stageCenter.videoStage.pkModeTimer.fadeOut()
                    when (onVotingEnd) {
                        is PKVotingEnd.HostWon,
                        is PKVotingEnd.GuestWon -> {
                            val hostWon = onVotingEnd is PKVotingEnd.HostWon
                            Timber.d("Pk won by: ${if (hostWon) "host" else "guest"}")
                            val flag = if (hostWon) R.drawable.banner_red else R.drawable.banner_blue
                            val flagDrawable = ContextCompat.getDrawable(this, flag)
                            pkModeWinnerView.winnerAvatar.setAvatar(onVotingEnd.userAvatar)
                            pkModeWinnerView.flag.setImageDrawable(flagDrawable)
                            pkModeWinnerView.root.scaleInAndFade()
                            delay(PK_WINNER_VIEW_VISIBILITY_TIME)
                            pkModeWinnerView.root.scaleOutAndFade()
                        }
                        PKVotingEnd.Draw -> {
                            Timber.d("Draw")
                        }
                        PKVotingEnd.Nothing -> {
                            Timber.d("Voting already finished")
                        }
                    }
                }
            }
        }

        collectLatestWithLifecycle(viewModel.onError) { error ->
            showErrorBar(error.errorResource)
        }

        collectLatestWithLifecycle(viewModel.onLoading) { isLoading ->
            Timber.d("Loading changed: $isLoading")
            binding.loadingView.root.fadeAlpha(isLoading)
        }

        val defaultGuidePercent = TypedValue()
        resources.getValue(R.integer.pk_mode_guideline_percent, defaultGuidePercent, true)
        collectLatestWithLifecycle(viewModel.pkModeScore) { score ->
            with(binding.stageCenter.videoStage) {
                Timber.d("PK mode score is - $score")
                val currentLeftScore = binding.stageButtons.voteScoreLeft.text.toString().toIntOrNull() ?: 0
                val currentRightScore = binding.stageButtons.voteScoreRight.text.toString().toIntOrNull() ?: 0
                val newLeftScore = if (score.hostScore < currentLeftScore && !score.shouldResetScore)
                    currentLeftScore else score.hostScore
                val newRightScore = if (score.guestScore < currentRightScore && !score.shouldResetScore)
                    currentRightScore else score.guestScore
                Timber.d("PK score: $currentLeftScore, $newLeftScore, $currentRightScore, $newRightScore")
                binding.stageButtons.voteScoreLeft.text = newLeftScore.toString()
                binding.stageButtons.voteScoreRight.text = newRightScore.toString()
                val params = animGuideline.layoutParams as ConstraintLayout.LayoutParams
                var endPercentValue = defaultGuidePercent.float + VOTE_STEP * (score.hostScore - score.guestScore)
                if (endPercentValue >= 1f) {
                    endPercentValue = 1f
                } else if (endPercentValue <= 0f) {
                    endPercentValue = 0f
                }
                val guidelineAnimation = ValueAnimator.ofFloat(params.guidePercent, endPercentValue)
                val animationDuration = 300L
                guidelineAnimation.interpolator = LinearInterpolator()
                guidelineAnimation.duration = animationDuration
                guidelineAnimation.addUpdateListener { animation ->
                    val progress = animation.animatedValue as Float
                    params.guidePercent = progress
                    animGuideline.layoutParams = params
                }
                guidelineAnimation.doOnEnd {
                    animLeft.adjustChildrenWidth(animLeftFlare, widthRation = 2)
                    animRight.adjustChildrenWidth(animRightFlare, widthRation = 2)
                }

                val blinkEffectAnimation = ValueAnimator.ofFloat(0f, 2f)
                val isMovingToRight = endPercentValue > params.guidePercent
                val backgroundRippleToAnimate = if (!isMovingToRight) animBgRightBlink else animBgLeftBlink
                blinkEffectAnimation.interpolator = LinearInterpolator()
                blinkEffectAnimation.duration = animationDuration
                blinkEffectAnimation.addUpdateListener { animation ->
                    val progress = animation.animatedValue as Float
                    if (progress <= 1f) {
                        backgroundRippleToAnimate.alpha = animation.animatedValue as Float
                    } else {
                        backgroundRippleToAnimate.alpha = 2f - animation.animatedValue as Float
                    }
                }

                blinkEffectAnimation.start()
                guidelineAnimation.start()
            }
        }

        collectLatestWithLifecycle(viewModel.onStageLike) {
            binding.stageButtons.heartView.addHeart()
        }

        collectLatestWithLifecycle(viewModel.onStageDeleted) { isDeleted ->
            Timber.d("Stage deleted - $isDeleted")
            if (isDeleted) {
                resetCenterCard()
            }
        }

        collectLatestWithLifecycle(viewModel.onCloseFeed) { shouldClose ->
            Timber.d("Should close feed and navigate back - $shouldClose")
            if (shouldClose) {
                navController.navigateUp()
            }
        }

        var cardMeasured = false
        collectLatestWithLifecycle(viewModel.stages) { stages ->
            Timber.d("Received stages: ${stages.stageCount}")
            if (stages.stageCount == 0) {
                viewModel.stopCollectingRTCStats()
            } else {
                viewModel.startCollectingRTCStats()
            }
            if (stages.stageCount == 0 || binding.stages?.stageCenter?.stageId != stages.stageCenter?.stageId) {
                hideKeyboard()
            }
            binding.stages = stages

            with(binding) {
                val canScroll = viewModel.canScroll()
                motionLayout.getTransition(R.id.transition_to_top).isEnabled = canScroll
                motionLayout.getTransition(R.id.transition_to_bottom).isEnabled = canScroll

                stages.stageCenter?.let { stage ->
                    // Delay fixes flickering issue, but black preview shows a bit longer
                    delay(100)
                    Timber.d("Updating center stage:\n$stage")
                    if (!cardMeasured) {
                        cardMeasured = true
                        binding.motionLayout.doOnLayout {
                            Timber.d("Re-measuring when stages received")
                            updateViewHeight(binding.root.measuredHeight)
                        }
                    }
                    val isCreatorVideoUpdated = stage.creatorVideo?.parent == null
                    val isGuestVideoUpdated = stage.guestVideo?.parent == null
                    if (isCreatorVideoUpdated) {
                        stageCenter.videoStage.creatorVideo.removeAllViews()
                    }
                    if (isGuestVideoUpdated) {
                        stageCenter.videoStage.guestVideo.removeAllViews()
                        stageCenter.videoStage.pkGuestVideo.removeAllViews()
                    }

                    if (isCreatorVideoUpdated && stage.creatorVideo != null) {
                        stageCenter.videoStage.creatorVideo.addView(stage.creatorVideo)
                    }
                    if (isGuestVideoUpdated && stage.guestVideo != null) {
                        if (stage.isPKMode) {
                            stageCenter.videoStage.pkGuestVideo.addView(stage.guestVideo)
                        } else {
                            stageCenter.videoStage.guestVideo.addView(stage.guestVideo)
                        }
                    }

                    if (stage.isGuestMode) {
                        stageCenter.videoStage.guestLayout.fadeIn()
                    } else {
                        stageCenter.videoStage.guestLayout.fadeOut()
                    }
                    checkPKModeTransition(stage)

                    Timber.d("Setting self avatar: ${stage.selfAvatar}")
                    stageButtons.userAvatar.setAvatar(stage.selfAvatar)
                    stageButtons.stageCreatorAvatar.setAvatar(stage.creatorAvatar)
                    stageButtons.speakingIndicator.setVisibleOr(
                        stage.isSpeaking
                                && !stage.isLocalAudioOff
                                && (stage.isParticipant || stage.isCreator)
                    )

                    stage.seats.let { seats ->
                        val stageSeats = seats.ifEmpty { emptySeats }
                        stageCenter.audioStage.audioSeats.suppressLayout(false)
                        seatAdapter.setCreator(viewModel.isCreator())
                        seatAdapter.submitList(stageSeats)
                        stageCenter.audioStage.audioSeats.postDelayed({
                            stageCenter.audioStage.audioSeats.suppressLayout(true)
                        }, CHAT_UPDATE_DELTA)
                    }
                    launchUI {
                        stageButtons.root.fadeIn()
                    }

                    // Handle TTV & Latency video stats
                    if (stage.creatorLatency != null) {
                        val creatorStats = if (stage.creatorTTV != null) {
                            getString(R.string.video_stats_pattern, stage.creatorTTV, stage.creatorLatency)
                        } else if (stage.creatorLatency.isNotBlank()) {
                            getString(R.string.video_latency_pattern, stage.creatorLatency)
                        } else {
                            ""
                        }
                        Timber.d("Creator Stats: $creatorStats")
                        stageCenter.videoStage.creatorVideoStats.text = creatorStats
                        stageCenter.videoStage.creatorPkVideoStats.text = creatorStats
                    }
                    if (stage.guestLatency != null) {
                        val guestStats = if (stage.guestTTV != null) {
                            getString(R.string.video_stats_pattern, stage.guestTTV, stage.guestLatency)
                        } else if (stage.guestLatency.isNotBlank()) {
                            getString(R.string.video_latency_pattern, stage.guestLatency)
                        } else {
                            ""
                        }
                        Timber.d("Guest Stats: $guestStats")
                        stageCenter.videoStage.guestVideoStats.text = guestStats
                        stageCenter.videoStage.guestPkVideoStats.text = guestStats
                    }
                } ?: run {
                    if (isPKMode) {
                        exitPKMode()
                    }
                }
            }
        }
        collectLatestWithLifecycle(viewModel.messages) { messages ->
            Timber.d("Received messages: $messages")
            with(binding) {
                stageButtons.chatMessages.suppressLayout(false)
                chatAdapter.submitList(messages)
                stageButtons.chatMessages.postDelayed({
                    stageButtons.chatMessages.scrollToPosition(messages.size - 1)
                    stageButtons.chatMessages.suppressLayout(true)
                    if (wasMessageSent) {
                        wasMessageSent = false
                        stageButtons.sendMessageField.requestFocusFromTouch()
                        showKeyboard()
                    }
                }, CHAT_UPDATE_DELTA)
            }
        }
    }

    private fun checkPKModeTransition(stage: StageUIModel) {
        if (stage.isPKMode) {
            enterPKMode()
        } else {
            exitPKMode()
        }
    }

    private fun enterPKMode() = with(binding) {
        if (isPKMode) return@with
        Timber.d("Starting transition to PK mode")
        isPKMode = true
        val creatorVideo = stageCenter.videoStage.creatorVideo
        val pkViews = listOf(
            stageCenter.videoStage.pkContainer,
            stageCenter.videoStage.pkIcon,
            stageCenter.videoStage.animGroup,
            stageCenter.videoStage.animLaser,
            stageCenter.videoStage.animLaserLine
        )
        val initialHeight = creatorVideo.measuredHeight
        val initialWidth = creatorVideo.measuredWidth
        startPKAnimation()
        stageButtons.voteBackground.fadeIn()
        stageButtons.voteLeft.fadeIn()
        stageButtons.voteRight.fadeIn()

        creatorVideo.resizeAnimated(
            initialHeight = initialHeight,
            initialWidth = initialWidth,
            finalHeight = resources.getDimensionPixelSize(R.dimen.size_pk_height),
            finalWidth = initialWidth / 2,
            marginTop = resources.getDimensionPixelSize(R.dimen.margin_gigantic)
        ) { progress ->
            pkViews.forEach { pkView ->
                pkView.alpha = progress
                if (progress > 0f && pkView.visibility == View.GONE) {
                    pkView.setVisibleOr(true)
                }
            }
        }
    }

    private fun exitPKMode() = with(binding) {
        if (!isPKMode) return@with
        Timber.d("Starting transition from PK mode")
        isPKMode = false
        val creatorVideo = stageCenter.videoStage.creatorVideo
        val pkViews = listOf(
            stageCenter.videoStage.pkContainer,
            stageCenter.videoStage.pkIcon,
            stageCenter.videoStage.animGroup,
            stageCenter.videoStage.animLaser,
            stageCenter.videoStage.animLaserLine
        )
        val initialHeight = creatorVideo.measuredHeight
        val initialWidth = creatorVideo.measuredWidth
        stageButtons.voteBackground.fadeOut(outVisibility = View.VISIBLE)
        stageButtons.voteLeft.fadeOut()
        stageButtons.voteRight.fadeOut()
        resetPKVoteValues()
        creatorVideo.resizeAnimated(
            initialHeight = initialHeight,
            initialWidth = initialWidth,
            finalHeight = stageCenter.videoStage.root.height,
            finalWidth = stageCenter.videoStage.root.width,
        ) { progress ->
            var animationStopped = false
            pkViews.forEach { pkView ->
                pkView.alpha = 1f - progress
                if (progress == 1f && pkView.visibility == View.VISIBLE) {
                    pkView.setVisibleOr(false)
                    if (!animationStopped) {
                        animationStopped = true
                        stopPKAnimation()
                    }
                }
            }
        }
    }

    private fun resetPKVoteValues() = with(binding) {
        viewModel.stopPkVoteJob()
        stageButtons.voteScoreLeft.text = "0"
        stageButtons.voteScoreRight.text = "0"
        stageCenter.videoStage.pkModeTimer.visibility = View.GONE
        val defaultParams = stageCenter.videoStage.animGuideline.layoutParams as ConstraintLayout.LayoutParams
        defaultParams.guidePercent = 0.5f
        stageCenter.videoStage.animGuideline.layoutParams = defaultParams
    }

    override fun handleBackPress() {
        Timber.d("Going back: ${viewModel.isCreator()}, ${viewModel.isParticipating()}")
        if (viewModel.isCreator()) {
            navigate(
                StageFragmentDirections.toLeaveDeleteStage(
                    mode = LeaveDeleteStageMode.DELETE,
                    shouldCloseFeed = true
                )
            )
        } else if (viewModel.isParticipating()) {
            navigate(
                StageFragmentDirections.toLeaveDeleteStage(
                    mode = LeaveDeleteStageMode.LEAVE,
                    shouldCloseFeed = true,
                    shouldDisconnectAndClearResources = true
                )
            )
        } else {
            viewModel.disconnectFromCurrentStage()
            viewModel.clearResources()
            isPKMode = false
            stopPKAnimation()
            resetPKVoteValues()
            navController.navigateUp()
        }
    }

    private fun resetCenterCard(direction: ScrollDirection = ScrollDirection.NONE) = with(binding) {
        Timber.d("Resetting center card")
        val nextStage = viewModel.getNextStageByDirection(direction)
        if (nextStage?.isPKMode == false) {
            stopPKAnimation()
            exitPKMode()

            val modeViews = listOf(
                stageCenter.videoStage.guestLayout,
                stageCenter.videoStage.pkContainer,
                stageCenter.videoStage.pkIcon,
                stageCenter.videoStage.animGroup,
                stageCenter.videoStage.animLaser,
                stageCenter.videoStage.animLaserLine,
                stageCenter.videoStage.animRight,
                stageCenter.videoStage.animLeft
            )
            modeViews.forEach { it.setVisibleOr(false) }
        }

        chatAdapter = ChatMessageAdapter()
        seatAdapter = AudioSeatAdapter { index ->
            if (!viewModel.isCreator()) {
                viewModel.seatClicked(index)
            }
        }
        stageButtons.voteScoreLeft.text = "0"
        stageButtons.voteScoreRight.text = "0"
        stageButtons.chatMessages.setHasFixedSize(true)
        stageButtons.chatMessages.invalidateItemDecorations()
        stageButtons.chatMessages.itemAnimator = ChatItemAnimator()
        stageButtons.chatMessages.adapter = chatAdapter
        stageButtons.chatMessages.suppressLayout(true)
        stageButtons.heartView.clearAllViews()
        stageCenter.audioStage.audioSeats.adapter = seatAdapter
        stageCenter.videoStage.creatorVideo.removeAllViews()
        stageCenter.videoStage.guestVideo.removeAllViews()
        seatAdapter.submitList(emptySeats)
    }

    private fun ViewStageCardBinding.setEmptyAdapter() {
        val emptyAudioAdapter = AudioSeatAdapter {}
        audioStage.audioSeats.adapter = emptyAudioAdapter
        emptyAudioAdapter.submitList(emptySeats)
    }

    private fun updateViewHeight(rootHeight: Int) {
        val marginBottom = resources.getDimensionPixelSize(R.dimen.margin_big)
        val marginTop = resources.getDimensionPixelSize(R.dimen.margin_small)
        val height = rootHeight - marginBottom
        // Top state
        binding.motionLayout.getConstraintSet(R.id.state_top).run {
            constrainHeight(R.id.stage_top, height)
            constrainHeight(R.id.stage_center, height)
            constrainHeight(R.id.stage_bottom, height)
            constrainHeight(R.id.stage_bottom_dummy, height)
            setMargin(R.id.stage_top, ConstraintSet.BOTTOM, marginBottom)
            setMargin(R.id.stage_center, ConstraintSet.BOTTOM, 0)
            setMargin(R.id.stage_center, ConstraintSet.TOP, marginTop)
        }

        // Center state
        binding.motionLayout.getConstraintSet(R.id.state_center).run {
            constrainHeight(R.id.stage_top, height)
            constrainHeight(R.id.stage_center, height)
            constrainHeight(R.id.stage_bottom, height)
            constrainHeight(R.id.stage_bottom_dummy, height)
            setMargin(R.id.stage_top, ConstraintSet.TOP, 0)
            setMargin(R.id.stage_top, ConstraintSet.BOTTOM, 0)
            setMargin(R.id.stage_center, ConstraintSet.BOTTOM, marginBottom)
            setMargin(R.id.stage_center, ConstraintSet.TOP, 0)
            setMargin(R.id.stage_bottom, ConstraintSet.TOP, marginTop)
            setMargin(R.id.stage_bottom, ConstraintSet.BOTTOM, 0)
            setMargin(R.id.stage_bottom_dummy, ConstraintSet.TOP, marginTop)
            setMargin(R.id.stage_bottom_dummy, ConstraintSet.BOTTOM, 0)
        }

        // Bottom state
        binding.motionLayout.getConstraintSet(R.id.state_bottom).run {
            constrainHeight(R.id.stage_top, height)
            constrainHeight(R.id.stage_center, height)
            constrainHeight(R.id.stage_bottom, height)
            constrainHeight(R.id.stage_bottom_dummy, height)
            setMargin(R.id.stage_center, ConstraintSet.BOTTOM, 0)
            setMargin(R.id.stage_bottom, ConstraintSet.TOP, 0)
            setMargin(R.id.stage_bottom, ConstraintSet.BOTTOM, marginBottom)
            setMargin(R.id.stage_bottom_dummy, ConstraintSet.TOP, marginTop)
        }
    }

    private fun startPKAnimation() = with(binding.stageCenter.videoStage) {
        Timber.d("PK animation start called")
        stopPKAnimation()
        adjustAnimViewsWidth()

        val sparksAnimationDuration = 500L
        val beamAnimationDuration = 500L
        val beamSmallAnimationDuration = 800L
        Timber.d("Starting PK animation")

        val animationSparksLeft = ValueAnimator.ofFloat(0.0f, -1.0f)
        animationSparksLeft.repeatCount = ValueAnimator.INFINITE
        animationSparksLeft.interpolator = LinearInterpolator()
        animationSparksLeft.duration = sparksAnimationDuration
        animationSparksLeft.addUpdateListener { animation ->
            if (!isAdded) return@addUpdateListener
            val progress = animation.animatedValue as Float
            val width = animSparksLeft1.width
            val translationX = width * progress
            animSparksLeft1.translationX = translationX
            animSparksLeft2.translationX = translationX + width
        }
        animationSparksLeft.start()
        pkAnimators.add(animationSparksLeft)

        val animationSparksRight = ValueAnimator.ofFloat(0.0f, 1.0f)
        animationSparksRight.repeatCount = ValueAnimator.INFINITE
        animationSparksRight.interpolator = LinearInterpolator()
        animationSparksRight.duration = sparksAnimationDuration
        animationSparksRight.addUpdateListener { animation ->
            if (!isAdded) return@addUpdateListener
            val progress = animation.animatedValue as Float
            val width = animSparksRight1.width
            val translationX = width * progress
            animSparksRight1.translationX = translationX
            animSparksRight2.translationX = translationX - width
        }
        animationSparksRight.start()
        pkAnimators.add(animationSparksRight)

        val animationBeam = ValueAnimator.ofFloat(0.1f, 0.5f, 0.1f)
        animationBeam.repeatCount = ValueAnimator.INFINITE
        animationBeam.interpolator = LinearInterpolator()
        animationBeam.duration = beamAnimationDuration
        animationBeam.addUpdateListener { animation ->
            if (!isAdded) return@addUpdateListener
            val progress = animation.animatedValue as Float
            animSparksOverlayLeft.alpha = progress
            animSparksOverlayRight.alpha = progress
        }
        animationBeam.start()
        pkAnimators.add(animationBeam)

        pkAnimators.add(
            animateRotationY(
                view = animLaser,
                duration = beamAnimationDuration,
                values = floatArrayOf(0f, 1f)
            )
        )

        pkAnimators.add(
            animateRotationY(
                view = animLaserSmall,
                duration = beamSmallAnimationDuration,
                values = floatArrayOf(0f, 1f)
            )
        )
    }

    private fun animateRotationY(
        view: View,
        duration: Long,
        repeatCount: Int = ValueAnimator.INFINITE,
        interpolator: Interpolator = LinearInterpolator(),
        vararg values: Float
    ): Animator {
        val animator = ValueAnimator.ofFloat(*values)
        animator.repeatCount = repeatCount
        animator.interpolator = interpolator
        animator.duration = duration
        animator.addUpdateListener { animation ->
            if (!isAdded) return@addUpdateListener
            val progress = animation.animatedValue as Float
            view.rotationY = 360 * progress
        }
        animator.start()
        return animator
    }

    private fun stopPKAnimation() {
        Timber.d("Stopping PK animation")
        pkAnimators.forEach { it.cancel() }
        pkAnimators.clear()
    }

    private fun adjustAnimViewsWidth() = with(binding.stageCenter.videoStage) {
        animLeft.adjustChildrenWidth(animSparksLeft1, animSparksLeft2, animLeftFlare, widthRation = 2)
        animRight.adjustChildrenWidth(animSparksRight1, animSparksRight2, animRightFlare, widthRation = 2)
    }

    private fun View.resizeAnimated(
        initialHeight: Int,
        initialWidth: Int,
        finalHeight: Int,
        finalWidth: Int,
        marginTop: Int = 0,
        progressCallback: (Float) -> Unit
    ) {
        Timber.d("View resize values: $initialHeight, $initialWidth, $finalHeight, $finalWidth")
        val animator = ValueAnimator.ofFloat(0f, 1f)
        val currentView = this@resizeAnimated

        animator.addUpdateListener { valueAnimator ->
            val progress = valueAnimator.animatedValue as Float
            val layoutParams = currentView.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.height = (initialHeight - (initialHeight - finalHeight) * progress).toInt()
            layoutParams.width = (initialWidth - (initialWidth - finalWidth) * progress).toInt()
            layoutParams.topMargin = (currentView.marginTop - (currentView.marginTop - marginTop) * progress).toInt()
            currentView.layoutParams = layoutParams

            progressCallback(progress)
        }

        animator.duration = PK_MODE_TRANSITION_ANIMATION_DURATION
        animator.start()
    }

    private fun FrameLayout.adjustChildrenWidth(vararg children: View, widthRation: Int = 1) {
        this.doOnLayout {
            val parentWidth = it.measuredWidth
            children.forEach { child ->
                val params = child.layoutParams
                params.width = parentWidth / widthRation
                child.layoutParams = params
            }
        }
    }
}
