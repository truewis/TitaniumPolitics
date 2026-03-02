package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.Party
import kotlinx.serialization.Serializable

/**
 * Drama: Credit Theft
 * A character with the "thief" trait claims credit for colleagues' work.
 * The thief's will rises slightly while the victim's will and trust in the thief fall.
 */
@Serializable
class Event_Drama_CreditTheft : EventObject("PartyDrama_CreditTheft", false) {

    override fun exec(a: Int, b: Int) {}

    override fun execInMeeting(meeting: Meeting) {
        parent.parties.values.filter { it.type == Party.Type.WORKPLACE }.forEach { party ->
            val inMeeting = party.realMembers.filter { it in meeting.currentCharacters }
            if (inMeeting.size < 2) return@forEach
            val thief = inMeeting.firstOrNull { "thief" in parent.characters[it]!!.trait }
                ?: return@forEach
            val victim = inMeeting.firstOrNull {
                it != thief && it != party.leader
            } ?: return@forEach

            parent.setMutuality(thief, thief, 3.0, "drama-creditTheft")
            parent.setMutuality(victim, victim, -5.0, "drama-creditTheft")
            parent.setMutuality(victim, thief, -5.0, "drama-creditTheft")
            PartyDramaUtils.checkMembersLeaving(party, parent)
        }
    }
}
