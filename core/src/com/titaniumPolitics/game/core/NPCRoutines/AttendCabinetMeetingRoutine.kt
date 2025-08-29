package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable

@Serializable
class AttendCabinetMeetingRoutine(override val meetingName: String) : MeetingRoutine() {
    init {
        priority = PRIORITY_MEETING
    }

    override fun newIMeetingRoutineCondition(
        name: String,
        place: String,
        subroutines: List<Routine>
    ): IMeetingRoutine? {
        supportProofOfWork(name)?.let { return it }
        return null
    }

    override fun executeInMeeting(name: String, place: String): GameAction {
        //If not speaker, wait if the mutuality to the speaker is high. Otherwise, if possible, interrupt the speaker.
        if (meeting.currentSpeaker != name) {
            if (routineStartTime + 7200 / ReadOnly.DT <= gState.time || meeting.currentAttention < 10)
                return LeaveMeeting(name, place)
            return interceptCondition(name, place)
        } else {
            gState.parties[meeting.involvedParty]!!

            //0. Execute a command if there is any. Here, we can move to the place actively if the command is not in the current place.
            //If there is a command that is within the set time window, issued party is trusted enough, and seems to be executable at some place(AvailableActions), start execution routine.
            //Note that the command may not be valid even if it in AvailableActions list. For example, if the character is already at the place, move command is not valid.
            executeRequestInMeeting(name, place)?.let { return it }


            //Proof of work should have corresponding request. If there is no request or no relevant information, do not propose proof of work.
            //Some information are more relevant than others.
            if (meeting.agendas.none { it.type == AgendaType.PROOF_OF_WORK }) {
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
                                    it.responsibleDivision != null && gState.parties[it.responsibleDivision]!!.leader in meeting.currentCharacters && it.shortestPathAndTimeTo(
                                        place1.name
                                    ) != null //Check connectivity so that the resource can be delivered.
                                }
                                    .filter { it.resources[res] > apparatus.currentConsumption[res]!! * place1.workHoursLength * 3 } //Check if the place has enough resource to supply for 3 work days.
                            //Check if there is already a request for the resource.
                            if (findResourceOutsideDivision.isEmpty()) return@forEach //If there is no place with the resource, skip.
                            val tgtPlace = findResourceOutsideDivision.first()
                            val tgtParty = gState.parties[tgtPlace.responsibleDivision]!!
                            tgtParty.leader?.let { leader ->
                                if (meeting.agendas.none { it.type == AgendaType.REQUEST && it.author == name && it.attachedRequest?.action is OfficialResourceTransfer }) {
                                    //Fill in the agenda based on variables in the routine, resource and character.
                                    val agenda = MeetingAgenda(AgendaType.REQUEST, name).apply {
                                        attachedRequest = Request(
                                            OfficialResourceTransfer(
                                                leader,
                                                tgtPlace.name,
                                                place1.name,
                                                Resources(res to apparatus.currentConsumption[res]!! * place1.workHoursLength * 3)
                                            ),
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
            val nextSpeaker = meeting.currentCharacters.minus(name)
                .maxByOrNull { gState.getMutuality(name, it) }
                ?: return EndMeeting(name, place)
            return EndSpeech(name, place, nextSpeaker, gState)
        }
        //TODO: do something in the meeting. Leave the meeting if nothing to do.


    }
}