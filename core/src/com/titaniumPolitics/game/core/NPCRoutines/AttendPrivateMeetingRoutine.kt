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
) : Routine(),
    IMeetingRoutine {
    var newMeetingName: String = ""
    private var startTalking = false
    override val meetingName: String get() = scheduledMeetingName ?: newMeetingName

    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        val conf =
            gState.ongoingMeetings[meetingName]
        //If there is no ongoing meeting, check if there is a scheduled meeting with the specified name or the character to meet.
        //If neither is specified, this routine fails.
        if (conf == null) {
            if (scheduledMeetingName != null) {
                if (gState.scheduledMeetings[scheduledMeetingName] == null) {
                    failed = true
                    return null
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
            failed = true
            return null
        } else {
            supportProofOfWork(conf, name)?.let { return it }
            return null
        }
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
            toWho?.run {
                startTalking = true
                //Note: This character can interfere with the meeting if it is already ongoing.
                Talk(name, place, this).apply {
                    injectParent(gState)
                    if (isValid())
                        return this
                }
            }


            //This happens if the number of people condition of the meeting is not met.
            return Wait(name, place)
        }
        //If not speaker, wait if the mutuality to the speaker is high. Otherwise, if possible, interrupt the speaker.
        newMeetingName = gState.meetingName(conf)
        if (conf.currentSpeaker != name) {
            return interceptCondition(conf, name, place)
        } else {
            //If it is my turn to speak
            //Check if I had an intention
            if (agenda != null) {
                return NewAgenda(name, place).also {
                    it.agenda = agenda

                }
            } else {
                //No particular intention

                //0. Execute a command if there is any. Here, we can move to the place actively if the command is not in the current place.
                //If there is a command that is within the set time window, issued party is trusted enough, and seems to be executable at some place(AvailableActions), start execution routine.
                //Note that the command may not be valid even if it in AvailableActions list. For example, if the character is already at the place, move command is not valid.
                executeRequestInMeeting(name, place)?.let { return it }

                proposeProofOfWork(conf, name, place)?.let { return it }

                //If there is a new request issued to me, and there is no matching request from me, try to match it.
                val reqAgendas = conf.agendas.filter { it.type == AgendaType.REQUEST }
                if (reqAgendas.any { name in it.attachedRequest!!.issuedTo } && reqAgendas.none { name in it.attachedRequest!!.issuedBy }) {
                    matchRequests(conf, name, place)?.let { return it }
                }
                gossip(this.gState, name, place)?.also { return it }
            }

            //If nothing else to talk about, end the speech. The next speaker is the character with the highest mutuality.
            return EndSpeech(
                name, place, conf.currentCharacters.minus(name)
                    .maxByOrNull { gState.getMutuality(name, it) }!!
            )

        }


    }

    override fun endCondition(name: String, place: String): Boolean {
        with(this as IMeetingRoutine) {
            if (scheduledMeetingName != null) {
                if (scheduledMeetingName in gState.scheduledMeetings)
                    return false //The meeting has not started yet.
                else if (scheduledMeetingName in gState.ongoingMeetings) {
                    val conf = gState.ongoingMeetings[scheduledMeetingName]!!
                    if (conf != gState.characters[name]!!.currentMeeting) return true //The character has been transferred to another meeting.
                    //Now, given that we are in the correct meeting, check if the meeting is over.
                    return routineStartTime + 7200 / ReadOnly.DT <= gState.time || conf.currentAttention < 10

                } else {
                    return true //The meeting has ended before I could join.
                }
            }

            //The talk is not scheduled in advance.
            if (!startTalking) return false //I have not started talking yet, so the routine should not end.
            val conf = gState.characters[name]!!.currentMeeting
                ?: return true //The meeting has ended, so the routine should end.
            //Now, given that we are in the correct meeting, check if the meeting is over.
            return routineStartTime + 7200 / ReadOnly.DT <= gState.time || conf.currentAttention < 10
        }
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
                return NewAgenda(name, place).also { action ->
                    action.agenda = MeetingAgenda(AgendaType.DENOUNCE, name).also {
                        it.subjectParams["character"] = enemy.key
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
                return NewAgenda(name, place).also { action ->
                    action.agenda = MeetingAgenda(AgendaType.PRAISE, name).also {
                        it.subjectParams["character"] = friend.key
                    }
                }
            return null
        }
    }
}