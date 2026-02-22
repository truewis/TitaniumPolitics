package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Event_ObserverIntro2 : EventObject(ReadOnly.questProp("ObserverIntro2-name"), true) {

    override fun exec(a: Int, b: Int) {
        onPlayDialogue("ObserverIntro2")
        parent.eventSystem.add(Event_ObserverIntro3())
        deactivate()
    }

}