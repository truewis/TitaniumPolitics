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
    var subject: String,
    var scheduledCharacters: HashSet<String>,
    var place: String,
    var currentCharacters: HashSet<String> = hashSetOf()
)
{
    var involvedParty: String = ""
    var auxSubject = ""
    var currentSpeaker = ""
    var currentAttention = 0
    var agendas = arrayListOf<MeetingAgenda>()

    fun endMeeting(gameState: GameState)
    {
        //Remove the meeting from the ongoingMeetings or ongoingConferences list.
        if (gameState.ongoingMeetings.containsValue(this))
            gameState.ongoingMeetings.remove(gameState.ongoingMeetings.filter { it.value == this }.keys.first())
        else
            gameState.ongoingConferences.remove(gameState.ongoingConferences.filter { it.value == this }.keys.first())
        //TODO: Execute dominant agendas.
        var dominantAgenda: ArrayList<MeetingAgenda> = arrayListOf()
        var maxAgendas = 0
        when (subject)
        {
            "divisionDailyConference" -> maxAgendas = 2
            "triumvirateMeeting" -> maxAgendas = 1
            "cabinetMeeting" -> maxAgendas = 2
            "hearing" -> maxAgendas = 1
            "impeachment" -> maxAgendas = 1
        }
        //Add dominant agendas to the dominantAgenda list. E.g. When maxAgendas is 2, the threshold for dominant agenda is 34%.
        agendas.forEach { if (it.agreement > 100 / (maxAgendas + 1)) dominantAgenda.add(it) }

        dominantAgenda.forEach {
            when (it.subjectType)
            {
                "proofOfWork" ->
                {


                }

                "budgetProposal" ->
                {
                    println("Budget proposal executed.")
                    gameState.isBudgetProposed = true
                    gameState.budget = it.subjectIntParams
                    println(it.subjectIntParams)

                }

                "budgetResolution" ->
                {
                    with(gameState) {
                        //Distribute resources according to the budget plan.
                        places["reservoirNorth"]!!.resources["water"] =
                            places["reservoirNorth"]!!.resources["water"]!! - budget.values.sum()

                        places["farm"]!!.resources["ration"] =
                            places["farm"]!!.resources["ration"]!! - budget.values.sum()

                        budget.forEach {
                            val guildHall = parties[it.key]!!.home;
                            places[guildHall]!!.resources["water"] =
                                (places[guildHall]!!.resources["water"] ?: 0) + it.value
                            places[guildHall]!!.resources["ration"] =
                                (places[guildHall]!!.resources["ration"] ?: 0) + it.value
                        }
                    }


                }

                "praise" ->
                {
                    currentCharacters.forEach { character ->
                        gameState.setMutuality(character, it.subjectParams["who"]!!, 1.0)
                    }


                }

                "denounce" ->
                {
                    currentCharacters.forEach { character ->
                        gameState.setMutuality(character, it.subjectParams["who"]!!, -3.0)
                    }


                }

                "workingHoursChange" ->
                {
                    gameState.places[it.subjectParams["where"]!!]!!.workHoursStart =
                        it.subjectIntParams["workHoursStart"]!!
                    gameState.places[it.subjectParams["where"]!!]!!.workHoursEnd = it.subjectIntParams["workHoursEnd"]!!


                }

                "reassignWorkersToApparatus" ->
                {


                }

                "salary" ->
                {
                    var amount = 2
                    var what1 = "ration"
                    var what2 = "water"
                    val party = gameState.parties[involvedParty]!!
                    val guildHall = party.home
                    party.members.forEach {

                        with(gameState) {

                            if (
                                (places[guildHall]!!.resources[what1]
                                    ?: 0) >= amount && (places[guildHall]!!.resources[what2] ?: 0) >= amount
                            )
                            {
                                places[guildHall]!!.resources[what1] =
                                    (places[guildHall]!!.resources[what1] ?: 0) - amount
                                characters[it]!!.resources[what1] =
                                    (characters[it]!!.resources[what1] ?: 0) + amount

                                places[guildHall]!!.resources[what2] =
                                    (places[guildHall]!!.resources[what2] ?: 0) - amount
                                characters[it]!!.resources[what2] =
                                    (characters[it]!!.resources[what2] ?: 0) + amount

                                println("$it is paid $amount $what1 and $amount $what2 from $${party.name}.")

                            } else
                            {
                                println("Not enough resources to pay salary to $it: $guildHall, ${places[guildHall]!!.resources}")
                                //Party integrity decreases
                                setPartyMutuality(party.name, party.name, -1.0)
                                //Opinion of the leader of the party decreases
                                if (party.leader != "")
                                {
                                    setMutuality(it, party.leader, -1.0)
                                }
                                // party.isDailySalaryPaid[tgtCharacter] = true TODO: this is a hack to prevent infinite loop. This is a lie, but who would be able to complain?
                            }

                        }

                    }
                    party.isSalaryPaid = true

                }

                "appointMeeting" ->
                {


                }
            }
        }
        //TODO: Affect mutuality based on lost agendas.

    }
}