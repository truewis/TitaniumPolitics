package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.MutualityMatrix
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
data class BuyDrink(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    override fun execute() {

        sbjCharObj.resources["phosphorus"] -= 1e-3
        sbjCharObj.thirst -= 20
        Logger.write("$sbjCharacter got a drink", Logger.LogLevel.ACTION_VERBOSE)
        super.execute()
    }

    override fun isValid(): Boolean {
        if (tgtPlace != "tavern") return false
        return reason(
            sbjCharObj.resources["phosphorus"] > 1e-3,
            "buyDrink-resources"
        )
    }

    override fun deltaWill(): MutualityMatrix {
        val w = MutualityMatrix()
        val amount = if (sbjCharObj.trait.contains("gourmand")) 15.0 else 10.0
        w.addWill(sbjCharacter, amount, "BuyDrink")
        return w
    }

}