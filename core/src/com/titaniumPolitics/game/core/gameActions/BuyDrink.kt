package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
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
        return reason(
            sbjCharObj.resources["phosphorus"] > 1e-3,
            "buyDrink-resources"
        )
    }

    override fun deltaWill(): Double {
        var w = super.deltaWill()
        w += 10
        if (sbjCharObj.trait.contains("gourmand"))
            w += 5
        return w
    }

}