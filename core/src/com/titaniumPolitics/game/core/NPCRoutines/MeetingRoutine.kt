package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.core.gameActions.EndMeeting
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Intercept
import com.titaniumPolitics.game.core.gameActions.JoinMeeting
import com.titaniumPolitics.game.core.gameActions.NewAgenda
import com.titaniumPolitics.game.core.gameActions.Repair
import com.titaniumPolitics.game.core.gameActions.StartMeeting
import com.titaniumPolitics.game.core.gameActions.UnofficialResourceTransfer
import com.titaniumPolitics.game.core.gameActions.Wait
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable
import kotlin.math.min

@Serializable
sealed class MeetingRoutine : Routine() {
    abstract val meetingName: String
    var hasAttended: Boolean = false
    val meeting
        get() =
            gState.ongoingMeetings[meetingName]!!

    final override fun newRoutineCondition(
        name: String,
        place: String,
        subroutines: List<Routine>
    ): Routine? {
        meetingControl(name, place)?.let { return it }
        if (gState.ongoingMeetings[meetingName] == null) return null //Wait until the meeting starts.
        newIMeetingRoutineCondition(name, place, subroutines)?.let { return it as Routine }
        return null
    }

    open fun newIMeetingRoutineCondition(
        name: String,
        place: String,
        subroutines: List<Routine>
    ): IMeetingRoutine? {
        return null
    }

    final override fun execute(name: String, place: String): GameAction {
        if (gState.characters[name]!!.currentMeeting == null)
            joinMeetingActions(name, place)?.let { return it }
        return executeInMeeting(name, place)
    }

    abstract fun executeInMeeting(name: String, place: String): GameAction

    open fun meetingControl(name: String, place: String): Routine? {
        if (meetingRoutineEndCondition(name)) {
            if (hasAttended) success() else failed()
            return null
        }
        val mt = gState.ongoingMeetings[meetingName] ?: gState.scheduledMeetings[meetingName]
        if (mt == null) {
            Logger.write(
                "Meeting routine failed because the character is not in a meeting and there is no meeting to join.",
                Logger.LogLevel.WARNING
            )
            failed()
            return null
        }
        if (mt !in gState.ongoingMeetings.values) {
            if (place != mt.place)
                return MoveRoutine(mt.place)
            else
                return null //Wait until the meeting starts.
        }
        return null
    }

