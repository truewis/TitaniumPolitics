package com.titaniumPolitics.game

import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.gameActions.Wait
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RequestTest {
    @Test
    fun completedRequestsAreNotPendingForTheirRecipient() {
        val request = request()

        assertTrue(request.isPendingFor("recipient"))
        assertFalse(request.isPendingFor("other"))

        request.completed = true

        assertFalse(request.isPendingFor("recipient"))
    }

    @Test
    fun nullAndZeroExecutionTimesMeanAnytime() {
        listOf(null, 0).forEach { executeTime ->
            val request = request(executeTime)

            assertTrue(request.isAnytime)
            assertTrue(request.matchesExecutionTime(time = 1000, tolerance = 0))
            assertFalse(request.isExpiredAt(time = 1000, lifetime = 1))
        }
    }

    @Test
    fun scheduledExecutionTimeUsesTheProvidedTolerance() {
        val request = request(executeTime = 100)

        assertFalse(request.isAnytime)
        assertTrue(request.matchesExecutionTime(time = 102, tolerance = 2))
        assertFalse(request.matchesExecutionTime(time = 103, tolerance = 2))
        assertFalse(request.isExpiredAt(time = 102, lifetime = 3))
        assertTrue(request.isExpiredAt(time = 103, lifetime = 3))
    }

    private fun request(executeTime: Int? = null) = Request(
        action = Wait("recipient", "home_recipient"),
        issuedTo = hashSetOf("recipient"),
        executeTime = executeTime
    )
}
