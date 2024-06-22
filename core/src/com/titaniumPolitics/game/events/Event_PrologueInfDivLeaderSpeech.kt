package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_PrologueInfDivLeaderSpeech : EventObject("Introduction of Alina.", true)
{
    //Infrastructure Division Leader gives a speech. Quest is completed when the game starts.
    override fun injectParent(gameState: GameState)
    {
        super.injectParent(gameState)
        //Injected at the start of the game. No action required.

    }

    @Transient
    val func = { _: Int, _: Int ->
        if (parent.player.currentMeeting != null && parent.parties["infrastructure"]!!.leader == "Alina" && parent.player.currentMeeting!!.currentCharacters.containsAll(
                listOf("Alina", "Krailin")
            )
        )
        {
            DialogueUI.instance.playDialogue("PrologueInfDivLeaderSpeech")
            parent.eventSystem.dataBase.add(Event_AlinaResign())
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

    override fun displayEmoji(who: String): Boolean
    {
        if (parent.timeChanged.contains(func) && who == "Alina" && parent.player.place.name == parent.parties["infrastructure"]!!.home && parent.characters["Alina"]!!.currentMeeting != null)
        {
            return true
        }
        return false
    }
}