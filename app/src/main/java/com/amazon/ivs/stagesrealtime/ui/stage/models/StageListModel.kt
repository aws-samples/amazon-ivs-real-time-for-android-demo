package com.amazon.ivs.stagesrealtime.ui.stage.models

data class StageListModel(
    val stageCount: Int = 0,
    var stageTop: StageUIModel? = null,
    var stageCenter: StageUIModel? = null,
    var stageBottom: StageUIModel? = null,
    var stageDummy: StageUIModel? = null,
)
