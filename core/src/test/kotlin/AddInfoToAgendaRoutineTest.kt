package com.titaniumPolitics.game.core.NPCRoutines

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AddInfoToAgendaRoutineTest {
    @Test
    fun selectsStrongestEvidenceInRequestedDirection() {
        val effectivities = listOf(
            "weak-support" to 2.0,
            "strong-support" to 8.0,
            "weak-opposition" to -3.0,
            "strong-opposition" to -9.0,
            "neutral" to 0.0
        )

        assertEquals("strong-support", selectBestInformation(effectivities, support = true))
        assertEquals("strong-opposition", selectBestInformation(effectivities, support = false))
        assertNull(selectBestInformation(listOf("neutral" to 0.0), support = true))
        assertNull(selectBestInformation(listOf("neutral" to 0.0), support = false))
    }
}
