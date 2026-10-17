package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.PolicyCatalog
import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Resources
import kotlinx.serialization.Serializable

@Serializable
data class ChangePolicy(
    override val sbjCharacter: String,
    override val tgtPlace: String,
    var newPolicy: String = ""
) : GameAction() {
    constructor(
        sbjCharacter: String,
        tgtPlace: String,
        newPolicy: String,
        gameState: GameState
    ) : this(sbjCharacter, tgtPlace, newPolicy) {
        injectParent(gameState)
    }

    val party get() = parent.parties[sbjCharObj.currentMeeting!!.involvedParty]!!
    val meeting get() = sbjCharObj.currentMeeting!!

    override fun execute() {
        // Remove any existing policy in the same category as newPolicy
        val category = PolicyCatalog.categoryOf(newPolicy)
        if (category != null) {
            party.policies.removeAll(PolicyCatalog.categories[category]!!.toSet())
        }
        party.policies.add(newPolicy)
        if (newPolicy == "workhourLimit") PolicyCatalog.enforceWorkHourLimits(parent)
        meeting.currentAttention -= ReadOnly.constInt("ChangePolicyMinAttention")
        parent.places[party.home]!!.resources -= Resources("phosphorus" to ReadOnly.const("ChangePolicyCost"))
        super.execute()
    }

    override fun isValid(): Boolean {
        if (sbjCharObj.currentMeeting == null) return false
        if (sbjCharObj.currentMeeting!!.involvedParty == null) return false
        if (!reason(party.type == Party.Type.DIVISION, "changePolicy-divisionOnly")) return false
        if (!reason(party.home != null, "changePolicy-noHome")) return false
        if (!reason(sbjCharacter == party.leader, "changePolicy-notLeader")) return false
        if (!reason(PolicyCatalog.isKnown(newPolicy), "changePolicy-unknownPolicy")) return false
        if (!reason(newPolicy !in party.policies, "changePolicy-alreadyActive")) return false
        if (!reason(
                meeting.currentAttention >= ReadOnly.constInt("ChangePolicyMinAttention"),
                "changePolicy-attention"
            )
        ) return false
        if (!reason(
                parent.places[party.home]!!.resources["phosphorus"] >= ReadOnly.const("ChangePolicyCost"),
                "changePolicy-resources"
            )
        ) return false
        if (!reason(newPolicy.isNotEmpty(), "changePolicy-noPolicy")) return false
        return true
    }

    companion object {
        val POLICY_CATEGORIES = PolicyCatalog.categories

        fun categoryOf(policy: String): String? =
            PolicyCatalog.categoryOf(policy)
    }
}
