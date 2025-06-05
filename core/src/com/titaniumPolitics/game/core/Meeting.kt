package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.ReadOnly.const
import com.titaniumPolitics.game.core.ReadOnly.constInt
import com.titaniumPolitics.game.core.ReadOnly.dt
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable
import kotlin.div
import kotlin.math.roundToInt
import kotlin.text.get
import kotlin.times

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
    var involvedParty: String = ""
    var currentSpeaker = ""
    var currentAttention = 0
    var agendas = arrayListOf<MeetingAgenda>()
    var voteResults = hashMapOf<String, Int>()
    var onVoteResults = ArrayList<() -> Unit>()

    fun endMeeting(gameState: GameState) {
        //If this is an election, elect the leader from the mutuality matrix.
        when (type) {
            MeetingType.DIVISION_LEADER_ELECTION -> {

                //involvedParty is not empty for divisionLeaderElections.
                val party = gameState.parties[involvedParty]!!

                if (party.leader != "") {
                    Logger.warning("The leader of the party $involvedParty exists as ${party.leader}, but the election is still happening.")
                    throw IllegalStateException("The leader of the party $involvedParty exists as ${party.leader}, but the election is still happening.")
                }
                voteResults = party.members
                    .filter { char ->
                        agendas.any {
                            it.type == AgendaType.NOMINATE && it.subjectParams["character"] == char
                        }
                    }.associateWith { candidate ->
                        val score = party.members.sumOf {
                            gameState.getMutuality(it, candidate) * party.getMultiplier(it)
                        }
                        println("The average support of $candidate is ${score / party.size}.")
                        score.roundToInt()
                    } as HashMap<String, Int>//TODO: This logic has to be more thorough. display the actual election process.
                onVoteResults.forEach { it() }

                val leader = voteResults.maxByOrNull { it.value }?.key ?: ""

                gameState.parties[involvedParty]!!.leader = leader
                println("The leader of the party $involvedParty is elected as $leader.")
            }

            MeetingType.DIVISION_DAILY_CONFERENCE -> {
                if (agendas.any { it.type == AgendaType.FIRE_MANAGER }) {
                    //TODO:
                }
            }

            else -> {}
        }
        //Remove the meeting from the ongoingMeetings.
        if (gameState.ongoingMeetings.containsValue(this)) {
            gameState.removeOngoingMeeting(gameState.ongoingMeetings.filter { it.value == this }.keys.first())
        } else {
            Logger.warning("Meeting $this is not found in the ongoingMeetings.")
            throw IllegalStateException("Meeting $this is not found in the ongoingMeetings.")
        }
    }


    //Agreement change is computed every turn based on deltaAgreement, rather than changing once when information are added.
    //This is to prevent the meeting going nowhere when there isn't enough supporting information.
    fun onTimeChange(gameState: GameState) {

        if (type == MeetingType.TALK) {
            //Chill meeting
            currentCharacters.forEach {
                gameState.setMutuality(it, delta = dt / const("ChillMeetingWillTau") * const("mutualityMax"))
            }
        } else {
            //Work meeting
            currentCharacters.forEach {
                gameState.setMutuality(it, delta = dt / const("WorkMeetingWillTau") * const("mutualityMax"))
            }
        }
        agendas.forEach { agenda ->


        }
    }

    fun isValidTimeToStart(tgtTime: Int): Boolean {
        //Check if the meeting is scheduled in the future.
        return tgtTime - time in -constInt("MeetingStartTolerance")..constInt("MeetingStartTolerance")
    }

    enum class MeetingType {
        TALK, DIVISION_LEADER_ELECTION, DIVISION_DAILY_CONFERENCE, BUDGET_PROPOSAL, BUDGET_RESOLUTION, CABINET_DAILY_CONFERENCE, TRIUMVIRATE_DAILY_CONFERENCE
    }
}