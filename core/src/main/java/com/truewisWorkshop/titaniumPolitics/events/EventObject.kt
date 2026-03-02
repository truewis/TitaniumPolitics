package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.EventSystem
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Meeting
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

    var active = true
        private set

    open fun injectParent(gameState: GameState) {
        parent = gameState
        //Only update the quest for active quests.
        if (active && this is IQuestEventObject) {
            try {
                parent.eventSystem.updateQuest(this, quest)
            } catch (e: Exception) {
                Logger.write("Error updating quest for event $name: ${e.message}")
            }
        }
    }

    abstract fun exec(a: Int, b: Int)

    /** Called for each action performed during an ongoing meeting. Drama events override this. */
    open fun execInMeeting(meeting: Meeting) {}

    //This event will not be triggered by the game. Unsubscribe from events here.
    fun deactivate(success: Boolean = true) {
        active = false
        if (this is IQuestEventObject) {
            try {
                parent.eventSystem.finishQuest(this, success)
            } catch (e: Exception) {
                Logger.write("Error finishing quest for event $name: ${e.message}")
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