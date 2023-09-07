package com.amazon.ivs.stagesrealtime.common

import com.amazon.ivs.stagesrealtime.common.extensions.firstCharUpperCase
import com.amazon.ivs.stagesrealtime.repository.models.UserAvatar
import com.amazon.ivs.stagesrealtime.ui.stage.models.AudioSeatUIModel

const val REQUEST_TIMEOUT = 30L
const val DEFAULT_LOADING_DELAY = 200L
const val DEFAULT_DIALOG_DISMISS_DELAY = 500L
const val DEFAULT_SCROLLING_DELAY = 1000L
const val DEFAULT_COLOR_BOTTOM = "#4ABBAE"
const val DEFAULT_COLOR_LEFT = "#000000"
const val DEFAULT_COLOR_RIGHT = "#FFFFFF"

const val COLOR_BOTTOM_ATTRIBUTE_NAME = "avatarColBottom"
const val COLOR_LEFT_ATTRIBUTE_NAME = "avatarColLeft"
const val COLOR_RIGHT_ATTRIBUTE_NAME = "avatarColRight"

const val RMS_SPEAKING_THRESHOLD = -40F

const val HEART_ANIMATION_FACTOR = 6
const val HEART_ANIMATION_DURATION = 800L
const val HEART_ANIMATION_FADE_DURATION = 1000L
const val PK_MODE_TRANSITION_ANIMATION_DURATION = 250L
const val SHAKE_FORCE_THRESHOLD = 9f
const val SHAKE_TIME_THRESHOLD = 100L
const val SHAKE_COUNT_THRESHOLD = 2
const val VOTE_STEP = 0.05f
const val VOTE_SESSION_TIME_SECONDS = 30L
const val PK_WINNER_START_SCALE = 0.6f
const val DEFAULT_VIDEO_BITRATE = 400000

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

val emptySeats = mutableListOf<AudioSeatUIModel>().apply {
    repeat(12) {
        add(AudioSeatUIModel(id = it))
    }
}

fun getNewStageId(): String {
    if (availableNames.isEmpty()) availableNames.addAll(userNames)
    val firstName = userNames.random()
    availableNames.remove(firstName)
    val lastName = userNames.random()
    availableNames.remove(lastName)
    return firstName.firstCharUpperCase() + lastName.firstCharUpperCase() + (0..9).random()
}

fun getNewUserAvatar() = UserAvatar(
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
