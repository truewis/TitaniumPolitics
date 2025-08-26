package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable

@Serializable
class AttendPrivateMeetingRoutine(
    val toWho: String? = null, val agenda: MeetingAgenda? = null,
    var scheduledMeetingName: String? = null
) : Routine(),
    IMeetingRoutine {
    var newMeetingName: String = ""
    override val meetingName: String get() = scheduledMeetingName ?: newMeetingName

    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        val conf =
            gState.ongoingMeetings[meetingName]
        //If there is no ongoing meeting, check if there is a scheduled meeting with the specified name or the character to meet.
        //If neither is specified, this routine fails.
        if (conf == null) {
            if (scheduledMeetingName != null) {
                val mtPlace = gState.scheduledMeetings[scheduledMeetingName]!!.place
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
            check(conf.type == Meeting.MeetingType.TALK) { "TalkRoutine can only be used in a meeting of type 'talk'.\n Current meeting: $conf \n Current routines: ${gState.nonPlayerAgents[name]!!.printStatus()}" }
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
            Talk(name, place, toWho!!).apply {
                injectParent(gState)
                if (isValid())
                    return this
            }
            return Wait(name, place).also {
            } //If no meeting found, wait. Note that this action is only executed once because the routine will end after this action.
            //This happens if the number of people condition of the meeting is not met.
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
        return meetingRoutineEndCondition(name, Meeting.MeetingType.TALK)
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