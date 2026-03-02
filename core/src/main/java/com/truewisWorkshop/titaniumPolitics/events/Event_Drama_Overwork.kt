package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.Party
import kotlinx.serialization.Serializable

/**
 * Drama: Overwork
 * A diligent, high-logos employee grows resentful of a low-logos colleague who doesn't
 * contribute equally.  The hard worker's respect for the slacker erodes; the slacker
 * feels the pressure and loses morale.
 */
@Serializable
class Event_Drama_Overwork : EventObject("PartyDrama_Overwork", false) {

    override fun exec(a: Int, b: Int) {}

    override fun execInMeeting(meeting: Meeting) {
        parent.parties.values.filter { it.type == Party.Type.WORKPLACE }.forEach { party ->
            val inMeeting = party.realMembers.filter { it in meeting.currentCharacters }
            if (inMeeting.size < 2) return@forEach
            val hardWorker = inMeeting.firstOrNull {
                parent.characters[it]!!.stats.logos > 12
            } ?: return@forEach
            val slacker = inMeeting.firstOrNull {
                it != hardWorker && parent.characters[it]!!.stats.logos < 8
            } ?: return@forEach

            parent.setMutuality(hardWorker, slacker, -4.0, "drama-overwork")
            parent.setMutuality(slacker, slacker, -4.0, "drama-overwork")
            PartyDramaUtils.checkMembersLeaving(party, parent)
        }
    }
}
