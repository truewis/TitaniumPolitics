package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.QuestSystem
import com.titaniumPolitics.game.quests.Quest1
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_SalvorElection : EventObject("Salvor speaks in the election.", true)
{
    //Infrastructure Division Leader gives a speech. Quest is completed when the game starts.
    override fun injectParent(gameState: GameState)
    {
        super.injectParent(gameState)
        //Injected at the start of the game. No action required.

    }

    @Transient
    val func = { _: Int, _: Int ->
        if (parent.parties["infrastructure"]!!.leader == "" && parent.player.currentMeeting!!.type == "divisionLeaderElection"
        )
        {
            if (parent.player.currentMeeting!!.currentCharacters.containsAll(
                    listOf("Krailin", "Veame", "Mentor")
                )
            )
            {
                DialogueUI.instance.playDialogue("SalvorElection")
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