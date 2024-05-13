package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.QuestSystem
import com.titaniumPolitics.game.quests.Quest1
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_AlinaIllTheory1 : EventObject("Illness of Alina.", true)
{
    //Infrastructure Division Leader gives a speech. Quest is completed when the game starts.
    override fun injectParent(gameState: GameState)
    {
        super.injectParent(gameState)
        //Injected at the start of the game. No action required.

    }

    @Transient
    val func = { _: Int, _: Int ->
        if (parent.hour == 10 && parent.player.currentMeeting != null && parent.parties["infrastructure"]!!.leader == ""
        )
        {
            if (parent.player.currentMeeting!!.currentCharacters.containsAll(
                    listOf("Krailin", "Veame", "Mentor")
                )
            )
            {
                DialogueUI.instance.playDialogue("AlinaIllTheory3")
                deactivate()
            } else if (parent.player.currentMeeting!!.currentCharacters.containsAll(
                    listOf("Krailin", "Veame", "Salvor")
                )
            )
            {
                DialogueUI.instance.playDialogue("AlinaIllTheory2")
                deactivate()
            } else if (parent.player.currentMeeting!!.currentCharacters.containsAll(
                    listOf("Krailin")
                )
            )
            {
                DialogueUI.instance.playDialogue("AlinaIllTheory1")
                deactivate()
            }
        }
    }

    override fun activate()
    {
        parent.timeChanged += func
    }

    override fun deactivate()
    {
        parent.timeChanged -= func
    }
}