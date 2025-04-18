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
                resources.all { (parent.characters[sbjCharacter]!!.resources[it.key] ?: .0) >= it.value }
            )
            {
                //Transfer resources.
                resources.forEach { (key, value) ->
                    parent.characters[sbjCharacter]!!.resources[key] =
                        (parent.characters[sbjCharacter]!!.resources[key] ?: .0) - value
                    parent.places[toWhere]!!.resources[key] = (parent.places[toWhere]!!.resources[key] ?: .0) + value
                }
                //Do not spread rumor
            } else
            {
                throw Exception("Not enough resources: $tgtPlace, ${parent.places[tgtPlace]!!.resources["water"]}")
            }
        } else
        {

            if (
                resources.all { (parent.places[tgtPlace]!!.resources[it.key] ?: .0) >= it.value }
            )
            {
                //Transfer resources.
                resources.forEach { (key, value) ->
                    parent.places[tgtPlace]!!.resources[key] = (parent.places[tgtPlace]!!.resources[key] ?: .0) - value
                    parent.places[toWhere]!!.resources[key] = (parent.places[toWhere]!!.resources[key] ?: .0) + value
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
            (parent.characters[sbjCharacter]!!.resources[it.key] ?: .0) >= it.value
        }) || resources.all { (parent.places[tgtPlace]!!.resources[it.key] ?: .0) >= it.value }) &&
                //either fromHome is true, or tgtPlace must be managed by tgtCharacter's party.
                (fromHome || (parent.parties[parent.places[tgtPlace]!!.responsibleParty]?.members?.contains(sbjCharacter)
                    ?: false))
    }

}