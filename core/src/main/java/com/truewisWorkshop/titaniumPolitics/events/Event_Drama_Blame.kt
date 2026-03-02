package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.Party
import kotlinx.serialization.Serializable

/**
 * Drama: Blame
 * When a party's cohesion is already low, members deflect responsibility onto each other.
 * A randomly selected pair exchanges mutual blame, eroding trust on both sides.
 */
@Serializable
class Event_Drama_Blame : EventObject("PartyDrama_Blame", false) {

    override fun exec(a: Int, b: Int) {}

    override fun execInMeeting(meeting: Meeting) {
        parent.parties.values.filter { it.type == Party.Type.WORKPLACE }.forEach { party ->
            if (party.integrityNorm >= -0.3) return@forEach
            val inMeeting = party.realMembers.filter { it in meeting.currentCharacters }.shuffled()
            if (inMeeting.size < 2) return@forEach

            val blamer = inMeeting[0]
            val blamed = inMeeting[1]

            parent.setMutuality(blamer, blamed, -5.0, "drama-blame")
            parent.setMutuality(blamed, blamer, -4.0, "drama-blame")
            parent.setMutuality(blamed, blamed, -4.0, "drama-blame")
            PartyDramaUtils.checkMembersLeaving(party, parent)
        }
    }
}
