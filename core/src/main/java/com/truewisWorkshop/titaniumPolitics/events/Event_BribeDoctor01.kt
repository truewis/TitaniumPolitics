package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Event_BribeDoctor01 : EventObject(ReadOnly.questProp("BribeDoctor01-name"), true) {

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "welfareStationEast") {
            onPlayDialogue("BribeDoctor01")
            parent.eventSystem.add(Event_BribeDoctor2())
            deactivate()
        }
    }

}
