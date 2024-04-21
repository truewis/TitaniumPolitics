package com.titaniumPolitics.game.core.gameActions

import kotlinx.serialization.Serializable

@Serializable
class Eat(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{

    override fun execute()
    {
        if ((parent.characters[tgtCharacter]!!.resources["ration"]
                ?: 0) > 0 && (parent.characters[tgtCharacter]!!.resources["water"] ?: 0) > 0
        )
        {
            parent.characters[tgtCharacter]!!.resources["ration"] =
                parent.characters[tgtCharacter]!!.resources["ration"]!! - 1
            parent.characters[tgtCharacter]!!.resources["water"] =
                parent.characters[tgtCharacter]!!.resources["water"]!! - 1
            parent.setMutuality(tgtCharacter, tgtCharacter, 10.0)//Increase will.
            parent.characters[tgtCharacter]!!.hunger -= 50
            parent.characters[tgtCharacter]!!.thirst -= 50
            println("$tgtCharacter ate a ration and drank some water.")
        } else
        {
            println("$tgtCharacter tried to eat, but there is nothing to eat.")
        }
        super.execute()
    }

    override fun isValid(): Boolean
    {
        //TODO: Check if the character is in a place where it can eat, and has the resources to eat.
        return tgtPlace.contains("home") && (parent.characters[tgtCharacter]!!.resources["ration"]
            ?: 0) > 0 && (parent.characters[tgtCharacter]!!.resources["water"] ?: 0) > 0
    }

}