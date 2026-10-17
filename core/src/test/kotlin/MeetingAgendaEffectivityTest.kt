package com.titaniumPolitics.game

import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.gameActions.*
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import java.util.Locale

class MeetingAgendaEffectivityTest {
    @Test
    fun budgetEvidenceProducesSignedPersuasiveness() = withInitializedGameState { state ->
        resourceEvidenceRewardsBudgetsCloserToExpectedConsumption(state)
        workforceEvidenceRewardsBudgetChangesMatchingStaffingNeeds(state)
        cabinetBudgetEvidenceUsesTheWorkplacesShareOfItsDivisionAllocation(state)
        otherAgendaEvidenceIsSpecificAndSigned(state)
        policyChangeEvidenceTracksAffectedMembersAndDomain(state)
    }

    private fun resourceEvidenceRewardsBudgetsCloserToExpectedConsumption(state: GameState) {
        val place = suitableWorkplace(state)
        val workplaceParty = place.workplaceParty!!
        val division = state.parties[place.responsibleDivision]!!
        val expected = Resources()
        place.apparatuses.forEach { apparatus ->
            expected += apparatus.hourlyOperationBudget *
                    place.workHoursLength * ReadOnly.constInt("quarterInDays")
        }
        val resource = expected.keys.first { expected[it] > 0.0 }
        val standardAllocation = workplaceParty.standardBudget.value[workplaceParty.name]!!
        val currentAllocation = Resources(standardAllocation.toHashMap().apply {
            put(resource, expected[resource] * 0.25)
        })
        val supportedAllocation = Resources(standardAllocation.toHashMap().apply {
            put(resource, expected[resource])
        })
        division.budget = Budget(hashMapOf(workplaceParty.name to currentAllocation))

        val meeting = budgetMeeting(place, division.name)
        val agenda = MeetingAgenda(
            AgendaType.BUDGET_PROPOSAL,
            "proposal",
            attachedBudget = Budget(hashMapOf(workplaceParty.name to supportedAllocation))
        )
        val info = Information(
            type = InformationType.RESOURCES,
            tgtPlace = place.name,
            resources = Resources()
        )

        assertTrue(agenda.effectivity(state, meeting, info, state.player).first > 0.0)

        division.budget = Budget(hashMapOf(workplaceParty.name to supportedAllocation))
        agenda.attachedBudget = Budget(hashMapOf(workplaceParty.name to currentAllocation))
        assertTrue(agenda.effectivity(state, meeting, info, state.player).first < 0.0)
    }

    private fun workforceEvidenceRewardsBudgetChangesMatchingStaffingNeeds(state: GameState) {
        val place = suitableWorkplace(state)
        val workplaceParty = place.workplaceParty!!
        val division = state.parties[place.responsibleDivision]!!
        val idealWorkers = place.apparatuses.sumOf { it.idealWorker }
        val standardAllocation = workplaceParty.standardBudget.value[workplaceParty.name]!!
        val currentAllocation = Resources(standardAllocation.toHashMap())
        val increasedAllocation = Resources(standardAllocation.toHashMap().apply {
            put("water", standardAllocation["water"] * 1.5)
            put("ration", standardAllocation["ration"] * 1.5)
        })
        division.budget = Budget(hashMapOf(workplaceParty.name to currentAllocation))

        val meeting = budgetMeeting(place, division.name)
        val agenda = MeetingAgenda(
            AgendaType.BUDGET_PROPOSAL,
            "proposal",
            attachedBudget = Budget(hashMapOf(workplaceParty.name to increasedAllocation))
        )
        val info = Information(
            type = InformationType.HUMAN_RESOURCES,
            tgtPlace = place.name,
            amount = idealWorkers - 1
        )

        assertTrue(agenda.effectivity(state, meeting, info, state.player).first > 0.0)

        agenda.attachedBudget = Budget(hashMapOf(workplaceParty.name to currentAllocation * 0.5))
        assertTrue(agenda.effectivity(state, meeting, info, state.player).first < 0.0)
    }

    private fun cabinetBudgetEvidenceUsesTheWorkplacesShareOfItsDivisionAllocation(state: GameState) {
        val place = suitableWorkplace(state)
        val division = state.parties[place.responsibleDivision]!!
        val cabinet = state.parties["cabinet"]!!
        val idealWorkers = place.apparatuses.sumOf { it.idealWorker }
        val standardBudget = cabinet.standardBudget
        val currentValues = hashMapOf<String, Resources>()
        val proposedValues = hashMapOf<String, Resources>()
        standardBudget.value.forEach { (partyName, allocation) ->
            currentValues[partyName] = Resources(allocation.toHashMap())
            proposedValues[partyName] = Resources(allocation.toHashMap())
        }
        val currentBudget = Budget(currentValues)
        val proposedBudget = Budget(proposedValues)
        val divisionAllocation = proposedBudget.value[division.name]!!
        divisionAllocation["water"] = divisionAllocation["water"] * 1.5
        divisionAllocation["ration"] = divisionAllocation["ration"] * 1.5
        cabinet.budget = currentBudget

        val meeting = budgetMeeting(place, cabinet.name)
        val agenda = MeetingAgenda(
            AgendaType.BUDGET_PROPOSAL,
            "proposal",
            attachedBudget = proposedBudget
        )
        val info = Information(
            type = InformationType.HUMAN_RESOURCES,
            tgtPlace = place.name,
            amount = idealWorkers - 1
        )

        assertTrue(agenda.effectivity(state, meeting, info, state.player).first > 0.0)
    }

