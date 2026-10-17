package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.MutualityMatrix
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
        val payments = paymentsForMembers()
        val totalPayment = totalPayment(payments)
        parent.places[guildHall]!!.resources -= totalPayment
        payments.forEach { (character, payment) ->
            parent.characters[character]!!.resources += payment
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

    override fun deltaWill(): MutualityMatrix {
        val ret = super.deltaWill()

        who.forEach { character ->
            ret.addMutuality(
                character,
                party.leader!!,
                ReadOnly.const("salaryMutualityIncrease"),
                "SalaryLeaderTrustIncrease"
            )
        }
        return ret
    }

    override fun isValid(): Boolean {
        if (sbjCharObj.currentMeeting == null) return false
        if (sbjCharObj.currentMeeting!!.involvedParty == null) return false
        if (party.isSalaryPaid || who.isEmpty() || sbjCharacter != party.leader) return false
        val home = party.home?.let { parent.places[it] } ?: return false
        val requiredResources = totalPayment(paymentsForMembers())
        return reason(home.resources.contains(requiredResources), "salary-resources")
    }

    private fun paymentsForMembers(): Map<String, Resources> =
        who.associateWith { character ->
            standardRate * salaryMultiplier(parent.characters[character]!!)
        }

    private fun totalPayment(payments: Map<String, Resources>): Resources {
        val total = Resources()
        payments.values.forEach { total += it }
        return total
    }

    private fun salaryMultiplier(character: com.titaniumPolitics.game.core.Character): Double {
        val policies = character.division?.policies ?: emptySet()
        val incentives = listOf(
            "engineerIncentive" to "engineer",
            "soldierIncentive" to "soldier",
            "administratorIncentive" to "administrator",
            "minerIncentive" to "miner",
            "laborerIncentive" to null
        )
        var multiplier = 1.0
        incentives.forEach { (policy, trait) ->
            if (policy in policies) {
                val qualifies = if (trait == null) {
                    character.type == com.titaniumPolitics.game.core.Character.Type.ANON
                } else trait in character.trait
                multiplier += if (qualifies) 0.5 else -0.2
            }
        }
        if ("seniority" in policies) {
            multiplier += 0.1 * (character.age - 40) / 10.0
        }
        return multiplier
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