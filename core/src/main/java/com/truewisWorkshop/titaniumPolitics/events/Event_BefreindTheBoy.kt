package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Event_BefreindTheBoy : EventObject(ReadOnly.questProp("BefreindTheBoy-name"), true) {

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "market" &&
            "Yuri" in parent.player.place.characters &&
            parent.player.currentMeeting == null
        ) {
            onPlayDialogue("BefreindTheBoy")
            parent.eventSystem.add(Event_BefreindTheBoy2())
            deactivate()
        }
    }

}
