package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_BoyFindingMom : EventObject("A boy with a box.", true) {
    //Infrastructure Division Leader gives a speech. Quest is completed when the game starts.
    override fun injectParent(gameState: GameState) {
        super.injectParent(gameState)
        //Injected at the start of the game. No action required.

    }

    @Transient
    val func = { _: Int, _: Int ->
        if (parent.hour in 9..12 && parent.player.currentMeeting == null && parent.player.place.name == "squareNorth"
        ) {
            onPlayDialogue("FindMom")
            parent.eventSystem.dataBase.add(Event_BoyFindingMom2())
            deactivate()
        }
    }

    override fun activate() {
        parent.timeChanged += func
    }

    override fun deactivate() {
        parent.timeChanged -= func
    }
}