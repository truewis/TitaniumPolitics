package com.titaniumPolitics.game.core.gameActions

import kotlinx.serialization.Serializable

@Serializable
class Sleep(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{

    override fun execute()
    {
        println("$tgtCharacter slept.")
        if (parent.characters[tgtCharacter]!!.trait.contains("old"))
            parent.characters[tgtCharacter]!!.health += 40
        else
            parent.characters[tgtCharacter]!!.health += 50
        super.execute()
    }

    override fun isValid(): Boolean
    {
        return tgtPlace == "home_$tgtCharacter"
    }

    override fun deltaWill(): Double
    {
        var w = super.deltaWill()
        if (parent.characters[tgtCharacter]!!.health < 50)
            w -= 5
        if (parent.characters[tgtCharacter]!!.trait.contains("old"))
            w += 10
        return w
    }

}