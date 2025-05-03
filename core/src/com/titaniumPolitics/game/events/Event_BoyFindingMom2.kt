package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_BoyFindingMom2 : EventObject("A boy with a box.", true)
{

    @Transient
    override val exec = { _: Int, _: Int ->
        if (parent.player.currentMeeting != null && parent.player.currentMeeting!!.currentCharacters.contains("Mom")
        )
        {
            onPlayDialogue("FindMom2")
            parent.eventSystem.dataBase.add(Event_BoyFindingMom3())
            deactivate()
        }
    }


}