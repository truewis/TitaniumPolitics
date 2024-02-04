package com.titaniumPolitics.game.core.gameActions

class BudgetProposal(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{
    val budget = hashMapOf<String, Int>()//"mainControlRoom" to 11520, "redMine" to 38400, "blackMine" to 38400,

    override fun chooseParams()
    {
        //TODO: set up the budget proposal
        parent.places.forEach {
            if (it.key == "home" || it.value.responsibleParty == "") return@forEach else budget[it.value.responsibleParty] =
                (budget[it.value.responsibleParty]
                    ?: 0) + it.value.plannedWorker * (it.value.workHoursEnd - it.value.workHoursStart) * 15
        }
    }

    override fun isValid(): Boolean =
        parent.ongoingConferences.any { it.value.subject == "budgetProposal" } and
                parent.ongoingConferences.filter { it.value.subject == "budgetProposal" }.values.first().currentCharacters.containsAll(
                    parent.parties["cabinet"]!!.members
                )

    override fun execute()
    {
        //TODO: vote on the budget proposal

        println("Budget proposal executed.")
        parent.isBudgetProposed = true
        parent.budget = budget
        //Now, take the time of all characters present.
        parent.ongoingConferences.filter { it.value.subject == "budgetProposal" }.values.first().currentCharacters.forEach { parent.characters[it]!!.frozen++ }
        println(budget)
    }

}