package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.gameActions.Resign
import kotlinx.serialization.Serializable

@Serializable
class Event_AlinaResign : EventObject(ReadOnly.questProp("AlinaResign-name"), true) {

    override fun exec(a: Int, b: Int) {
        if (parent.day > 1 && parent.player.currentMeeting != null && parent.parties["infrastructure"]!!.leader == "Alina" && parent.player.currentMeeting!!.currentSpeaker == "Alina"
            && parent.player.currentMeeting!!.type == com.titaniumPolitics.game.core.Meeting.MeetingType.DIVISION_DAILY_CONFERENCE
        ) {
            onPlayDialogue("AlinaResign")
            parent.eventSystem.add(Event_BecameDivLeader())
            //Create request for Resign. This is a system request, so issuedBy is empty.
            Request(
                action = Resign("Alina", parent.parties["infrastructure"]!!.home!!),
                issuedTo = hashSetOf("Alina"),
            ).apply {
                parent.requests[generateName()] = this
            }
            deactivate()
        }
    }


}