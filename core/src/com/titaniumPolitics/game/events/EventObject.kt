package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.EventSystem
import com.titaniumPolitics.game.core.GameState
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 *
 */
@Serializable
sealed class EventObject(var name: String, val oneTime: Boolean) {
    @Transient
    lateinit var parent: GameState

    var completed = false
    open fun injectParent(gameState: GameState) {
        parent = gameState
        if (this is IQuestEventObject) {
            quest.parent = parent
            parent.eventSystem.updateQuest(quest)
        }
    }

    abstract fun exec(a: Int, b: Int)


    //This event will not be triggered by the game. Unsubscribe from events here.
    fun deactivate() {
        completed = true
        if (this is IQuestEventObject) {
            quest.isCompleted = true
            parent.eventSystem.updateQuest(quest)
        }
    }

    open fun displayEmoji(who: String): Boolean {
        return false
    }

    fun onPlayDialogue(dialogueKey: String) {
        EventSystem.onPlayDialogue.forEach { it(dialogueKey) }
    }

}