package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable

@Serializable
class AttendMeetingRoutine() : Routine(), IMeetingRoutine
{
    //Sometimes, this routine is created before joining the meeting. In that case, the action required to join the meeting is stored here.
    var actionDelegated: GameAction? = null

    //TODO: Also check AttendConferenceRoutine for the same function.
    override fun newRoutineCondition(name: String, place: String): Routine?
    {
        val character = gState.characters[name]!!
        val meeting = character.currentMeeting!!
        //If there is a proof of work agenda about the request you have finished, support it.
        if (meeting.agendas.any { it.type == AgendaType.PROOF_OF_WORK && character.finishedRequests.contains(it.subjectParams["request"]) })
        {
            //If we haven't tried this branch in the current routine
            if (intVariables["try_support_proofOfWork"] != 1)
            {
                //If the agenda is already proposed, and we have a supporting information, support it.
                intVariables["try_support_proofOfWork"] = 1
                return SupportAgendaRoutine().apply {
                    intVariables["agendaIndex"] =
                        meeting.agendas.indexOfFirst { it.type == AgendaType.PROOF_OF_WORK }
                }
            }
        }
        return null
    }

    //TODO: Also check AttendConferenceRoutine for the same function.
    override fun execute(name: String, place: String): GameAction
    {
        if (actionDelegated != null)
        {
            val action = actionDelegated!!
            actionDelegated = null //The action is executed.
            return action
        }
        val character = gState.characters[name]!!
        val meeting = character.currentMeeting!!
        //If not speaker, wait if the mutuality to the speaker is high. Otherwise, if possible, interrupt the speaker.
        if (meeting.currentSpeaker != name)
        {
            if (gState.getMutuality(
                    name,
                    meeting.currentSpeaker
                ) > 50.0
            )
                return Wait(name, place)
            else
            {
                val action = Intercept(name, place)
                if (action.isValid())
                    return action
                return Wait(name, place)
            }
        }
        //check the subject variable to request something.
        when (variables["subject"])
        {
            "requestResource" ->
            {
                //Fill in the agenda based on variables in the routine, resource and character.
                val agenda = MeetingAgenda(AgendaType.REQUEST).apply {
                    attachedRequest = Request(
                        UnofficialResourceTransfer(
                            variables["character"]!!,
                            tgtPlace = "" /*Anywhere*/
                        ).also {
                            it.toWhere = "home_$name"//This character's home
                            it.resources = hashMapOf(
                                variables["resource"]!! to intVariables["amount"]!!
                            )
                        }//Created a command to transfer the resource.
                        , issuedTo = hashSetOf(variables["character"]!!)
                    ).apply {
                        executeTime = gState.time
                        issuedBy = hashSetOf(name)
                    }
                }
                variables["subject"] = "" //The subject is resolved.
                return NewAgenda(name, place).also {
                    it.agenda = agenda
                }
            }
        }
        return Wait(name, place)
        //TODO: do something in the meeting. Leave the meeting if nothing to do.

    }

    //TODO: Also check AttendConferenceRoutine for the same function.
    override fun endCondition(name: String, place: String): Boolean
    {
        if (actionDelegated != null)//If there is still an action to be executed from the previous routine, do not leave the routine.
        {
            return false
        }
        val character = gState.characters[name]!!
        //If the conference is over, leave the routine.
        if (character.currentMeeting == null)
        {
            return true
        }
        val conf =
            character.currentMeeting!!
        //If two hours has passed since the meeting started, leave the meeting. TODO: what if the meeting has started late?
        //TODO: stay in the meeting until I have something else to do, or the work hours are over.
        if (intVariables["routineStartTime"]!! + 4 <= gState.time)
        {
            return true
        }
        return false
    }

}