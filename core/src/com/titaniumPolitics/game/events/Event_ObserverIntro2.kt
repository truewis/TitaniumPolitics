package com.titaniumPolitics.game.events

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_ObserverIntro2 : EventObject("Introduction of the Observer.", true)
{

    override fun exec(a: Int, b: Int)
    {
        onPlayDialogue("ObserverIntro2")
        parent.eventSystem.add(Event_ObserverIntro3())
        deactivate()
    }

}