package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable
import kotlin.math.max

@Serializable
class LeadDivisionMeetingRoutine : Routine(), IMeetingRoutine {
    init {
        priority = PRIORITY_MEETING
    }

    override fun newRoutineCondition(name: String, place: String, routines: List<Routine>): Routine? {
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting ?: return null
        check(conf.type == Meeting.MeetingType.DIVISION_DAILY_CONFERENCE) {
            "For $name, LeadDivisionMeetingRoutine can only be used in divisionDailyConference, but got ${conf.type}."
        }
        val party = gState.parties[conf.involvedParty]!!
        check(party.leader == name) {
            "For $name, LeadDivisionMeetingRoutine can only be used by the division leader, but got $name"
        }

        //DO not support proof of work, as we are the leader.


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
                    gState.ongoingMeetings.filter { it.value.type == Meeting.MeetingType.DIVISION_DAILY_CONFERENCE && it.value.place == place }
                        .keys.firstOrNull()
                        ?: return@apply
                if (isValid())
                    return this
            }
            StartMeeting(name, place).apply {
                injectParent(gState)
                meetingName =
                    gState.scheduledMeetings.filter { it.value.type == Meeting.MeetingType.DIVISION_DAILY_CONFERENCE && it.value.place == place }
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
            //1. Pay the salary if not paid yet.
            if (!party.isSalaryPaid) {
                return Salary(name, place)
            }
            //2. request information about the commands issued today, by putting ProofOfWork agenda forward.
            proposeProofOfWork(conf, name, place)?.let { return it }
            //3. Praise or criticize the division members, if there is any relevant information.
            //It should be noted that the content of the information is not checked here. Think about this later.
            party.members.forEach { member ->
                if (member != name && gState.informations.values.any {
                        it.tgtCharacter == member && it.knownTo.contains(
                            name
                        )
                    }) {
                    //praise if the mutuality is high, criticize if the mutuality is low.
                    val mutuality = gState.getMutuality(name, member)
                    if (mutuality > 80) {
                        return NewAgenda(name, place).also {
                            it.agenda =
                                MeetingAgenda(
                                    AgendaType.PRAISE,
                                    name,
                                    subjectParams = hashMapOf("character" to member)
                                )
                        }
                    } else if (mutuality < 20) {
                        return NewAgenda(name, place).also {
                            it.agenda =
                                MeetingAgenda(
                                    AgendaType.DENOUNCE, name,
                                    subjectParams = hashMapOf("character" to member)
                                )
                        }
                    }
                }//TODO: there must be a cooldown, stored in party class.
            }
            //4. If it is not covered above, if the division is short of resources, share the information about the resource shortage.
            //However, right now, the resource information is available to everyone immediately, no need to share.

            //5. Criticize the common enemies of the division. It is determined by the party with the low mutuality with the division.
            val enemyParty = gState.parties.values.filter { it.name != conf.involvedParty }
                .minBy { gState.getPartyMutuality(it.name, conf.involvedParty) }.name
            if (gState.getPartyMutuality(
                    conf.involvedParty,
                    enemyParty
                ) < ReadOnly.const("EnemyPartyMutualityThreshold")
            )
                return NewAgenda(name, place).also { action ->
                    action.agenda = MeetingAgenda(AgendaType.DENOUNCE_PARTY, name).also {
                        it.subjectParams["party"] = enemyParty
                    }
                }
            //6. If the world is short of resources and we have an apparatus producing that, increase the production. //TODO: this decision must depend on a personal parameter
            adjustResourceProd(name, place)?.also { return it }

            //7. Gossip
            TalkRoutine.gossip(gState, name, place)?.also { return it }
            
            //8. End meeting if attention is low.
            endMeetingIfLowAttention(conf, name, place)?.let { return it }
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
                if (wantPlace.manager?.let {
                        it in charObj.currentMeeting!!.currentCharacters && it != name
                    }
                        ?: false) {
                    //Fill in the agenda based on variables in the routine, resource and character.
                    val agenda = MeetingAgenda(AgendaType.REQUEST, name).apply {
                        attachedRequest = Request(
                            SetWorkers(
                                wantPlace.manager!!,
                                tgtPlace = wantPlace.name
                            ).apply {
                                workers = minProdApp.plannedWorker - reductionAmount
                                apparatusID = minProdApp.ID
                            }//Created a command to transfer the resource.
                            ,
                            issuedTo = hashSetOf(wantPlace.manager!!)
                        ).apply {
                            executeTime = gState.time
                            issuedBy = hashSetOf(name)
                        }
                    }
                    return NewAgenda(name, place).also {
                        it.agenda = agenda
                    }
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
                if (wantPlace.manager != null && wantPlace.manager!! in charObj.currentMeeting!!.currentCharacters) {
                    val agenda = MeetingAgenda(AgendaType.REQUEST, name).apply {
                        attachedRequest = Request(
                            SetWorkers(
                                wantPlace.manager!!,
                                tgtPlace = wantPlace.name
                            ).apply {
                                workers = maxProdApp.plannedWorker + increaseAmount
                                apparatusID = maxProdApp.ID
                            }//Created a command to transfer the resource.
                            ,
                            issuedTo = hashSetOf(wantPlace.manager!!)
                        ).apply {
                            executeTime = gState.time
                            issuedBy = hashSetOf(name)
                        }
                    }
                    return NewAgenda(name, place).also {
                        it.agenda = agenda
                    }
                }

            }
        return null
    }

    override fun endCondition(name: String, place: String): Boolean {
        //If the conference is over, leave the routine. But the condition is not checked here, because the routine is not ended until the action is executed.
        //See NonPlayerAgent.selectRoutine()
        //If two hours has passed since the meeting started, leave the meeting. TODO: what if the meeting has started late?
        //TODO: stay in the meeting until I have something else to do, or the work hours are over.
        return meetingRoutineEndCondition(name, Meeting.MeetingType.DIVISION_DAILY_CONFERENCE)
    }
}