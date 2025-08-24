package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable

@Serializable
class AttendDivisionBudgetResolutionRoutine : Routine(), IMeetingRoutine {
    init {
        priority = PRIORITY_MEETING
    }

    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting ?: return null
        check(conf.type == Meeting.MeetingType.BUDGET_RESOLUTION) {
            "AttendDivisionMeetingRoutine can only be used for budget resolution, but got ${conf.type}"
        }

        return null
    }

    override fun execute(name: String, place: String): GameAction {
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting
        if (conf == null) {
            JoinMeeting(name, place).apply {
                injectParent(gState)
                meetingName =
                    gState.ongoingMeetings.filter { it.value.type == Meeting.MeetingType.BUDGET_PROPOSAL && it.value.place == place }
                        .keys.firstOrNull()
                        ?: return@apply
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

            //If I am the leader, and the budget is not resolved, resolve it.
            if (party.leader == name && !party.isBudgetResolved) {
                //Filter proposed budgets that can be resolved.
                val validProposedBudgets = party.proposedBudgets.filter { entry ->
                    NewAgenda(name, place).also {
                        it.agenda = MeetingAgenda(
                            type = AgendaType.BUDGET_RESOLUTION,
                            author = name
                        ).also {
                            it.subjectParams["whoseProposal"] = entry.key
                        }
                        it.injectParent(gState)
                    }.isValid()
                }
                if (!validProposedBudgets.isEmpty()) {
                    //Pick the first budget from the valid proposed budgets.
                    val whoseBudgetToResolve = validProposedBudgets.maxBy {
                        //Pick the budget from the person I have the highest mutuality with.
                        gState.getMutuality(name, it.key)
                    }
                    return NewAgenda(name, place).also {
                        it.agenda = MeetingAgenda(
                            type = AgendaType.BUDGET_RESOLUTION,
                            author = name
                        ).also {
                            it.subjectParams["whoseProposal"] = whoseBudgetToResolve.key
                        }
                    }
                }

            }


            //If nothing else to talk about, end the speech. The next speaker is the character with the highest mutuality.
            val nextSpeaker = conf.currentCharacters.minus(name)
                .maxByOrNull { gState.getMutuality(name, it) }
                ?: return EndMeeting(name, place)
            return EndSpeech(name, place).also {
                it.nextSpeaker = nextSpeaker
            }
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
}