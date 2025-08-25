package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import kotlinx.serialization.Serializable

@Serializable
class UseItem(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    override fun execute() {
        //TODO: use item from the inventory.
    }

}