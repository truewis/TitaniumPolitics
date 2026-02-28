package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Event_Salvor1 : EventObject(ReadOnly.questProp("Salvor1-name"), true) {

    override fun exec(a: Int, b: Int) {
        if (parent.player.currentMeeting?.currentCharacters == hashSetOf("Salvor", "Rui")) {
            onPlayDialogue("Salvor1")
            parent.eventSystem.add(Event_Salvor2())
            deactivate()
        }
    }

}
