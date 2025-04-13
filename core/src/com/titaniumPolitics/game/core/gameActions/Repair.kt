package com.titaniumPolitics.game.core.gameActions

import kotlinx.serialization.Serializable

@Serializable
class Repair(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    var amount = 30
    override fun execute()
    {


        parent.places[tgtPlace]!!.apparatuses.forEach {

            if (it.durability < 100 - amount)
            {//Don't repair if it's too good
                it.durability += amount
            } else
                println("$sbjCharacter tried to repair $it, but it's already too good.")
        }
        super.execute()

    }

    override fun isValid(): Boolean
    {
        return parent.places[tgtPlace]!!.apparatuses.isNotEmpty() && parent.characters[sbjCharacter]!!.trait.contains("technician")
    }

}