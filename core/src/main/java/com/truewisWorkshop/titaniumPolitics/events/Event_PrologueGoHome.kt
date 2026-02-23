package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Event_PrologueGoHome : EventObject(ReadOnly.questProp("PrologueGoHome-name"), true) {

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name.startsWith("home_")) {
            onPlayDialogue("Prologue3")
            parent.eventSystem.add(Event_PrologueFindNoFood())
            deactivate()
        }
    }

}
