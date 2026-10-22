package io.github.abhik9.vocabuilder.feature.study

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class SwipeAnswerTest {
    private val width = 1000f
    private val flingVelocity = 1500f

    private fun swipe(distance: Float, velocity: Float = 0f) = swipeAnswer(distance, velocity, width, flingVelocity)

    @Test
    fun aDragFarEnoughAnswersTheWayItWent() {
        assertEquals(true, swipe(400f))
        assertEquals(false, swipe(-400f))
    }

    @Test
    fun aShortSlowDragGoesBack() {
        assertNull(swipe(250f, velocity = 1000f))
        assertNull(swipe(-250f))
    }

    @Test
    fun aQuickFlickTheWayItWasDraggedAnswers() {
        assertEquals(true, swipe(150f, velocity = 2000f))
        assertEquals(false, swipe(-150f, velocity = -2000f))
    }

    @Test
    fun aFlickBackOrATinyOneDoesNot() {
        assertNull(swipe(250f, velocity = -2000f))
        assertNull(swipe(50f, velocity = 2000f))
    }
}
