package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Resources
import kotlinx.serialization.Serializable
import kotlin.collections.get

@Serializable
//Salary is performed by the party leader. It decides the amount of resources to be paid to the party members.
data class Salary(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    val who
        get() =
            party.members - sbjCharacter

    val party get() = parent.parties[sbjCharObj.currentMeeting!!.involvedParty]!!

    val standardRate
        get() = standardQuarterlyRate(parent.parties[sbjCharObj.currentMeeting!!.involvedParty!!]!!.type!!)

    override fun chooseParams() {
    }

    override fun execute() {

        val guildHall = party.home

        who.forEach { character ->
            var multiplyer = 1.0
            val charObj = parent.characters[character]!!
            if ("engineerIncentive" in parent.characters[character]!!.division!!.policies) {
                if ("engineer" in charObj.trait) {
                    multiplyer += 0.5
                } else {
                    multiplyer -= 0.2
                }
            }
            if ("soldierIncentive" in parent.characters[character]!!.division!!.policies) {
                if ("soldier" in charObj.trait) {
                    multiplyer += 0.5
                } else {
                    multiplyer -= 0.2
                }
            }
            if ("administratorIncentive" in parent.characters[character]!!.division!!.policies) {
                if ("administrator" in charObj.trait) {
                    multiplyer += 0.5
                } else {
                    multiplyer -= 0.2
                }
            }
            if ("minerIncentive" in parent.characters[character]!!.division!!.policies) {
                if ("miner" in charObj.trait) {
                    multiplyer += 0.5
                } else {
                    multiplyer -= 0.2
                }
            }
            parent.places[guildHall]!!.resources -= standardRate * multiplyer
            charObj.resources += standardRate * multiplyer
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
        super.execute()

    }

    override fun isValid(): Boolean {
        if (sbjCharObj.currentMeeting == null) return false
        if (sbjCharObj.currentMeeting!!.involvedParty == null) return false
        return !party.isSalaryPaid && who.isNotEmpty() && sbjCharacter == party.leader && reason(
            standardRate.all { (what, amount) -> parent.places[party.home]!!.resources[what] >= amount * who.size },
            "salary-resources"
        )
    }

    companion object {
        fun standardQuarterlyRate(partyType: Party.Type): Resources {
            return when (partyType) {
                Party.Type.CABINET -> Resources("ration" to 50.0, "water" to 50.0, "phosphorus" to 0.1)
                Party.Type.DIVISION -> Resources("ration" to 30.0, "water" to 30.0, "phosphorus" to 0.03)
                Party.Type.WORKPLACE -> Resources("ration" to 15.0, "water" to 15.0, "phosphorus" to 0.01)
                else -> throw IllegalArgumentException("Salary can only be performed in cabinet or division daily conferences.")
            }
        }
    }

}