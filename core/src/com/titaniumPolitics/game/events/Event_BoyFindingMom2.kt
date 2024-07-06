package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_BoyFindingMom2 : EventObject("A boy with a box.", true)
{
    //Infrastructure Division Leader gives a speech. Quest is completed when the game starts.
    override fun injectParent(gameState: GameState)
    {
        super.injectParent(gameState)
        //Injected at the start of the game. No action required.

    }

    @Transient
    val func = { _: Int, _: Int ->
        if (parent.player.currentMeeting != null && parent.player.currentMeeting!!.currentCharacters.contains("Mom")
        )
        {
            DialogueUI.instance.playDialogue("FindMom2")
            parent.eventSystem.dataBase.add(Event_BoyFindingMom3())
            deactivate()
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