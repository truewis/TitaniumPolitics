package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.quests.Quest1
import com.titaniumPolitics.game.quests.QuestObject
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class QuestSystem : GameStateElement()
{
    override val name: String
        get() = "QuestSystem" //There is only one QuestSystem object in the game.
    val dataBase = arrayListOf<QuestObject>()

    @Transient
    var onChange = arrayListOf<() -> Unit>()


    fun add(obj: QuestObject)
    {
        dataBase.add(obj)
        onChange.forEach { it() }
        if (obj.due != 0)
        {
            //parent.log.appendLog(CapsuleText.log("log-todo", formatTime(obj.due - parent.time), obj.title))
        } else
        {
        }
        //parent.log.appendLog(CapsuleText.log("log-todo-notime", obj.title))
    }

    override fun injectParent(gameState: GameState)
    {
        super.injectParent(gameState)
        gameState.timeChanged += { old, new ->
            dataBase.forEach {
                if (it.state == QuestObject.QuestState.ACTIVE)
                {
                    if (it.due != null && it.due < new)
                    {
                        it.expire()
                        onChange.forEach { it() }
                    }
                }

            }
        }
        dataBase.forEach {
            it.injectParent(parent)
            if (it.state == QuestObject.QuestState.ACTIVE)
                it.activate()
        }
        //expired+={ parent.log.appendLog(CapsuleText.log("log-todo-expired",it.title)) }
        //completed+={ parent.log.appendLog(CapsuleText.log("log-todo-complete",it.title)) }
    }

    //This should not be called before the parent is injected, nor concurrently.
    fun refresh()
    {
        dataBase.forEach {
            if (it.state == QuestObject.QuestState.ACTIVE)//If active
                it.deactivate()
        }
        dataBase.forEach {
            if (it.state == QuestObject.QuestState.ACTIVE)
                it.activate()
        }

    }

}
