package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Party
import kotlinx.serialization.Serializable

/**
 * Drama: Gossip
 * An extrovert character spreads negative talk about a colleague they already dislike.
 * The target's will and standing in the party both decline.
 */
@Serializable
class Event_Drama_Gossip : EventObject("PartyDrama_Gossip", false) {

    override fun exec(a: Int, b: Int) {
        if (!PartyDramaUtils.isNewDay(a, b)) return
        parent.parties.values.filter { it.type == Party.Type.WORKPLACE }.forEach { party ->
            val gossiper = party.realMembers.firstOrNull {
                "extrovert" in parent.characters[it]!!.trait &&
                    party.realMembers.any { target ->
                        target != it && parent.getMutNorm(it, target) < -0.2
                    }
            } ?: return@forEach
            val target = party.realMembers.firstOrNull {
                it != gossiper && parent.getMutNorm(gossiper, it) < -0.2
            } ?: return@forEach

            parent.setMutuality(target, target, -5.0, "drama-gossip")
            party.realMembers.filter { it != gossiper && it != target }.forEach { bystander ->
                parent.setMutuality(bystander, target, -3.0, "drama-gossip")
            }
            PartyDramaUtils.checkMembersLeaving(party, parent)
        }
    }
}
