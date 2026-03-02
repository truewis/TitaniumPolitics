package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.Party
import kotlinx.serialization.Serializable

/**
 * Drama: Rivalry
 * Two highly ambitious employees compete for status and recognition.
 * Continuous friction chips away at their mutual respect.
 */
@Serializable
class Event_Drama_Rivalry : EventObject("PartyDrama_Rivalry", false) {

    override fun exec(a: Int, b: Int) {}

    override fun execInMeeting(meeting: Meeting) {
        parent.parties.values.filter { it.type == Party.Type.WORKPLACE }.forEach { party ->
            val inMeeting = party.realMembers.filter { it in meeting.currentCharacters }
            val ambitious = inMeeting.filter {
                parent.characters[it]!!.stats.riskTaking > 12
            }.shuffled()
            if (ambitious.size < 2) return@forEach

            val rivalA = ambitious[0]
            val rivalB = ambitious[1]

            parent.setMutuality(rivalA, rivalB, -4.0, "drama-rivalry")
            parent.setMutuality(rivalB, rivalA, -4.0, "drama-rivalry")
            PartyDramaUtils.checkMembersLeaving(party, parent)
        }
    }
}
