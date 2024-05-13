package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.QuestSystem
import com.titaniumPolitics.game.quests.Quest1
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_ObserverIntro : EventObject("Introduction of the Observer.", true)
{
    //Infrastructure Division Leader gives a speech. Quest is completed when the game starts.
    override fun injectParent(gameState: GameState)
    {
        super.injectParent(gameState)
        //Injected at the start of the game. No action required.

    }

    @Transient
    val func = {
        DialogueUI.instance.playDialogue("ObserverIntro")
    }

    override fun activate()
    {
        parent.onStart += func
    }

    override fun deactivate()
    {
        parent.onStart -= func
    }
}