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
class LeadCabinetMeetingRoutine : Routine(), IMeetingRoutine {
    override fun newRoutineCondition(name: String, place: String, routines: List<Routine>): Routine? {
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting ?: return null
        check(conf.type == Meeting.MeetingType.CABINET_DAILY_CONFERENCE) {
            "LeadCabinetMeetingRoutine can only be used in cabinetDailyConference, but got ${conf.type}"
        }
        val party = gState.parties[conf.involvedParty]!!
        check(party.leader == name) {
            "LeadCabinetMeetingRoutine can only be used for cabinetDailyConference when the leader, but got $name as the leader of ${party.name}"
        }

        //If speaker, propose proof of work if nothing else is important.
        //Proof of work should have corresponding request. If there is no request or no relevant information, do not propose proof of work.
        //Some information are more relevant than others.
        if (conf.agendas.any { it.type == AgendaType.PROOF_OF_WORK }) {

            //If we haven't tried this branch in the current routine
            if (intVariables["try_support_proofOfWork"] != 1) {
                //If the agenda is already proposed, and we have a supporting information, support it.
                intVariables["try_support_proofOfWork"] = 1
                return (
                        SupportAgendaRoutine().apply {
                            intVariables["agendaIndex"] =
                                conf.agendas.indexOfFirst { it.type == AgendaType.PROOF_OF_WORK }
                        })//Add a routine, priority higher than work.
            }
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
            //1. No salary in cabinet meeting, so no need to support salary agenda.


            //2. request information about the commands issued today, by putting ProofOfWork agenda forward.
            if (!conf.agendas.any { it.type == AgendaType.PROOF_OF_WORK })
                return NewAgenda(name, place).also {
                    it.agenda = MeetingAgenda(AgendaType.PROOF_OF_WORK, name)
                }
            //3. Praise or criticize the cabinet members, if there is any relevant information.
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
            //6. Cabinet does not manage resources, so no need to adjust resource production.

            //7. Gossip
            TalkRoutine.gossip(gState, name, place)?.also { return it }

//If nothing else to talk about, end the speech. The next speaker is the character with the highest mutuality.
            return EndSpeech(name, place).also {
                it.nextSpeaker = conf.currentCharacters.minus(name)
                    .maxByOrNull { gState.getMutuality(name, it) }!!
            }
        }

        //TODO: do something in the meeting. Leave the meeting if nothing to do.

    }

    override fun endCondition(name: String, place: String): Boolean {
        gState.characters[name]!!
        //If the conference is over, leave the routine. But the condition is not checked here, because the routine is not ended until the action is executed.
        //See NonPlayerAgent.selectRoutine()
        //If two hours has passed since the meeting started, leave the meeting. TODO: what if the meeting has started late?
        //TODO: stay in the meeting until I have something else to do, or the work hours are over.
        return intVariables["routineStartTime"]!! + 7200 / ReadOnly.dt <= gState.time
    }
}