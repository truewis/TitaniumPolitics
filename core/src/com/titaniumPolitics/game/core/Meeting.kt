package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable

/*
*  This class represents a meeting in the game. It is used to represent meetings that are scheduled to happen in the future.
*  It is also used to represent meetings that are happening right now.
*
*  Conferences are meetings that are scheduled to happen regularly, and is run by a specific party.
* */
@Serializable
class Meeting(
    var time: Int,
    var type: String,
    var scheduledCharacters: HashSet<String>,
    var place: String,
    var currentCharacters: HashSet<String> = hashSetOf()
)
{
    var involvedParty: String = ""
    var currentSpeaker = ""
    var currentAttention = 0
    var agendas = arrayListOf<MeetingAgenda>()

    fun endMeeting(gameState: GameState)
    {
        //If this is an election, elect the leader from the mutuality matrix.
        if (type == "divisionLeaderElection")
        {

            if (gameState.parties[involvedParty]!!.leader != "")
                throw IllegalStateException("The leader of the party $involvedParty exists as ${gameState.parties[involvedParty]!!.leader}, but the election is still happening.")
            val leader = gameState.parties[involvedParty]!!.members.maxByOrNull { s ->
                gameState.parties[involvedParty]!!.members.sumOf { gameState.getMutuality(it, s) }
            }!!//TODO: This logic has to be more thorough. display the actual election process.
            gameState.parties[involvedParty]!!.leader = leader
        }
        //Remove the meeting from the ongoingMeetings or ongoingConferences.
        if (gameState.ongoingMeetings.containsValue(this))
        {
            gameState.ongoingMeetings.remove(gameState.ongoingMeetings.filter { it.value == this }.keys.first())
        } else if (gameState.ongoingConferences.containsValue(this))
        {
            gameState.ongoingConferences.remove(gameState.ongoingConferences.filter { it.value == this }.keys.first())
        } else
            throw IllegalStateException("Meeting $this is not found in the ongoingMeetings or ongoingConferences.")
    }
}