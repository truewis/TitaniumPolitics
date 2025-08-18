package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.gameActions.Resign
import kotlinx.serialization.Serializable

@Serializable
class Event_AlinaResign : EventObject("Introduction of Alina.", true) {

    override fun exec(a: Int, b: Int) {
        if (parent.day > 3 && parent.player.currentMeeting != null && parent.parties["infrastructure"]!!.leader == "Alina" && parent.player.currentMeeting!!.currentSpeaker == "Alina"
        ) {
            onPlayDialogue("AlinaResign")
            parent.eventSystem.add(Event_BecameDivLeader())
            //Create request for Resign. This is a system request, so issuedBy is empty.
            Request(
                action = Resign("Alina", parent.player.place.name),
                issuedTo = hashSetOf("Alina"),
            ).apply {
                executeTime = parent.time
                parent.requests[generateName()] = this
            }
            deactivate()
        }
    }


}