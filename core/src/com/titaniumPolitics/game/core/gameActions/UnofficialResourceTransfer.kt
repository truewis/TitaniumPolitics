package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.Information

class UnofficialResourceTransfer(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{
    var resources = hashMapOf<String, Int>()
    var toWhere = ""

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
            parent.characters[tgtCharacter]!!.frozen++
            //Spread rumor only when someone sees it
            if (parent.places[tgtPlace]!!.currentWorker != 0)
            {
                Information(
                    author = "",
                    creationTime = parent.time,
                    type = "action",
                    tgtPlace = tgtPlace,
                    resources = resources,
                    action = "unofficialResourceTransfer"
                )/*spread rumor in the responsible party*/.also {
                    parent.informations[it.generateName()] =
                        it
                    it.publicity[parent.places[tgtPlace]!!.responsibleParty] = 1
                }
            }
        } else
        {
            println("Not enough resources: $tgtPlace, ${parent.places[tgtPlace]!!.resources["water"]}")
        }

    }

    override fun isValid(): Boolean
    {
        return resources.all { (parent.places[tgtPlace]!!.resources[it.key] ?: 0) >= it.value }
    }

}