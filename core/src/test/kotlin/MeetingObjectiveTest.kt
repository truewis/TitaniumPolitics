package com.titaniumPolitics.game

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.EventSystem
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.JoinMeeting
import com.titaniumPolitics.game.debugTools.Logger
import com.titaniumPolitics.game.events.Event_ImprovisedMeetingObjective
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import java.util.Locale

class MeetingObjectiveTest {
    @Test
    fun joiningMeetingCreatesOneImprovisedObjective() {
        val workingDirectory = Files.createTempDirectory("meeting-objective-test")
        var gameState: GameState? = null

        try {
            val state = Json.decodeFromString(
                GameState.serializer(),
                File("../assets/json/init.json").readText()
            )
            gameState = state
            state.workingDirectory = workingDirectory.toString()
            Logger.gState = state
            Logger.init()
            ReadOnly.setLocale(Locale.ENGLISH)
            state.initialize()

            val player = state.player
            val meeting = Meeting(
                time = state.time,
                type = Meeting.MeetingType.TALK,
                scheduledCharacters = hashSetOf(state.playerName),
                place = player.place.name
            )
            state.addOngoingMeeting(meeting)

            val joinMeeting = JoinMeeting(state.playerName, meeting.place, state)
            assertTrue(joinMeeting.isValid())
            joinMeeting.execute()
            state.time += 1

            val objectives = state.eventSystem.activeQuests.filter {
                it.isImprovised && it.meetingId == meeting.ID
            }
            assertEquals(1, objectives.size)
            assertTrue(objectives.single().event is Event_ImprovisedMeetingObjective)

            val serializedEventSystem = Json.encodeToString(
                EventSystem.serializer(),
                state.eventSystem
            )
            assertTrue(serializedEventSystem.contains(meeting.ID))
        } finally {
            try {
                gameState?.destroy()
            } finally {
                Files.walk(workingDirectory).use { paths ->
                    paths.sorted(Comparator.reverseOrder<Path>())
                        .forEach { Files.deleteIfExists(it) }
                }
            }
        }
    }
}
