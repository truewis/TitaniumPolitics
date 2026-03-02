package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.Party
import kotlinx.serialization.Serializable

/**
 * Drama: Exclusion
 * Members sharing a dominant trait form an in-group that sidelines a colleague who
 * lacks that trait.  The excluded member's morale and standing in the party decline.
 */
@Serializable
class Event_Drama_Exclusion : EventObject("PartyDrama_Exclusion", false) {

    override fun exec(a: Int, b: Int) {}

    override fun execInMeeting(meeting: Meeting) {
        parent.parties.values.filter { it.type == Party.Type.WORKPLACE }.forEach { party ->
            val inMeeting = party.realMembers.filter { it in meeting.currentCharacters }
            if (inMeeting.size < 3) return@forEach

            val dominantTrait = inMeeting
                .flatMap { parent.characters[it]!!.trait }
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.takeIf { it.value >= 2 }
                ?.key ?: return@forEach

            val inGroup = inMeeting.filter { dominantTrait in parent.characters[it]!!.trait }
            val excluded = inMeeting.firstOrNull { dominantTrait !in parent.characters[it]!!.trait }
                ?: return@forEach

            inGroup.forEach { member ->
                parent.setMutuality(member, excluded, -3.0, "drama-exclusion")
            }
            parent.setMutuality(excluded, excluded, -5.0, "drama-exclusion")
            PartyDramaUtils.checkMembersLeaving(party, parent)
        }
    }
}
