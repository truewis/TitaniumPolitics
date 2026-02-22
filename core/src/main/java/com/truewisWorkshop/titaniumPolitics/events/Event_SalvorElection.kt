package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Event_SalvorElection : EventObject(ReadOnly.questProp("SalvorElection-name"), true) {

    override fun exec(a: Int, b: Int) {
        if (parent.parties["infrastructure"]!!.leader == null && parent.player.currentMeeting?.type == Meeting.MeetingType.DIVISION_LEADER_ELECTION
        ) {
            if (parent.player.currentMeeting!!.currentCharacters.containsAll(
                    listOf("Krailin", "Veame", "Yuhoa")
                )
            ) {
                onPlayDialogue("SalvorElection")
                deactivate()
            }
        }
    }


}