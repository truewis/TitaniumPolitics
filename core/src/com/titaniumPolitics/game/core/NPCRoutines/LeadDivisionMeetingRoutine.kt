package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable
import kotlin.math.max

@Serializable
class LeadDivisionMeetingRoutine(override val meetingName: String) : MeetingRoutine() {
    init {
        priority = PRIORITY_MEETING
    }

    override fun executeInMeeting(name: String, place: String): GameAction {
        val character = gState.characters[name]!!
        val party = gState.parties[meeting.involvedParty]!!
        //If not speaker, wait if the mutuality to the speaker is high. Otherwise, if possible, interrupt the speaker.
        if (meeting.currentSpeaker != name) {
            return interceptCondition(name, place)
        } else //If it is my turn to speak
        {
            //0. Execute a command if there is any. Here, we can move to the place actively if the command is not in the current place.
            //If there is a command that is within the set time window, issued party is trusted enough, and seems to be executable at some place(AvailableActions), start execution routine.
            //Note that the command may not be valid even if it in AvailableActions list. For example, if the character is already at the place, move command is not valid.
            executeRequestInMeeting(name, place)?.let { return it }
            //1. Pay the salary if not paid yet.
            Salary(name, place).also {
                it.injectParent(gState)
                if (it.isValid()) return it
            }
            //2. request information about the commands issued today, by putting ProofOfWork agenda forward.
            proposeProofOfWork(name, place)?.let { return it }

            //Warning: Some apparatus info may be missing because this only checks if there is at least one apparatus information about the place.
            fun queryInfo(queryPl: Place, type: InformationType): GameAction? {
                if (gState.informations.values.none { info ->
                        info.tgtPlace == queryPl.name && info.type == type && name in info.knownTo
                    }) {
                    val agenda = MeetingAgenda(AgendaType.REQUEST, name).apply {
                        attachedRequest = Request(
                            Examine(
                                sbjCharacter = meeting.involvedParty!!,
                                tgtPlace = queryPl.name,
                                what = type
                            ),
                            issuedTo = (meeting.currentCharacters - name).toHashSet(),
                            issuedBy = hashSetOf(name),
                            executeTime = gState.time
                        )
                    }
                    NewAgenda(name, place, gState).also {
                        it.agenda = agenda
                        if (it.isValid())
                            return it
                    }
                }
                return null
            }
            //2.1. If there is a place within my division that I don't have resource information about, request the examination of that place.
            if (party.type == Party.Type.DIVISION)
                party.divisionPlaces.forEach { divPlace ->
                    queryInfo(divPlace, InformationType.RESOURCES)?.let { return it }
                    queryInfo(divPlace, InformationType.HUMAN_RESOURCES)?.let { return it }
                    queryInfo(divPlace, InformationType.APPARATUS)?.let { return it }
                }
            if (party.type == Party.Type.WORKPLACE)
                party.workplace.let { divPlace ->
                    queryInfo(divPlace, InformationType.RESOURCES)?.let { return it }
                    queryInfo(divPlace, InformationType.HUMAN_RESOURCES)?.let { return it }
                    queryInfo(divPlace, InformationType.APPARATUS)?.let { return it }
                }
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
                        NewAgenda(name, place, gState).also {
                            it.agenda =
                                MeetingAgenda(
                                    AgendaType.PRAISE,
                                    name,
                                    subjectParams = hashMapOf("character" to member)
                                )
                            if (it.isValid())
                                return it
                        }
                    } else if (mutuality < 20) {
                        NewAgenda(name, place, gState).also {
                            it.agenda =
                                MeetingAgenda(
                                    AgendaType.DENOUNCE, name,
                                    subjectParams = hashMapOf("character" to member)
                                )
                            if (it.isValid())
                                return it
                        }
                    }
                }//TODO: there must be a cooldown, stored in party class.
            }
            //3. If sufficiently discontent with a current meeting attendant, and have an information to fire him, fire the person.
            meeting.currentCharacters.forEach { char ->
                if (char != name && gState.getMutNorm(name, char) < -0.5) {
                    val agenda = MeetingAgenda(AgendaType.FIRE_MANAGER, name).also {
                        it.subjectParams["character"] = char
                    }
                    if (character.preparedInfoKeys.any {
                            agenda.effectivity(gState, meeting, gState.informations[it]!!, character) > 0
                        }) {
                        NewAgenda(name, place, gState).also {
                            it.agenda = agenda
                            if (it.isValid())
                                return it
                        }
                    }
                }
            }

            //4. If it is not covered above, if the division is short of resources, share the information about the resource shortage.
            //However, right now, the resource information is available to everyone immediately, no need to share.

            //5. Criticize the common enemies of the division. It is determined by the party with the low mutuality with the division.
            val enemyParty = gState.parties.values.filter { it.name != meeting.involvedParty }
                .minBy { gState.getPartyMutuality(it.name, meeting.involvedParty!!) }.name
            if (gState.getPartyMutuality(
                    meeting.involvedParty!!,
                    enemyParty
                ) < ReadOnly.const("EnemyPartyMutualityThreshold")
            )
                NewAgenda(name, place, gState).also { action ->
                    action.agenda = MeetingAgenda(AgendaType.DENOUNCE_PARTY, name).also {
                        it.subjectParams["party"] = enemyParty
                    }
                    if (action.isValid())
                        return action
                }
            //6. If the world is short of resources and we have an apparatus producing that, increase the production. //TODO: this decision must depend on a personal parameter
            if (meeting.involvedParty!!.contains("workplace"))
                adjustResourceProd(name, place)?.also { return it }
            //Adjust resource production request is only issued in workplace meetings.
            //In director meetings, the resource production request is not issued, as the director does not manage resources.s

            //7. Gossip
            AttendPrivateMeetingRoutine.gossip(gState, name, place)?.also { return it }

            //8. End meeting if attention is low.
            endMeetingIfLowAttention(name, place)?.let { return it }
            //If nothing else to talk about, end the speech. The next speaker is the character with the highest mutuality.
            val nextSpeaker = meeting.currentCharacters.minus(name)
                .maxByOrNull { gState.getMutuality(name, it) }
                ?: return EndMeeting(name, place)
            return EndSpeech(name, place, nextSpeaker, gState)
        }

        //TODO: do something in the meeting. Leave the meeting if nothing to do.

    }

    fun productivity(toWhom: String, apparatus: Apparatus): Double {
        return gState.characters[toWhom]!!.itemValue(apparatus.currentProduction) / apparatus.currentWorker
    }

    fun adjustResourceProd(name: String, place: String): GameAction? {
        val charObj = gState.characters[name]!!
        //1. If the party is short of workers, reduce the production of the section which has the minimum productivity per worker hour
        val minProdApp = charObj
            .division!!.divisionPlaces.flatMap { it.apparatuses }.filter { it.currentWorker != 0 }.minByOrNull {
                productivity(name, it)
            }
        if (minProdApp != null)
            if (productivity(name, minProdApp) < gState.laborValuePerHour) {
                val reductionAmount = max(minProdApp.plannedWorker / 5, 1)
                val wantPlace = gState.getApparatusPlace(minProdApp.ID)
                if (wantPlace.workplaceParty?.overseer?.let {
                        it in charObj.currentMeeting!!.currentCharacters && it != name
                    }
                        ?: false) {
                    //Fill in the agenda based on variables in the routine, resource and character.
                    val agenda = MeetingAgenda(AgendaType.REQUEST, name).apply {
                        attachedRequest = Request(
                            SetWorkers(
                                wantPlace.workplaceParty!!.overseer!!,
                                tgtPlace = wantPlace.name,
                                workers = minProdApp.plannedWorker - reductionAmount,
                                apparatusID = minProdApp.ID
                            ),
                            issuedTo = hashSetOf(wantPlace.workplaceParty!!.overseer!!),
                            issuedBy = hashSetOf(name),
                            executeTime = gState.time
                        )
                    }
                    NewAgenda(name, place, gState).also {
                        it.agenda = agenda
                        if (it.isValid())
                            return it
                    }
                }

            }

        //2. Increase the production of the section which has the maximum productivity per worker hour. The productivity must be higher than the labor cost.
        val maxProdApp = charObj
            .division!!.divisionPlaces.flatMap { it.apparatuses }.filter { it.currentWorker != 0 }.maxByOrNull {
                productivity(name, it)
            }
        if (maxProdApp != null)
            if (productivity(name, maxProdApp) > gState.laborValuePerHour) {
                val increaseAmount = max(maxProdApp.plannedWorker / 5, 1)
                val wantPlace = gState.getApparatusPlace(maxProdApp.ID)
                //Fill in the agenda based on variables in the routine, resource and character.
                if (wantPlace.workplaceParty?.overseer != null && wantPlace.workplaceParty?.overseer in charObj.currentMeeting!!.currentCharacters) {
                    val agenda = MeetingAgenda(AgendaType.REQUEST, name).apply {
                        attachedRequest = Request(
                            SetWorkers(
                                wantPlace.workplaceParty!!.overseer!!,
                                tgtPlace = wantPlace.name,
                                workers = maxProdApp.plannedWorker + increaseAmount,
                                apparatusID = maxProdApp.ID
                            ),
                            issuedTo = hashSetOf(wantPlace.workplaceParty!!.overseer!!),
                            issuedBy = hashSetOf(name),
                            executeTime = gState.time
                        )
                    }
                    NewAgenda(name, place, gState).also {
                        it.agenda = agenda
                        if (it.isValid())
                            return it
                    }
                }

            }
        return null
    }
}