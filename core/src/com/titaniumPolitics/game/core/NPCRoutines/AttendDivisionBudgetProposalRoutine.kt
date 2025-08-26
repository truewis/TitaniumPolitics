package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable
import java.lang.Math.pow
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow

@Serializable
class AttendDivisionBudgetProposalRoutine(override val meetingName: String) : Routine(), IMeetingRoutine {
    init {
        priority = PRIORITY_MEETING
    }

    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        val conf =
            gState.ongoingMeetings[meetingName] ?: gState.scheduledMeetings[meetingName]
        meetingStartMethod(conf, place)?.let { return it }
        if (conf == null) return null

        return null
    }

    override fun execute(name: String, place: String): GameAction {
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting
        if (conf == null) {
            JoinMeeting(name, place).apply {
                injectParent(gState)
                if (isValid())
                    return this
            }
            StartMeeting(name, place).apply {
                injectParent(gState)
                if (isValid())
                    return this
            }
            return Wait(name, place).also {
            } //If no meeting found, wait. Note that this action is only executed once because the routine will end after this action.
            //This happens if the number of people condition of the meeting is not met.
        }
        //If not speaker, wait if the mutuality to the speaker is high. Otherwise, if possible, interrupt the speaker.
        if (conf.currentSpeaker != name) {
            //If the meeting is not boring, but the mutuality to the speaker is low, intercept the speaker.
            return interceptCondition(conf, name, place)
        } else {
            val party = gState.parties[conf.involvedParty]!!

            //If there is no budget proposed yet, propose the budget.
            if (!party.isBudgetProposed) {
                val availableBudget = if (party.type == "cabinet") Resources(
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
            val nextSpeaker = conf.currentCharacters.minus(name)
                .maxByOrNull { gState.getMutuality(name, it) }
                ?: return EndMeeting(name, place)
            return EndSpeech(name, place, nextSpeaker, gState)
        }

        //If everything else, wait.
        return Wait(name, place)
        //TODO: do something in the meeting. Leave the meeting if nothing to do.


    }

    //TODO: Also check AttendMeetingRoutine for the same function.
    override fun endCondition(name: String, place: String): Boolean {
        //If the conference is over, leave the routine. But the condition is not checked here, because the routine is not ended until the action is executed.
        //See NonPlayerAgent.selectRoutine()
        //If two hours has passed since the meeting started, leave the meeting. TODO: what if the meeting has started late?
        //TODO: stay in the meeting until I have something else to do, or the work hours are over.
        return meetingRoutineEndCondition(name, Meeting.MeetingType.DIVISION_DAILY_CONFERENCE)
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