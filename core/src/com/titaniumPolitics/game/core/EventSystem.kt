package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.events.*
import kotlinx.serialization.Serializable


//Events are quests that never expire. Some can be triggered many times, some only once.
@Serializable
class EventSystem : GameStateElement() {
    override val name: String
        get() = "EventSystem" //There is only one EventSystem object in the game.
    val dataBase = arrayListOf<EventObject>()


    //Add an objective with a time limit.

    override fun injectParent(gameState: GameState) {
        super.injectParent(gameState)
        dataBase.add(Event_PrologueInfDivLeaderSpeech())
        dataBase.add(Event_BribeDoctor1())
        dataBase.add(Event_BoyFindingMom())
        //dataBase.add(Event_ObserverIntro())
        dataBase.add(Event_AlinaIllTheory1())
        dataBase.add(Event_SalvorElection())
        dataBase.forEach {
            it.injectParent(parent)
            it.activate()
        }
    }

    fun refresh() {
        dataBase.forEach {
            it.deactivate()
        }
        dataBase.forEach {
            it.activate()
        }

    }

    companion object {
        val onPlayDialogue = arrayListOf<(String) -> Unit>()
    }

}
