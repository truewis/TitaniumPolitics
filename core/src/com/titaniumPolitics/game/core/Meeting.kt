package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.ReadOnly.const
import com.titaniumPolitics.game.core.ReadOnly.dt
import com.titaniumPolitics.game.debugTools.Logger
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
        when (type)
        {
            "divisionLeaderElection" ->
            {

                //involvedParty is not empty for divisionLeaderElections.
                val party = gameState.parties[involvedParty]!!

                if (party.leader != "")
                {
                    Logger.warning("The leader of the party $involvedParty exists as ${party.leader}, but the election is still happening.")
                    throw IllegalStateException("The leader of the party $involvedParty exists as ${party.leader}, but the election is still happening.")
                }
                val leader = party.members.filter { char ->
                    agendas.any {
                        //In order to be a candidate, the character has to be nominated first.
                        it.type == AgendaType.NOMINATE && it.subjectParams["character"] == char
                    }
                }.maxByOrNull { s ->
                    (party.members.sumOf {
                        gameState.getMutuality(
                            it,
                            s
                        ) * party.getMultiplier(it)
                    }).also { println("The average support of $s is ${it / party.size}.") }
                }!!//TODO: This logic has to be more thorough. display the actual election process.
                gameState.parties[involvedParty]!!.leader = leader
                println("The leader of the party $involvedParty is elected as $leader.")
            }

            "divisionDailyConference" ->
            {
                if (agendas.any { it.type == AgendaType.FIRE_MANAGER })
                {
                    //TODO: This logic has to be more thorough. display the actual election process.
                }
            }
        }
        //Remove the meeting from the ongoingMeetings.
        if (gameState.ongoingMeetings.containsValue(this))
        {
            gameState.ongoingMeetings.remove(gameState.ongoingMeetings.filter { it.value == this }.keys.first())
        } else
        {
            Logger.warning("Meeting $this is not found in the ongoingMeetings.")
            throw IllegalStateException("Meeting $this is not found in the ongoingMeetings.")
        }
    }


    //Agreement change is computed every turn based on deltaAgreement, rather than changing once when information are added.
    //This is to prevent the meeting going nowhere when there isn't enough supporting information.
    fun onTimeChange(gameState: GameState)
    {

        if (type == "")
        {
            //Chill meeting
            currentCharacters.forEach {
                gameState.setMutuality(it, delta = dt / const("ChillMeetingWillTau") * const("mutualityMax"))
            }
        } else
        {
            //Work meeting
            currentCharacters.forEach {
                gameState.setMutuality(it, delta = dt / const("WorkMeetingWillTau") * const("mutualityMax"))
            }
        }
        agendas.forEach { agenda ->


        }
    }
}