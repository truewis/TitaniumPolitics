package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable

@Serializable
class AttendPrivateMeetingRoutine : Routine(), IMeetingRoutine {
    init {
        priority = PRIORITY_MEETING
    }

    override fun newRoutineCondition(name: String, place: String, routines: List<Routine>): Routine? {
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting ?: return null
        check(conf.type == Meeting.MeetingType.TALK) {
            "AttendPrivateMeetingRoutine can only be used for talk, but got ${conf.type}"
        }
        supportProofOfWork(conf, name)?.let { return it }


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
                    gState.ongoingMeetings.filter { it.value.type == Meeting.MeetingType.CABINET_DAILY_CONFERENCE && it.value.place == place }
                        .keys.firstOrNull()
                        ?: return@apply
                if (isValid())
                    return this
            }
            StartMeeting(name, place).apply {
                injectParent(gState)
                meetingName =
                    gState.scheduledMeetings.filter { it.value.type == Meeting.MeetingType.CABINET_DAILY_CONFERENCE && it.value.place == place }
                        .keys.firstOrNull()
                        ?: return@apply
                if (isValid())
                    return this
            }
            return Wait(name, place).also {
            } //If no meeting found, wait. Note that this action is only executed once because the routine will end after this action.
            //This happens if the number of people condition of the meeting is not met.
        }
        //If not speaker, wait if the mutuality to the speaker is high. Otherwise, if possible, interrupt the speaker.
        if (conf.currentSpeaker != name) {
            if (gState.getMutuality(
                    name,
                    conf.currentSpeaker
                ) > ReadOnly.const("SpeakerInterceptMutualityThreshold")
            )
                return Wait(name, place)
            else {
                val action = Intercept(name, place).also { it.injectParent(gState) }
                if (action.isValid())
                    return action
                return Wait(name, place)
            }
        } else {
            proposeProofOfWork(conf, name, place)?.let { return it }

            //If there is a new request issued to me, and there is no matching request from me, try to match it.
            val reqAgendas = conf.agendas.filter { it.type == AgendaType.REQUEST }
            if (reqAgendas.any { name in it.attachedRequest!!.issuedTo } && reqAgendas.none { name in it.attachedRequest!!.issuedBy }) {
                matchRequests(conf, name, place)?.let { return it }
            }


            //If nothing else to talk about, end the speech. The next speaker is the character with the highest mutuality.
            return EndSpeech(name, place).also {
                it.nextSpeaker = conf.currentCharacters.minus(name)
                    .maxByOrNull { gState.getMutuality(name, it) }!!
            }
        }

        //If everything else, wait.
        return Wait(name, place)
        //TODO: do something in the meeting. Leave the meeting if nothing to do.


    }

    //TODO: Also check AttendMeetingRoutine for the same function.
    override fun endCondition(name: String, place: String): Boolean {
        gState.characters[name]!!
        //If the conference is over, leave the routine. But the condition is not checked here, because the routine is not ended until the action is executed.
        //See NonPlayerAgent.selectRoutine()
        //If two hours has passed since the meeting started, leave the meeting. TODO: what if the meeting has started late?
        //TODO: stay in the meeting until I have something else to do, or the work hours are over.
        return routineStartTime + 7200 / ReadOnly.dt <= gState.time || gState.characters[name]!!.currentMeeting?.let { it.type != Meeting.MeetingType.TALK } ?: false /*Sometimes characters are transferred between different meetings without their turn. In that case, the previous meeting routine is killed here.*/
    }
}