    private fun otherAgendaEvidenceIsSpecificAndSigned(state: GameState) {
        val place = suitableWorkplace(state)
        val division = state.parties[place.responsibleDivision]!!
        val candidate = division.members.first { state.characters[it]?.type != Character.Type.ANON }
        val speaker = state.characters.values.first { it.name != candidate }
        val source = state.characters.values.first { it.name != candidate && it.name != speaker.name }
        val apparatus = place.apparatuses.first()
        val meeting = Meeting(
            time = state.time,
            type = Meeting.MeetingType.TALK,
            scheduledCharacters = hashSetOf(),
            place = place.name,
            currentCharacters = hashSetOf(speaker.name, source.name)
        ).apply {
            involvedParty = division.name
        }

        val repairInformation = Information(
            author = source.name,
            creationTime = state.time,
            type = InformationType.ACTION,
            tgtTime = state.time,
            tgtPlace = place.name,
            tgtCharacter = candidate,
            action = Repair(candidate, place.name, apparatus.ID)
        )
        val praise = MeetingAgenda(
            AgendaType.PRAISE,
            speaker.name,
            subjectParams = hashMapOf("character" to candidate)
        )
        val originalMutuality = state.getMutuality(speaker.name, source.name)
        state.setMutuality(
            speaker.name,
            source.name,
            ReadOnly.const("mutualityMax") - originalMutuality
        )
        val trustedEvidence = praise.effectivity(state, meeting, repairInformation, speaker).first
        state.setMutuality(speaker.name, source.name, -state.getMutuality(speaker.name, source.name))
        val distrustedEvidence = praise.effectivity(state, meeting, repairInformation, speaker).first
        assertTrue(trustedEvidence > distrustedEvidence)
        repairInformation.author = null

        val repeatedAgenda = MeetingAgenda(
            AgendaType.PRAISE,
            speaker.name,
            subjectParams = hashMapOf("character" to candidate)
        )
        val firstReport = repairInformation.copy(author = null)
        state.addInformation(firstReport)
        val firstScore = repeatedAgenda.effectivity(state, meeting, firstReport, speaker).first
        repeatedAgenda.informationKeys.add(firstReport.name)
        val duplicateReport = firstReport.copy()
        val repeatedScore = repeatedAgenda.effectivity(state, meeting, duplicateReport, speaker).first
        assertTrue(repeatedScore in 0.0..firstScore)
        assertTrue(repeatedScore < firstScore)

        val manager = place.manager ?: division.leader!!
        val fireAgenda = MeetingAgenda(
            AgendaType.FIRE_MANAGER,
            speaker.name,
            subjectParams = hashMapOf("character" to manager)
        )
        val apparatusReport = Information(
            type = InformationType.APPARATUS,
            tgtPlace = place.name,
            tgtApparatusID = apparatus.ID,
            variables = hashMapOf("durability" to 10.0)
        )
        val freshFireScore = fireAgenda.effectivity(state, meeting, apparatusReport, speaker).first
        apparatusReport.life = 0.0
        assertTrue(freshFireScore > 0.0)
        assertEquals(0.0, fireAgenda.effectivity(state, meeting, apparatusReport, speaker).first)

        val request = Request(
            Repair(speaker.name, place.name, apparatus.ID),
            issuedTo = hashSetOf(speaker.name),
            issuedBy = hashSetOf(source.name),
            executeTime = state.time
        )
        val requestAgenda = MeetingAgenda(
            AgendaType.REQUEST,
            speaker.name,
            attachedRequest = request
        )
        val wornApparatusReport = Information(
            type = InformationType.APPARATUS,
            tgtPlace = place.name,
            tgtApparatusID = apparatus.ID,
            tgtTime = state.time,
            variables = hashMapOf("durability" to 10.0)
        )
        val healthyApparatusReport = wornApparatusReport.copy(
            variables = hashMapOf("durability" to 100.0)
        )
        assertTrue(requestAgenda.effectivity(state, meeting, wornApparatusReport, speaker).first > 0.0)
        assertTrue(requestAgenda.effectivity(state, meeting, healthyApparatusReport, speaker).first < 0.0)

        val proof = Information(
            type = InformationType.ACTION,
            tgtTime = state.time,
            tgtPlace = place.name,
            tgtCharacter = speaker.name,
            action = Repair(source.name, place.name, apparatus.ID)
        )
        assertTrue(requestAgenda.effectivity(state, meeting, proof, speaker).first < 0.0)
        val proofAgenda = MeetingAgenda(
            AgendaType.PROOF_OF_WORK,
            speaker.name,
            attachedRequest = request
        )
        assertTrue(proofAgenda.effectivity(state, meeting, proof, speaker).first > 0.0)

        val destination = state.places.values.first { it.name != place.name }
        val transferAction = OfficialResourceTransfer(
            speaker.name,
            place.name,
            destination.name,
            Resources("water" to 10.0)
        )
        val transferAgenda = MeetingAgenda(
            AgendaType.REQUEST,
            speaker.name,
            attachedRequest = Request(
                transferAction,
                issuedTo = hashSetOf(speaker.name),
                issuedBy = hashSetOf(source.name),
                executeTime = state.time
            )
        )
        val availableAtSource = Information(
            type = InformationType.RESOURCES,
            tgtTime = state.time,
            tgtPlace = place.name,
            resources = Resources("water" to 20.0)
        )
        val alreadyAtDestination = Information(
            type = InformationType.RESOURCES,
            tgtTime = state.time,
            tgtPlace = destination.name,
            resources = Resources("water" to 20.0)
        )
        assertTrue(transferAgenda.effectivity(state, meeting, availableAtSource, speaker).first > 0.0)
        assertTrue(transferAgenda.effectivity(state, meeting, alreadyAtDestination, speaker).first < 0.0)

        val examineAgenda = MeetingAgenda(
            AgendaType.REQUEST,
            speaker.name,
            attachedRequest = Request(
                Examine(speaker.name, place.name, InformationType.RESOURCES),
                issuedTo = hashSetOf(speaker.name),
                issuedBy = hashSetOf(source.name),
                executeTime = state.time
            )
        )
        val existingResourceReport = Information(
            type = InformationType.RESOURCES,
            tgtTime = state.time,
            tgtPlace = place.name
        )
        assertTrue(examineAgenda.effectivity(state, meeting, existingResourceReport, speaker).first < 0.0)
        existingResourceReport.life = 0.0
        assertTrue(examineAgenda.effectivity(state, meeting, existingResourceReport, speaker).first > 0.0)

        val setWorkersAction = SetWorkers(
            speaker.name,
            place.name,
            apparatus.idealWorker,
            apparatus.ID
        )
        val staffingAgenda = MeetingAgenda(
            AgendaType.REQUEST,
            speaker.name,
            attachedRequest = Request(
                setWorkersAction,
                issuedTo = hashSetOf(speaker.name),
                issuedBy = hashSetOf(source.name),
                executeTime = state.time
            )
        )
        val apparatusWorkers = apparatus.idealWorker / 2.0
        val staffingReport = Information(
            type = InformationType.APPARATUS,
            tgtTime = state.time,
            tgtPlace = place.name,
            tgtApparatusID = apparatus.ID,
            variables = hashMapOf(
                "currentWorker" to apparatusWorkers,
                "idealWorker" to apparatus.idealWorker.toDouble()
            )
        )
        assertTrue(staffingAgenda.effectivity(state, meeting, staffingReport, speaker).first > 0.0)

        val salaryAction = Salary(speaker.name, division.home!!)
        val requiredSalary =
            Salary.standardQuarterlyRate(division.type) * (division.members - speaker.name).size
        val salaryAgenda = MeetingAgenda(
            AgendaType.REQUEST,
            speaker.name,
            attachedRequest = Request(
                salaryAction,
                issuedTo = hashSetOf(speaker.name),
                issuedBy = hashSetOf(source.name),
                executeTime = state.time
            )
        )
        val salaryResources = Information(
            type = InformationType.RESOURCES,
            tgtTime = state.time,
            tgtPlace = division.home!!,
            resources = requiredSalary
        )
        assertTrue(salaryAgenda.effectivity(state, meeting, salaryResources, speaker).first > 0.0)
        salaryResources.resources = Resources()
        assertTrue(salaryAgenda.effectivity(state, meeting, salaryResources, speaker).first < 0.0)

        val wrongRequestAgenda = MeetingAgenda(
            AgendaType.PROOF_OF_WORK,
            speaker.name,
            attachedRequest = Request(
                Repair(speaker.name, place.name, "${apparatus.ID}_other"),
                issuedTo = hashSetOf(speaker.name),
                issuedBy = hashSetOf(source.name),
                executeTime = state.time
            )
        )
        assertEquals(0.0, wrongRequestAgenda.effectivity(state, meeting, proof, speaker).first)

        val pastPromiseRequest = Request(
            Repair(speaker.name, place.name, apparatus.ID),
            issuedTo = hashSetOf(speaker.name),
            issuedBy = hashSetOf(source.name)
        ).apply { completed = true }
        val pastPromise = NewAgenda(speaker.name, place.name).apply {
            agenda = MeetingAgenda(
                AgendaType.PROMISE,
                speaker.name,
                attachedRequest = pastPromiseRequest
            )
        }
        val pastPromiseInfo = Information(
            type = InformationType.ACTION,
            tgtCharacter = speaker.name,
            action = pastPromise
        )
        val promiseAgenda = MeetingAgenda(
            AgendaType.PROMISE,
            speaker.name,
            attachedRequest = request
        )
        assertTrue(promiseAgenda.effectivity(state, meeting, pastPromiseInfo, speaker).first > 0.0)
        pastPromiseRequest.completed = false
        assertEquals(0.0, promiseAgenda.effectivity(state, meeting, pastPromiseInfo, speaker).first)

        val nomineeAgenda = MeetingAgenda(
            AgendaType.NOMINATE,
            speaker.name,
            subjectParams = hashMapOf("character" to candidate)
        )
        assertTrue(nomineeAgenda.effectivity(state, meeting, repairInformation, speaker).first > 0.0)
        val partyPraise = MeetingAgenda(
            AgendaType.PRAISE_PARTY,
            speaker.name,
            subjectParams = hashMapOf("party" to division.name)
        )
        val partyDenounce = MeetingAgenda(
            AgendaType.DENOUNCE_PARTY,
            speaker.name,
            subjectParams = hashMapOf("party" to division.name)
        )
        assertTrue(partyPraise.effectivity(state, meeting, repairInformation, speaker).first > 0.0)
        assertTrue(partyDenounce.effectivity(state, meeting, repairInformation, speaker).first < 0.0)
        assertEquals(
            0.0,
            MeetingAgenda(AgendaType.APPOINT_MEETING, speaker.name)
                .effectivity(state, meeting, repairInformation, speaker).first
        )
    }

