package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_BribeDoctor1 : EventObject("Illness of Sylvia.", true)
{

    @Transient
    override val exec = { _: Int, _: Int ->
        if (parent.hour == 10 && parent.day in 5..8 && parent.player.currentMeeting != null
        )
        {
            if (parent.player.currentMeeting!!.currentCharacters.containsAll(
                    listOf("Mentor")
                )
            )
            {
                onPlayDialogue("BribeDoctor1")
                parent.eventSystem.dataBase.add(Event_BribeDoctor2())
                deactivate()

            }
        }
    }


}