package com.titaniumPolitics.game.events

import kotlinx.serialization.Serializable

@Serializable
class Event_BoyFindingMom2 : EventObject("A boy with a box.", true) {

    override fun exec(a: Int, b: Int) {
        if (parent.player.currentMeeting != null && parent.player.currentMeeting!!.currentCharacters.contains("Mom")
        ) {
            onPlayDialogue("FindMom2")
            parent.eventSystem.add(Event_BoyFindingMom3())
            deactivate()
        }
    }


}