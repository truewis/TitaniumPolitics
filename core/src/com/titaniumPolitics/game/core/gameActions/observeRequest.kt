package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import kotlinx.serialization.Serializable

@Serializable
class observeRequest(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    override fun execute() {
        //TODO: request to observe something.
    }

}