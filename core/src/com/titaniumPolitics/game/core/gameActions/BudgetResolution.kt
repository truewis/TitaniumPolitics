package com.titaniumPolitics.game.core.gameActions

@Deprecated("This class is deprecated. BudgetResolution is a separate agenda item.")
class BudgetResolution(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    // If budgetResolution conference is ongoing and has the triumvirate, then the action is valid.
    override fun isValid(): Boolean =
        parent.ongoingMeetings.any { it.value.type == "budgetResolution" } &&
                parent.ongoingMeetings.filter { it.value.type == "budgetResolution" }.values.first().currentCharacters.containsAll(
                    parent.parties["triumvirate"]!!.members
                )


    override fun execute()
    {
        parent.isBudgetResolved = true

        with(parent) {
            //take the time of all characters present.
            ongoingMeetings.filter { it.value.type == "budgetResolution" }.values.first().currentCharacters.forEach { characters[it]!!.frozen++ }
            //Distribute resources according to the budget plan.
            places["reservoirNorth"]!!.resources["water"] =
                places["reservoirNorth"]!!.resources["water"]!! - budget.values.sum()

            places["farm"]!!.resources["ration"] =
                places["farm"]!!.resources["ration"]!! - budget.values.sum()

            budget.forEach {
                val guildHall = parties[it.key]!!.home
                places[guildHall]!!.resources["water"] = (places[guildHall]!!.resources["water"] ?: .0) + it.value
                places[guildHall]!!.resources["ration"] = (places[guildHall]!!.resources["ration"] ?: .0) + it.value
            }
        }


    }

}