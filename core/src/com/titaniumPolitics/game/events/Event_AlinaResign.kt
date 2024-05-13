package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.QuestSystem
import com.titaniumPolitics.game.quests.Quest1
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_AlinaResign : EventObject("Introduction of Alina.", true)
{
    //Infrastructure Division Leader gives a speech. Quest is completed when the game starts.
    override fun injectParent(gameState: GameState)
    {
        super.injectParent(gameState)
        //Injected at the start of the game. No action required.

    }

    @Transient
    val func = { _: Int, newTime: Int ->
        if (newTime > 96 && parent.player.currentMeeting != null && parent.parties["infrastructure"]!!.leader == "Alina" && parent.player.currentMeeting!!.currentCharacters.containsAll(
                listOf("Alina")
            )
        )
        {
            DialogueUI.instance.playDialogue("AlinaResign")
            parent.questSystem.add(Quest1())
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