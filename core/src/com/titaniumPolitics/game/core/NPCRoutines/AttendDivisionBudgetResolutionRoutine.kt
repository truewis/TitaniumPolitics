package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable

@Serializable
class AttendDivisionBudgetResolutionRoutine(override val meetingName: String) : MeetingRoutine() {
    init {
        priority = PRIORITY_MEETING
    }

    override fun executeInMeeting(name: String, place: String): GameAction {
        //If not speaker, wait if the mutuality to the speaker is high. Otherwise, if possible, interrupt the speaker.
        if (meeting.currentSpeaker != name) {
            //If the meeting is not boring, but the mutuality to the speaker is low, intercept the speaker.
            return interceptCondition(name, place)
        } else {
            val party = gState.parties[meeting.involvedParty]!!

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
            val nextSpeaker = meeting.currentCharacters.minus(name)
                .maxByOrNull { gState.getMutuality(name, it) }
                ?: return EndMeeting(name, place)
            return EndSpeech(name, place, nextSpeaker, gState)
        }

        //If everything else, wait.
        return Wait(name, place)
        //TODO: do something in the meeting. Leave the meeting if nothing to do.


    }
}