package com.titaniumPolitics.game.core.gameActions

import kotlinx.serialization.Serializable

@Serializable
class Eat(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{

    override fun execute()
    {
        if ((parent.characters[sbjCharacter]!!.resources["ration"]
                ?: .0) > 0 && (parent.characters[sbjCharacter]!!.resources["water"] ?: .0) > 0
        )
        {
            parent.characters[sbjCharacter]!!.resources["ration"] =
                parent.characters[sbjCharacter]!!.resources["ration"]!! - 1
            parent.characters[sbjCharacter]!!.resources["water"] =
                parent.characters[sbjCharacter]!!.resources["water"]!! - 1
            parent.setMutuality(sbjCharacter, sbjCharacter, 10.0)//Increase will.
            parent.characters[sbjCharacter]!!.hunger -= 50
            parent.characters[sbjCharacter]!!.thirst -= 50
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
        return tgtPlace.contains("home") && (parent.characters[sbjCharacter]!!.resources["ration"]
            ?: .0) > 0 && (parent.characters[sbjCharacter]!!.resources["water"] ?: .0) > 0
    }

    override fun deltaWill(): Double
    {
        var w = super.deltaWill()
        if (parent.characters[sbjCharacter]!!.hunger < 50)
            w -= 5
        if (parent.characters[sbjCharacter]!!.thirst < 50)
            w -= 5
        if (parent.characters[sbjCharacter]!!.trait.contains("gourmand"))
            w += 10
        return w
    }

}