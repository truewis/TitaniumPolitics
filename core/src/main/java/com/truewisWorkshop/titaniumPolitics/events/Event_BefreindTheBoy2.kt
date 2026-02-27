package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Event_BefreindTheBoy2 : EventObject(ReadOnly.questProp("BefreindTheBoy2-name"), true) {

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "market" &&
            "Vaeme" in parent.player.place.characters &&
            parent.player.currentMeeting == null
        ) {
            onPlayDialogue("BefreindTheBoy2")
            deactivate()
        }
    }

}
