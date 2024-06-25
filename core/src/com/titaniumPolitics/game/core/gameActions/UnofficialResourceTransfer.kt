package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.Information
import kotlinx.serialization.Serializable

@Serializable
class UnofficialResourceTransfer(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{
    var resources = hashMapOf<String, Int>()
    var toWhere = ""
    var fromHome = false

    override fun execute()
    {
        if (fromHome)
        {
            if (
                resources.all { (parent.places["home_$tgtCharacter"]!!.resources[it.key] ?: 0) >= it.value }
            )
            {
                //Transfer resources.
                resources.forEach { (key, value) ->
                    parent.places["home_$tgtCharacter"]!!.resources[key] =
                        (parent.places["home_$tgtCharacter"]!!.resources[key] ?: 0) - value
                    parent.places[toWhere]!!.resources[key] = (parent.places[toWhere]!!.resources[key] ?: 0) + value
                }
                //Do not spread rumor
            } else
            {
                throw Exception("Not enough resources: $tgtPlace, ${parent.places[tgtPlace]!!.resources["water"]}")
            }
        } else
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
                throw Exception("Not enough resources: $tgtPlace, ${parent.places[tgtPlace]!!.resources["water"]}")
            }
        }
        super.execute()

    }

    override fun isValid(): Boolean
    {
        return resources.all { (parent.places[tgtPlace]!!.resources[it.key] ?: 0) >= it.value } &&
                //either fromHome is true, or tgtPlace must be managed by tgtCharacter's party.
                (fromHome || parent.parties[parent.places[tgtPlace]!!.responsibleParty]!!.members.contains(tgtCharacter))
    }

}