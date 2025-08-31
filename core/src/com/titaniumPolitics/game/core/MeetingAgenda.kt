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
    var attachedBudget: Budget? = null


) {
    //Compute the effectivity of an information for this agenda for a character in the meeting.
    fun effectivity(parent: GameState, meeting: Meeting, info: Information, sbjCharObj: Character): Double {
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
                ) return 5.0 * sbjCharObj.stats.lScale


                //If there are any interesting (to this character) news about the division, share it.
                if (info.tgtTime in parent.day * ReadOnly.constInt("lengthOfDay")..<parent.day * ReadOnly.constInt(
                        "lengthOfDay"
                    ) + ReadOnly.constInt("lengthOfDay")
                )
                    return newsPeople.sumOf { parent.characters[it]!!.infoPreference(info) } / newsPeople.size * sbjCharObj.stats.eScale//Share the most interesting news.

            }

            AgendaType.NOMINATE, AgendaType.PRAISE -> {
                return parent.characters[subjectParams["character"]]!!.infoPreference(info) * sbjCharObj.stats.eScale
            }

            AgendaType.REQUEST -> return if (info.tgtPlace == attachedRequest!!.action.tgtPlace) 10.0 else 0.0

            AgendaType.DENOUNCE -> {
                return -parent.characters[subjectParams["character"]]!!.infoPreference(info) * sbjCharObj.stats.pScale
            }

            AgendaType.PRAISE_PARTY -> {
                val pt = parent.parties[subjectParams["party"]]!!
                return pt.members.sumOf { parent.characters[it]!!.infoPreference(info) } / pt.members.size * sbjCharObj.stats.eScale

            }

            AgendaType.DENOUNCE_PARTY -> {
                val pt = parent.parties[subjectParams["party"]]!!
                return pt.members.sumOf { -parent.characters[it]!!.infoPreference(info) } / pt.members.size * sbjCharObj.stats.pScale
            }

            AgendaType.BUDGET_PROPOSAL -> TODO()
            AgendaType.BUDGET_RESOLUTION -> TODO()
            AgendaType.APPOINT_MEETING -> return 0.0
            AgendaType.FIRE_MANAGER -> return -parent.characters[subjectParams["character"]]!!.infoPreference(
                info
            ) * sbjCharObj.stats.pScale

            else -> return 0.0
        }
        return 0.0
    }
}

@Serializable
enum class AgendaType {
    PROOF_OF_WORK, NOMINATE, REQUEST, PRAISE, DENOUNCE, PRAISE_PARTY, DENOUNCE_PARTY, BUDGET_PROPOSAL, BUDGET_RESOLUTION, APPOINT_MEETING, FIRE_MANAGER
}