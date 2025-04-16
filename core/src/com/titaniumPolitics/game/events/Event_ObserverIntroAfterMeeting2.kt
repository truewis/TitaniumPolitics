package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_ObserverIntroAfterMeeting2 : EventObject("Mysterious orders from the Observer.", true) {
    //Infrastructure Division Leader gives a speech. Quest is completed when the game starts.
    override fun injectParent(gameState: GameState) {
        super.injectParent(gameState)
        //Injected at the start of the game. No action required.

    }

    @Transient
    val func = { _: Int, _: Int ->
        if (parent.player.place.name == "spacePort" && parent.player.currentMeeting?.currentCharacters?.contains("observer") == true) {
            onPlayDialogue("ObserverIntroAfterMeeting2")
            deactivate()
        }
    }

    override fun activate() {
        //Play dialogue right after the meeting
        //TODO: check if the player has followed the orders.
        parent.timeChanged += func
    }

    override fun deactivate() {
        parent.timeChanged -= func
    }
}