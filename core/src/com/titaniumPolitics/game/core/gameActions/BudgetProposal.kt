package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.Meeting

@Deprecated("This class is deprecated. BudgetProposal is a separate agenda item.")
class BudgetProposal(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    val budget = hashMapOf<String, Double>()//"mainControlRoom" to 11520, "redMine" to 38400, "blackMine" to 38400,

    override fun chooseParams() {
        //TODO: set up the budget proposal
        parent.places.forEach {
            if (it.key == "home" || it.value.responsibleDivision == "") return@forEach else budget[it.value.responsibleDivision] =
                (budget[it.value.responsibleDivision]
                    ?: .0) + it.value.plannedWorker * (it.value.workHoursEnd - it.value.workHoursStart) * 15.0
        }
    }

    override fun isValid(): Boolean =
        parent.ongoingMeetings.any { it.value.type == Meeting.MeetingType.BUDGET_PROPOSAL } and
                parent.ongoingMeetings.filter { it.value.type == Meeting.MeetingType.BUDGET_PROPOSAL }.values.first().currentCharacters.containsAll(
                    parent.parties["cabinet"]!!.members
                )

    override fun execute() {
        //TODO: vote on the budget proposal

        println("Budget proposal executed.")
        parent.isBudgetProposed = true
        parent.budget = budget
        //Now, take the time of all characters present.
        parent.ongoingMeetings.filter { it.value.type == Meeting.MeetingType.BUDGET_PROPOSAL }.values.first().currentCharacters.forEach { parent.characters[it]!!.frozen++ }
        println(budget)
    }

}