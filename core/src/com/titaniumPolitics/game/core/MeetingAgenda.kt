package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable

@Serializable
data class MeetingAgenda(
    var subjectType: String,
    var subjectParams: HashMap<String, String>,
    var subjectIntParams: HashMap<String, Int>,
    var agreement: Int,
    var informationKeys: ArrayList<String>
)
