package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Party
import kotlinx.serialization.Serializable

/**
 * Drama: Credit Theft
 * A character with the "thief" trait claims credit for colleagues' work.
 * The thief's will rises slightly while the victim's will and trust in the thief fall.
 */
@Serializable
class Event_Drama_CreditTheft : EventObject("PartyDrama_CreditTheft", false) {

    override fun exec(a: Int, b: Int) {
        if (!PartyDramaUtils.isNewDay(a, b)) return
        parent.parties.values.filter { it.type == Party.Type.WORKPLACE }.forEach { party ->
            val thief = party.realMembers.firstOrNull { "thief" in parent.characters[it]!!.trait }
                ?: return@forEach
            val victim = party.realMembers.firstOrNull {
                it != thief && it != party.leader
            } ?: return@forEach

            parent.setMutuality(thief, thief, 0.3, "drama-creditTheft")
            parent.setMutuality(victim, victim, -0.5, "drama-creditTheft")
            parent.setMutuality(victim, thief, -0.5, "drama-creditTheft")
            PartyDramaUtils.checkMembersLeaving(party, parent)
        }
    }
}
