package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.ReadOnly.const
import com.titaniumPolitics.game.core.ReadOnly.constInt
import com.titaniumPolitics.game.core.ReadOnly.DT
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.UUID
import kotlin.collections.get

/*
*  This class represents a meeting in the game. It is used to represent meetings that are scheduled to happen in the future.
*  It is also used to represent meetings that are happening right now.
*
*  Conferences are meetings that are scheduled to happen regularly, and is run by a specific party.
* */
@Serializable
class Meeting(
    var time: Int,
    var type: MeetingType,
    var scheduledCharacters: HashSet<String>,
    var place: String,
    var currentCharacters: HashSet<String> = hashSetOf()
) {
    val ID = UUID.randomUUID().toString()
    var startTime = 0 //The time when the meeting actually starts.
    var involvedParty: String? = null
    var currentSpeaker: String? = null
    var currentAttention = 0
        set(value) {
            field = when {
                value < 0 -> 0
                else -> value
            }
        }
    var agendas = arrayListOf<MeetingAgenda>()
    var voteResults = hashMapOf<String, Int>()

    @Transient
    var onCandidatesSet = ArrayList<(Set<String>) -> Unit>() //Called when the candidates for the election are set.

    @Transient
    var onVoteResults = ArrayList<() -> Unit>()

    var nominationFinishedTime: Int? =
        null //This is the time when the nomination is finished. It is used to determine when the election is over.

    fun finishNomination() {
        //This is called when the nomination is finished.
        //It will call the onCandidatesSet callbacks with the candidates.
        val candidates = agendas.filter { it.type == AgendaType.NOMINATE }
            .map { it.subjectParams["character"]!! }
            .toSet()
        onCandidatesSet.forEach { it(candidates) }
        //Set the nomination finished time to the current time.
        nominationFinishedTime = time
        Logger.write("Nomination finished at time $time with candidates: $candidates", Logger.LogLevel.INFO)
    }

    fun startVoting(gameState: GameState) {
        //involvedParty is not empty for divisionLeaderElections.
        val party = gameState.parties[involvedParty]!!

        if (party.leader != "") {
            Logger.write("The leader of the party $involvedParty exists as ${party.leader}, but the election is still happening.")
            throw IllegalStateException("The leader of the party $involvedParty exists as ${party.leader}, but the election is still happening.")
        }
        val candidates = party.members.filter { char ->
            agendas.any {
                it.type == AgendaType.NOMINATE && it.subjectParams["character"] == char
            }
        }
        voteResults = party.getVotes(candidates.toSet())
        onVoteResults.forEach { it() }

        val winner =
            voteResults.maxByOrNull { it.value }?.key ?: throw IllegalStateException("No winner found in the election.")

        gameState.parties[involvedParty]!!.changeLeader(winner)
        Logger.write("The leader of the party $involvedParty is elected as $winner.", Logger.LogLevel.INFO)
    }

    fun endMeeting(gameState: GameState) {
        //If budget is not resolved in a budget resolution meeting, remove all proposed budgets, and decrease party integrity.
        if (type == MeetingType.BUDGET_RESOLUTION) {
            val party = gameState.parties[involvedParty]!!
            if (!party.isBudgetResolved) {
                party.proposedBudgets.clear()
                party.isBudgetProposed = false
                gameState.setPartyMutuality(
                    party.name,
                    weightedDelta = const("BudgetNotResolvedDeltaPartyIntegrity"), reasonKey =
                        "mutuality-BudgetNotResolved"
                )
                Logger.write(
                    "The budget of the party ${party.name} is not resolved. Proposed budgets are cleared, and party integrity is decreased.",
                    Logger.LogLevel.INFO
                )
            }
        }
        //If there are any unsatisfied proof of work requests, affect the mutualities.
        agendas.forEach {
            if (it.type == AgendaType.PROOF_OF_WORK && it.attachedRequest != null && it.informationKeys.isEmpty()) {
                //Mutuality decreases.
                it.attachedRequest!!.issuedBy.forEach { issuedBy ->
                    if (gameState.characters[issuedBy]!!.trait.contains("psychopath"))
                        it.attachedRequest!!.issuedTo.forEach { issuedTo ->
                            gameState.setMutuality(
                                issuedBy,
                                issuedTo,
                                -const("RequestFinishDeltaMutuality") * 2,
                                "RequestFinishDeltaMutuality-Psychopath"
                            )
                        }
                    else {
                        it.attachedRequest!!.issuedTo.forEach { issuedTo ->
                            gameState.setMutuality(
                                issuedBy,
                                issuedTo,
                                -const("RequestFinishDeltaMutuality"),
                                "RequestFinishDeltaMutuality"
                            )
                        }
                    }
                }
            }
        }
        //Remove the meeting from the ongoingMeetings.
        if (gameState.ongoingMeetings.containsValue(this)) {
            gameState.removeOngoingMeeting(gameState.ongoingMeetings.filter { it.value == this }.keys.first())
        } else {
            Logger.write("Meeting $this is not found in the ongoingMeetings.")
            throw IllegalStateException("Meeting $this is not found in the ongoingMeetings.")
        }
    }


    //Agreement change is computed every turn based on deltaAgreement, rather than changing once when information are added.
    //This is to prevent the meeting going nowhere when there isn't enough supporting information.
    fun onTimeChange(gameState: GameState) {

//        if (type == MeetingType.TALK) {
//            //Chill meeting
//            currentCharacters.forEach {
//                gameState.setMutuality(
//                    it, it, delta = DT / const("ChillMeetingWillTau") * const("mutualityMax"),
//                    "ChillMeetingWill"
//                )
//            }
//        } else {
//            //Work meeting
//            currentCharacters.forEach {
//                gameState.setMutuality(
//                    it, it, delta = DT / const("WorkMeetingWillTau") * const("mutualityMax"),
//                    "WorkMeetingWill"
//                )
//            }
//        }
        agendas.forEach { agenda ->


        }
    }

    fun isValidTimeToStart(tgtTime: Int): Boolean {
        //Check if the meeting is scheduled in the future.
        return tgtTime - time in -constInt("MeetingStartTolerance")..constInt("MeetingStartTolerance")
    }

    override fun toString(): String {
        return "Meeting(type=$type, time=$time, place='$place', scheduledCharacters=$scheduledCharacters, currentCharacters=$currentCharacters, involvedParty=$involvedParty, currentSpeaker=$currentSpeaker, currentAttention=$currentAttention, agendas=$agendas, voteResults=$voteResults, nominationFinishedTime=$nominationFinishedTime)"
    }

    enum class MeetingType {
        TALK, DIVISION_LEADER_ELECTION, DIVISION_DAILY_CONFERENCE, BUDGET_PROPOSAL, BUDGET_RESOLUTION, CABINET_DAILY_CONFERENCE, TRIUMVIRATE_DAILY_CONFERENCE
    }
}
