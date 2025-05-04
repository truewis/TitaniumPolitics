package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_SalvorElection : EventObject("Salvor speaks in the election.", true)
{

    override fun exec(a: Int, b: Int)
    {
        if (parent.parties["infrastructure"]!!.leader == "" && parent.player.currentMeeting!!.type == "divisionLeaderElection"
        )
        {
            if (parent.player.currentMeeting!!.currentCharacters.containsAll(
                    listOf("Krailin", "Veame", "Mentor")
                )
            )
            {
                onPlayDialogue("SalvorElection")
                deactivate()
            }
        }
    }


}