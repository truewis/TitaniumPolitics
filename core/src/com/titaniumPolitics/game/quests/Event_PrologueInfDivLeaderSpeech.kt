package com.titaniumPolitics.game.quests

import com.titaniumPolitics.game.core.EventObject
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.QuestObject
import com.titaniumPolitics.game.ui.DialogueUI

class Event_PrologueInfDivLeaderSpeech : EventObject("Introduction of InfDivLeader.", true)
{
    //Infrastructure Division Leader gives a speech. Quest is completed when the game starts.
    override fun injectParent(gameState: GameState)
    {
        super.injectParent(gameState)
        //Injected at the start of the game. No action required.

    }

    override val isTriggered: Boolean
        get()
        {
            DialogueUI.instance.playDialogue("PrologueInfDivLeaderSpeech")
            return true
        }
}