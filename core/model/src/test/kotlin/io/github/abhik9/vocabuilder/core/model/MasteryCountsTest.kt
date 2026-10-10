package io.github.abhik9.vocabuilder.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MasteryCountsTest {
    @Test
    fun countsEachState() {
        val counts = MasteryCounts.of(
            listOf(MasteryState.NEW, MasteryState.NEW, MasteryState.LEARNING, MasteryState.MASTERED),
        )

        assertEquals(MasteryCounts(new = 2, learning = 1, reviewing = 0, mastered = 1), counts)
        assertEquals(4, counts.total)
        assertEquals(2, counts.started)
    }

    @Test
    fun addsUp() {
        assertEquals(
            MasteryCounts(new = 3, learning = 1, reviewing = 2, mastered = 4),
            MasteryCounts(new = 1, reviewing = 2) + MasteryCounts(new = 2, learning = 1, mastered = 4),
        )
    }
}
