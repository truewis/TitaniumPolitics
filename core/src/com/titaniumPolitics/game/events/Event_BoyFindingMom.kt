package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_BoyFindingMom : EventObject("A boy with a box.", true)
{

    @Transient
    override val exec = { _: Int, _: Int ->
        if (parent.hour in 9..12 && parent.player.currentMeeting == null && parent.player.place.name == "squareNorth"
        )
        {
            onPlayDialogue("FindMom")
            parent.eventSystem.dataBase.add(Event_BoyFindingMom2())
            deactivate()
        }
    }


}