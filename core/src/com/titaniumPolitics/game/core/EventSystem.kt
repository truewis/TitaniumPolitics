package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.events.*
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable


//Events are quests that never expire. Some can be triggered many times, some only once.
@Serializable
class EventSystem : GameStateElement() {
    override val name: String
        get() = "EventSystem" //There is only one EventSystem object in the game.
    private val dataBase = arrayListOf<EventObject>()
    private val tmpdataBase = arrayListOf<EventObject>()
    val quests =
        arrayListOf<Quest>() //Do not use haseSet, it is not meant to be used with objects that can be modified.

    //Utility function called once when a new game starts.
    fun newGame() {
        add(Event_PrologueAlinaSpeech())
        add(Event_BribeDoctor1())
        add(Event_BoyFindingMom())
        //dataBase.add(Event_ObserverIntro())
        add(Event_AlinaIllTheory1())
        add(Event_SalvorElection())
    }

    fun updateQuest(quest: Quest) {
        if (quests.any { it.name == quest.name }) {
            quests.removeIf { it.name == quest.name }
        }
        quests.add(quest)
    }

    override fun injectParent(gameState: GameState) {
        super.injectParent(gameState)
        dataBase.forEach {
            it.injectParent(gameState)
        }
        gameState.timeChanged += { a, b ->
            dataBase.forEach { if (!it.completed) it.exec(a, b) }
            tmpdataBase.forEach { dataBase += it }
            tmpdataBase.clear()
        }

    }

    fun add(event: EventObject) {
        tmpdataBase.add(event)
        event.injectParent(parent)
    }

    fun displayEmoji(who: String): Boolean {
        return dataBase.any { !it.completed && it.displayEmoji(who) }
    }

    companion object {
        val onPlayDialogue = arrayListOf<(String) -> Unit>()
    }

}
