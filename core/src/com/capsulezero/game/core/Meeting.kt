package com.capsulezero.game.core

import kotlinx.serialization.Serializable

@Serializable
class Meeting(var time: Int, var subject: String, var characters: HashSet<String>, var place: String, var currentCharacters: HashSet<String> = hashSetOf()) {
}