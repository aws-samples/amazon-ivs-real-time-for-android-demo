package com.amazon.ivs.stagesrealtimecompose.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastJoinToString
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amazon.ivs.stagesrealtimecompose.R
import com.amazon.ivs.stagesrealtimecompose.appContext
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.firstCharUpperCase
import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.formatStringOrEmpty
import com.amazon.ivs.stagesrealtimecompose.core.common.mockRTCData
import com.amazon.ivs.stagesrealtimecompose.core.handlers.NavigationHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.RTCData
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.StageHandler
import com.amazon.ivs.stagesrealtimecompose.core.handlers.stage.StageManager
import com.amazon.ivs.stagesrealtimecompose.ui.components.ButtonPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.components.PreviewSurface
import com.amazon.ivs.stagesrealtimecompose.ui.theme.GraySecondary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.InterDebug
import com.amazon.ivs.stagesrealtimecompose.ui.theme.InterSmall
import com.amazon.ivs.stagesrealtimecompose.ui.theme.RobotoMonoSecondary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.RobotoMonoSecondaryBold
import com.amazon.ivs.stagesrealtimecompose.ui.theme.RobotoPrimary
import com.amazon.ivs.stagesrealtimecompose.ui.theme.WhitePrimary
import com.amazonaws.ivs.broadcast.BroadcastSession
import timber.log.Timber

@Composable
fun DebugDialog() {
    val rtcData by StageManager.rtcData.collectAsStateWithLifecycle()
    val rtcDataList by StageManager.rtcDataList.collectAsStateWithLifecycle()
    val stage = StageHandler.currentStage

    DebugDialogContent(
        isParticipating = stage?.isJoined == true,
        isVideoStage = stage?.isAudioRoom == false,
        rtcData = rtcData,
        rtcDataList = rtcDataList,
    )
}

@Composable
private fun DebugDialogContent(
    isParticipating: Boolean,
    isVideoStage: Boolean,
    rtcData: RTCData,
    rtcDataList: List<RTCData>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.local_debug_data),
                        style = RobotoPrimary,
                    )
                    Text(
                        text = stringResource(R.string.local_debug_data_description),
                        style = InterSmall,
                    )
                }
            }
            item {
                if (!isParticipating) return@item
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isVideoStage) {
                        DebugItem(
                            title = stringResource(R.string.stream_quality),
                            value = rtcData.streamQuality?.name?.lowercase()?.firstCharUpperCase() ?: "-"
                        )
                        DebugItem(
                            title = stringResource(R.string.cpu_limited_time),
                            value = rtcData.cpuLimitedTime?.run { stringResource(R.string.s_template, this) } ?: "-"
                        )
                        DebugItem(
                            title = stringResource(R.string.network_limited_time),
                            value = rtcData.networkLimitedTime?.run { stringResource(R.string.s_template, this) } ?: "-"
                        )
                        DebugDivider()
                    }
                    DebugColumn(
                        rtcData = rtcData,
                        isVideoStage = isVideoStage
                    )
                }
            }
            item {
                if (isParticipating) return@item
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val hostData = rtcDataList.find { it.isCreator }
                    val participants = rtcDataList.filter { it.isParticipant }

                    Column {
                        Text(
                            modifier = Modifier.padding(bottom = 8.dp),
                            text = stringResource(R.string.host_details),
                            style = InterDebug
                        )
                        DebugDivider()
                    }
                    DebugColumn(
                        rtcData = hostData,
                        isVideoStage = isVideoStage,
                        showDivider = false,
                    )
                    if (isVideoStage) {
                        Column(
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text(
                                modifier = Modifier.padding(bottom = 8.dp),
                                text = stringResource(R.string.guest_details),
                                style = InterDebug
                            )
                            DebugDivider()
                        }
                        DebugColumn(
                            rtcData = participants.firstOrNull(),
                            isVideoStage = true,
                            showDivider = false,
                        )
                    } else {
                        participants.forEach { participantData ->
                            Column(
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text(
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    text = stringResource(R.string.details_template, participantData.userName ?: "-"),
                                    style = InterDebug
                                )
                                DebugDivider()
                            }
                            DebugColumn(
                                rtcData = participantData,
                                isVideoStage = false,
                                showDivider = false,
                            )
                        }
                    }
                    DebugDivider()
                }
            }
            item {
                val version = if (LocalInspectionMode.current) "1.29.0" else BroadcastSession.getVersion()
                DebugItem(
                    modifier = Modifier.padding(16.dp),
                    title = stringResource(R.string.sdk_version),
                    value = version
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val stats = if (isParticipating) rtcData.rawRTCStats ?: "" else rtcDataList.asStringData()

            ButtonPrimary(
                text = "Copy to clipboard",
                background = GraySecondary
            ) {
                try {
                    ContextCompat.getSystemService(appContext, ClipboardManager::class.java)?.run {
                        setPrimaryClip(ClipData.newPlainText("RTC stats", stats))
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to copy to clipboard")
                }
            }
            ButtonPrimary(
                text = "Dismiss",
                background = Color.Transparent,
                onClick = NavigationHandler::goBack
            )
        }
    }
}

