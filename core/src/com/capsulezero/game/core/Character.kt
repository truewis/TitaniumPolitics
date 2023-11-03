package com.capsulezero.game.core

import kotlinx.serialization.Serializable

@Serializable
class Character: GameStateElement() {
    var name = ""
    var alive = true
    var trait = hashSetOf<String>()
    var resources = hashMapOf<String, Int>()
    var health = 0
        set(value) {
            field = if(value<100) value else 100//Max health is 100.
        }
    var hunger = 0
        set(value) {
            field = when
            {
                value<0 -> 0
                value>100 -> 100
                else -> value
            }//Max hunger is 100.
        }
    var thirst = 0
        set(value) {
            field = when
            {
                value<0 -> 0
                value>100 -> 100
                else -> value
            }//Max thirst is 100.
        }
    var reliants = hashSetOf<String>() //Characters that this character is responsible for. If they die, this character will be sad. They consume water and ration every day.
    var scheduledMeetings = hashSetOf<Meeting>()
    var home = ""
    var frozen = 0

    val place
    get() = parent.places.values.first{it.characters.contains(name)}

}