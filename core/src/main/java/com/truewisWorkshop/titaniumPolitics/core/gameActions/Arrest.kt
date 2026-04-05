package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
data class Arrest(override val sbjCharacter: String, override val tgtPlace: String, var who: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, who: String, gameState: GameState) : this(
        sbjCharacter,
        tgtPlace,
        who
    ) {
        injectParent(gameState)
    }

    override fun execute() {
        val nearestStation = parent.places.values
            .filter { it.name.contains("rescueStation") }
            .minByOrNull { parent.places[tgtPlace]!!.distanceTo(it.name) ?: Int.MAX_VALUE }?.name
            ?: "tavern" // Fallback if no rescue station exists.
        val distance = parent.places[tgtPlace]!!.distanceTo(nearestStation) ?: 1
        parent.characters[who]?.let {
            it.forceMoveToPlace(nearestStation)
            Logger.write(
                "${it.name} has been arrested and moved to $nearestStation.",
                Logger.LogLevel.ACTION_VERBOSE
            )
        }
        sbjCharObj.forceMoveToPlace(nearestStation)
        sbjCharObj.frozen += ReadOnly.constInt("MoveDuration") * distance
    }

    override fun isValid(): Boolean {
        return parent.characters.containsKey(who) &&
            parent.characters[who]!!.place.name == tgtPlace &&
            parent.characters[sbjCharacter]!!.place.name == tgtPlace &&
            !parent.characters[who]!!.isUnconscious &&
            parent.characters[sbjCharacter]!!.trait.contains("soldier") &&
            sbjCharacter in (parent.parties["safety"]?.members ?: emptySet())
    }
}