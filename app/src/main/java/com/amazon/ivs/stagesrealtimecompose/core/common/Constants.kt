package com.amazon.ivs.stagesrealtimecompose.core.common

import com.amazon.ivs.stagesrealtimecompose.core.common.extensions.firstCharUpperCase
import com.amazon.ivs.stagesrealtimecompose.core.handlers.UserAvatar

const val ANIMATION_DURATION_NORMAL = 200L
const val ANIMATION_DURATION_MEDIUM = 300L
const val ANIMATION_DURATION_LONG = 400L
const val ERROR_BAR_DURATION = 5000L
const val BITRATE_MIN = 100000f
const val BITRATE_MAX = 900000f
const val BITRATE_STEP = 50000f
const val BITRATE_DEFAULT = 400000f
const val MAX_MESSAGE_COUNT = 4
const val GET_STAGES_REFRESH_DELAY = 5000L
const val RMS_SPEAKING_THRESHOLD = -40F

const val COLOR_BOTTOM_ATTRIBUTE_NAME = "avatarColBottom"
const val COLOR_LEFT_ATTRIBUTE_NAME = "avatarColLeft"
const val COLOR_RIGHT_ATTRIBUTE_NAME = "avatarColRight"
const val USERNAME_ATTRIBUTE_NAME = "username"

private val userNames = listOf(
    "apple",
    "apricot",
    "avocado",
    "banana",
    "bilberry",
    "blackberry",
    "blueberry",
    "breadfruit",
    "cantaloupe",
    "cherimoya",
    "cherry",
    "clementine",
    "cloudberry",
    "coconut",
    "cranberry",
    "cucumber",
    "currant",
    "damson",
    "date",
    "dragonfruit",
    "durian",
    "eggplant",
    "elderberry",
    "feijoa",
    "fig",
    "gooseberry",
    "grape",
    "grapefruit",
    "guava",
    "honeydew",
    "huckleberry",
    "jackfruit",
    "jambul",
    "jujube",
    "lemon",
    "lime",
    "lychee",
    "mandarine",
    "mango",
    "mulberry",
    "nectarine",
    "olive",
    "orange",
    "papaya",
    "passionfruit",
    "peach",
    "pear",
    "persimmon",
    "pineapple",
    "plum",
    "pomegranate",
    "pomelo",
    "quince",
    "raisin",
    "rambutan",
    "raspberry",
    "satsuma",
    "starfruit",
    "strawberry",
    "tamarillo",
    "tangerine",
    "tomato",
    "watermelon"
)
private val demoColors = listOf(
    "#FF0000", "#FFA500", "#FFFF00", "#008000", "#00FFFF", "#0000FF", "#4B0082", "#9400D3", "#800000", "#FFC0CB",
    "#FF4500", "#FF8C00", "#9ACD32", "#00FF7F", "#00CED1", "#6A5ACD", "#FF69B4", "#DC143C", "#2E8B57", "#8B4513",
    "#FFFFFF", "#000000", "#AAAAAA", "#4ABBAE"
)
private val availableColors = mutableListOf<String>().apply { addAll(demoColors) }
private val availableNames = mutableListOf<String>().apply { addAll(userNames) }

fun getNewStageId(): String {
    if (availableNames.isEmpty()) availableNames.addAll(userNames)
    val firstName = userNames.random()
    availableNames.remove(firstName)
    val lastName = userNames.random()
    availableNames.remove(lastName)
    return firstName.firstCharUpperCase() + lastName.firstCharUpperCase() + (0..9).random()
}

fun getNewUserAvatar(
    hasBorder: Boolean = false,
) = UserAvatar(
    hasBorder = hasBorder,
    colorLeft = getRandomColor(),
    colorRight = getRandomColor(),
    colorBottom = getRandomColor()
)

private fun getRandomColor(): String {
    if (availableColors.isEmpty()) availableColors.addAll(demoColors)
    val color = availableColors.random()
    availableColors.remove(color)
    return color
}
