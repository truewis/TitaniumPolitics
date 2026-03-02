package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.Party
import kotlinx.serialization.Serializable

/**
 * Drama: Favoritism
 * A party leader visibly favours members who share their traits while sidelining others.
 * Non-favoured members' respect for the leader gradually decreases.
 */
@Serializable
class Event_Drama_Favoritism : EventObject("PartyDrama_Favoritism", false) {

    override fun exec(a: Int, b: Int) {}

    override fun execInMeeting(meeting: Meeting) {
        parent.parties.values.filter { it.type == Party.Type.WORKPLACE }.forEach { party ->
            val inMeeting = party.realMembers.filter { it in meeting.currentCharacters }
            if (inMeeting.size < 2) return@forEach
            val leaderName = party.leader ?: return@forEach
            if (leaderName !in meeting.currentCharacters) return@forEach
            val leaderTraits = parent.characters[leaderName]!!.trait
            if (leaderTraits.isEmpty()) return@forEach

            val (favoured, unfavoured) = inMeeting.filter { it != leaderName }
                .partition { parent.characters[it]!!.trait.any { t -> t in leaderTraits } }
            if (favoured.isEmpty() || unfavoured.isEmpty()) return@forEach

            unfavoured.forEach { char ->
                parent.setMutuality(char, leaderName, -4.0, "drama-favoritism")
                parent.setMutuality(char, char, -3.0, "drama-favoritism")
            }
            PartyDramaUtils.checkMembersLeaving(party, parent)
        }
    }
}
