package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable

@Serializable
data class MeetingAgenda(
    var subjectType: String,
    var subjectParams: HashMap<String, String> = hashMapOf(),
    var subjectIntParams: HashMap<String, Int> = hashMapOf(),
    var agreement: Int = 0,
    var informationKeys: ArrayList<String> = arrayListOf()
)
