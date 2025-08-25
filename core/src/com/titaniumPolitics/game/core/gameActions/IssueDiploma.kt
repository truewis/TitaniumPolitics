package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
data class IssueDiploma(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    var amount = 30
    override fun execute() {

        TODO()
        super.execute()

    }

    override fun isValid(): Boolean {
        return parent.places[tgtPlace]!!.apparatuses.isNotEmpty() && parent.characters[sbjCharacter]!!.trait.contains("technician")
    }

}