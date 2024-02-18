package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.quests.Event_PrologueInfDivLeaderSpeech
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


//Events are quests that never expire. Some can be triggered many times, some only once.
@Serializable
class Events : GameStateElement()
{
    override val name: String
        get() = "Events" //There is only one Quests object in the game.
    val dataBase = arrayListOf<EventObject>()


    @Transient
    var triggered = arrayListOf<(EventObject) -> Unit>()

    //Add an objective with a time limit.

    override fun injectParent(gameState: GameState)
    {
        super.injectParent(gameState)
        dataBase.add(Event_PrologueInfDivLeaderSpeech())
        gameState.timeChanged += { old, new ->
            val removalList = arrayListOf<EventObject>()
            dataBase.forEach {
                if (it.isTriggered)
                {
                    if (it.oneTime)
                    {
                        removalList.add(it)
                    }
                    triggered.forEach { a -> a(it) }
                }
            }
            removalList.forEach { dataBase.remove(it) }

        }
        //expired+={ parent.log.appendLog(CapsuleText.log("log-todo-expired",it.title)) }
        //completed+={ parent.log.appendLog(CapsuleText.log("log-todo-complete",it.title)) }
    }

}
