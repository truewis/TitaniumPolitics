package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.DialogueUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_BecameDivLeader : EventObject("I am the infrastructure division leader.", true)
{

    override fun exec(a: Int, b: Int)
    {
        if (parent.parties["infrastructure"]!!.leader == parent.playerName
        )
        {

            onPlayDialogue("BecameDivLeader")
            deactivate()

        }
    }


}