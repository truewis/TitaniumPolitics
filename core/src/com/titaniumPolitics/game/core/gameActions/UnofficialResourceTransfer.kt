package com.titaniumPolitics.game.core.gameActions

import kotlinx.serialization.Serializable

@Serializable
class UnofficialResourceTransfer(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    var resources = hashMapOf<String, Double>()
    var toWhere = ""
    var fromHome = false

    override fun execute()
    {
        if (fromHome)
        {
            if (
                resources.all { parent.characters[sbjCharacter]!!.resources[it.key] >= it.value }
            )
            {
                //Transfer resources.
                resources.forEach { (key, value) ->
                    parent.characters[sbjCharacter]!!.resources[key] -= value
                    parent.places[toWhere]!!.resources[key] += value
                }
                //Do not spread rumor
            } else
            {
                throw Exception("Not enough resources: $tgtPlace, ${parent.places[tgtPlace]!!.resources["water"]}")
            }
        } else
        {

            if (
                resources.all { parent.places[tgtPlace]!!.resources[it.key] >= it.value }
            )
            {
                //Transfer resources.
                resources.forEach { (key, value) ->
                    parent.places[tgtPlace]!!.resources[key] -= value
                    parent.places[toWhere]!!.resources[key] += value
                }
                //Spread rumor only when there is a character in the place. i.e. don't have to do anything here.
            } else
            {
                throw Exception("Not enough resources: $tgtPlace, ${parent.places[tgtPlace]!!.resources["water"]}")
            }
        }
        super.execute()

    }

    override fun isValid(): Boolean
    {
        return ((fromHome && resources.all {
            parent.characters[sbjCharacter]!!.resources[it.key] >= it.value
        }) || resources.all { parent.places[tgtPlace]!!.resources[it.key] >= it.value }) &&
                //either fromHome is true, or tgtPlace must be managed by tgtCharacter's party.
                (fromHome || (parent.parties[parent.places[tgtPlace]!!.responsibleParty]?.members?.contains(sbjCharacter)
                    ?: false))
    }

}