package com.titaniumPolitics.game

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.PolicyCatalog
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Character
import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.core.NPCRoutines.AddInfoToAgendaRoutine
import com.titaniumPolitics.game.core.NPCRoutines.AttendDivisionMeetingRoutine
import com.titaniumPolitics.game.core.NPCRoutines.DowntimeRoutine
import com.titaniumPolitics.game.core.NPCRoutines.LeadDivisionMeetingRoutine
import com.titaniumPolitics.game.core.NPCRoutines.Routine
import com.titaniumPolitics.game.core.gameActions.Intercept
import com.titaniumPolitics.game.core.gameActions.NewAgenda
import com.titaniumPolitics.game.core.gameActions.Salary
import com.titaniumPolitics.game.core.gameActions.SetWorkHours
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import java.util.Locale

class PolicyImplementationTest {
    private fun policyMeetingAgentsProposeAndRespondToPolicyAgendas(state: GameState) {
        val party = state.parties.values.first {
            it.type == Party.Type.WORKPLACE && it.leader != null && it.members.size >= 3
        }
        val leader = party.leader!!
        val workplace = party.workplace
        val home = state.places[party.home!!]!!
        party.policies.clear()
        party.members.forEach { member ->
            state.characters[member]!!.stats.riskTaking = 5
            if (state.characters[member]!!.type != Character.Type.ANON) {
                state.characters[member]!!.reliant = 0
            }
        }
        party.members.forEach { actor ->
            party.members.forEach { target ->
                state.setMutuality(
                    actor,
                    target,
                    ReadOnly.const("mutualityMax") - state.getMutuality(actor, target)
                )
            }
        }
        val relationshipReport = party.members.first { it != leader }
        state.characters[relationshipReport]!!.stats.riskTaking = 10
        state.setMutuality(relationshipReport, leader, -100.0)
        state.setMutuality(
            relationshipReport,
            leader,
            ReadOnly.const("mutualityMin") - state.getMutuality(relationshipReport, leader)
        )
        val hostileStance = PolicyCatalog.preferenceScore(
            party,
            "workhourLimit",
            relationshipReport,
            leader
        )
        state.setMutuality(relationshipReport, leader, 100.0)
        state.setMutuality(
            relationshipReport,
            leader,
            ReadOnly.const("mutualityMax") - state.getMutuality(relationshipReport, leader)
        )
        val friendlyStance = PolicyCatalog.preferenceScore(
            party,
            "workhourLimit",
            relationshipReport,
            leader
        )
        assertTrue(friendlyStance > hostileStance)
        state.characters[relationshipReport]!!.stats.riskTaking = 5
        workplace.workHoursStart = 0
        workplace.workHoursEnd = PolicyCatalog.WORKHOUR_LIMIT_MAX_HOURS * 2
        workplace.apparatuses.forEach {
            it.durability = ReadOnly.const("DurabilityMax")
            it.plannedWorker = it.idealWorker
        }
        party.isSalaryPaid = true
        home.resources["phosphorus"] = 1000.0
        state.requests.values.forEach { it.completed = true }
        state.characters[leader]!!.apply {
            stats.ethos = 1
            trait.remove("gourmand")
            trait.remove("charismatic")
        }

        listOf(
            InformationType.RESOURCES,
            InformationType.HUMAN_RESOURCES,
            InformationType.APPARATUS
        ).forEach { type ->
            Information(
                author = leader,
                creationTime = state.time,
                type = type,
                tgtTime = state.time,
                tgtPlace = workplace.name
            ).also {
                it.knownTo += leader
                state.addInformation(it)
            }
        }

        val meeting = Meeting(
            time = state.time,
            type = Meeting.MeetingType.DIVISION_DAILY_CONFERENCE,
            scheduledCharacters = party.members.toHashSet(),
            place = home.name,
            currentCharacters = party.members.toHashSet()
        ).apply {
            involvedParty = party.name
            currentSpeaker = leader
            currentAttention = 500
            startTime = state.time
        }
        state.addOngoingMeeting(meeting)

        val leadRoutine = LeadDivisionMeetingRoutine(state.meetingName(meeting)).also {
            it.injectParent(state)
        }
        val proposedAction = leadRoutine.executeInMeeting(leader, home.name)
        assertTrue(proposedAction is NewAgenda)
        val policyProposal = proposedAction as NewAgenda
        assertEquals(AgendaType.POLICY_CHANGE, policyProposal.agenda.type)
        assertEquals("workhourLimit", policyProposal.agenda.subjectParams["policy"])
        meeting.agendas += policyProposal.agenda
        meeting.setCurrentAgenda(policyProposal.agenda)
        assertTrue(leadRoutine.newIMeetingRoutineCondition(leader, home.name, emptyList()) is AddInfoToAgendaRoutine)
        assertEquals(null, leadRoutine.newIMeetingRoutineCondition(leader, home.name, emptyList()))

        val beneficiary = party.members.first {
            it != leader && state.characters[it]!!.type != Character.Type.ANON
        }
        val opponent = party.members.first { it != leader && it != beneficiary }
        state.characters[beneficiary]!!.reliant = 1
        state.characters[opponent]!!.reliant = 0
        val responseAgenda = MeetingAgenda(
            AgendaType.POLICY_CHANGE,
            leader,
            subjectParams = hashMapOf("policy" to "paternityLeave")
        )
        meeting.agendas += responseAgenda
        meeting.setCurrentAgenda(responseAgenda)
        meeting.currentAttention = 20
        meeting.currentSpeaker = beneficiary

        val supporterRoutine = AttendDivisionMeetingRoutine(state.meetingName(meeting)).also {
            it.injectParent(state)
        }
        val supportResponse = supporterRoutine.newIMeetingRoutineCondition(
            beneficiary,
            home.name,
            emptyList()
        )
        assertTrue(supportResponse is AddInfoToAgendaRoutine && supportResponse.support)
        assertEquals(
            null,
            supporterRoutine.newIMeetingRoutineCondition(beneficiary, home.name, emptyList())
        )

        meeting.currentSpeaker = leader
        val opponentRoutine = AttendDivisionMeetingRoutine(state.meetingName(meeting)).also {
            it.injectParent(state)
        }
        assertTrue(opponentRoutine.executeInMeeting(opponent, home.name) is Intercept)
        meeting.currentSpeaker = opponent
        val oppositionResponse = opponentRoutine.newIMeetingRoutineCondition(
            opponent,
            home.name,
            emptyList()
        )
        assertTrue(oppositionResponse is AddInfoToAgendaRoutine && !oppositionResponse.support)
        assertEquals(
            null,
            opponentRoutine.newIMeetingRoutineCondition(opponent, home.name, emptyList())
        )
        state.removeOngoingMeeting(meeting.ID)
    }

