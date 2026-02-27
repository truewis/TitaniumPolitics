package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Event_Salvor2 : EventObject(ReadOnly.questProp("Salvor2-name"), true) {

    override fun exec(a: Int, b: Int) {
        if (parent.player.currentMeeting?.currentCharacters?.containsAll(listOf("Salvor", "Rui")) == true) {
            onPlayDialogue("Salvor2")
            parent.eventSystem.add(Event_Salvor3())
            deactivate()
        }
    }

}
