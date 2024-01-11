package com.titaniumPolitics.game.core.gameActions

class budgetResolution(override val tgtCharacter: String, override val tgtPlace: String) : GameAction() {
    override fun isValid(): Boolean =
        parent.ongoingConferences.filter { it.value.subject == "budgetProposal" }.values.first().currentCharacters.containsAll(
            listOf(
                "observer",
                "ctrler"
            )
        )&& parent.ongoingConferences.filter { it.value.subject == "budgetProposal" }.values.first().currentCharacters.any { parent.characters[it]!!.trait.contains("mechanic") }

    override fun execute() {
        parent.isBudgetResolved = true

        with(parent) {
            //Now, take the time of all characters present.
            ongoingConferences.filter { it.value.subject == "budgetProposal" }.values.first().currentCharacters.forEach { characters[it]!!.frozen++ }
            //Distribute resources according to the budget plan.
            places["reservoirNorth"]!!.resources["water"] =
                places["reservoirNorth"]!!.resources["water"]!! - budget.values.sum()

            places["farm"]!!.resources["ration"] =
                places["farm"]!!.resources["ration"]!! - budget.values.sum()

            budget.forEach {
                val guildHall = parties[it.key]!!.home;
                places[guildHall]!!.resources["water"] = (places[guildHall]!!.resources["water"]?:0) + it.value
                places[guildHall]!!.resources["ration"] = (places[guildHall]!!.resources["ration"]?:0) + it.value
            }
        }


    }

}