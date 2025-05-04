package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.EventSystem
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

    var completed = false
    open fun injectParent(gameState: GameState)
    {
        parent = gameState
    }

    abstract fun exec(a: Int, b: Int)

    //This event will be triggered by the game. Subscribe to events here.
    open fun activate()
    {
        parent.timeChanged += this::exec
    }

    //This event will not be triggered by the game. Unsubscribe from events here.
    open fun deactivate()
    {
        completed = true
        parent.timeChanged -= this::exec
    }

    open fun displayEmoji(who: String): Boolean
    {
        return false
    }

    fun onPlayDialogue(dialogueKey: String)
    {
        EventSystem.onPlayDialogue.forEach { it(dialogueKey) }
    }

}