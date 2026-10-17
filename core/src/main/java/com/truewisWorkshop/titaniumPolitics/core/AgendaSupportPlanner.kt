package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.gameActions.Examine
import com.titaniumPolitics.game.core.gameActions.Salary
import kotlin.math.abs

data class PredictedAgendaSupport(
    val meeting: Meeting,
    val agenda: MeetingAgenda,
    val support: Boolean
)

data class AgendaEvidenceNeed(
    val targetPlace: String,
    val informationType: InformationType
)

object AgendaSupportPlanner {
    private const val RELATIONSHIP_THRESHOLD = 0.1
    private const val FRESH_INFO_THRESHOLD = 0.75

    fun predictFor(state: GameState, characterName: String): List<PredictedAgendaSupport> {
        val horizon = 2 * ReadOnly.constInt("lengthOfDay")
        val predictions = arrayListOf<PredictedAgendaSupport>()

        state.scheduledMeetings.values
            .filter {
                characterName in it.scheduledCharacters &&
                    it.time >= state.time &&
                    it.time - state.time <= horizon &&
                    it.type == Meeting.MeetingType.DIVISION_DAILY_CONFERENCE
            }
            .sortedBy { it.time }
            .forEach { scheduledMeeting ->
                val party = scheduledMeeting.involvedParty?.let { state.parties[it] } ?: return@forEach
                val leader = party.leader ?: return@forEach
                if (characterName !in party.members ||
                    characterName == leader ||
                    leader !in scheduledMeeting.scheduledCharacters
                ) return@forEach

                val relationship = state.getMutNorm(characterName, leader)
                val favorsLeader = when {
                    relationship > RELATIONSHIP_THRESHOLD -> true
                    relationship < -RELATIONSHIP_THRESHOLD -> false
                    else -> null
                }
                val meeting = scoringMeeting(scheduledMeeting)

                if (party.type in setOf(Party.Type.WORKPLACE, Party.Type.DIVISION)) {
                    PolicyCatalog.bestProposal(party, leader, scheduledMeeting.scheduledCharacters)
                        ?.let { policy ->
                            val stance = PolicyCatalog.preferenceScore(party, policy, characterName, leader)
                            if (abs(stance) >= PolicyCatalog.POLICY_STANCE_THRESHOLD) {
                                predictions += PredictedAgendaSupport(
                                    meeting,
                                    MeetingAgenda(
                                        AgendaType.POLICY_CHANGE,
                                        leader,
                                        subjectParams = hashMapOf("policy" to policy)
                                    ),
                                    support = stance > 0.0
                                )
                            }
                        }
                }

                if (party.home != null && !party.isSalaryPaid) {
                    scheduledMeeting.scheduledCharacters
                        .filter { it != leader && it in party.members }
                        .forEach { requester ->
                            predictions += PredictedAgendaSupport(
                                meeting,
                                MeetingAgenda(
                                    AgendaType.REQUEST,
                                    requester,
                                    attachedRequest = Request(
                                        Salary(leader, party.home!!),
                                        issuedTo = hashSetOf(leader),
                                        issuedBy = hashSetOf(requester),
                                        executeTime = scheduledMeeting.time
                                    )
                                ),
                                support = true
                            )
                        }
                }

                if (favorsLeader != null) {
                    val places = when (party.type) {
                        Party.Type.DIVISION -> party.divisionPlaces.toList()
                        Party.Type.WORKPLACE -> listOf(party.workplace)
                        else -> emptyList()
                    }
                    places.forEach { place ->
                        listOf(
                            InformationType.RESOURCES,
                            InformationType.HUMAN_RESOURCES,
                            InformationType.APPARATUS
                        ).forEach { type ->
                            if (state.informations.values.none {
                                    it.tgtPlace == place.name &&
                                        it.type == type &&
                                        leader in it.knownTo
                                }
                            ) {
                                val recipients = scheduledMeeting.scheduledCharacters
                                    .filter { it != leader }
                                    .toHashSet()
                                if (recipients.isNotEmpty()) {
                                    predictions += PredictedAgendaSupport(
                                        meeting,
                                        MeetingAgenda(
                                            AgendaType.REQUEST,
                                            leader,
                                            attachedRequest = Request(
                                                Examine(leader, place.name, type),
                                                issuedTo = recipients,
                                                issuedBy = hashSetOf(leader),
                                                executeTime = scheduledMeeting.time
                                            )
                                        ),
                                        support = favorsLeader
                                    )
                                }
                            }
                        }
                    }
                }
            }

        return predictions
    }

