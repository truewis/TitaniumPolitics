package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.events.EventObject
import com.titaniumPolitics.game.events.Event_PrologueInfDivLeaderSpeech
import com.titaniumPolitics.game.quests.QuestObject
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


//Events are quests that never expire. Some can be triggered many times, some only once.
@Serializable
class EventSystem : GameStateElement()
{
    override val name: String
        get() = "EventSystem" //There is only one EventSystem object in the game.
    val dataBase = arrayListOf<EventObject>()


    //Add an objective with a time limit.

    override fun injectParent(gameState: GameState)
    {
        super.injectParent(gameState)
        dataBase.add(Event_PrologueInfDivLeaderSpeech())
        dataBase.forEach {
            it.injectParent(parent)
            it.activate()
        }
    }

    fun refresh()
    {
        dataBase.forEach {
            it.deactivate()
        }
        dataBase.forEach {
            it.activate()
        }

    }

}
