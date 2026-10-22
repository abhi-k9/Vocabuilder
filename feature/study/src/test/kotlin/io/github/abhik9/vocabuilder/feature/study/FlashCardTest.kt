package io.github.abhik9.vocabuilder.feature.study

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import io.github.abhik9.vocabuilder.core.designsystem.theme.VocabuilderTheme
import io.github.abhik9.vocabuilder.core.model.CardFront
import io.github.abhik9.vocabuilder.core.testing.TestData
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FlashCardTest {
    @get:Rule
    val compose = createComposeRule()

    private var answer: Boolean? = null
    private var flips = 0

    private fun showCard(isFlipped: Boolean) = compose.setContent {
        VocabuilderTheme {
            FlashCard(
                word = TestData.word("abate").copy(synonyms = listOf("lessen", "wane"), antonyms = listOf("grow")),
                cardFront = CardFront.WORD,
                isFlipped = isFlipped,
                onFlip = { flips++ },
                onAnswer = { answer = it },
                onLookUp = {},
                modifier = Modifier
                    .size(width = 300.dp, height = 440.dp)
                    .testTag(CARD),
            )
        }
    }

    @Test
    fun swipingATurnedCardRightAnswersKnewIt() {
        showCard(isFlipped = true)

        compose.onNodeWithTag(CARD).performTouchInput { swipeRight() }
        compose.waitForIdle()

        assertEquals(true, answer)
    }

    @Test
    fun swipingATurnedCardLeftAnswersDidNotKnow() {
        showCard(isFlipped = true)

        compose.onNodeWithTag(CARD).performTouchInput { swipeLeft() }
        compose.waitForIdle()

        assertEquals(false, answer)
    }

    @Test
    fun swipingACardBeforeTurningItOverDoesNotAnswerIt() {
        showCard(isFlipped = false)

        compose.onNodeWithTag(CARD).performTouchInput { swipeRight() }
        compose.waitForIdle()

        assertNull(answer)
        assertEquals(0, flips)
    }

    @Test
    fun aQuickFlickAnswersButAShortSlowDragDoesNot() {
        showCard(isFlipped = true)

        compose.onNodeWithTag(CARD).performTouchInput {
            swipe(center, center + Offset(width * 0.25f, 0f), durationMillis = 1_500)
        }
        compose.waitForIdle()
        assertNull(answer)

        compose.onNodeWithTag(CARD).performTouchInput {
            swipe(center, center - Offset(width * 0.25f, 0f), durationMillis = 60)
        }
        compose.waitForIdle()
        assertEquals(false, answer)
    }

    @Test
    fun tappingACardTurnsItOver() {
        showCard(isFlipped = false)

        compose.onNodeWithTag(CARD).performClick()
        compose.waitForIdle()

        assertEquals(1, flips)
    }

    private companion object {
        const val CARD = "card"
    }
}
