package com.titaniumPolitics.game.events

import kotlinx.serialization.Serializable

@Serializable
class Event_BribeDoctor1 : EventObject("Illness of Sylvia.", true) {

    override fun exec(a: Int, b: Int) {
        if (parent.hour == 10 && parent.day in 5..8 && parent.player.currentMeeting != null
        ) {
            if (parent.player.currentMeeting!!.currentCharacters.containsAll(
                    listOf("Mentor")
                )
            ) {
                onPlayDialogue("BribeDoctor1")
                parent.eventSystem.add(Event_BribeDoctor2())
                deactivate()

            }
        }
    }

    override fun displayEmoji(who: String): Boolean {
        return who == "Mentor"
    }


}