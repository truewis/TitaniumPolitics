package com.titaniumPolitics.game.quests

import com.titaniumPolitics.game.core.GameState
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 *
 */
@Serializable
sealed class QuestObject(var name: String, val due: Int? = null)
{
    @Transient
    lateinit var parent: GameState
    open fun injectParent(gameState: GameState)
    {
        parent = gameState
    }

    var state = QuestState.ACTIVE
        private set

    /**
     * Time of completion. null when not completed.
     */
    var completedTime: Int? = null
        set(value)
        {
            if (field != null && value == null) throw IllegalArgumentException(this.name + " is already completed, cannot be uncompleted.")
            field = value

        }

    //Call this function to complete the quest.
    fun complete()
    {
        state = QuestState.COMPLETED
        completedTime = parent.time
    }

    open fun expire()
    {
        state = QuestState.EXPIRED
        deactivate()
    }

    //This quest will be tracked by the game. Subscribe to events here.
    abstract fun activate()

    //This quest will not be tracked by the game. Unsubscribe from events here.
    abstract fun deactivate()

    enum class QuestState
    {
        ACTIVE,
        COMPLETED,
        EXPIRED
    }
}