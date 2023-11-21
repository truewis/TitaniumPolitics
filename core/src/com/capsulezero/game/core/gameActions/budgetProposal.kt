package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameState

class budgetProposal(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {
    val budget = hashMapOf<String, Int>()//"mainControlRoom" to 11520, "redMine" to 38400, "blackMine" to 38400,

    override fun chooseParams() {
        //TODO: set up the budget proposal
        tgtState.places.forEach { if(it.key=="home")return@forEach else budget[it.key] = it.value.plannedWorker*(it.value.workHoursEnd-it.value.workHoursStart)*15 }
        }
    override fun isValid():Boolean = tgtState.ongoingConferences.filter { it.value.subject=="budgetProposal" }.values.first().currentCharacters.count()==8//TODO: cancel if not fully attended
    override fun execute() {
        //TODO: vote on the budget proposal

        println("Budget proposal executed.")
        tgtState.isBudgetProposed = true
        tgtState.budget = budget
        //Now, take the time of all characters present.
        tgtState.ongoingConferences.filter { it.value.subject=="budgetProposal" }.values.first().currentCharacters.forEach { tgtState.characters[it]!!.frozen++ }
        println(budget)
    }

}