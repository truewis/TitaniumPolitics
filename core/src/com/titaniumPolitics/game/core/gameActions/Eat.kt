package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
class Eat(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    val amount get() = sbjCharObj.reliant * 1.0

    override fun execute() {

        sbjCharObj.resources["ration"] -= amount
        sbjCharObj.resources["water"] -= amount
        sbjCharObj.hunger -= 50
        sbjCharObj.thirst -= 50
        tgtPlaceObj.gasResources["water"] += amount * 3.0//TODO: Calculate the amount of gas from Digestion
        Logger.write("$sbjCharacter ate a ration and drank some water.", Logger.LogLevel.ACTION_VERBOSE)
        super.execute()
    }

    override fun isValid(): Boolean {
        return tgtPlace.contains("home") && reason(
            sbjCharObj.resources["ration"] > amount && sbjCharObj.resources["water"] > amount,
            "eat-resources"
        )
    }

    override fun deltaWill(): Double {
        var w = super.deltaWill()
        w += 7
        if (sbjCharObj.hunger < 50)
            w -= 5
        if (sbjCharObj.thirst < 50)
            w -= 5
        if (sbjCharObj.trait.contains("gourmand"))
            w += 5
        return w
    }

}