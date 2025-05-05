package com.titaniumPolitics.game.core.gameActions

import kotlinx.serialization.Serializable

@Serializable
class Eat(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    val amount get() = sbjCharObj.reliant * 1.0

    override fun execute()
    {

        if (sbjCharObj.resources["ration"] > amount && sbjCharObj.resources["water"] > amount
        )
        {
            sbjCharObj.resources["ration"] -= amount
            sbjCharObj.resources["water"] -= amount
            sbjCharObj.hunger -= 50
            sbjCharObj.thirst -= 50
            tgtPlaceObj.gasResources["water"] += amount * 3.0//TODO: Calculate the amount of gas from Digestion
            println("$sbjCharacter ate a ration and drank some water.")
        } else
        {
            println("$sbjCharacter tried to eat, but there is nothing to eat.")
        }
        super.execute()
    }

    override fun isValid(): Boolean
    {
        return tgtPlace.contains("home") && sbjCharObj.resources["ration"] > amount && sbjCharObj.resources["water"] > amount
    }

    override fun deltaWill(): Double
    {
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