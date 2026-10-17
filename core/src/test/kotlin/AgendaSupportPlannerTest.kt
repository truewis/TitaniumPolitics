package com.titaniumPolitics.game

import com.titaniumPolitics.game.core.AgendaSupportPlanner
import com.titaniumPolitics.game.core.AgendaEvidenceNeed
import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.CharacterGenerator
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.NPCRoutines.AddInfoToAgendaRoutine
import com.titaniumPolitics.game.core.NPCRoutines.AttendDivisionMeetingRoutine
import com.titaniumPolitics.game.core.gameActions.Examine
import com.titaniumPolitics.game.core.gameActions.PrepareInfo
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import java.util.Locale

class AgendaSupportPlannerTest {
    @Test
    fun predictsBossEvidenceRequestsAndPreparesEvidenceForTheRelationship() =
        withInitializedGameState { state ->
            val party = state.parties.values.first {
                it.type == Party.Type.WORKPLACE &&
                    it.leader != null &&
                    it.members.size >= 3
            }
            val leader = party.leader!!
            val report = party.treasurer
                ?.takeIf { it != leader }
                ?: party.members.first { it != leader }
            if (party.treasurer != report) {
                party.addMember(report, Party.Role.TREASURER)
            }
            val workplace = party.workplace
            party.isSalaryPaid = true
            state.setMutuality(
                report,
                leader,
                ReadOnly.const("mutualityMax") - state.getMutuality(report, leader)
            )
            state.informations.values
                .filter { it.tgtPlace == workplace.name && it.type == InformationType.RESOURCES }
                .forEach { it.knownTo.remove(leader) }

            val meeting = upcomingMeeting(state, party)
            state.addScheduledMeeting(meeting)

            val info = Information(
                author = report,
                creationTime = state.time,
                type = InformationType.RESOURCES,
                tgtTime = state.time,
                tgtPlace = workplace.name,
                resources = Resources()
            ).apply {
                life = ReadOnly.const("InfoLifetime") * 0.2
                knownTo.add(report)
            }
            state.addInformation(info)

            val positivePrediction = AgendaSupportPlanner.predictFor(state, report)
                .firstOrNull {
                    it.agenda.type == AgendaType.REQUEST &&
                        (it.agenda.attachedRequest?.action as? Examine)?.let { examine ->
                            examine.tgtPlace == workplace.name &&
                                examine.what == InformationType.RESOURCES
                        } == true
                }
            assertNotNull(positivePrediction)
            assertTrue(positivePrediction!!.support)
            assertTrue(
                info.name in AgendaSupportPlanner.recommendInformationKeys(state, report),
                "Known stale workplace evidence should be prepared to support the boss's request."
            )
            val preparation = PrepareInfo(report, workplace.name, state)
            preparation.recommendKeys()
            assertTrue(info.name in preparation.newSetOfPrepInfoKeys)

            state.setMutuality(
                report,
                leader,
                ReadOnly.const("mutualityMin") - state.getMutuality(report, leader)
            )
            info.life = ReadOnly.const("InfoLifetime") * 0.9

            val negativePrediction = AgendaSupportPlanner.predictFor(state, report)
                .firstOrNull {
                    it.agenda.type == AgendaType.REQUEST &&
                        (it.agenda.attachedRequest?.action as? Examine)?.let { examine ->
                            examine.tgtPlace == workplace.name &&
                                examine.what == InformationType.RESOURCES
                        } == true
                }
            assertNotNull(negativePrediction)
            assertFalse(negativePrediction!!.support)
            assertTrue(
                info.name in AgendaSupportPlanner.recommendInformationKeys(state, report),
                "Fresh evidence should be prepared to oppose a boss request the report rejects."
            )

            state.informations.values
                .filter {
                    it.tgtPlace == workplace.name &&
                        it.type == InformationType.RESOURCES &&
                        report in it.knownTo
                }
                .forEach { it.knownTo.remove(report) }
            assertTrue(
                AgendaSupportPlanner.nextInformationNeed(state, report) == AgendaEvidenceNeed(
                    workplace.name,
                    InformationType.RESOURCES
                ),
                "An authorized report should be offered a targeted Examine action to collect counter-evidence."
            )

            meeting.currentCharacters = party.members.toHashSet()
            meeting.currentSpeaker = report
            meeting.currentAttention = 500
            meeting.startTime = state.time
            val requestAgenda = com.titaniumPolitics.game.core.MeetingAgenda(
                AgendaType.REQUEST,
                leader,
                attachedRequest = Request(
                    Examine(leader, workplace.name, InformationType.RESOURCES),
                    issuedTo = hashSetOf(report),
                    issuedBy = hashSetOf(leader),
                    executeTime = meeting.time
                )
            )
            meeting.agendas += requestAgenda
            meeting.setCurrentAgenda(requestAgenda)
            state.removeScheduledMeeting(meeting.ID)
            state.addOngoingMeeting(meeting)

            val response = AttendDivisionMeetingRoutine(state.meetingName(meeting)).also {
                it.injectParent(state)
            }.newIMeetingRoutineCondition(report, meeting.place, emptyList())
            assertTrue(
                response is AddInfoToAgendaRoutine && !response.support,
                "A report with a hostile relationship to the leader should oppose the leader's request."
            )
            state.removeOngoingMeeting(meeting.ID)
        }

    private fun upcomingMeeting(state: GameState, party: Party) = Meeting(
        time = state.time + ReadOnly.constInt("lengthOfDay") / 2,
        type = Meeting.MeetingType.DIVISION_DAILY_CONFERENCE,
        scheduledCharacters = party.members.toHashSet(),
        place = party.home ?: party.workplace.name
    ).apply {
        involvedParty = party.name
    }

    private fun withInitializedGameState(test: (GameState) -> Unit) {
        val workingDirectory = Files.createTempDirectory("agenda-support-planner-test")
        var state: GameState? = null
        CharacterGenerator.generatedNames.clear()
        try {
            val initializedState = Json.decodeFromString(
                GameState.serializer(),
                File("../assets/json/init.json").readText()
            )
            state = initializedState
            initializedState.workingDirectory = workingDirectory.toString()
            Logger.gState = initializedState
            Logger.init()
            ReadOnly.setLocale(Locale.ENGLISH)
            initializedState.initialize()
            test(initializedState)
        } finally {
            try {
                state?.destroy()
            } finally {
                try {
                    CharacterGenerator.generatedNames.clear()
                } finally {
                    Files.walk(workingDirectory).use { paths ->
                        paths.sorted(Comparator.reverseOrder<Path>())
                            .forEach { Files.deleteIfExists(it) }
                    }
                }
            }
        }
    }
}
