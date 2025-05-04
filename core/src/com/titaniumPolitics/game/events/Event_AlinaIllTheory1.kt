package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_AlinaIllTheory1 : EventObject("Illness of Alina.", true)
{

    override fun exec(a: Int, b: Int)
    {
        if (parent.hour == 10 && parent.player.currentMeeting != null && parent.parties["infrastructure"]!!.leader == ""
        )
        {
            if (parent.player.currentMeeting!!.currentCharacters.containsAll(
                    listOf("Krailin", "Veame", "Mentor")
                )
            )
            {
                onPlayDialogue("AlinaIllTheory3")
                deactivate()
            } else if (parent.player.currentMeeting!!.currentCharacters.containsAll(
                    listOf("Krailin", "Veame", "Salvor")
                )
            )
            {
                onPlayDialogue("AlinaIllTheory2")
                deactivate()
            } else if (parent.player.currentMeeting!!.currentCharacters.containsAll(
                    listOf("Krailin")
                )
            )
            {
                onPlayDialogue("AlinaIllTheory1")
                deactivate()
            }
        }
    }

}