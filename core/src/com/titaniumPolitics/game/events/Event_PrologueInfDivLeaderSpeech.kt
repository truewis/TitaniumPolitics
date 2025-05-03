package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_PrologueInfDivLeaderSpeech : EventObject("Introduction of Alina.", true)
{

    @Transient
    override val exec = { _: Int, _: Int ->
        if (parent.player.currentMeeting != null && parent.parties["infrastructure"]!!.leader == "Alina" && parent.player.currentMeeting!!.currentCharacters.containsAll(
                listOf("Alina", "Krailin")
            )
        )
        {
            onPlayDialogue("PrologueInfDivLeaderSpeech")
            parent.eventSystem.dataBase.add(Event_AlinaResign())
            parent.eventSystem.dataBase.add(Event_ObserverIntroAfterMeeting1())
            deactivate()
        }
    }


    override fun displayEmoji(who: String): Boolean
    {
        return parent.timeChanged.contains(exec) && who == "Alina" && parent.player.place.name == parent.parties["infrastructure"]!!.home && parent.characters["Alina"]!!.currentMeeting != null
    }
}