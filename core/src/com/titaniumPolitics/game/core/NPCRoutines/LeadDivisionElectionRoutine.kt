package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.Apparatus
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable
import kotlin.math.max

@Serializable
class LeadDivisionElectionRoutine : Routine(), IMeetingRoutine {
    init {
        priority = PRIORITY_MEETING
    }

    override fun newRoutineCondition(name: String, place: String, routines: List<Routine>): Routine? {
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting ?: return null
        check(conf.type == Meeting.MeetingType.DIVISION_LEADER_ELECTION) {
            "LeadDivisionElectionRoutine can only be used in divisionLeaderElection , but got ${conf.type}"
        }
        check(name == "ctrler") {
            "LeadDivisionElectionRoutine can only be used by the ctrler, but got $name"
        }

        //Don't do anything because the controller is not a division member.


        return null
    }

    override fun execute(name: String, place: String): GameAction {
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting
        if (conf == null) {
            StartMeeting(name, place).apply {
                injectParent(gState)
                meetingName =
                    gState.scheduledMeetings.filter { it.value.type == Meeting.MeetingType.DIVISION_LEADER_ELECTION && it.value.place == place }
                        .keys.firstOrNull()
                        ?: return@apply
                if (isValid())
                    return this
            }
            return Wait(name, place).also {
            } //If no meeting found, wait. Note that this action is only executed once because the routine will end after this action.
            //This happens if the number of people condition of the meeting is not met.
        }
        val party = gState.parties[conf.involvedParty]!!
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
        } else //If it is my turn to speak
        {
            val nominee = gState.characters.keys.filter { it != name && party.members.contains(it) }
                .maxByOrNull { gState.getMutuality(name, it) }!!
            //Nominate the person with the highest mutuality, if not nominated yet.
            //Note that nomination is only valid at the beginning of the conference.

            if (conf.agendas.none { it.type == AgendaType.NOMINATE && it.subjectParams["character"] == nominee } && conf.time == gState.time) {
                return NewAgenda(name, place).also {
                    it.agenda =
                        MeetingAgenda(
                            AgendaType.NOMINATE,
                            author = name,
                            subjectParams = hashMapOf("character" to nominee)
                        )
                }
            }
            //otherwise, support the nominee.


//If nothing else to talk about, end the speech. The next speaker is the character with the highest mutuality.
            return EndSpeech(name, place).also {
                it.nextSpeaker = conf.currentCharacters.minus(name)
                    .maxByOrNull { gState.getMutuality(name, it) }!!
            }
        }

        //TODO: do something in the meeting. Leave the meeting if nothing to do.

    }

    fun productivity(toWhom: String, apparatus: Apparatus): Double {
        return apparatus.currentProduction.entries.sumOf { (key, value) -> gState.characters[toWhom]!!.itemValue(key) * value } / apparatus.currentWorker
    }

    fun adjustResourceProd(name: String, place: String): GameAction? {
        val charObj = gState.characters[name]!!
        //1. If the party is short of workers, reduce the production of the section which has the minimum productivity per worker hour
        val minProdApp = charObj
            .division!!.places.flatMap { it.apparatuses }.filter { it.currentWorker != 0 }.minByOrNull {
                productivity(name, it)
            }
        if (minProdApp != null)
            if (productivity(name, minProdApp) < gState.laborValuePerHour) {
                val reductionAmount = max(minProdApp.plannedWorker / 5, 1)
                val wantPlace = gState.getApparatusPlace(minProdApp.ID)
                //Fill in the agenda based on variables in the routine, resource and character.
                val agenda = MeetingAgenda(AgendaType.REQUEST, name).apply {
                    attachedRequest = Request(
                        SetWorkers(
                            wantPlace.manager,
                            tgtPlace = wantPlace.name
                        ).apply {
                            workers = minProdApp.plannedWorker - reductionAmount
                            apparatusID = minProdApp.ID
                        }//Created a command to transfer the resource.
                        ,
                        issuedTo = hashSetOf(wantPlace.manager)
                    ).apply {
                        executeTime = gState.time
                        issuedBy = hashSetOf(name)
                    }
                }
                return NewAgenda(name, place).also {
                    it.agenda = agenda
                }

            }

        //2. Increase the production of the section which has the maximum productivity per worker hour. The productivity must be higher than the labor cost.
        val maxProdApp = charObj
            .division!!.places.flatMap { it.apparatuses }.filter { it.currentWorker != 0 }.maxByOrNull {
                productivity(name, it)
            }
        if (maxProdApp != null)
            if (productivity(name, maxProdApp) > gState.laborValuePerHour) {
                val increaseAmount = max(maxProdApp.plannedWorker / 5, 1)
                val wantPlace = gState.getApparatusPlace(maxProdApp.ID)
                //Fill in the agenda based on variables in the routine, resource and character.
                val agenda = MeetingAgenda(AgendaType.REQUEST, name).apply {
                    attachedRequest = Request(
                        SetWorkers(
                            wantPlace.manager,
                            tgtPlace = wantPlace.name
                        ).apply {
                            workers = maxProdApp.plannedWorker + increaseAmount
                            apparatusID = maxProdApp.ID
                        }//Created a command to transfer the resource.
                        ,
                        issuedTo = hashSetOf(wantPlace.manager)
                    ).apply {
                        executeTime = gState.time
                        issuedBy = hashSetOf(name)
                    }
                }
                return NewAgenda(name, place).also {
                    it.agenda = agenda
                }

            }
        return null
    }

    //TODO: Also check AttendMeetingRoutine for the same function.
    override fun endCondition(name: String, place: String): Boolean {
        gState.characters[name]!!
        //If the conference is over, leave the routine. But the condition is not checked here, because the routine is not ended until the action is executed.
        //See NonPlayerAgent.selectRoutine()
        //If two hours has passed since the meeting started, leave the meeting. TODO: what if the meeting has started late?
        //TODO: stay in the meeting until I have something else to do, or the work hours are over.
        return routineStartTime + 7200 / ReadOnly.dt <= gState.time
    }
}