    fun recommendInformationKeys(state: GameState, characterName: String): List<String> {
        val character = state.characters[characterName] ?: return emptyList()
        val recommended = linkedSetOf<String>()
        predictFor(state, characterName).forEach { prediction ->
            val candidates = state.informations.filter { (_, info) ->
                characterName in info.knownTo
            }.mapNotNull { (key, info) ->
                val effectivity = prediction.agenda.effectivity(
                    state,
                    prediction.meeting,
                    info,
                    character
                ).first
                if (if (prediction.support) effectivity > 0.0 else effectivity < 0.0) {
                    key to effectivity
                } else {
                    null
                }
            }
            val best = if (prediction.support) {
                candidates.maxByOrNull { it.second }
            } else {
                candidates.minByOrNull { it.second }
            }
            best?.first?.let(recommended::add)
        }
        return recommended.toList()
    }

    fun nextInformationNeed(state: GameState, characterName: String): AgendaEvidenceNeed? {
        val character = state.characters[characterName] ?: return null
        val lifetime = ReadOnly.const("InfoLifetime")

        predictFor(state, characterName).forEach { prediction ->
            val requestAction = prediction.agenda.attachedRequest?.action
            val needs = when (requestAction) {
                is Examine -> {
                    if (prediction.support) emptyList()
                    else listOf(AgendaEvidenceNeed(requestAction.tgtPlace, requestAction.what))
                }

                is Salary -> listOf(
                    AgendaEvidenceNeed(requestAction.tgtPlace, InformationType.RESOURCES)
                )

                else -> if (prediction.agenda.type == AgendaType.POLICY_CHANGE) {
                    val party = prediction.meeting.involvedParty?.let { state.parties[it] }
                        ?: return@forEach
                    policyEvidenceNeeds(
                        party,
                        prediction.agenda.subjectParams["policy"],
                        characterName
                    )
                } else {
                    emptyList()
                }
            }

            needs.forEach { need ->
                val hasFreshInformation = state.informations.values.any { info ->
                    characterName in info.knownTo &&
                        info.type == need.informationType &&
                        info.tgtPlace == need.targetPlace &&
                        (lifetime <= 0.0 || info.life / lifetime >= FRESH_INFO_THRESHOLD)
                }
                if (hasFreshInformation) return@forEach

                val action = Examine(characterName, need.targetPlace, need.informationType).apply {
                    injectParent(state)
                }
                if (action.isValid() &&
                    character.place.shortestPathAndTimeTo(need.targetPlace, characterName) != null
                ) return need
            }
        }
        return null
    }

    private fun policyEvidenceNeeds(
        party: Party,
        policy: String?,
        characterName: String
    ): List<AgendaEvidenceNeed> {
        val types = when (policy) {
            "workhourLimit" -> listOf(
                InformationType.HUMAN_RESOURCES,
                InformationType.APPARATUS
            )

            "lockoutExperiments" -> listOf(InformationType.APPARATUS)
            else -> emptyList()
        }
        if (types.isEmpty()) return emptyList()

        val places = when (party.type) {
            Party.Type.WORKPLACE -> listOf(party.workplace)
            Party.Type.DIVISION -> party.divisionPlaces.toList()
            else -> emptyList()
        }
        return places.flatMap { place ->
            types.map { AgendaEvidenceNeed(place.name, it) }
        }.filter { need ->
            val action = Examine(characterName, need.targetPlace, need.informationType).apply {
                injectParent(party.parent)
            }
            action.isValid()
        }
    }

    private fun scoringMeeting(source: Meeting): Meeting =
        Meeting(
            time = source.time,
            type = source.type,
            scheduledCharacters = source.scheduledCharacters.toHashSet(),
            place = source.place,
            currentCharacters = source.scheduledCharacters.toHashSet()
        ).apply {
            involvedParty = source.involvedParty
        }
}
