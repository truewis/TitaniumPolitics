package com.capsulezero.game.core

import kotlinx.serialization.Serializable

@Serializable
class Character {
    var name = ""
    var alive = true
    var trait = hashSetOf<String>()
    var mutuality = hashMapOf<String, Int>()
    var resources = hashMapOf<String, Int>()
    var health = 0
        set(value) {
            field = if(value<100) value else 100//Max health is 100.
        }
    var will = 0
    var scheduledMeetings = hashSetOf<Meeting>()
    var home = ""
    var frozen = 0
    var approval = 0
    var division = "" //Determines which division the character is responsible for.

}