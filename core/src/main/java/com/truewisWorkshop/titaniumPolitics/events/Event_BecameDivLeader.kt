package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Event_BecameDivLeader : EventObject(ReadOnly.questProp("BecameDivLeader-name"), true) {

    override fun exec(a: Int, b: Int) {
        if (parent.parties["infrastructure"]!!.leader == parent.playerName
        ) {

            onPlayDialogue("BecameDivLeader")
            deactivate()

        }
    }


}