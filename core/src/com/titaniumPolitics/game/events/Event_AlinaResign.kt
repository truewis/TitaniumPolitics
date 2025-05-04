package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_AlinaResign : EventObject("Introduction of Alina.", true)
{

    override fun exec(a: Int, b: Int)
    {
        if (b > 96 && parent.player.currentMeeting != null && parent.parties["infrastructure"]!!.leader == "Alina" && parent.player.currentMeeting!!.currentCharacters.containsAll(
                listOf("Alina")
            )
        )
        {
            onPlayDialogue("AlinaResign")
            parent.eventSystem.add(Event_BecameDivLeader())
            deactivate()
        }
    }


}