package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max

@Serializable
data class MeetingAgenda(
    var type: AgendaType,
    var author: String,
    var subjectParams: HashMap<String, String> = hashMapOf(),
    var subjectIntParams: HashMap<String, Int> = hashMapOf(),
    var informationKeys: ArrayList<String> = arrayListOf(),
    var attachedRequest: Request? = null,
    var attachedBudget: Budget? = null,
    var persuasiveness: Double = 0.0,
    var lastSupporter: String? = null,
    var lastAttacker: String? = null


) {
    fun applyPersuasivenessDelta(actor: String, delta: Double) {
        if (delta > 0) {
            lastSupporter = actor
        } else if (delta < 0) {
            lastAttacker = actor
        }
        persuasiveness = (persuasiveness + delta).coerceIn(
            ReadOnly.const("AgendaPersuasivenessMin"),
            ReadOnly.const("AgendaPersuasivenessMax")
        )
    }

    fun isConstructed(): Boolean = persuasiveness >= ReadOnly.const("AgendaPersuasivenessMax")
    fun isRejected(): Boolean = persuasiveness <= ReadOnly.const("AgendaPersuasivenessMin")

    /**
     * Compute the effectivity of an information for this agenda for a character in the meeting.
     * Unit: Mutuality
     */
    fun effectivity(
        parent: GameState,
        meeting: Meeting,
        info: Information,
        sbjCharObj: Character
    ): Pair<Double, String> {
        val rawEffectivity = calculateEffectivity(parent, meeting, info, sbjCharObj)
        if (rawEffectivity.first == 0.0) return rawEffectivity

        val confidence = sourceConfidence(parent, meeting, info)
        val freshness = informationFreshness(info)
        val repetition = repetitionFactor(parent, info)
        val score = rawEffectivity.first * confidence * freshness * repetition
        return if (score == 0.0) Pair(0.0, "") else Pair(score, rawEffectivity.second)
    }

    private fun calculateEffectivity(
        parent: GameState,
        meeting: Meeting,
        info: Information,
        speaker: Character
    ): Pair<Double, String> {
        when (type) {
            AgendaType.PROOF_OF_WORK -> return proofOfWorkEffectivity(meeting, info, speaker)
            AgendaType.NOMINATE -> return characterPerformanceEffectivity(
                parent, meeting, info, speaker, subjectParams["character"], AgendaType.NOMINATE
            )
            AgendaType.PRAISE -> return characterPerformanceEffectivity(
                parent, meeting, info, speaker, subjectParams["character"], AgendaType.PRAISE
            )
            AgendaType.DENOUNCE -> return characterPerformanceEffectivity(
                parent, meeting, info, speaker, subjectParams["character"], AgendaType.DENOUNCE
            )
            AgendaType.FIRE_MANAGER -> return characterPerformanceEffectivity(
                parent, meeting, info, speaker, subjectParams["character"], AgendaType.FIRE_MANAGER
            )
            AgendaType.PRAISE_PARTY -> return partyPerformanceEffectivity(
                parent, info, speaker, subjectParams["party"], denounce = false
            )
            AgendaType.DENOUNCE_PARTY -> return partyPerformanceEffectivity(
                parent, info, speaker, subjectParams["party"], denounce = true
            )
            AgendaType.REQUEST -> return requestEffectivity(parent, meeting, info, speaker)
            AgendaType.PROMISE -> return promiseEffectivity(parent, meeting, info, speaker)
            AgendaType.BUDGET_PROPOSAL -> return budgetProposalEffectivity(parent, meeting, info, speaker)
            AgendaType.APPOINT_MEETING -> return Pair(0.0, "")
        }
    }

    private fun sourceConfidence(parent: GameState, meeting: Meeting, info: Information): Double {
        val author = info.author ?: return if (
            info.type == InformationType.ACTION &&
            info.action == null &&
            info.tgtCharacter == null &&
            info.tgtPlace.isEmpty()
        ) 0.25 else 1.0
        if (parent.characters[author] == null) return 0.5

        val audienceTrust = meeting.currentCharacters
            .filter { it != author && parent.characters.containsKey(it) }
            .map { parent.getMutNorm(it, author) }
        if (audienceTrust.isEmpty()) return 1.0
        return ((audienceTrust.average() + 1.0) / 2.0).coerceIn(0.0, 1.0)
    }

    private fun informationFreshness(info: Information): Double {
        if (type in setOf(AgendaType.REQUEST, AgendaType.PROMISE) &&
            attachedRequest?.action is Examine
        ) return 1.0
        return snapshotFreshness(info)
    }

    private fun snapshotFreshness(info: Information): Double {
        if (info.type !in setOf(
                InformationType.RESOURCES,
                InformationType.HUMAN_RESOURCES,
                InformationType.APPARATUS,
                InformationType.CASUALTY,
                InformationType.ACCIDENT,
                InformationType.SOUND
            )
        ) return 1.0

        val lifetime = ReadOnly.const("InfoLifetime")
        if (lifetime <= 0.0) return 1.0
        return (info.life / lifetime).coerceIn(0.0, 1.0)
    }

    private fun repetitionFactor(parent: GameState, info: Information): Double {
        val repetitions = informationKeys.mapNotNull { parent.informations[it] }
            .count { sameEvidenceScope(it, info) }
        return 1.0 / (1.0 + repetitions * 0.5)
    }

    private fun sameEvidenceScope(first: Information, second: Information): Boolean {
        if (first.type != second.type || abs(first.tgtTime - second.tgtTime) > ReadOnly.IDTH)
            return false
        return when (first.type) {
            InformationType.ACTION ->
                first.tgtCharacter == second.tgtCharacter &&
                        first.tgtPlace == second.tgtPlace &&
                        first.action == second.action
            InformationType.MUTUALITY ->
                first.tgtCharacter == second.tgtCharacter && first.auxCharacter == second.auxCharacter
            InformationType.PARTY_MUTUALITY ->
                first.tgtParty == second.tgtParty && first.auxParty == second.auxParty
            InformationType.TRAIT -> first.tgtCharacter == second.tgtCharacter &&
                    first.variables["trait"] == second.variables["trait"]
            else -> first.tgtPlace == second.tgtPlace && first.tgtCharacter == second.tgtCharacter
        }
    }

    private fun proofOfWorkEffectivity(
        meeting: Meeting,
        info: Information,
        speaker: Character
    ): Pair<Double, String> {
        val request = attachedRequest ?: return Pair(0.0, "")
        if (request.issuedBy.none { it in meeting.currentCharacters }) return Pair(0.0, "")
        if (!request.action.isProofOfWork(info) || !requestTimeMatches(request, info)) return Pair(0.0, "")
        return Pair(5.0 * speaker.stats.lScale, "ProofOfWork")
    }

    private fun requestTimeMatches(request: Request, info: Information): Boolean =
        request.executeTime?.let { abs(it - info.tgtTime) <= ReadOnly.IDTH } ?: true

    private fun requestEffectivity(
        parent: GameState,
        meeting: Meeting,
        info: Information,
        speaker: Character
    ): Pair<Double, String> {
        val request = attachedRequest ?: return Pair(0.0, "")
        val signal = requestEvidenceSignal(parent, meeting, request, info)
        if (signal == 0.0) return Pair(0.0, "")
        val scale = if (signal > 0) speaker.stats.eScale else speaker.stats.pScale
        return Pair(10.0 * signal * scale, "Request")
    }

    private fun promiseEffectivity(
        parent: GameState,
        meeting: Meeting,
        info: Information,
        speaker: Character
    ): Pair<Double, String> {
        val request = attachedRequest ?: return Pair(0.0, "")
        val pastPromise = info.action as? NewAgenda
        if (pastPromise?.agenda?.type == AgendaType.PROMISE &&
            pastPromise.agenda.author == author &&
            pastPromise.agenda.attachedRequest?.action?.let { actionsAreSimilar(it, request.action) } == true &&
            pastPromiseWasCompleted(parent, pastPromise)
        ) {
            return Pair(5.0 * speaker.stats.eScale, "Promise")
        }

        val signal = requestEvidenceSignal(parent, meeting, request, info)
        if (signal == 0.0) return Pair(0.0, "")
        val scale = if (signal > 0) speaker.stats.lScale else speaker.stats.pScale
        return Pair(5.0 * signal * scale, "Promise")
    }

    private fun pastPromiseWasCompleted(parent: GameState, pastPromise: NewAgenda): Boolean {
        val promiseRequest = pastPromise.agenda.attachedRequest ?: return false
        if (promiseRequest.completed) return true
        return parent.requests.values.any {
            it.completed &&
                    it.issuedTo.contains(pastPromise.sbjCharacter) &&
                    it.issuedBy == promiseRequest.issuedBy &&
                    it.action == promiseRequest.action
        }
    }

    private fun actionsAreSimilar(first: GameAction, second: GameAction): Boolean =
        first::class == second::class && first.tgtPlace == second.tgtPlace

    private fun requestEvidenceSignal(
        parent: GameState,
        meeting: Meeting,
        request: Request,
        info: Information
    ): Double {
        val action = request.action
        if (info.type == InformationType.ACTION &&
            action.isProofOfWork(info) &&
            requestTimeMatches(request, info)
        ) return -1.0

        return when (action) {
            is Examine -> {
                if (info.tgtPlace != action.tgtPlace || info.type != action.what) 0.0
                else {
                    val freshness = snapshotFreshness(info)
                    1.0 - 2.0 * freshness
                }
            }

            is OfficialResourceTransfer ->
                transferEvidenceSignal(info, action.tgtPlace, action.toWhere, action.resources)

            is UnofficialResourceTransfer -> {
                val source = if (action.fromHome) null else action.tgtPlace
                transferEvidenceSignal(info, source, action.toWhere, action.resources, action.sbjCharacter)
            }

            is Repair -> repairEvidenceSignal(parent, action, info)

            is SetWorkers -> {
                if (info.type != InformationType.APPARATUS ||
                    info.tgtPlace != action.tgtPlace ||
                    info.tgtApparatusID != action.apparatusID
                ) 0.0 else {
                    val current = info.variables["currentWorker"] ?: return 0.0
                    val ideal = info.variables["idealWorker"] ?: return 0.0
                    val currentGap = abs(current - ideal)
                    val proposedGap = abs(action.workers - ideal)
                    ((currentGap - proposedGap) / max(1.0, ideal)).coerceIn(-1.0, 1.0)
                }
            }

            is Salary -> salaryEvidenceSignal(parent, meeting, action, info)

            is InvestigateAccidentScene -> accidentRequestSignal(parent, action.tgtPlace, info)

            is ClearAccidentScene -> accidentRequestSignal(parent, action.tgtPlace, info)

            is AnnounceInfo -> {
                if (info.name != action.infoKey ||
                    info.type !in setOf(InformationType.ACCIDENT, InformationType.CASUALTY)
                ) 0.0 else if (parent.activeCharacters.keys.all { it in info.knownTo }) -1.0 else 1.0
            }

            else -> 0.0
        }
    }

    private fun transferEvidenceSignal(
        info: Information,
        sourcePlace: String?,
        destinationPlace: String,
        resources: Resources,
        sourceCharacter: String? = null
    ): Double {
        if (info.type != InformationType.RESOURCES) return 0.0
        val isSource = (sourcePlace != null && info.tgtPlace == sourcePlace) ||
                (sourceCharacter != null && info.tgtCharacter == sourceCharacter)
        val isDestination = info.tgtPlace == destinationPlace
        if (!isSource && !isDestination) return 0.0

        val signals = resources.keys.mapNotNull { resource ->
            val requested = resources[resource]
            if (requested <= 0.0) return@mapNotNull null
            val available = info.resources[resource]
            val signal = if (isSource) {
                (available - requested) / requested
            } else {
                (requested - available) / requested
            }
            signal.coerceIn(-1.0, 1.0)
        }
        return if (signals.isEmpty()) 0.0 else signals.average()
    }

    private fun repairEvidenceSignal(parent: GameState, action: Repair, info: Information): Double {
        if (info.tgtPlace != action.tgtPlace) return 0.0
        if (info.type == InformationType.APPARATUS && info.tgtApparatusID == action.apparatusID) {
            val durability = info.variables["durability"] ?: return 0.0
            return ((70.0 - durability) / 30.0).coerceIn(-1.0, 1.0)
        }
        if (info.type == InformationType.RESOURCES) {
            val apparatus = parent.places[action.tgtPlace]?.apparatuses
                ?.find { it.ID == action.apparatusID } ?: return 0.0
            val repairLevel = Repair.checkRepairLevel(apparatus).first
            val required = apparatus.requiredResourcePerRepair[repairLevel]
            return if (info.resources.contains(required)) 1.0 else -1.0
        }
        return 0.0
    }

    private fun salaryEvidenceSignal(
        parent: GameState,
        meeting: Meeting,
        action: Salary,
        info: Information
    ): Double {
        if (info.type != InformationType.RESOURCES || info.tgtPlace != action.tgtPlace) return 0.0
        val party = meeting.involvedParty?.let { parent.parties[it] } ?: return 0.0
        if (party.type !in setOf(Party.Type.CABINET, Party.Type.DIVISION, Party.Type.WORKPLACE)) return 0.0
        val required = Salary.standardQuarterlyRate(party.type) * (party.members - action.sbjCharacter).size
        return if (info.resources.contains(required)) 1.0 else -1.0
    }

    private fun accidentRequestSignal(parent: GameState, tgtPlace: String, info: Information): Double {
        if (info.tgtPlace != tgtPlace ||
            info.type !in setOf(InformationType.ACCIDENT, InformationType.CASUALTY)
        ) return 0.0
        return if (parent.places[tgtPlace]?.isAccidentScene == true) 1.0 else -1.0
    }

    private fun characterPerformanceEffectivity(
        parent: GameState,
        meeting: Meeting,
        info: Information,
        speaker: Character,
        subject: String?,
        agendaType: AgendaType
    ): Pair<Double, String> {
        val character = subject?.let { parent.characters[it] } ?: return Pair(0.0, "")
        if (agendaType == AgendaType.NOMINATE) {
            val party = meeting.involvedParty?.let { parent.parties[it] } ?: return Pair(0.0, "")
            if (character.name !in party.members) return Pair(0.0, "")
            if (info.type == InformationType.MUTUALITY && info.auxCharacter == character.name) {
                val scale = ReadOnly.const("mutualityMax") - ReadOnly.const("mutualityMin")
                if (scale <= 0.0) return Pair(0.0, "")
                val relation = ((2.0 * info.amount - ReadOnly.const("mutualityMax") -
                        ReadOnly.const("mutualityMin")) / scale).coerceIn(-1.0, 1.0)
                return Pair(5.0 * relation * speaker.stats.eScale, "Nominate")
            }
        }

        val performance = characterPerformanceSignal(parent, info, character.name)
        if (performance == 0.0) return Pair(0.0, "")
        val (signal, statScale) = when (agendaType) {
            AgendaType.NOMINATE, AgendaType.PRAISE ->
                performance to if (performance > 0) speaker.stats.eScale else speaker.stats.pScale
            AgendaType.DENOUNCE, AgendaType.FIRE_MANAGER ->
                -performance to if (performance < 0) speaker.stats.pScale else speaker.stats.eScale
            else -> return Pair(0.0, "")
        }
        val reason = when (agendaType) {
            AgendaType.NOMINATE -> "Nominate"
            AgendaType.PRAISE -> "Praise"
            AgendaType.DENOUNCE -> "Denounce"
            else -> "FireManager"
        }
        val base = if (agendaType == AgendaType.FIRE_MANAGER) 10.0 else 5.0
        return Pair(base * signal * statScale, reason)
    }

    private fun characterPerformanceSignal(parent: GameState, info: Information, character: String): Double {
        if (info.type == InformationType.ACTION) {
            val action = info.action ?: return 0.0
            if (action.sbjCharacter == character || info.author == character) {
                when (action) {
                    is UnofficialResourceTransfer -> if (!action.fromHome) return -1.0
                    is Repair, is InvestigateAccidentScene, is ClearAccidentScene, is Salary ->
                        return 0.75
                    is OfficialResourceTransfer -> return 0.5
                    else -> Unit
                }
            }
        }

        val place = parent.places[info.tgtPlace] ?: return 0.0
        val managesPlace = place.manager == character ||
                place.responsibleDivision?.let { parent.parties[it]?.leader == character } == true
        if (!managesPlace) return 0.0
        return placeOutcomeSignal(place, info)
    }

    private fun partyPerformanceEffectivity(
        parent: GameState,
        info: Information,
        speaker: Character,
        partyName: String?,
        denounce: Boolean
    ): Pair<Double, String> {
        val party = partyName?.let { parent.parties[it] } ?: return Pair(0.0, "")
        val signal = if (info.type == InformationType.PARTY_MUTUALITY && info.auxParty == party.name) {
            val scale = ReadOnly.const("mutualityMax") - ReadOnly.const("mutualityMin")
            if (scale <= 0.0) 0.0 else ((2.0 * info.amount - ReadOnly.const("mutualityMax") -
                    ReadOnly.const("mutualityMin")) / scale).coerceIn(-1.0, 1.0)
        } else if (info.type == InformationType.ACTION &&
            info.action?.sbjCharacter?.let { it in party.members } == true
        ) {
            info.action?.let { actionPerformanceSignal(it) } ?: 0.0
        } else {
            val place = parent.places[info.tgtPlace] ?: return Pair(0.0, "")
            if (!placeInPartyDomain(parent, place, party)) return Pair(0.0, "")
            placeOutcomeSignal(place, info)
        }
        if (signal == 0.0) return Pair(0.0, "")
        val signedSignal = if (denounce) -signal else signal
        val statScale = if (signedSignal > 0) {
            if (denounce) speaker.stats.pScale else speaker.stats.eScale
        } else {
            if (denounce) speaker.stats.eScale else speaker.stats.pScale
        }
        val reason = if (denounce) "DenounceParty" else "PraiseParty"
        return Pair(5.0 * signedSignal * statScale, reason)
    }

    private fun actionPerformanceSignal(action: GameAction): Double = when (action) {
        is UnofficialResourceTransfer -> if (action.fromHome) 0.0 else -1.0
        is Repair, is InvestigateAccidentScene, is ClearAccidentScene, is Salary -> 0.75
        is OfficialResourceTransfer -> 0.5
        else -> 0.0
    }

    private fun placeInPartyDomain(parent: GameState, place: Place, party: Party): Boolean = when (party.type) {
        Party.Type.WORKPLACE -> place.workplaceParty == party
        Party.Type.DIVISION -> place.responsibleDivision == party.name
        Party.Type.CABINET, Party.Type.TRIUMVIRATE -> place.responsibleDivision?.let { divisionName ->
            parent.parties[divisionName]?.members?.any { it in party.members } == true
        } == true
        else -> false
    }

    private fun placeOutcomeSignal(place: Place, info: Information): Double {
        return when (info.type) {
            InformationType.CASUALTY -> {
                val casualties = info.amount.toDouble()
                -(casualties / (casualties + 3.0)).coerceIn(0.0, 1.0)
            }
            InformationType.ACCIDENT -> -0.75
            InformationType.SOUND -> -0.25
            InformationType.APPARATUS -> {
                val durability = info.variables["durability"] ?: return 0.0
                ((durability - 70.0) / 30.0).coerceIn(-1.0, 1.0)
            }
            InformationType.HUMAN_RESOURCES -> {
                val idealWorkers = place.apparatuses.sumOf { it.idealWorker }
                if (idealWorkers == 0) 0.0 else {
                    val ratio = info.amount.toDouble() / idealWorkers
                    if (ratio <= 1.0) (2.0 * ratio - 1.0).coerceIn(-1.0, 1.0)
                    else (3.0 - 2.0 * ratio).coerceIn(-1.0, 1.0)
                }
            }
            InformationType.RESOURCES -> {
                val expected = Resources()
                place.apparatuses.forEach { apparatus ->
                    expected += apparatus.hourlyOperationBudget * place.workHoursLength * 3
                }
                val coverage = expected.keys.mapNotNull { resource ->
                    val required = expected[resource]
                    if (required <= 0.0) null else
                        (info.resources[resource] / required - 1.0).coerceIn(-1.0, 1.0)
                }
                if (coverage.isEmpty()) 0.0 else coverage.average()
            }
            else -> 0.0
        }
    }

    private fun budgetProposalEffectivity(
        parent: GameState,
        meeting: Meeting,
        info: Information,
        speaker: Character
    ): Pair<Double, String> {
        if (info.type != InformationType.RESOURCES && info.type != InformationType.HUMAN_RESOURCES)
            return Pair(0.0, "")

        val place = parent.places[info.tgtPlace] ?: return Pair(0.0, "")
        val involvedParty = meeting.involvedParty ?: return Pair(0.0, "")
        val budgetPartyName =
            if (involvedParty == "cabinet" || involvedParty == "triumvirate") "cabinet" else involvedParty
        val budgetParty = parent.parties[budgetPartyName] ?: return Pair(0.0, "")
        val proposedBudget = attachedBudget ?: return Pair(0.0, "")
        val baselineBudget = budgetParty.budget
        val standardBudget = budgetParty.standardBudget

        val evidenceScore = when (info.type) {
            InformationType.RESOURCES -> {
                val expectedConsumption = Resources()
                place.apparatuses.forEach { apparatus ->
                    expectedConsumption += apparatus.hourlyOperationBudget *
                            place.workHoursLength * ReadOnly.constInt("quarterInDays")
                }

                val improvements = expectedConsumption.keys.mapNotNull { resource ->
                    val expected = expectedConsumption[resource]
                    if (expected <= 0.0) return@mapNotNull null

                    val proposed = budgetAllocationForPlace(
                        parent, proposedBudget, budgetParty, place, resource
                    ) ?: return@mapNotNull null
                    val current = budgetAllocationForPlace(
                        parent, baselineBudget, budgetParty, place, resource
                    ) ?: budgetAllocationForPlace(
                        parent, standardBudget, budgetParty, place, resource
                    ) ?: return@mapNotNull null

                    val requiredBudget = max(expected - info.resources[resource], 0.0)
                    val currentGap = abs(current - requiredBudget)
                    val proposedGap = abs(proposed - requiredBudget)
                    (currentGap - proposedGap) / max(1.0, max(currentGap, proposedGap))
                }
                if (improvements.isEmpty()) 0.0 else 10.0 * improvements.average()
            }

            InformationType.HUMAN_RESOURCES -> {
                val idealWorkers = place.apparatuses.sumOf { it.idealWorker }
                if (idealWorkers == 0) return Pair(0.0, "")

                val currentLaborBudget = laborBudgetForPlace(
                    parent, baselineBudget, budgetParty, place
                ) ?: laborBudgetForPlace(parent, standardBudget, budgetParty, place)
                    ?: return Pair(0.0, "")
                val proposedLaborBudget = laborBudgetForPlace(
                    parent, proposedBudget, budgetParty, place
                ) ?: return Pair(0.0, "")
                val staffingGap = ((idealWorkers - info.amount).toDouble() / idealWorkers).coerceIn(-1.0, 1.0)
                val budgetScale = max(
                    1.0,
                    max(abs(currentLaborBudget), abs(proposedLaborBudget))
                )
                val budgetChange = ((proposedLaborBudget - currentLaborBudget) / budgetScale).coerceIn(-1.0, 1.0)
                10.0 * staffingGap * budgetChange
            }

            else -> 0.0
        }

        if (evidenceScore == 0.0) return Pair(0.0, "")
        return Pair(evidenceScore * speaker.stats.lScale, "BudgetProposal")
    }

    private fun laborBudgetForPlace(
        parent: GameState,
        budget: Budget,
        budgetParty: Party,
        place: Place
    ): Double? {
        val water = budgetAllocationForPlace(parent, budget, budgetParty, place, "water") ?: return null
        val ration = budgetAllocationForPlace(parent, budget, budgetParty, place, "ration") ?: return null
        return water + ration
    }

    private fun budgetAllocationForPlace(
        parent: GameState,
        budget: Budget,
        budgetParty: Party,
        place: Place,
        resource: String
    ): Double? {
        val workplaceParty = place.workplaceParty ?: return null
        budget.value[workplaceParty.name]?.let { return it[resource] }

        if (budgetParty.type != Party.Type.CABINET) return null
        val divisionName = place.responsibleDivision ?: return null
        val divisionAllocation = budget.value[divisionName]?.get(resource) ?: return null
        val division = parent.parties[divisionName] ?: return null
        val divisionStandard = division.standardBudget.sum(resource)
        val workplaceStandard = workplaceParty.standardBudget.sum(resource)
        if (divisionStandard <= 0.0 || workplaceStandard <= 0.0) return null

        // Cabinet budgets are grouped by division, so estimate this workplace's share by its standard allocation.
        return divisionAllocation * workplaceStandard / divisionStandard
    }
}

@Serializable
enum class AgendaType {
    PROOF_OF_WORK, NOMINATE, REQUEST, PROMISE, PRAISE, DENOUNCE, PRAISE_PARTY, DENOUNCE_PARTY, BUDGET_PROPOSAL, APPOINT_MEETING, FIRE_MANAGER
}