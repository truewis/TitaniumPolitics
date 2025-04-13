package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import kotlinx.serialization.Serializable

@Serializable
class OfficialResourceTransfer(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    var toWhere = ""
    var resources = hashMapOf<String, Int>()

    override fun execute()
    {

        if (
            resources.all { (parent.places[tgtPlace]!!.resources[it.key] ?: 0) >= it.value }
        )
        {
            //Transfer resources.
            resources.forEach { (key, value) ->
                parent.places[tgtPlace]!!.resources[key] = (parent.places[tgtPlace]!!.resources[key] ?: 0) - value
                parent.places[toWhere]!!.resources[key] = (parent.places[toWhere]!!.resources[key] ?: 0) + value
            }


        } else
        {
            println("Not enough resources: $tgtPlace, $resources")
        }
        super.execute()

    }

    override fun isValid(): Boolean
    {
        return resources.all { (parent.places[tgtPlace]!!.resources[it.key] ?: 0) >= it.value }
    }

}