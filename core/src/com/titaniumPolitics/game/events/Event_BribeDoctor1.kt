package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_BribeDoctor1 : EventObject("Illness of Sylvia.", true)
{
    override fun injectParent(gameState: GameState)
    {
        super.injectParent(gameState)
        //Injected at the start of the game. No action required.

    }

    @Transient
    val func = { _: Int, _: Int ->
        if (parent.hour == 10 && parent.day in 5..8 && parent.player.currentMeeting != null
        )
        {
            if (parent.player.currentMeeting!!.currentCharacters.containsAll(
                    listOf("Mentor")
                )
            )
            {
                DialogueUI.instance.playDialogue("BribeDoctor1")
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