package com.titaniumPolitics.game.events

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_ObserverIntro3 : EventObject("Introduction of the Observer.", true)
{

    override fun exec(a: Int, b: Int)
    {
        if (parent.player.place.name == "constructionYardNorth"
        )
        {
            onPlayDialogue("ObserverIntro3")
            deactivate()
        }
    }


}