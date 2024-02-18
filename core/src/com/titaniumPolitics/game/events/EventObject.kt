package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 *
 */
@Serializable
sealed class EventObject(var name: String, val oneTime: Boolean)
{
    @Transient
    lateinit var parent: GameState
    open fun injectParent(gameState: GameState)
    {
        parent = gameState
    }

    //This event will be triggered by the game. Subscribe to events here.
    abstract fun activate()

    //This event will not be triggered by the game. Unsubscribe from events here.
    abstract fun deactivate()

}