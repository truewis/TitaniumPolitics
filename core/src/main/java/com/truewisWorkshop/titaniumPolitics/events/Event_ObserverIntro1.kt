package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Event_ObserverIntro1 : EventObject(ReadOnly.questProp("ObserverIntro1-name"), true) {

    override fun exec(a: Int, b: Int) {
        onPlayDialogue("ObserverIntro")
        parent.eventSystem.add(Event_ObserverIntro2())
        deactivate()
    }

}