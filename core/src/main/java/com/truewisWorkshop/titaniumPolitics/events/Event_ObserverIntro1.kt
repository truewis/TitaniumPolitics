package com.titaniumPolitics.game.events

import kotlinx.serialization.Serializable

@Serializable
class Event_ObserverIntro1 : EventObject("Introduction of the Observer.", true) {

    override fun exec(a: Int, b: Int) {
        onPlayDialogue("ObserverIntro")
        parent.eventSystem.add(Event_ObserverIntro2())
        deactivate()
    }

}