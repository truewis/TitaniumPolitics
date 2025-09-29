package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.MutualityMatrix
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
data class Eat(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    val amount get() = sbjCharObj.reliant * 1.0 //In kilograms, 1.0 kg per reliant.

    override fun execute() {

        sbjCharObj.resources["ration"] -= amount
        sbjCharObj.resources["water"] -= amount
        sbjCharObj.hunger -= 50
        sbjCharObj.thirst -= 50
        tgtPlaceObj.gasResources["water"] += 1.68 * amount //All hydrogen in the ration plus the liquid water consumed is counted. Look at apparatus json at farmArray.
        tgtPlaceObj.gasResources["carbonDioxide"] += amount * 1.16
        Logger.write("$sbjCharacter ate a ration and drank some water.", Logger.LogLevel.ACTION_VERBOSE)
        super.execute()
    }

    override fun isValid(): Boolean {
        return tgtPlace.contains("home") && reason(
            sbjCharObj.resources["ration"] > amount && sbjCharObj.resources["water"] > amount,
            "eat-resources"
        )
    }

    override fun deltaWill(): MutualityMatrix {
        var amount = 7.0
        if (sbjCharObj.hunger < 50)
            amount -= 5
        if (sbjCharObj.thirst < 50)
            amount -= 5
        if (sbjCharObj.trait.contains("gourmand"))
            amount += 5

        val w = MutualityMatrix()
        w.addWill(sbjCharacter, amount, "Eat")
        return w
    }

}