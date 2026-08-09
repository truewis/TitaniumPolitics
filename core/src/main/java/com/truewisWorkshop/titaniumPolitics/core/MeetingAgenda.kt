package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable

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
        val newsPeople = meeting.currentCharacters.intersect(info.knownTo)
        when (type) {
            AgendaType.PROOF_OF_WORK -> {
                //if there is any supporting information, add it.

                if (sbjCharObj.executedRequests.any {
                        parent.requests[it]!!.action.isProofOfWork(info) &&
                                parent.requests[it]!!.issuedBy.any {
                                    meeting.currentCharacters.contains(
                                        it
                                    )
                                }
                    }
                )
                    return Pair(5.0 * sbjCharObj.stats.lScale, "ProofOfWork")
            }

            AgendaType.NOMINATE, AgendaType.PRAISE -> {
                return Pair(
                    parent.characters[subjectParams["character"]]!!.infoPreference(info) * sbjCharObj.stats.eScale,
                    "Praise"
                )
            }

            AgendaType.REQUEST -> return if (info.tgtPlace == attachedRequest!!.action.tgtPlace) Pair(
                10.0,
                "Request"
            ) else Pair(0.0, "")

            AgendaType.DENOUNCE -> {
                return Pair(
                    -parent.characters[subjectParams["character"]]!!.infoPreference(info) * sbjCharObj.stats.pScale,
                    "Denounce"
                )
            }

            AgendaType.PRAISE_PARTY -> {
                val pt = parent.parties[subjectParams["party"]]!!
                return Pair(pt.members.sumOf { parent.characters[it]!!.infoPreference(info) } / pt.members.size * sbjCharObj.stats.eScale,
                    "PraiseParty")

            }

            AgendaType.DENOUNCE_PARTY -> {
                val pt = parent.parties[subjectParams["party"]]!!
                return Pair(pt.members.sumOf { -parent.characters[it]!!.infoPreference(info) } / pt.members.size * sbjCharObj.stats.pScale,
                    "DenounceParty")
            }

            AgendaType.BUDGET_PROPOSAL -> return Pair(0.0, "")
            AgendaType.BUDGET_RESOLUTION -> return Pair(0.0, "")
            AgendaType.APPOINT_MEETING -> return Pair(0.0, "")
            AgendaType.FIRE_MANAGER -> return Pair(
                -parent.characters[subjectParams["character"]]!!.infoPreference(
                    info
                ) * sbjCharObj.stats.pScale, "FireManager"
            )

            AgendaType.PROMISE -> return if (info.tgtPlace == attachedRequest!!.action.tgtPlace) Pair(
                10.0,
                "Promise"
            ) else Pair(0.0, "")

            else -> return Pair(0.0, "")
        }
        return Pair(0.0, "")
    }
}

@Serializable
enum class AgendaType {
    PROOF_OF_WORK, NOMINATE, REQUEST, PROMISE, PRAISE, DENOUNCE, PRAISE_PARTY, DENOUNCE_PARTY, BUDGET_PROPOSAL, BUDGET_RESOLUTION, APPOINT_MEETING, FIRE_MANAGER
}