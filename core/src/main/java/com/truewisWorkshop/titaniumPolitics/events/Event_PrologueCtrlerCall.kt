package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Event_PrologueCtrlerCall : EventObject(ReadOnly.questProp("PrologueCtrlerCall-name"), true) {

    override fun exec(a: Int, b: Int) {
        onPlayDialogue("Prologue1")
        parent.eventSystem.add(Event_PrologueAlinaAccident2())
        deactivate()
    }

}
