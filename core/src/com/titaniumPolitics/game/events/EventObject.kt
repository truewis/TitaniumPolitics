package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.EventSystem
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.debugTools.Logger
import com.titaniumPolitics.game.ui.widget.SpeechUI
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
        //Only update the quest for incomplete quests.
        if (!completed && this is IQuestEventObject) {
            try {
                parent.eventSystem.updateQuest(quest)
            } catch (e: Exception) {
                Logger.write("Error updating quest for event $name: ${e.message}")
            }
        }
    }

    abstract fun exec(a: Int, b: Int)


    //This event will not be triggered by the game. Unsubscribe from events here.
    fun deactivate() {
        completed = true
        if (this is IQuestEventObject) {
            try {
                parent.eventSystem.updateQuest(quest)
            } catch (e: Exception) {
                Logger.write("Error updating quest for event $name: ${e.message}")
            }
        }
    }

    open fun displayEmoji(who: String): SpeechUI.EmojiType {
        return SpeechUI.EmojiType.NONE
    }

    fun onPlayDialogue(dialogueKey: String) {
        EventSystem.onPlayDialogue.forEach { it(dialogueKey) }
    }

}