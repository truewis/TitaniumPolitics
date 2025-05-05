package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Sleep(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{

    override fun execute()
    {
        println("$sbjCharacter slept.")
        if (parent.characters[sbjCharacter]!!.trait.contains("old"))
            parent.characters[sbjCharacter]!!.health += 40
        else
            parent.characters[sbjCharacter]!!.health += 50
        super.execute()
    }

    override fun isValid(): Boolean
    {
        return tgtPlace == "home_$sbjCharacter"
    }

    override fun deltaWill(): Double
    {
        var w = super.deltaWill()
        if (parent.characters[sbjCharacter]!!.health < ReadOnly.const("CriticalHealth"))
            w += 10
        if (parent.characters[sbjCharacter]!!.trait.contains("old"))
            w += 5
        if (sbjCharObj.hunger > 50)
            w -= 5
        if (sbjCharObj.thirst > 50)
            w -= 5
        return w
    }

}