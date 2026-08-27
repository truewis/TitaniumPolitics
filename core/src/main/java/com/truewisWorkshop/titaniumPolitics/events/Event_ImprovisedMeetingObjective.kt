package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
class Event_ImprovisedMeetingObjective(val meetingId: String) : EventObject("Meeting Objective", true), IQuestEventObject {
    private var targetCharacter: String? = null
    private var targetParty: String? = null
    private var objectiveKind: String = "generic"
    private var startMutuality: Double = 0.0

    override val quest: Quest by lazy {
        val meeting = parent.ongoingMeetings[meetingId]
        val objective = meeting?.let { resolveObjective(it) } ?: defaultObjective()
        targetCharacter = objective.targetCharacter
        targetParty = objective.targetParty
        objectiveKind = objective.kind
        startMutuality = objective.startMutuality
        Quest(
            name = ReadOnly.questProp(objective.titleKey).format(*objective.titleArgs.toTypedArray()),
            description = ReadOnly.questProp(objective.descriptionKey).format(*objective.descriptionArgs.toTypedArray()),
            meetingId = meetingId,
            isImprovised = true,
            tgtMeeting = meetingId,
            tgtPlace = meeting?.place
        )
    }

    override fun injectParent(gameState: com.titaniumPolitics.game.core.GameState) {
        super.injectParent(gameState)
        val meeting = gameState.ongoingMeetings[meetingId]
        if (meeting != null) {
            val objective = resolveObjective(meeting)
            targetCharacter = objective.targetCharacter
            targetParty = objective.targetParty
            objectiveKind = objective.kind
            startMutuality = objective.startMutuality
        }
    }

    override fun exec(a: Int, b: Int) {
        val meeting = parent.ongoingMeetings[meetingId] ?: run {
            deactivate(false)
            return
        }
        if (parent.playerName !in meeting.currentCharacters) {
            deactivate(false)
            return
        }
        if (isObjectiveComplete(meeting)) {
            deactivate(true)
        }
    }

    override fun execInMeeting(meeting: Meeting) {
        if (parent.playerName !in meeting.currentCharacters) {
            deactivate(false)
            return
        }
        if (meeting.ID != meetingId) return
        if (isObjectiveComplete(meeting)) {
            deactivate(true)
        }
    }

    private fun isObjectiveComplete(meeting: Meeting): Boolean {
        return when (objectiveKind) {
            "praise-character" -> meeting.agendas.any {
                it.type == AgendaType.PRAISE && it.subjectParams["character"] == targetCharacter
            }

            "denounce-character" -> meeting.agendas.any {
                it.type == AgendaType.DENOUNCE && it.subjectParams["character"] == targetCharacter
            }

            "praise-party" -> meeting.agendas.any {
                it.type == AgendaType.PRAISE_PARTY && it.subjectParams["party"] == targetParty
            }

            "denounce-party" -> meeting.agendas.any {
                it.type == AgendaType.DENOUNCE_PARTY && it.subjectParams["party"] == targetParty
            }

            "request-resource" -> meeting.agendas.any {
                it.type == AgendaType.REQUEST && it.attachedRequest?.action?.tgtPlace == targetCharacter
            }

            "fire-manager" -> meeting.agendas.any {
                it.type == AgendaType.FIRE_MANAGER && it.subjectParams["character"] == targetCharacter
            }

            "mutuality" -> targetCharacter != null && parent.getMutNorm(parent.playerName, targetCharacter!!) > startMutuality + 0.2
            else -> false
        }
    }

    private data class ObjectiveSpec(
        val titleKey: String,
        val descriptionKey: String,
        val titleArgs: List<Any> = listOf(),
        val descriptionArgs: List<Any> = listOf(),
        val kind: String,
        val targetCharacter: String? = null,
        val targetParty: String? = null,
        val startMutuality: Double = 0.0
    )