@Composable
private fun ColumnScope.DebugColumn(
    rtcData: RTCData?,
    isVideoStage: Boolean,
    showDivider: Boolean = true,
) {
    DebugItem(
        title = stringResource(R.string.latency),
        value = rtcData?.latency?.run { stringResource(R.string.ms_template, this) } ?: "-"
    )
    if (isVideoStage) {
        DebugItem(
            title = stringResource(R.string.fps),
            value = rtcData?.fps ?: "-"
        )
    }
    DebugItem(
        title = stringResource(R.string.packets_lost),
        value = rtcData?.packetLoss?.run { stringResource(R.string.percentage_template, this) } ?: "-"
    )
    if (showDivider) {
        DebugDivider()
    }
}

@Composable
private fun DebugDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color = GraySecondary)
    )
}

@Composable
private fun DebugItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = RobotoMonoSecondary,
        )
        Text(
            text = value,
            style = RobotoMonoSecondaryBold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Preview
@Composable
private fun DebugDialogVideoParticipating() {
    DebugDialogPreview(
        isParticipating = true,
        isVideoStage = true,
        rtcData = mockRTCData,
    )
}

@Preview
@Composable
private fun DebugDialogVideoViewing() {
    DebugDialogPreview(
        isParticipating = false,
        isVideoStage = true,
        rtcData = mockRTCData,
        rtcDataList = listOf(
            mockRTCData.copy(isCreator = true),
            mockRTCData.copy(isParticipant = true),
        ),
    )
}

@Preview
@Composable
private fun DebugDialogAudioParticipating() {
    DebugDialogPreview(
        isParticipating = true,
        isVideoStage = false,
        rtcData = mockRTCData,
    )
}

@Preview
@Composable
private fun DebugDialogAudioViewing() {
    DebugDialogPreview(
        isParticipating = false,
        isVideoStage = false,
        rtcData = mockRTCData,
        rtcDataList = listOf(
            mockRTCData.copy(isCreator = true, userName = "Host"),
            mockRTCData.copy(isParticipant = true, userName = "Guest 1"),
            mockRTCData.copy(isParticipant = true, userName = "Guest 2"),
        ),
    )
}

@Composable
private fun DebugDialogPreview(
    isParticipating: Boolean,
    isVideoStage: Boolean,
    rtcData: RTCData = RTCData(),
    rtcDataList: List<RTCData> = emptyList<RTCData>()
) {
    PreviewSurface(
        background = WhitePrimary
    ) {
        DebugDialogContent(
            isParticipating = isParticipating,
            isVideoStage = isVideoStage,
            rtcData = rtcData,
            rtcDataList = rtcDataList
        )
    }
}

@Composable
private fun List<RTCData>?.asStringData() = if (this == null) "" else map { data ->
    "${data.userName}:" +
        data.latency.formatStringOrEmpty { "\n${stringResource(R.string.latency)}:$it" }.prependIndent() +
        data.fps.formatStringOrEmpty { "\n${stringResource(R.string.fps)}:$it" }.prependIndent() +
        data.packetLoss.formatStringOrEmpty {
            "\n${stringResource(R.string.packets_lost)}:$it"
        }.prependIndent() + "\n"
}.fastJoinToString(separator = "")
