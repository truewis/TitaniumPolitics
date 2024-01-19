package com.titaniumPolitics.game.core.gameActions

class repair(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{
    var amount = 30
    override fun execute()
    {


        parent.places[tgtPlace]!!.apparatuses.forEach {

            if (it.durability < 100 - amount)
            {//Don't repair if it's too good
                it.durability += amount
                parent.characters[tgtCharacter]!!.frozen++
            } else
                println("$tgtCharacter tried to repair $it, but it's already too good.")
        }


    }

}