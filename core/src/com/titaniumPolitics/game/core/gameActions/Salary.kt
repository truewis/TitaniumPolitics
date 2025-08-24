package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable
import kotlin.collections.get

@Serializable
//Salary is performed by the party leader. It decides the amount of resources to be paid to the party members.
class Salary(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    val standardRate
        get() = standardQuarterlyRate(parent.parties[sbjCharObj.currentMeeting!!.involvedParty!!]!!.type!!)

    override fun chooseParams() {
    }

    override fun execute() {
        val who =
            (parent.ongoingMeetings.filter { it.value.currentCharacters.contains(sbjCharacter) }
                .flatMap { it.value.currentCharacters }).toHashSet()

        val party = parent.parties.values.find { it.members.containsAll(who + sbjCharacter) }!!
        val guildHall = party.home

        who.forEach { character ->
            standardRate.forEach { (what, amount) ->
                parent.places[guildHall]!!.resources[what] =
                    parent.places[guildHall]!!.resources[what] - amount
                parent.characters[character]!!.resources[what] =
                    parent.characters[character]!!.resources[what] + amount
            }
            //Opinion of the leader of the party increases.

            parent.setMutuality(
                character,
                party.leader!!,
                ReadOnly.const("salaryMutualityIncrease"),
                "SalaryLeaderTrustIncrease"
            )
        }
        //Party integrity increases
        parent.setPartyMutuality(
            party.name,
            weightedDelta = ReadOnly.const("salaryMutualityIncrease"),
            reasonKey = "SalaryIntegrityIncrease"
        )

        party.isSalaryPaid =
            true//Even if some members are not paid, the salary is considered paid, and cannot be paid again this quarter.

    }

    override fun isValid(): Boolean {
        val who =
            sbjCharObj.currentMeeting?.currentCharacters
                ?: return false //If there is no meeting, the salary cannot be paid.

        val party = parent.parties.values.find { it.name == sbjCharObj.currentMeeting!!.involvedParty }!!
        return !party.isSalaryPaid && who.isNotEmpty() && sbjCharacter == party.leader && reason(
            standardRate.all { (what, amount) -> parent.places[party.home]!!.resources[what] >= amount },
            "salary-resources"
        )
    }

    companion object {
        fun standardQuarterlyRate(partyType: String): Map<String, Double> {
            return when (partyType) {
                "cabinet" -> hashMapOf("ration" to 50.0, "water" to 50.0, "phosphorite" to 0.1)
                "division" -> hashMapOf("ration" to 30.0, "water" to 30.0, "phosphorite" to 0.03)
                "workplace" -> hashMapOf("ration" to 15.0, "water" to 15.0, "phosphorite" to 0.01)
                else -> throw IllegalArgumentException("Salary can only be performed in cabinet or division daily conferences.")
            }
        }
    }

}