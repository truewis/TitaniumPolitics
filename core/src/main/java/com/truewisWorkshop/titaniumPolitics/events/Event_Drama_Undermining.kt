package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.Party
import kotlinx.serialization.Serializable

/**
 * Drama: Undermining
 * An administrator subtly erodes confidence in the party leader, steering other members'
 * opinions and weakening the leader's effective authority.
 */
@Serializable
class Event_Drama_Undermining : EventObject("PartyDrama_Undermining", false) {

    override fun exec(a: Int, b: Int) {}

    override fun execInMeeting(meeting: Meeting) {
        parent.parties.values.filter { it.type == Party.Type.WORKPLACE }.forEach { party ->
            val leaderName = party.leader ?: return@forEach
            val underminer = party.administrator ?: return@forEach
            if (underminer == leaderName) return@forEach
            val inMeeting = party.realMembers.filter { it in meeting.currentCharacters }
            if (inMeeting.size < 2) return@forEach
            if (leaderName !in meeting.currentCharacters) return@forEach
            if (underminer !in meeting.currentCharacters) return@forEach

            inMeeting.filter { it != underminer && it != leaderName }.forEach { member ->
                parent.setMutuality(member, leaderName, -3.0, "drama-undermining")
            }
            parent.setMutuality(leaderName, leaderName, -4.0, "drama-undermining")
            parent.setMutuality(leaderName, underminer, -3.0, "drama-undermining")
            PartyDramaUtils.checkMembersLeaving(party, parent)
        }
    }
}