    open fun joinMeetingActions(
        name: String,
        place: String
    ): GameAction? {
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
        return Wait(
            name,
            place
        )//If no meeting found, wait. Note that this action is only executed once because the routine will end after this action.
        //This happens if the number of people condition of the meeting is not met.
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //Shared Functions

    var try_support_proofOfWork = 0
    fun supportProofOfWork(name: String): IMeetingRoutine? {

        //If speaker, try supporting proof of work if I am involved.
        //Proof of work should have corresponding request. If there is no request or no relevant information, do not propose proof of work.
        if (meeting.agendas.any {
                it.type == AgendaType.PROOF_OF_WORK && (it.attachedRequest == null /*If request is null, proof of work is about the general attire, so support it anyways.*/ || (name in it.attachedRequest!!.issuedBy && it.attachedRequest!!.issuedTo.intersect(
                    meeting.currentCharacters
                ).isNotEmpty()))
            }) {

            //If we haven't tried this branch in the current routine
            if (try_support_proofOfWork == 0) {
                //If the agenda is already proposed, and we have a supporting information, support it.
                try_support_proofOfWork += 1
                return (
                        AddInfoToAgendaRoutine(
                            meeting.agendas.indexOfFirst { it.type == AgendaType.PROOF_OF_WORK },
                            support = true
                        ))//Add a routine, priority higher than work.
            }
        }
        return null
    }

    fun proposeProofOfWork(name: String, place: String): GameAction? {
        //Proof of work should have corresponding request. If there is no request or no relevant information, do not propose proof of work.
        //Some information are more relevant than others.
        if (meeting.agendas.none { it.type == AgendaType.PROOF_OF_WORK }) {
            gState.requests.values.firstOrNull {
                name in it.issuedBy && it.issuedTo.intersect(meeting.currentCharacters)
                    .isNotEmpty() && !it.completed && meeting.agendas.none { agenda -> agenda.type == AgendaType.REQUEST && agenda.attachedRequest == it } /*Do not demand the request submitted in this meeting to be proved right away.*/
            }?.let { req ->
                return NewAgenda(name, place).also {
                    it.agenda = MeetingAgenda(AgendaType.PROOF_OF_WORK, name, attachedRequest = req)
                }
            }

        }
        return null
    }

    fun matchRequests(name: String, place: String): GameAction? {
        //Find all requests in this meeting that is issued to me.
        val requests = meeting.agendas.filter { it.type == AgendaType.REQUEST }.map { it.attachedRequest }
            .filter { it != null && name in it.issuedTo }
        val char = gState.characters[name]!!
        if (requests.isEmpty()) return null

        //Compute the aggregate value of the requests.
        val totalValue = requests.sumOf { char.actionValue(it!!.action) }

        //Compute the total item value of each of the issuers.
        val issuers = requests.flatMap { it!!.issuedBy }.distinct()
        val issuersItemValues =
            issuers
                .map { n1 -> Pair(n1, char.itemValue(gState.characters[n1]!!.resources)) }

        val issuersActionValues =
            issuers
                .map { n1 -> Pair(n1, askForValuableAction(n1, name)?.let { char.actionValue(it) } ?: .0) }


        val maxItemValue = issuersItemValues.maxOfOrNull { it.second } ?: 0.0
        val maxActionValue = issuersActionValues.maxOfOrNull { it.second } ?: 0.0

        //If the maximum value is 0, then there is no matching request.
        if (maxItemValue <= 0 && maxActionValue <= 0) return null

        if (maxItemValue > maxActionValue) {

            //Find the issuer with the highest item value.
            val bestIssuer = issuersItemValues.maxByOrNull { it.second }?.first ?: return null

            //If the best issuer's item value is higher than the total value of the requests, propose a new agenda to match the requests.
            if (issuersItemValues.maxOf { it.second } > totalValue) {

                //Pick resources from the best issuer until the total value of the requests is met.
                val resourcesToTransfer = gState.characters[bestIssuer]!!.resources
                    .keys.filter { char.itemValue(it) > 0 }

                // Sort the resource keys by my relative demand.
                val sortedResources = resourcesToTransfer.sortedByDescending { char.itemValueModifier(it) }

                // Pick resources until the total value of the requests is met.
                val resourcesToTransferMap = hashMapOf<String, Double>()
                var remainingValue = totalValue
                for (resource in sortedResources) {
                    if (remainingValue <= 0) break
                    val amount = min(
                        gState.characters[bestIssuer]!!.resources[resource],
                        remainingValue
                    )
                    if (amount > 0) {
                        resourcesToTransferMap[resource] = amount
                        remainingValue -= amount * char.itemValueModifier(resource)
                    }
                }


                return NewAgenda(name, place).also {
                    it.agenda = MeetingAgenda(
                        AgendaType.REQUEST,
                        author = name,
                        attachedRequest = Request(
                            action = UnofficialResourceTransfer(
                                bestIssuer, "home_$bestIssuer", fromHome = true,
                                toWhere = "home_$name",
                                resources = Resources(resourcesToTransferMap)
                            ),
                            issuedTo = hashSetOf(bestIssuer),
                            issuedBy = hashSetOf(name)
                        )
                    )
                }
            }
        } else {
            //If the maximum action value is higher than the total value of the requests, propose a new agenda to match the requests.
            val bestActionIssuer = issuersActionValues.maxByOrNull { it.second }?.first ?: return null
            val action = askForValuableAction(bestActionIssuer, name) ?: return null

            if (gState.characters[bestActionIssuer]!!.actionValue(action) >= totalValue) {
                return NewAgenda(name, place).also {
                    it.agenda = MeetingAgenda(
                        AgendaType.REQUEST,
                        author = name,
                        attachedRequest = Request(
                            action = action,
                            issuedTo = hashSetOf(bestActionIssuer),
                            issuedBy = hashSetOf(name)
                        )
                    )
                }
            }
        }

        return null
    }

    fun executeRequestInMeeting(name: String, place: String): GameAction? {
        gState.requests.values.firstOrNull {
            if (name !in it.issuedTo) return@firstOrNull false
            val eta =
                gState.places[it.action.tgtPlace]!!.shortestPathAndTimeTo(place)?.second ?: return@firstOrNull false
            return@firstOrNull (it.executeTime in gState.time - ReadOnly.constInt("CommandExecuteTolerance") + eta..gState.time + ReadOnly.constInt(
                "CommandExecuteTolerance"
            ) + eta || it.executeTime == 0) && (it.issuedBy.isEmpty() /*System request must be executed regardless of mutualities.*/ || it.issuedBy.sumOf {
                gState.getMutuality(
                    name,
                    it
                )
            } / it.issuedBy.size > it.difficulty()) && it.action.let { it.injectParent(gState); return@let it.isValid() }
        }?.also { request ->
            return request.action.apply { injectParent(gState) }
        }
        return null
    }

    fun askForValuableAction(who: String, name: String): GameAction? {
        val tgtChar = gState.characters[who] ?: return null
        if ("engineer" in tgtChar.trait) {
            //If the place I am managing has a broken apparatus, request repair.
            gState.places.values.filter { it.manager == name }.forEach { placeObj ->
                if (placeObj.apparatuses.any { it.durability < 70f })
                    return Repair(who, placeObj.name, placeObj.apparatuses.first { it.durability < 70f }.ID, gState)
            }


        }
        return null
    }

    fun meetingRoutineEndCondition(name: String): Boolean {
        if (meetingName in gState.scheduledMeetings) {
            return false //The meeting has not started yet.
        }
        val mt = gState.characters[name]!!.currentMeeting
        if (mt == null) {
            return true //The meeting has ended, so the routine should end.
        }

        if (mt != meeting) {
            return true //The character has been transferred to another meeting.
        }
        //Now, given that we are in the correct meeting, check if the meeting is over.
        if (meeting.type == Meeting.MeetingType.DIVISION_LEADER_ELECTION && meeting.voteResults.isEmpty()) return false //If the meeting is a division leader election and there are no vote results, the meeting is not over yet.
        if (meeting.time + 1800 / ReadOnly.DT >= gState.time) return false //At least, wait until the meeting has happened for 30 minutes.
        return routineStartTime + 7200 / ReadOnly.DT <= gState.time || meeting.currentAttention < 10 //Leave the meeting if it is boring or it is getting too long.

    }

    fun interceptCondition(
        name: String,
        place: String
    ): GameAction {
        if (gState.getMutuality(
                name,
                meeting.currentSpeaker!!
            ) > ReadOnly.const("SpeakerInterceptMutualityThreshold")
        )
            return Wait(name, place)
        else {
            val action = Intercept(name, place).also { it.injectParent(gState) }
            if (action.isValid())
                return action
            return Wait(name, place)
        }
    }

    fun endMeetingIfLowAttention(
        name: String,
        place: String
    ): GameAction? {
        //If the attention of the meeting is low, end the meeting.
        if (meeting.currentCharacters.count() > 1 && meeting.currentAttention < 10) {
            return EndMeeting(name, place)
        }
        return null
    }
}