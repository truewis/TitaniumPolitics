package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_ObserverIntroAfterMeeting2 : EventObject("Mysterious orders from the Observer.", true)
{

    override fun exec(a: Int, b: Int)
    {
        if (parent.player.place.name == "spacePort" && parent.player.currentMeeting?.currentCharacters?.contains("observer") == true)
        {
            onPlayDialogue("ObserverIntroAfterMeeting2")
            deactivate()
        }
    }

    override fun activate()
    {
        //Play dialogue right after the meeting
        //TODO: check if the player has followed the orders.
        parent.timeChanged += this::exec
    }

    override fun deactivate()
    {
        completed = true
        parent.timeChanged -= this::exec
    }
}