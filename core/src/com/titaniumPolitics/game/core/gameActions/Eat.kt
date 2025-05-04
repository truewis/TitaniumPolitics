package com.titaniumPolitics.game.core.gameActions

import kotlinx.serialization.Serializable

@Serializable
class Eat(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{

    override fun execute()
    {
        val amount = sbjCharObj.reliant * 1.0
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
        //TODO: Check if the character is in a place where it can eat, and has the resources to eat.
        return tgtPlace.contains("home") && sbjCharObj.resources["ration"] > 0 && sbjCharObj.resources["water"] > 0
    }

    override fun deltaWill(): Double
    {
        var w = super.deltaWill()
        if (sbjCharObj.hunger < 50)
            w -= 5
        if (sbjCharObj.thirst < 50)
            w -= 5
        if (sbjCharObj.trait.contains("gourmand"))
            w += 10
        return w
    }

}