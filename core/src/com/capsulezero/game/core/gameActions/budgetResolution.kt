package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameState

class budgetResolution(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(
    targetState, targetCharacter,
    targetPlace
) {
    override fun isValid(): Boolean =
        tgtState.ongoingConferences.filter { it.value.subject == "budgetProposal" }.values.first().currentCharacters.containsAll(
            listOf(
                "observer",
                "ctrler"
            )
        )&& tgtState.ongoingConferences.filter { it.value.subject == "budgetProposal" }.values.first().currentCharacters.any { tgtState.characters[it]!!.trait.contains("mechanic") }

    override fun execute() {
        tgtState.isBudgetResolved = true

        with(tgtState) {
            //Now, take the time of all characters present.
            ongoingConferences.filter { it.value.subject == "budgetProposal" }.values.first().currentCharacters.forEach { characters[it]!!.frozen++ }
            //Distribute resources according to the budget plan.
            places["reservoirNorth"]!!.resources["water"] =
                places["reservoirNorth"]!!.resources["water"]!! - budget.values.sum()

            budget.forEach {
                val guildHall = parties[it.key]!!.home;
                places[guildHall]!!.resources["water"] = (places[guildHall]!!.resources["water"]?:0) + it.value
            }
        }


    }

}