package com.titaniumPolitics.game.events

import kotlinx.serialization.Serializable

@Serializable
class Event_BecameDivLeader : EventObject("I am the infrastructure division leader.", true) {

    override fun exec(a: Int, b: Int) {
        if (parent.parties["infrastructure"]!!.leader == parent.playerName
        ) {

            onPlayDialogue("BecameDivLeader")
            deactivate()

        }
    }


}