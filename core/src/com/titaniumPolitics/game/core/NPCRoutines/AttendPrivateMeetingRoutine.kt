package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable

/**
 * A routine that attends a private meeting.
 * If the meeting is scheduled, the character will move to the place of the meeting and wait for it to start.
 * If the meeting is not scheduled, the character will try to find the character to meet and start talking.
 * If the character is already in a meeting, the character will try to participate in the meeting.
 * This is only meeting routine that can be used for any meeting type.
 * The routine ends when the meeting ends or the character leaves the meeting.
 * If the meeting is scheduled, but does not exist, the routine fails.
 * If the character cannot find the character to meet, the routine fails.
 */
@Serializable
class AttendPrivateMeetingRoutine(
    val toWho: String? = null, val agenda: MeetingAgenda? = null,
    var scheduledMeetingName: String? = null
) : MeetingRoutine() {
    private var hasUnresolvedAgenda = agenda != null
    override val meetingName: String
        get() = scheduledMeetingName
            ?: gState.ongoingMeetings.filter { toWho in it.value.currentCharacters }.keys.firstOrNull()
            ?: "NonExistentMeeting" //The meeting does not exist yet, so newRoutineCondition will return null.

    override fun newIMeetingRoutineCondition(
        name: String,
        place: String,
        subroutines: List<Routine>
    ): IMeetingRoutine? {
        supportProofOfWork(name)?.let { return it }
        return null
    }

    private fun condition(name: String): Boolean {
        val currentMeeting = gState.characters[name]!!.currentMeeting
        if (currentMeeting == null) {
            return hasAttended //The routine should end iff the meeting has finished.
        } else {
            if (toWho?.let { it !in currentMeeting.currentCharacters } == true) {
                return true //The character has been transferred to another meeting.
            }
            if (scheduledMeetingName?.let { it != gState.meetingName(currentMeeting) }
                    ?: false)//The character has been transferred to another meeting, if there was a scheduled meeting name provided.
                return true
            //if (meeting.time + 1800 / ReadOnly.DT >= gState.time) false //For talks, we don't wait until the meeting has happened for 30 minutes.
            return false

        }
    }

    override fun meetingControl(name: String, place: String): Routine? {
        //////////////////////Routine End Condition Check/////////////////////////
        if (condition(name)) {
            if (hasAttended && !hasUnresolvedAgenda) return success() else return failed()
        }
        //////////////////////////////////////////////////////////////////////////
        //If there is no ongoing meeting, check if there is a scheduled meeting with the specified name or the character to meet.
        //If neither is specified, this routine fails.
        if (gState.characters[name]!!.currentMeeting == null) {
            if (scheduledMeetingName != null) {
                if (gState.scheduledMeetings[scheduledMeetingName] == null) {
                    return failed()
                    //Scheduled meeting name exists but the meeting does not exist. Either the meeting was cancelled or it is already over. Fail the routine.
                }
                val mtPlace = gState.scheduledMeetings[scheduledMeetingName]!!.place //
                return if (mtPlace != place)
                    MoveRoutine(mtPlace)
                else null //Turn to execute
            } else if (toWho != null) {
                //Check if I am in the same place as the character to meet.
                return if (gState.characters[toWho]!!.place.name != place)
                    FindCharacterRoutine(toWho)
                else null //Turn to execute
            }
            throw Exception("Either scheduledMeetingName or toWho must be provided.")
        }
        return null
    }

    override fun executeInMeeting(name: String, place: String): GameAction {
        if (meeting.currentSpeaker != name) {
            if (sharedMeetingEndCondition()) //Leave the meeting if it is boring or it is getting too long.
                return LeaveMeeting(name, place)
            return interceptCondition(name, place)
        } else {
            //If it is my turn to speak
            //Check if I had an intention
            if (agenda != null && hasUnresolvedAgenda) {
                NewAgenda(name, place, gState).also {
                    it.agenda = agenda
                    if (it.isValid()) {
                        hasUnresolvedAgenda = false
                        println("$name proposed an agenda: ${it.agenda}")
                        return it
                    }
                }
            } else {
                //No particular intention

                //0. Execute a command if there is any. Here, we can move to the place actively if the command is not in the current place.
                //If there is a command that is within the set time window, issued party is trusted enough, and seems to be executable at some place(AvailableActions), start execution routine.
                //Note that the command may not be valid even if it in AvailableActions list. For example, if the character is already at the place, move command is not valid.
                executeRequestInMeeting(name, place)?.let { return it }

                proposeProofOfWork(name, place)?.let { return it }

                //If there is a new request issued to me, and there is no matching request from me, try to match it.
                val reqAgendas = meeting.agendas.filter { it.type == AgendaType.REQUEST }
                if (reqAgendas.any { name in it.attachedRequest!!.issuedTo } && reqAgendas.none { name in it.attachedRequest!!.issuedBy }) {
                    matchRequests(name, place)?.let { return it }
                }
                gossip(this.gState, name, place)?.also { return it }
            }

            endMeetingIfLowAttention(name, place)?.let { return it }
            //If nothing else to talk about, end the speech. The next speaker is the character with the highest mutuality.
            return EndSpeech(
                name, place, meeting.currentCharacters.minus(name)
                    .maxByOrNull { gState.getMutuality(name, it) }!!
            )

        }


    }

    override fun joinMeetingActions(name: String, place: String): GameAction? {
        toWho?.run {
            //Note: This character can interfere with the meeting if it is already ongoing.
            Talk(name, place, this).apply {
                injectParent(gState)
                if (isValid()) {
                    hasAttended = true
                    return this
                }
            }
        }

        JoinMeeting(name, place).apply {
            injectParent(gState)
            if (isValid()) {
                hasAttended = true
                return this
            }
        }
        StartMeeting(name, place).apply {
            injectParent(gState)
            if (isValid()) {
                hasAttended = true
                return this
            }
        }

        //This happens if the number of people condition of the meeting is not met.
        return Wait(name, place)
    }

    companion object {
        fun gossip(gState: GameState, name: String, place: String): GameAction? {
            //Criticize the enemy. It is determined by individual mutuality.
            val enemy = gState.characters.minBy { ch ->
                gState.getMutuality(
                    name,
                    ch.key
                )
            }
            if (gState.getMutuality(
                    name,
                    enemy.key
                ) < ReadOnly.const("EnemyMutualityThreshold")
            )
                NewAgenda(name, place, gState).also { action ->
                    action.agenda = MeetingAgenda(AgendaType.DENOUNCE, name).also {
                        it.subjectParams["character"] = enemy.key
                    }
                    if (action.isValid()) {
                        return action
                    }
                }

            //Praise the friend.
            //Criticize the enemy. It is determined by individual mutuality.
            val friend = gState.characters.maxBy { ch ->
                gState.getMutuality(
                    name,
                    ch.key
                )
            }
            if (gState.getMutuality(
                    name,
                    friend.key
                ) > ReadOnly.const("FriendMutualityThreshold")
            )
                NewAgenda(name, place, gState).also { action ->
                    action.agenda = MeetingAgenda(AgendaType.PRAISE, name).also {
                        it.subjectParams["character"] = friend.key
                    }
                    if (action.isValid()) {
                        return action
                    }
                }
            return null
        }
    }
}