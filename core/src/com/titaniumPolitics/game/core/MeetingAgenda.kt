package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.gameActions.GameAction
import kotlinx.serialization.Serializable

@Serializable
data class MeetingAgenda(
    var subjectType: String,
    var subjectParams: HashMap<String, String> = hashMapOf(),
    var subjectIntParams: HashMap<String, Int> = hashMapOf(),
    var informationKeys: ArrayList<String> = arrayListOf(),
    var attachedRequest: Request? = null
)
