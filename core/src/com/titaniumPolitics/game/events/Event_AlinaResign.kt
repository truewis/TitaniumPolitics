package com.titaniumPolitics.game.events

import kotlinx.serialization.Serializable

@Serializable
class Event_AlinaResign : EventObject("Introduction of Alina.", true) {

    override fun exec(a: Int, b: Int) {
        if (parent.day > 3 && parent.player.currentMeeting != null && parent.parties["infrastructure"]!!.leader == "Alina" && parent.player.currentMeeting!!.currentCharacters.containsAll(
                listOf("Alina")
            )
        ) {
            onPlayDialogue("AlinaResign")
            parent.eventSystem.add(Event_BecameDivLeader())
            deactivate()
        }
    }


}