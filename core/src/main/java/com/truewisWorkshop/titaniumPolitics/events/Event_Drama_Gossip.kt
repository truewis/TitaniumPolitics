package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.Party
import kotlinx.serialization.Serializable

/**
 * Drama: Gossip
 * An extrovert character spreads negative talk about a colleague they already dislike.
 * The target's will and standing in the party both decline.
 */
@Serializable
class Event_Drama_Gossip : EventObject("PartyDrama_Gossip", false) {

    override fun exec(a: Int, b: Int) {}

    override fun execInMeeting(meeting: Meeting) {
        parent.parties.values.filter { it.type == Party.Type.WORKPLACE }.forEach { party ->
            val inMeeting = party.realMembers.filter { it in meeting.currentCharacters }
            if (inMeeting.size < 2) return@forEach
            val gossiper = inMeeting.firstOrNull {
                "extrovert" in parent.characters[it]!!.trait &&
                    inMeeting.any { target ->
                        target != it && parent.getMutNorm(it, target) < -0.2
                    }
            } ?: return@forEach
            val target = inMeeting.firstOrNull {
                it != gossiper && parent.getMutNorm(gossiper, it) < -0.2
            } ?: return@forEach

            parent.setMutuality(target, target, -5.0, "drama-gossip")
            inMeeting.filter { it != gossiper && it != target }.forEach { bystander ->
                parent.setMutuality(bystander, target, -3.0, "drama-gossip")
            }
            PartyDramaUtils.checkMembersLeaving(party, parent)
        }
    }
}
