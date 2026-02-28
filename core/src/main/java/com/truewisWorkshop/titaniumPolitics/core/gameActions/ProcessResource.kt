package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.MutualityMatrix
import com.titaniumPolitics.game.core.Resources
import kotlinx.serialization.Serializable

/**
 * An action for store processors (cook, jeweler, chemist) to convert raw ingredients
 * into a luxury product at their store. Requires a specific trait and takes a long time.
 */
@Serializable
data class ProcessResource(
    override val sbjCharacter: String,
    override val tgtPlace: String,
    val inputResources: Resources,
    val outputResources: Resources,
) : GameAction() {
    constructor(
        sbjCharacter: String,
        tgtPlace: String,
        inputResources: Resources,
        outputResources: Resources,
        gameState: GameState,
    ) : this(sbjCharacter, tgtPlace, inputResources, outputResources) {
        injectParent(gameState)
    }

    /** The trait required to perform this processing action. */
    val requiredTrait: String
        get() = when {
            outputResources.containsKey("fineFood") -> "cook"
            outputResources.containsKey("diamond") -> "jeweler"
            outputResources.containsKey("ammonia") -> "chemist"
            else -> ""
        }

    override fun execute() {
        tgtPlaceObj.resources -= inputResources
        tgtPlaceObj.resources += outputResources
        super.execute()
    }

    override fun isValid(): Boolean {
        if (!reason(requiredTrait.isNotEmpty(), "processResource-unknownProduct")) return false
        if (!reason(sbjCharObj.trait.contains(requiredTrait), "processResource-missingTrait")) return false
        return reason(tgtPlaceObj.resources.contains(inputResources), "processResource-insufficientIngredients")
    }

    override fun deltaWill(): MutualityMatrix {
        val w = MutualityMatrix()
        w.addWill(sbjCharacter, 5.0, "ProcessResource")
        return w
    }
}
