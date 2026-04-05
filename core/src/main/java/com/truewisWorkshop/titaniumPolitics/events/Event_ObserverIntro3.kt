package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Event_ObserverIntro3 : EventObject(ReadOnly.questProp("ObserverIntro3-name"), true) {

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "techSchool"
        ) {
            onPlayDialogue("ObserverIntro3")
            deactivate()
        }
    }


}