package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.MutualityMatrix
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
data class Rescue(override val sbjCharacter: String, override val tgtPlace: String, var who: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, who: String, gameState: GameState) : this(
        sbjCharacter,
        tgtPlace,
        who
    ) {
        injectParent(gameState)
    }

    override fun execute() {
        val nearestHospital = parent.places.values.filter { it.name.contains("rescueStation") }
            .minByOrNull { parent.characters[sbjCharacter]!!.place.distanceTo(it.name) ?: Int.MAX_VALUE }?.name
            ?: "tavern" //Fallback to tavern if no hospital exists.
        parent.characters[who]?.let {
            it.forceMoveToPlace(nearestHospital)
            it.health += 50.0
            Logger.write(
                "${it.name} has been rescued to $nearestHospital and recovered 50 health.",
                Logger.LogLevel.ACTION_VERBOSE
            )
        }
        super.execute()
    }

    override fun isValid(): Boolean {
        return parent.characters.containsKey(who) &&
            parent.characters[who]!!.place.name == tgtPlace &&
            parent.characters[sbjCharacter]!!.place.name == tgtPlace &&
            parent.characters[who]!!.isUnconscious &&
            parent.characters[sbjCharacter]!!.trait.contains("emt") //Only EMTs can perform rescue.
    }

    override fun deltaWill(): MutualityMatrix {
        val w = MutualityMatrix()
        // The subject gain will
        w.addWill(sbjCharacter, 5.0, "rescue")
        w.addMutuality(who, sbjCharacter, 30.0, "rescuedMe")
        return w
    }

}
