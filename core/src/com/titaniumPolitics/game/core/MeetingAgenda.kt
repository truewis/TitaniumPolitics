package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable

@Serializable
data class MeetingAgenda(
    var type: AgendaType,
    var author: String,
    var subjectParams: HashMap<String, String> = hashMapOf(),
    var subjectIntParams: HashMap<String, Int> = hashMapOf(),
    var informationKeys: ArrayList<String> = arrayListOf(),
    var attachedRequest: Request? = null


)

@Serializable
enum class AgendaType
{
    PROOF_OF_WORK, NOMINATE, REQUEST, PRAISE, DENOUNCE, PRAISE_PARTY, DENOUNCE_PARTY, BUDGET_PROPOSAL, BUDGET_RESOLUTION, APPOINT_MEETING, FIRE_MANAGER
}