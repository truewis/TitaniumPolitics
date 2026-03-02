package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Party
import kotlinx.serialization.Serializable

/**
 * Drama: Favoritism
 * A party leader visibly favours members who share their traits while sidelining others.
 * Non-favoured members' respect for the leader gradually decreases.
 */
@Serializable
class Event_Drama_Favoritism : EventObject("PartyDrama_Favoritism", false) {

    override fun exec(a: Int, b: Int) {
        if (!PartyDramaUtils.isNewDay(a, b)) return
        parent.parties.values.filter { it.type == Party.Type.WORKPLACE }.forEach { party ->
            val leaderName = party.leader ?: return@forEach
            val leaderTraits = parent.characters[leaderName]!!.trait
            if (leaderTraits.isEmpty()) return@forEach

            val favoured = party.realMembers.filter { char ->
                char != leaderName && parent.characters[char]!!.trait.any { it in leaderTraits }
            }
            val unfavoured = party.realMembers.filter { char ->
                char != leaderName && parent.characters[char]!!.trait.none { it in leaderTraits }
            }
            if (favoured.isEmpty() || unfavoured.isEmpty()) return@forEach

            unfavoured.forEach { char ->
                parent.setMutuality(char, leaderName, -4.0, "drama-favoritism")
                parent.setMutuality(char, char, -3.0, "drama-favoritism")
            }
            PartyDramaUtils.checkMembersLeaving(party, parent)
        }
    }
}
