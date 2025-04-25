package com.titaniumPolitics.game.core.gameActions

import kotlinx.serialization.Serializable

@Serializable
//Salary is performed by the party leader. It decides the amount of resources to be paid to the party members.
class SetWorkers(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    var workers = 0
    var apparatusID = ""
    override fun chooseParams()
    {
    }

    override fun execute()
    {
        parent.getApparatus(apparatusID).plannedWorker == workers

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
        return parent.getApparatusPlace(apparatusID).responsibleParty == party.name && sbjCharacter == party.leader
    }

}