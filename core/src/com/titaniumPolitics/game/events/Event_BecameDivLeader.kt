package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_BecameDivLeader : EventObject("I am the infrastructure division leader.", true) {
    //Infrastructure Division Leader gives a speech. Quest is completed when the game starts.
    override fun injectParent(gameState: GameState) {
        super.injectParent(gameState)
        //Injected at the start of the game. No action required.

    }

    @Transient
    val func = { _: Int, _: Int ->
        if (parent.parties["infrastructure"]!!.leader == parent.playerName
        ) {

            onPlayDialogue("BecameDivLeader")
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