    private fun policyChangeEvidenceTracksAffectedMembersAndDomain(state: GameState) {
        val party = state.parties.values.first {
            it.type == Party.Type.WORKPLACE && it.leader != null && it.members.size >= 3
        }
        val leader = party.leader!!
        val beneficiary = party.members.first {
            it != leader && state.characters[it]?.type != Character.Type.ANON
        }
        val harmed = party.members.first { it != leader && it != beneficiary }
        state.characters[beneficiary]!!.reliant = 1
        state.characters[harmed]!!.reliant = 0

        val meeting = Meeting(
            time = state.time,
            type = Meeting.MeetingType.DIVISION_DAILY_CONFERENCE,
            scheduledCharacters = hashSetOf(),
            place = party.home!!,
            currentCharacters = hashSetOf(leader, beneficiary, harmed)
        ).apply {
            involvedParty = party.name
        }
        val agenda = MeetingAgenda(
            AgendaType.POLICY_CHANGE,
            leader,
            subjectParams = hashMapOf("policy" to "paternityLeave")
        )

        val beneficiaryInformation = Information(
            type = InformationType.ACTION,
            tgtTime = state.time,
            tgtCharacter = beneficiary
        )
        val harmfulInformation = Information(
            type = InformationType.ACTION,
            tgtTime = state.time,
            tgtCharacter = harmed
        )
        assertTrue(
            agenda.effectivity(state, meeting, beneficiaryInformation, state.characters[beneficiary]!!).first > 0.0
        )
        assertTrue(
            agenda.effectivity(state, meeting, harmfulInformation, state.characters[harmed]!!).first < 0.0
        )

        val unrelatedInformation = Information(
            type = InformationType.ACTION,
            tgtTime = state.time,
            tgtCharacter = state.characters.keys.first { it !in party.members }
        )
        assertEquals(
            0.0,
            agenda.effectivity(state, meeting, unrelatedInformation, state.player).first
        )
    }

    private fun suitableWorkplace(state: GameState): Place =
        state.places.values.first {
            it.workplaceParty != null &&
                    it.responsibleDivision != null &&
                    it.workHoursLength > 0 &&
                    it.apparatuses.any { apparatus -> apparatus.idealWorker > 0 }
        }

    private fun budgetMeeting(place: Place, divisionName: String) = Meeting(
        time = 0,
        type = Meeting.MeetingType.BUDGET_PROPOSAL,
        scheduledCharacters = hashSetOf(),
        place = place.name
    ).apply {
        involvedParty = divisionName
    }

    private fun withInitializedGameState(test: (GameState) -> Unit) {
        val workingDirectory = Files.createTempDirectory("budget-effectivity-test")
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
