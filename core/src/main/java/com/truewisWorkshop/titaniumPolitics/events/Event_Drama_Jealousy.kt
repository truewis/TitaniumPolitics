package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.Party
import kotlinx.serialization.Serializable

/**
 * Drama: Jealousy
 * A struggling employee with low morale envies a thriving colleague.
 * The jealous character's resentment toward the envied grows and their own morale
 * spirals further downward.
 */
@Serializable
class Event_Drama_Jealousy : EventObject("PartyDrama_Jealousy", false) {

    override fun exec(a: Int, b: Int) {}

    override fun execInMeeting(meeting: Meeting) {
        parent.parties.values.filter { it.type == Party.Type.WORKPLACE }.forEach { party ->
            val inMeeting = party.realMembers.filter { it in meeting.currentCharacters }
            if (inMeeting.size < 2) return@forEach
            val jealous = inMeeting.firstOrNull {
                parent.getMutNorm(it, it) < -0.2
            } ?: return@forEach
            val envied = inMeeting.firstOrNull {
                it != jealous &&
                    parent.getMutNorm(it, it) - parent.getMutNorm(jealous, jealous) > 0.3
            } ?: return@forEach

            parent.setMutuality(jealous, envied, -5.0, "drama-jealousy")
            parent.setMutuality(jealous, jealous, -3.0, "drama-jealousy")
            PartyDramaUtils.checkMembersLeaving(party, parent)
        }
    }
}
