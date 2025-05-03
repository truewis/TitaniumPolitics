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


    //Utility function called once when a new game starts.
    fun newGame()
    {
        add(Event_PrologueInfDivLeaderSpeech())
        add(Event_BribeDoctor1())
        add(Event_BoyFindingMom())
        //dataBase.add(Event_ObserverIntro())
        add(Event_AlinaIllTheory1())
        add(Event_SalvorElection())
    }

    override fun injectParent(gameState: GameState)
    {
        super.injectParent(gameState)
        dataBase.forEach {
            if (!it.completed)
                it.activate()//If loaded from disk, all events are unsubscribed, hence we have to subscribe them again.
        }

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

    companion object
    {
        val onPlayDialogue = arrayListOf<(String) -> Unit>()
    }

}
