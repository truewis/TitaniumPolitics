package com.titaniumPolitics.game.events

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

    override fun exec(a: Int, b: Int) {
        if (!PartyDramaUtils.isNewDay(a, b)) return
        parent.parties.values.filter { it.type == Party.Type.WORKPLACE }.forEach { party ->
            val jealous = party.realMembers.firstOrNull {
                parent.getMutNorm(it, it) < -0.2
            } ?: return@forEach
            val envied = party.realMembers.firstOrNull {
                it != jealous &&
                    parent.getMutNorm(it, it) - parent.getMutNorm(jealous, jealous) > 0.3
            } ?: return@forEach

            parent.setMutuality(jealous, envied, -0.5, "drama-jealousy")
            parent.setMutuality(jealous, jealous, -0.3, "drama-jealousy")
            PartyDramaUtils.checkMembersLeaving(party, parent)
        }
    }
}
