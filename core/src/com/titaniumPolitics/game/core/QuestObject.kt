package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable

/**
 *
 */
@Serializable
open class QuestObject(override var name: String, val due: Int) : GameStateElement()
{
    var expired = false

    /**
     * Time of completion. 0 when not completed.
     */
    var completed = 0
        set(value)
        {
            if (field != 0 && value == 0) throw IllegalArgumentException(this.name + " is already completed, cannot be uncompleted.")
            field = value
            parent.quests.completed.forEach { it(this) }

        }

    open val isCompleted: Boolean
        get() = false

    open fun onExpired()
    {
    }
}