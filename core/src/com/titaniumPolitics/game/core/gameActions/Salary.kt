package com.titaniumPolitics.game.core.gameActions

import kotlinx.serialization.Serializable

@Serializable
//Salary is performed by the party leader. It decides the amount of resources to be paid to the party members.
class Salary(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    var resources = hashMapOf("ration" to 2, "water" to 2)
    override fun chooseParams()
    {
    }

    override fun execute()
    {
        val who =
            (parent.ongoingMeetings.filter { it.value.currentCharacters.contains(sbjCharacter) }
                .flatMap { it.value.currentCharacters }).toHashSet()

        val party = parent.parties.values.find { it.members.containsAll(who + sbjCharacter) }!!
        val guildHall = party.home
//        if (party.isDailySalaryPaid.keys.none { it == tgtCharacter })
//        {
//            println("Warning: $tgtCharacter is not eligible to be paid from ${party.name}.")
//            return
//        }
//        if (party.isDailySalaryPaid[tgtCharacter] == true)
//        {
//            println("Warning: $tgtCharacter has already been paid from ${party.name} today.")
//            return
//        }
        who.forEach { character ->
            if (
                resources.all { (what, amount) -> (parent.places[guildHall]!!.resources[what] ?: .0) >= amount }
            )
            {
                resources.forEach { (what, amount) ->
                    parent.places[guildHall]!!.resources[what] =
                        (parent.places[guildHall]!!.resources[what] ?: .0) - amount
                    parent.characters[character]!!.resources[what] =
                        (parent.characters[character]!!.resources[what] ?: .0) + amount
                }
                //party.isDailySalaryPaid[tgtCharacter] = true
                println("$character is paid $resources from $${party.name}.")
                parent.characters[character]!!.frozen++

            } else
            {
                println("Not enough resources to pay salary to $character: $tgtPlace, ${parent.places[tgtPlace]!!.resources}")
                //Party integrity decreases
                parent.setPartyMutuality(party.name, party.name, -1.0)
                //Opinion of the leader of the party decreases
                if (party.leader != "")
                {
                    parent.setMutuality(character, party.leader, -1.0)
                }
                //TODO: are we sure that if the unpaid people are not in the meeting, there is no penalty to the party integrity?
//
            }
        }
        party.isSalaryPaid =
            true//Even if some members are not paid, the salary is considered paid, and cannot be paid again this quarter.

    }

    override fun isValid(): Boolean
    {
        val who =
            (parent.ongoingMeetings.filter { it.value.currentCharacters.contains(sbjCharacter) }
                .flatMap { it.value.currentCharacters }).toHashSet()

        val party = parent.parties.values.find { it.members.containsAll(who + sbjCharacter) }!!
//        if (party.isDailySalaryPaid.keys.none { it == tgtCharacter })
//        {
//            //println("Warning: $tgtCharacter is not eligible to be paid from ${party.name}.")
//            return false
//        }
//        if (party.isDailySalaryPaid[tgtCharacter] == true)
//        {
//            //println("Warning: $tgtCharacter has already been paid from ${party.name} today.")
//            return false
//        }
        return !party.isSalaryPaid && who.isNotEmpty() && sbjCharacter == party.leader
    }

}