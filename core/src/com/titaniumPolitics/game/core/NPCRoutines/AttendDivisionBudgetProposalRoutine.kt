package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable
import kotlin.math.min
import kotlin.math.pow

@Serializable
class AttendDivisionBudgetProposalRoutine(override val meetingName: String) : MeetingRoutine() {
    init {
        priority = PRIORITY_MEETING
    }

    override fun executeInMeeting(name: String, place: String): GameAction {
        //If not speaker, wait if the mutuality to the speaker is high. Otherwise, if possible, interrupt the speaker.
        if (meeting.currentSpeaker != name) {
            if (routineStartTime + 7200 / ReadOnly.DT <= gState.time || meeting.currentAttention < 10)
                return LeaveMeeting(name, place)
            //If the meeting is not boring, but the mutuality to the speaker is low, intercept the speaker.
            return interceptCondition(name, place)
        } else {
            val party = gState.parties[meeting.involvedParty]!!

            //If there is no budget proposed yet, propose the budget.
            if (!party.isBudgetProposed) {
                val availableBudget = if (party.type == Party.Type.CABINET) Resources(
                    "water" to gState.places["reservoirEast"]!!.resources["water"],
                    "ration" to gState.places["farm"]!!.resources["ration"],
                    "phosphorus" to gState.places["mainControlRoom"]!!.resources["phosphorus"], positive = true
                )
                else gState.places[party.home]!!.resources
                //Propose a budget.
                val standardBudget = scaleBudget(name, party.standardBudget, availableBudget)
                return NewAgenda(name, place).also {
                    it.agenda = MeetingAgenda(
                        type = AgendaType.BUDGET_PROPOSAL,
                        author = name,
                        attachedBudget = standardBudget
                    )
                }
            }


            //If nothing else to talk about, end the speech. The next speaker is the character with the highest mutuality.
            val nextSpeaker = meeting.currentCharacters.minus(name)
                .maxByOrNull { gState.getMutuality(name, it) }
                ?: return EndMeeting(name, place)
            return EndSpeech(name, place, nextSpeaker, gState)
        }

        //If everything else, wait.
        return Wait(name, place)
        //TODO: do something in the meeting. Leave the meeting if nothing to do.


    }

    fun scaleBudget(name: String, budget: Budget, availableBudget: Resources): Budget {
        if (availableBudget.contains(budget.sum()))
            return budget //No need to scale
        while (!availableBudget.contains(budget.sum())) {
            val resourceTypeShortOf = availableBudget.keys.first {
                budget.sum()[it] > availableBudget[it]
            }
            if (availableBudget[resourceTypeShortOf] < 1e-6) //If there is no resource of this type, kill all the budget of this type.
            {
                budget.value.forEach { entry ->
                    budget.value[entry.key]!![resourceTypeShortOf] = 0.0
                }
                continue
            }
            //Cut at least 5% of the budget of this resource type.
            val cutRatio =
                min(availableBudget[resourceTypeShortOf] / budget.sum()[resourceTypeShortOf], 0.95)
            //Make list of budget parties that contains the resource type short of.
            val budgetKeysWithResourceTypeShortOf = budget.value.keys.filter {
                budget.value[it]!!.containsKey(resourceTypeShortOf)
            }
            val budgetScalersWithResourceTypeShortOf = budgetKeysWithResourceTypeShortOf.map {
                cutRatio.pow(2.0 - (gState.parties[it]!!.leader?.let { gState.getMutNorm(name, it) } ?: 0.0))
                //Multiply budget by cutRadio powers of 1-3, depending on the mutuality to the leader of the party.
            }
            budgetKeysWithResourceTypeShortOf.forEach {
                budget.value[it]!![resourceTypeShortOf] =
                    budget.value[it]!![resourceTypeShortOf] * budgetScalersWithResourceTypeShortOf[budgetKeysWithResourceTypeShortOf.indexOf(
                        it
                    )]
            }
        }
        return budget

    }
}