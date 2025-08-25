package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Place
import com.titaniumPolitics.game.core.Resources
import kotlinx.serialization.Serializable

@Serializable
data class UnofficialResourceTransfer(
    override val sbjCharacter: String, override val tgtPlace: String,
    var toWhere: String,
    var fromHome: Boolean,
    var resources: Resources,
) : GameAction() {
    constructor(
        sbjCharacter: String, tgtPlace: String, toWhere: String,
        fromHome: Boolean,
        resources: Resources, gameState: GameState
    ) : this(sbjCharacter, tgtPlace, toWhere, fromHome, resources) {
        injectParent(gameState)
    }


    override fun execute() {
        if (fromHome) {
            if (
                resources.all { parent.characters[sbjCharacter]!!.resources[it.key] >= it.value }
            ) {
                //Transfer resources.
                resources.forEach { (key, value) ->
                    parent.characters[sbjCharacter]!!.resources[key] -= value
                    parent.places[toWhere]!!.resources[key] += value
                }

                //Do not spread rumor
            } else {
                throw Exception("Not enough resources: $tgtPlace, ${parent.places[tgtPlace]!!.resources["water"]}")
            }
        } else {

            if (
                resources.all { parent.places[tgtPlace]!!.resources[it.key] >= it.value }
            ) {
                //Transfer resources.
                resources.forEach { (key, value) ->
                    parent.places[tgtPlace]!!.resources[key] -= value
                    parent.places[toWhere]!!.resources[key] += value
                }
                //Spread rumor only when there is a character in the place. i.e. don't have to do anything here.
            } else {
                throw Exception("Not enough resources: $tgtPlace, ${parent.places[tgtPlace]!!.resources["water"]}")
            }
        }
        //If you have sent someone resources
        if (toWhere.contains("home"))
        //The mutuality from the recipient increases.
            Place.whoseHome(toWhere)
                ?.also {
                    parent.setMutuality(
                        it,
                        sbjCharacter,
                        parent.characters[it]!!.itemValue(resources),
                        "UnofficialResourceTransfer"
                    )
                }
        super.execute()

    }

    override fun isValid(): Boolean {
        //Can't send to the same place
        if (toWhere == tgtPlace) return false
        return parent.places[tgtPlace]!!.resources.contains(
            resources
        )
    }

}