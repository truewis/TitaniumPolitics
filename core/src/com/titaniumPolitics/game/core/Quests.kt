package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Quests : GameStateElement()
{
    override val name: String
        get() = "Quests" //There is only one Quests object in the game.
    val dataBase = arrayListOf<QuestObject>()

    @Transient
    var newItemAdded = arrayListOf<() -> Unit>()

    @Transient
    var expired = arrayListOf<(QuestObject) -> Unit>()

    @Transient
    var completed = arrayListOf<(QuestObject) -> Unit>()

    //Add an objective with a time limit.
    fun add(new: String, due: Int)
    {
        add(QuestObject(new, due))

    }

    fun add(obj: QuestObject)
    {
        obj.injectParent(parent)
        dataBase.add(obj)
        newItemAdded.forEach { it() }
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
                if (it.isCompleted)
                {
                    it.completed = new
                    completed.forEach { a -> a(it) }
                }
                if (it.due != 0 && it.due < new && !it.expired)
                    expired.forEach { a ->
                        a(it)
                        it.expired = true
                        it.onExpired()
                        expired.forEach { expireditem -> expireditem(it) }
                    }
            }

        }
        //expired+={ parent.log.appendLog(CapsuleText.log("log-todo-expired",it.title)) }
        //completed+={ parent.log.appendLog(CapsuleText.log("log-todo-complete",it.title)) }
    }

}
