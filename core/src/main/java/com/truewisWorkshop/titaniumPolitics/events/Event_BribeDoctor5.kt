package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Event_BribeDoctor5 : EventObject(ReadOnly.questProp("BribeDoctor5-name"), true) {

    override fun exec(a: Int, b: Int) {
        if (parent.hour == 10 && parent.day in 5..8 && parent.player.currentMeeting != null
        ) {
            if (parent.player.currentMeeting!!.currentCharacters.containsAll(
                    listOf("Mentor")
                )
            ) {
                onPlayDialogue("BribeDoctor5")
                parent.progression.add("UnofficialResourceTransfer")
                deactivate()

            }
        }
    }


}