    @Test
    fun policyAgendasTargetTheMeetingPartyAndApplyOnlyAfterPassing() = withInitializedGameState { state ->
        val longShiftPlace = state.places.values.first { place ->
            place.workHoursLength > PolicyCatalog.WORKHOUR_LIMIT_MAX_HOURS &&
                    place.workplaceParty?.leader != null
        }
        val workplaceParty = longShiftPlace.workplaceParty!!
        val leader = workplaceParty.leader!!
        val home = state.places[workplaceParty.home!!]!!
        val meeting = Meeting(
            time = state.time,
            type = Meeting.MeetingType.DIVISION_DAILY_CONFERENCE,
            scheduledCharacters = workplaceParty.realMembers,
            place = workplaceParty.home!!,
            currentCharacters = workplaceParty.realMembers
        ).apply {
            involvedParty = workplaceParty.name
            currentSpeaker = leader
            currentAttention = 500
        }
        state.addOngoingMeeting(meeting)
        home.resources["phosphorus"] = 1000.0

        assertTrue(PolicyCatalog.isAllowed("workhourLimit", Party.Type.WORKPLACE))
        assertFalse(PolicyCatalog.isAllowed("workhourLimit", Party.Type.DIVISION))
        assertTrue(PolicyCatalog.isAllowed("jobStability", Party.Type.DIVISION))
        assertFalse(PolicyCatalog.isAllowed("jobStability", Party.Type.WORKPLACE))

        val originalStart = longShiftPlace.workHoursStart
        val invalidPolicy = NewAgenda(leader, home.name, state).apply {
            agenda = MeetingAgenda(
                AgendaType.POLICY_CHANGE,
                leader,
                subjectParams = hashMapOf("policy" to "notARealPolicy")
            )
        }
        assertFalse(invalidPolicy.isValid())
        val wrongPartyPolicy = NewAgenda(leader, home.name, state).apply {
            agenda = MeetingAgenda(
                AgendaType.POLICY_CHANGE,
                leader,
                subjectParams = hashMapOf("policy" to "jobStability")
            )
        }
        assertFalse(wrongPartyPolicy.isValid())
        val otherMember = workplaceParty.realMembers.first { it != leader }
        val nonLeaderProposal = NewAgenda(otherMember, home.name, state).apply {
            agenda = MeetingAgenda(
                AgendaType.POLICY_CHANGE,
                otherMember,
                subjectParams = hashMapOf("policy" to "workhourLimit")
            )
        }
        assertFalse(nonLeaderProposal.isValid())

        val policyAgenda = MeetingAgenda(
            AgendaType.POLICY_CHANGE,
            leader,
            subjectParams = hashMapOf("policy" to "workhourLimit")
        )
        val policyProposal = NewAgenda(leader, home.name, state).apply { agenda = policyAgenda }
        assertTrue(policyProposal.isValid())
        policyProposal.execute()
        assertFalse("workhourLimit" in workplaceParty.policies)
        assertEquals(
            1000.0 - ReadOnly.const("ChangePolicyCost"),
            home.resources["phosphorus"],
            1e-6
        )
        policyAgenda.applyPersuasivenessDelta(
            leader,
            ReadOnly.const("AgendaPersuasivenessMax")
        )
        meeting.resolveAgendasByPersuasiveness(state)
        assertTrue("workhourLimit" in workplaceParty.policies)
        assertTrue(longShiftPlace.workHoursLength <= PolicyCatalog.WORKHOUR_LIMIT_MAX_HOURS)
        assertEquals(
            minOf(
                originalStart + PolicyCatalog.WORKHOUR_LIMIT_MAX_HOURS,
                ReadOnly.constInt("lengthOfDay") / ReadOnly.IDTH
            ),
            longShiftPlace.workHoursEnd
        )
        assertFalse(NewAgenda(leader, home.name, state).apply {
            agenda = MeetingAgenda(
                AgendaType.POLICY_CHANGE,
                leader,
                subjectParams = hashMapOf("policy" to "workhourLimit")
            )
        }.isValid())

        val manager = longShiftPlace.manager!!
        assertFalse(
            SetWorkHours(
                manager,
                longShiftPlace.name,
                originalStart,
                originalStart + PolicyCatalog.WORKHOUR_LIMIT_MAX_HOURS + 1,
                state
            ).isValid()
        )
        assertTrue(
            SetWorkHours(
                manager,
                longShiftPlace.name,
                originalStart,
                originalStart + PolicyCatalog.WORKHOUR_LIMIT_MAX_HOURS,
                state
            ).isValid()
        )

        state.removeOngoingMeeting(meeting.ID)
        val division = state.parties.values.first {
            it.type == Party.Type.DIVISION && it.leader != null && it.home != null &&
                    it.members.any { member -> member != it.leader }
        }
        val divisionLeader = division.leader!!
        val divisionMeeting = Meeting(
            time = state.time,
            type = Meeting.MeetingType.DIVISION_DAILY_CONFERENCE,
            scheduledCharacters = division.directorMembers,
            place = division.home!!,
            currentCharacters = division.directorMembers
        ).apply {
            involvedParty = division.name
            currentSpeaker = divisionLeader
            currentAttention = 500
        }
        state.addOngoingMeeting(divisionMeeting)
        val divisionHome = state.places[division.home!!]!!
        divisionHome.resources["phosphorus"] = 1000.0
        division.policies.removeAll(PolicyCatalog.categories["social"]!!.toSet())
        val divisionPolicyAgenda = MeetingAgenda(
            AgendaType.POLICY_CHANGE,
            divisionLeader,
            subjectParams = hashMapOf("policy" to "noTitles")
        )
        val divisionPolicyProposal = NewAgenda(divisionLeader, divisionHome.name, state).apply {
            agenda = divisionPolicyAgenda
        }
        assertTrue(divisionPolicyProposal.isValid())
        divisionPolicyProposal.execute()
        assertFalse("noTitles" in division.policies)
        divisionPolicyAgenda.applyPersuasivenessDelta(
            divisionLeader,
            ReadOnly.const("AgendaPersuasivenessMax")
        )
        divisionMeeting.resolveAgendasByPersuasiveness(state)
        assertTrue("noTitles" in division.policies)
        assertFalse("noTitles" in workplaceParty.policies)

        val managerCandidate = division.directorMembers.first { it != divisionLeader }
        division.policies.add("jobStability")
        val fireAgenda = NewAgenda(divisionLeader, divisionHome.name, state).apply {
            agenda = MeetingAgenda(
                AgendaType.FIRE_MANAGER,
                divisionLeader,
                subjectParams = hashMapOf("character" to managerCandidate)
            )
        }
        assertFalse(fireAgenda.isValid())

        val paternityWorkplace = state.parties.values.first {
            it.type == Party.Type.WORKPLACE && it.leader != null &&
                    it.members.any { member ->
                        state.characters[member]?.let {
                            it.type != Character.Type.ANON && it.reliant > 0
                        } == true
                    }
        }
        paternityWorkplace.policies.add("paternityLeave")
        val employee = paternityWorkplace.members.first {
            state.characters[it]?.let { character ->
                character.type != Character.Type.ANON &&
                        "robot" !in character.trait &&
                        character.reliant > 0
            } == true
        }
        val leaveDay = Math.floorMod(employee.hashCode(), 7)
        val workplace = paternityWorkplace.workplace
        val character = state.characters[employee]!!
        character.health = ReadOnly.const("HealthMax")
        character.hunger = 0.0
        character.thirst = 0.0
        while (state.getMutuality(employee) < ReadOnly.const("DowntimeWill") + 1.0) {
            state.setMutuality(
                employee,
                delta = minOf(
                    100.0,
                    ReadOnly.const("DowntimeWill") + 1.0 - state.getMutuality(employee)
                )
            )
        }
        state.time = leaveDay * ReadOnly.constInt("lengthOfDay") +
                (workplace.workHoursStart + workplace.workHoursLength / 2) * ReadOnly.IDTH
        assertTrue(Routine.isPaternityLeaveDay(employee, state))
        val characterPlace = character.place.name
        assertTrue(
            Routine.isWorkHourWithETA(state, employee, characterPlace, workplace.name)
        )
        assertFalse(
            Routine.isWorkCondition(employee, characterPlace, workplace.name, state)
        )
        val downtimeRoutine = DowntimeRoutine(workplace.name).also { it.injectParent(state) }
        downtimeRoutine.newRoutineCondition(employee, characterPlace, emptyList())
        assertFalse(downtimeRoutine.success)

        val leaveDayMeeting = Meeting(
            time = state.time,
            type = Meeting.MeetingType.TALK,
            scheduledCharacters = hashSetOf(employee),
            place = state.characters[employee]!!.place.name,
            currentCharacters = hashSetOf(employee)
        )
        state.addOngoingMeeting(leaveDayMeeting)
        assertFalse(Routine.isPaternityLeaveDay(employee, state))
        state.removeOngoingMeeting(leaveDayMeeting.ID)
        state.time = ((leaveDay + 1) % 7) * ReadOnly.constInt("lengthOfDay")
        assertFalse(Routine.isPaternityLeaveDay(employee, state))

        val salaryWorkplace = state.parties.values.first {
            it.type == Party.Type.WORKPLACE && it.leader != null &&
                    it.members.any { member ->
                        state.characters[member]?.type == Character.Type.ANON && member != it.leader
                    }
        }
        val salaryLeader = salaryWorkplace.leader!!
        val salaryHome = state.places[salaryWorkplace.home!!]!!
        salaryWorkplace.isSalaryPaid = false
        salaryWorkplace.workplace.responsibleDivision?.let { divisionName ->
            state.parties[divisionName]?.policies?.removeAll(
                setOf(
                    "engineerIncentive",
                    "administratorIncentive",
                    "soldierIncentive",
                    "minerIncentive",
                    "laborerIncentive",
                    "seniority"
                )
            )
        }
        val laborerName = salaryWorkplace.members.first {
            it != salaryLeader && state.characters[it]?.type == Character.Type.ANON
        }
        val laborer = state.characters[laborerName]!!
        val salaryMeeting = Meeting(
            time = state.time,
            type = Meeting.MeetingType.DIVISION_DAILY_CONFERENCE,
            scheduledCharacters = hashSetOf(salaryLeader),
            place = salaryHome.name,
            currentCharacters = hashSetOf(salaryLeader)
        ).apply {
            involvedParty = salaryWorkplace.name
            currentSpeaker = salaryLeader
            currentAttention = 500
        }
        state.addOngoingMeeting(salaryMeeting)
        salaryWorkplace.policies.clear()
        salaryWorkplace.policies.add("laborerIncentive")
        val salary = Salary(salaryLeader, salaryHome.name, state)
        val expectedPayment = Resources()
        salary.who.forEach { recipient ->
            val multiplier = if (state.characters[recipient]!!.type == Character.Type.ANON) 1.5 else 0.8
            expectedPayment += salary.standardRate * multiplier
        }
        expectedPayment.keys.forEach { resource ->
            salaryHome.resources[resource] = expectedPayment[resource]
        }
        salaryHome.resources["ration"] = expectedPayment["ration"] - 0.01
        assertFalse(salary.isValid())
        expectedPayment.keys.forEach { resource ->
            salaryHome.resources[resource] = expectedPayment[resource] + 1e-6
        }
        val rationBefore = laborer.resources["ration"]
        val baseRate = salary.standardRate["ration"]
        assertTrue(salary.isValid(), salary.invalidReason)
        salary.execute()
        assertEquals(baseRate * 1.5, laborer.resources["ration"] - rationBefore, 1e-6)

        val legacyPolicyParty = state.parties.values.firstOrNull { "jobStability" in it.policies }
        assertTrue(state.parties.values.none { "jobSecurity" in it.policies })
        assertTrue(legacyPolicyParty != null)
        state.removeOngoingMeeting(divisionMeeting.ID)
        state.removeOngoingMeeting(salaryMeeting.ID)
        policyMeetingAgentsProposeAndRespondToPolicyAgendas(state)
    }

    private fun withInitializedGameState(test: (GameState) -> Unit) {
        val workingDirectory = Files.createTempDirectory("policy-implementation-test")
        var state: GameState? = null

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
                Files.walk(workingDirectory).use { paths ->
                    paths.sorted(Comparator.reverseOrder<Path>())
                        .forEach { Files.deleteIfExists(it) }
                }
            }
        }
    }
}