    private fun resolveObjective(meeting: Meeting): ObjectiveSpec {
        val player = parent.playerName
        val candidates = mutableListOf<ObjectiveSpec>()

        val liked = meeting.currentCharacters.filter { it != player }
            .filter { parent.getMutNorm(player, it) > 0.25 }
        val hated = meeting.currentCharacters.filter { it != player }
            .filter { parent.getMutNorm(player, it) < -0.25 }

        liked.firstOrNull()?.let { char ->
            candidates += ObjectiveSpec(
                titleKey = "MeetingObjective-praiseCharacter-title",
                descriptionKey = "MeetingObjective-praiseCharacter-desc",
                titleArgs = listOf(ReadOnly.charProp(char)),
                descriptionArgs = listOf(ReadOnly.charProp(char)),
                kind = "praise-character",
                targetCharacter = char,
                startMutuality = parent.getMutNorm(player, char)
            )
        }
        hated.firstOrNull()?.let { char ->
            candidates += ObjectiveSpec(
                titleKey = "MeetingObjective-denounceCharacter-title",
                descriptionKey = "MeetingObjective-denounceCharacter-desc",
                titleArgs = listOf(ReadOnly.charProp(char)),
                descriptionArgs = listOf(ReadOnly.charProp(char)),
                kind = "denounce-character",
                targetCharacter = char,
                startMutuality = parent.getMutNorm(player, char)
            )
        }

        meeting.involvedParty?.let { party ->
            val partyName = parent.parties[party]?.name ?: party
            val partyMut = parent.getPartyMutNorm(party, partyName)
            if (partyMut > 0.25) {
                candidates += ObjectiveSpec(
                    titleKey = "MeetingObjective-praiseParty-title",
                    descriptionKey = "MeetingObjective-praiseParty-desc",
                    titleArgs = listOf(ReadOnly.prop(partyName)),
                    descriptionArgs = listOf(ReadOnly.prop(partyName)),
                    kind = "praise-party",
                    targetParty = partyName,
                    startMutuality = partyMut
                )
            } else if (partyMut < -0.25) {
                candidates += ObjectiveSpec(
                    titleKey = "MeetingObjective-denounceParty-title",
                    descriptionKey = "MeetingObjective-denounceParty-desc",
                    titleArgs = listOf(ReadOnly.prop(partyName)),
                    descriptionArgs = listOf(ReadOnly.prop(partyName)),
                    kind = "denounce-party",
                    targetParty = partyName,
                    startMutuality = partyMut
                )
            }
        }

        if (parent.player.thirst < 35.0 || parent.player.hunger < 35.0) {
            val beneficiary = meeting.currentCharacters.filter { it != player }
                .firstOrNull { parent.getMutNorm(player, it) > 0.25 }
            if (beneficiary != null) {
                candidates += ObjectiveSpec(
                    titleKey = "MeetingObjective-requestResource-title",
                    descriptionKey = "MeetingObjective-requestResource-desc",
                    titleArgs = listOf(ReadOnly.charProp(beneficiary)),
                    descriptionArgs = listOf(ReadOnly.charProp(beneficiary)),
                    kind = "request-resource",
                    targetCharacter = beneficiary,
                    startMutuality = parent.getMutNorm(player, beneficiary)
                )
            }
        }

        val directReport = parent.player.division?.members?.filter { it != player }
            ?.firstOrNull { parent.getMutNorm(player, it) < -0.25 }
        if (directReport != null) {
            candidates += ObjectiveSpec(
                titleKey = "MeetingObjective-fireManager-title",
                descriptionKey = "MeetingObjective-fireManager-desc",
                titleArgs = listOf(ReadOnly.charProp(directReport)),
                descriptionArgs = listOf(ReadOnly.charProp(directReport)),
                kind = "fire-manager",
                targetCharacter = directReport,
                startMutuality = parent.getMutNorm(player, directReport)
            )
        }

        val warmContact = meeting.currentCharacters.filter { it != player }
            .firstOrNull { parent.getMutNorm(player, it) > 0.25 && parent.getMutNorm(it, player) < 0.25 }
        if (warmContact != null) {
            candidates += ObjectiveSpec(
                titleKey = "MeetingObjective-mutuality-title",
                descriptionKey = "MeetingObjective-mutuality-desc",
                titleArgs = listOf(ReadOnly.charProp(warmContact)),
                descriptionArgs = listOf(ReadOnly.charProp(warmContact)),
                kind = "mutuality",
                targetCharacter = warmContact,
                startMutuality = parent.getMutNorm(player, warmContact)
            )
        }

        return candidates.randomOrNull() ?: defaultObjective()
    }

    private fun defaultObjective(): ObjectiveSpec = ObjectiveSpec(
        titleKey = "MeetingObjective-generic-title",
        descriptionKey = "MeetingObjective-generic-desc",
        kind = "generic"
    )

    init {
        if (Random.nextDouble() >= 0.5) {
            objectiveKind = "generic"
        }
    }
}
