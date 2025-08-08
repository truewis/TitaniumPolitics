package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable

@Serializable
class AttendCabinetMeetingRoutine : Routine(), IMeetingRoutine {
    init {
        priority = PRIORITY_MEETING
    }

    override fun newRoutineCondition(name: String, place: String, routines: List<Routine>): Routine? {
        val character = gState.characters[name]!!
        val conf =
            character.currentMeeting ?: return null
        check(conf.type == Meeting.MeetingType.CABINET_DAILY_CONFERENCE) {
            "AttendCabinetMeetingRoutine can only be used for cabinetDailyConference, but got ${conf.type}"
        }

        val party = gState.parties[conf.involvedParty]!!
        check(party.leader != name) {
            "AttendCabinetMeetingRoutine can only be used for cabinetDailyConference when not the leader, but got $name as the leader of ${party.name}"
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
            return interceptCondition(conf, name, place)
        } else {
            gState.parties[conf.involvedParty]!!

            //Proof of work should have corresponding request. If there is no request or no relevant information, do not propose proof of work.
            //Some information are more relevant than others.
            if (conf.agendas.none { it.type == AgendaType.PROOF_OF_WORK }) {
                return NewAgenda(name, place).also {
                    it.agenda = MeetingAgenda(AgendaType.PROOF_OF_WORK, name)
                }
            }

            //If there is a place in my division with a resource that is short of, and if there is other division with the resource, request the resource from that division.
            gState.places.values.forEach { place1 -> //TODO: right now, supply resource to any place regardless of the division. In the future, agents will not supply resources to hostile divisions.
                place1.apparatuses.forEach { apparatus ->
                    val res = place1.resourceShortOfHourly(apparatus) //Type of resource that is short of.
                    if (res != null)
                    //if there is a place within my division with the resource, skip.
                    {
                        val resplace =
                            gState.places.values.filter {
                                it.responsibleDivision != null && name in gState.parties[it.responsibleDivision]!!.members && it.shortestPathAndTimeTo(
                                    place1.name
                                ) != null //Check connectivity so that the resource can be delivered.
                            }
                                .filter { it.resources[res] > apparatus.currentConsumption[res]!! * place1.workHoursLength * 3 } //Check if the place has enough resource to supply for 3 work days.
                        if (resplace.isEmpty()) //Only if there is no place in my division with the resource, request the resource from other divisions.
                        {
                            val findResourceOutsideDivision =
                                gState.places.values.filter {
                                    it.responsibleDivision != null && gState.parties[it.responsibleDivision]!!.leader in conf.currentCharacters && it.shortestPathAndTimeTo(
                                        place1.name
                                    ) != null //Check connectivity so that the resource can be delivered.
                                }
                                    .filter { it.resources[res] > apparatus.currentConsumption[res]!! * place1.workHoursLength * 3 } //Check if the place has enough resource to supply for 3 work days.
                            //Check if there is already a request for the resource.
                            if (findResourceOutsideDivision.isEmpty()) return@forEach //If there is no place with the resource, skip.
                            val tgtPlace = findResourceOutsideDivision.first()
                            val tgtParty = gState.parties[tgtPlace.responsibleDivision]!!
                            tgtParty.leader?.let { leader ->
                                if (conf.agendas.none { it.type == AgendaType.REQUEST && it.author == name && it.attachedRequest?.action is OfficialResourceTransfer }) {
                                    //Fill in the agenda based on variables in the routine, resource and character.
                                    val agenda = MeetingAgenda(AgendaType.REQUEST, name).apply {
                                        attachedRequest = Request(
                                            OfficialResourceTransfer(
                                                leader, tgtPlace.name
                                            ).apply {
                                                resources =
                                                    Resources(res to apparatus.currentConsumption[res]!! * place1.workHoursLength * 3)
                                                toWhere = place1.name
                                            },
                                            issuedTo = hashSetOf(leader),
                                            issuedBy = hashSetOf(name)
                                        ) //Created a command to transfer the resource.
                                    }
                                    return NewAgenda(name, place).also { it.agenda = agenda }
                                }
                            }

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
        //TODO: do something in the meeting. Leave the meeting if nothing to do.


    }

    //TODO: Also check AttendMeetingRoutine for the same function.
    override fun endCondition(name: String, place: String): Boolean {
        //If the conference is over, leave the routine. But the condition is not checked here, because the routine is not ended until the action is executed.
        //See NonPlayerAgent.selectRoutine()
        //If two hours has passed since the meeting started, leave the meeting. TODO: what if the meeting has started late?
        //TODO: stay in the meeting until I have something else to do, or the work hours are over.
        return meetingRoutineEndCondition(name, Meeting.MeetingType.CABINET_DAILY_CONFERENCE)
    }
}