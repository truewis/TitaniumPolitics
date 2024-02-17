package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable

@Serializable
data class MeetingAgenda(var type: String, var subject: String, var agreement: Int, var informationKeys: List<String>)
