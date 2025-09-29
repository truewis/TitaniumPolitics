package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import kotlinx.serialization.Serializable

@Serializable
data class ClearAccidentScene(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    override fun execute() {
        parent.places[tgtPlace]!!.isAccidentScene = false
        parent.places[tgtPlace]!!.accidentInformationKeys.clear()//Remove all accident information from the place.
        super.execute()
    }

    override fun isValid(): Boolean {
        return parent.places[tgtPlace]!!.isAccidentScene
    }

}