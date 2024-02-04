package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine

class OfficialResourceTransfer(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{
    var toWhere = ""
    var what = ""
    var amount = 10
    override fun chooseParams()
    {
        toWhere = GameEngine.acquire(parent.places.keys.toList())
        what = GameEngine.acquire(parent.places[tgtPlace]!!.resources.keys.toList())
    }

    override fun execute()
    {

        if (
            (parent.places[tgtPlace]!!.resources[what] ?: 0) >= amount
        )
        {
            parent.places[tgtPlace]!!.resources[what] = (parent.places[tgtPlace]!!.resources[what] ?: 0) - amount
            parent.places[toWhere]!!.resources[what] =
                (parent.places[toWhere]!!.resources[what] ?: 0) + amount
            parent.characters[tgtCharacter]!!.frozen++

        } else
        {
            println("Not enough resources: $tgtPlace, $what, ${parent.places[tgtPlace]!!.resources[what]}")
        }

    }

    override fun isValid(): Boolean
    {
        return (parent.places[tgtPlace]!!.resources[what] ?: 0) >= amount
    }

}