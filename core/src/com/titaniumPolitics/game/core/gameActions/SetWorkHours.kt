package com.titaniumPolitics.game.core.gameActions

import kotlinx.serialization.Serializable

@Serializable
//Salary is performed by the party leader. It decides the amount of resources to be paid to the party members.
class SetWorkHours(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    var start = 8
    var end = 17
    var where = ""
    override fun chooseParams()
    {
    }

    override fun execute()
    {
        parent.places[where]!!.workHoursStart = start
        parent.places[where]!!.workHoursEnd = end
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
        return parent.places[where]!!.responsibleParty == party.name && sbjCharacter == party.leader
    }

}