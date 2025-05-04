package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_BoyFindingMom3 : EventObject("A boy with a box.", true)
{

    override fun exec(a: Int, b: Int)
    {
        if (parent.hour in 20..23 || parent.hour in 0..4 && parent.player.currentMeeting == null && parent.player.place.name == "squareNorth"
        )
        {
            onPlayDialogue("FindMom3")
            deactivate()
        }
    }


}