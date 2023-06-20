package com.amazon.ivs.stagesrealtime.common

import timber.log.Timber

private const val TIMBER_TAG = "StagesRT"
class LineNumberDebugTree : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement) =
        "$TIMBER_TAG: (${element.fileName}:${element.lineNumber}) #${element.methodName} "
}
