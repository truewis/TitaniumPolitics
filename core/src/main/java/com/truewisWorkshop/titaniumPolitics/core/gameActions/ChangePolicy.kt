package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.PolicyCatalog
import kotlinx.serialization.Serializable

@Serializable
@Deprecated("Policy changes are now submitted as policy-change agendas.")
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

    override fun execute() {
        throw IllegalStateException("Direct policy changes are no longer supported; submit a policy-change agenda.")
    }

    override fun isValid(): Boolean = reason(false, "changePolicy-useAgenda")

    companion object {
        val POLICY_CATEGORIES = PolicyCatalog.categories

        fun categoryOf(policy: String): String? =
            PolicyCatalog.categoryOf(policy)
    }
}
