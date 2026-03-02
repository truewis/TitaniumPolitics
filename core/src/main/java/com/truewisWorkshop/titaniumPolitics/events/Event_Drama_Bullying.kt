package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Party
import kotlinx.serialization.Serializable

/**
 * Drama: Bullying
 * A high-risk-taking character intimidates a low-risk-taking colleague.
 * The bully gains a slight will boost; the victim's will and trust in the bully erode.
 */
@Serializable
class Event_Drama_Bullying : EventObject("PartyDrama_Bullying", false) {

    override fun exec(a: Int, b: Int) {
        if (!PartyDramaUtils.isNewDay(a, b)) return
        parent.parties.values.filter { it.type == Party.Type.WORKPLACE }.forEach { party ->
            val bully = party.realMembers.firstOrNull {
                parent.characters[it]!!.stats.riskTaking > 13
            } ?: return@forEach
            val victim = party.realMembers.firstOrNull {
                it != bully && parent.characters[it]!!.stats.riskTaking < 7
            } ?: return@forEach

            parent.setMutuality(bully, bully, 3.0, "drama-bullying")
            parent.setMutuality(victim, victim, -6.0, "drama-bullying")
            parent.setMutuality(victim, bully, -5.0, "drama-bullying")
            PartyDramaUtils.checkMembersLeaving(party, parent)
        }
    }
}
