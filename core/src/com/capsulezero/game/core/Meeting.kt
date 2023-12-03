package com.capsulezero.game.core

import kotlinx.serialization.Serializable

@Serializable
class Meeting(var time: Int, var subject: String, var scheduledCharacters: HashSet<String>, var place: String, var currentCharacters: HashSet<String> = hashSetOf()) {
    var involvedParty : String = ""
    var auxSubject = ""
}