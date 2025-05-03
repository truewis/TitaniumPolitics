package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.events.*
import kotlinx.serialization.Serializable


//Events are quests that never expire. Some can be triggered many times, some only once.
@Serializable
class EventSystem : GameStateElement()
{
    override val name: String
        get() = "EventSystem" //There is only one EventSystem object in the game.
    private val dataBase = arrayListOf<EventObject>()


    //Add an objective with a time limit.

    override fun injectParent(gameState: GameState)
    {
        super.injectParent(gameState)
        add(Event_PrologueInfDivLeaderSpeech())
        add(Event_BribeDoctor1())
        add(Event_BoyFindingMom())
        //dataBase.add(Event_ObserverIntro())
        add(Event_AlinaIllTheory1())
        add(Event_SalvorElection())
    }

    fun add(event: EventObject)
    {
        dataBase.add(event)
        event.injectParent(parent)
        event.activate()
    }

    fun displayEmoji(who: String): Boolean
    {
        return dataBase.any { it.displayEmoji(who) }
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

    companion object
    {
        val onPlayDialogue = arrayListOf<(String) -> Unit>()
    }

}
