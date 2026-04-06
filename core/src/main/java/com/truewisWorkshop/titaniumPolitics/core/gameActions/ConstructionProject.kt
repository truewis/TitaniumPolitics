package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.Apparatus
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

/**
 * An engineer constructs a new piece of transport infrastructure (a pipe, power line, railway, etc.)
 * at a corridor place.
 *
 * Prerequisites:
 * - The subject must have the "engineer" trait.
 * - The target place must be a corridor (name contains "corridor").
 * - The target place must have enough available cross-sectional space for the new apparatus.
 * - The target place (or its responsible division's stockpile) must hold enough resources
 *   equivalent to the level-3 (most expensive) repair cost of the apparatus being built.
 *
 * On execution:
 * - Resources are deducted from the target place.
 * - The apparatus is added at 100% durability.
 * - Duration is 12 hours (720 minutes, see ConstructionProjectDuration in consts.json).
 *
 * @param apparatusName  The name key from apparatus.json (e.g. "powerLineI", "liquidPipeII").
 * @param parameters     Per-instance parameters (e.g. "resourceFilter" = "water" for a water pipe).
 */
@Serializable
data class ConstructionProject(
    override val sbjCharacter: String,
    override val tgtPlace: String,
    var apparatusName: String,
    var parameters: HashMap<String, String> = hashMapOf()
) : GameAction() {

    constructor(
        sbjCharacter: String, tgtPlace: String,
        apparatusName: String, parameters: HashMap<String, String>, gameState: GameState
    ) : this(sbjCharacter, tgtPlace, apparatusName, parameters) {
        injectParent(gameState)
    }

    /** A temporary Apparatus instance used only for property look-ups (not added to the world). */
    private fun protoApparatus(): Apparatus = Apparatus().apply { name = apparatusName }

    override fun execute() {
        val proto = protoApparatus()
        val repairCost = proto.requiredResourcePerRepair.lastOrNull() ?: return
        tgtPlaceObj.resources -= repairCost

        val newApp = Apparatus().apply {
            name = apparatusName
            durability = ReadOnly.const("DurabilityMax")
            ID = "${apparatusName}_${tgtPlace}_${System.currentTimeMillis()}"
            this.parameters.putAll(this@ConstructionProject.parameters)
        }
        tgtPlaceObj.apparatuses.add(newApp)
        super.execute()
    }

    override fun isValid(): Boolean {
        if (!reason(sbjCharObj.trait.contains("engineer"), "constructionProject-notEngineer")) return false
        if (!reason(tgtPlace.contains("corridor"), "constructionProject-notCorridor")) return false

        val proto = protoApparatus()

        // Space check
        val space = tgtPlaceObj.availableSpace
        if (!reason(
                space == null || space >= proto.spaceConsumption,
                "constructionProject-noSpace"
            )
        ) return false

        // Resource check (level-3 repair cost)
        val repairCost = proto.requiredResourcePerRepair.lastOrNull() ?: return true
        return reason(tgtPlaceObj.resources.contains(repairCost), "constructionProject-noResources")
    }

    override fun isProofOfWork(info: Information): Boolean {
        return super.isProofOfWork(info) || (info.action is ConstructionProject &&
            (info.action as ConstructionProject).let {
                it.tgtPlace == this.tgtPlace && it.apparatusName == this.apparatusName
            })
    }
}
