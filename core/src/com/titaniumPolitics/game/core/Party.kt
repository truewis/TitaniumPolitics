package com.titaniumPolitics.game.core

import com.badlogic.gdx.math.MathUtils.clamp
import com.titaniumPolitics.game.core.gameActions.Salary
import kotlinx.serialization.Serializable
import kotlin.collections.set

@Serializable
class Party : GameStateElement() {
    private var _name: String? = null
    override val name: String
        get() = _name ?: parent.parties.filter { it.value == this }.keys.first().also { _name = it }
    var leader: String? = null
    var administrator: String? = null //The person who manages the party.
    var treasurer: String? = null //The person who manages the party's finances.
    var overseer: String? = null //The person who oversees worker's activities.
    var type: String? = null
    var home: String? = null //The place where the party is based.
    var members = hashSetOf<String>()
    var isSalaryPaid = false //This variable is reset every quarter.
    val anonMembers: HashSet<String>
        get() = members.filter { parent.characters[it]!!.type == Character.Type.ANON }
            .toHashSet() //Anonymous members are those who are not registered in the party.
    val numAnonMembers: Int
        get() = anonMembers.sumOf { parent.characters[it]!!.reliant }
    val realMembers: HashSet<String>
        get() = (members - anonMembers).toHashSet()
    val directorMembers: HashSet<String>
        get() = members.filter { char -> parent.places.any { it.value.manager == char } }
            .toHashSet() //Directors are the ones who can make decisions in the party.
    val nonDirectorMembers: HashSet<String>
        get() = members.filter { char -> parent.places.none { it.value.manager == char } }
            .toHashSet() //Non-directors are the ones who cannot make decisions in the party.
    val size: Int
        get() = members.sumOf { getMultiplier(it) }

    var isBudgetProposed = false
    var isBudgetResolved = false

    /**Party name to resource budget map. This is cleared each quarter, and filled when the budget is resolved.*/
    var budget = Budget(hashMapOf())

    /**Budgets proposed in the current quarter. These are cleared when the budget is resolved.*/
    val proposedBudgets = hashMapOf<String, Budget>()

    /**
     * This is a standard budget for the party, calculated based on its type and members' standard salary.
     */
    val standardBudget: Budget
        get() {
            when (type) {
                "cabinet" -> {
                    val divisions = parent.parties.filter { it.value.type == "division" }.values
                    val resMap = hashMapOf<String, Resources>()
                    divisions.forEach { division ->
                        val directorWage =
                            Resources(Salary.standardQuarterlyRate("division")) * (division.directorMembers - division.leader).size
                        resMap[division.name] = division.standardBudget.sum() + directorWage
                    }
                    return Budget(resMap)
                }

                "division" -> {
                    val workplaces = parent.places.filter { it.value.responsibleDivision == name }.values
                    val resMap = hashMapOf<String, Resources>()
                    workplaces.forEach {
                        resMap[it.name] = it.workplaceParty?.standardBudget?.sum() ?: Resources()
                    }
                    return Budget(resMap)
                }

                "workplace" -> {
                    val employeeWage =
                        Resources(Salary.standardQuarterlyRate("workplace")) * (realMembers - directorMembers).size
                    //Workplace directors are paid from the division budget, not workplace budget.
                    //Laborer salary is included in apparatus operation cost.
                    val workplace = places.first { place -> place.workplaceParty == this }
                    val apparatusOperationCost = Resources()
                    workplace.apparatuses.forEach {
                        apparatusOperationCost += it.hourlyOperationBudget * workplace.workHoursLength * ReadOnly.constInt(
                            "quarterInDays"
                        )//Length of quarter
                    }
                    return Budget(hashMapOf(name to (employeeWage + apparatusOperationCost)))
                }

                else -> return Budget(hashMapOf())
            }
        }

    /**This is average person to person mutuality of all members.*/
    fun individualMutuality(name: String): Double = members.sumOf { parent.getMutuality(it, name) } / members.size

    var resources = hashMapOf<String, Int>()
    val integrity: Double
        get() = parent.getPartyMutuality(this.name, this.name)

    val places: Collection<Place>
        get() = parent.places.filter { it.value.responsibleDivision == name }.values

    val currentWorker: Int
        get() = places.sumOf { it.currentWorker }

    val plannedWorker: Int
        get() = places.sumOf { it.plannedWorker }

    fun causeDeaths(num: Int) {
        if (numAnonMembers >= num) {
            killAnonMembers(num) //If there are anon members left, kill them first.
        } else if (num >= size) {
            killAnonMembers(numAnonMembers)
            members.forEach { parent.characters[it]!!.alive = false }
        } else {
            //kill members
            for (i in 0..<num - numAnonMembers)
                members.filter { parent.characters[it]!!.alive }.random()
                    .let { parent.characters[it]!!.alive = false }//kill num - anonymousMembers members

            killAnonMembers(numAnonMembers)
        }
        parent.popChanged.forEach { it() }

    }

    fun vacancyRole(): String? {
        return when {
            leader == null -> "leader"
            type == "division" && parent.places.values.any { it.responsibleDivision == name && it.manager == null } -> "director_" + parent.places.values.first { it.responsibleDivision == name && it.manager == null }.name//Director role is not filled.
            administrator == null || administrator == leader -> "administrator"
            treasurer == null || treasurer == leader -> "treasurer"
            overseer == null || overseer == leader -> "overseer"
            else -> null
        }
    }

    //Used in mutuality calculation. Is 1 for characters.
    fun getMultiplier(char: String): Int {
        return if (members.contains(char))
            1
        else if (char.startsWith("$name-Anon")) {
            parent.characters[char]!!.reliant
        } else
            0
    }

    val totalVotes
        get() =
            members.sumOf { getMultiplier(it) }

    fun getVotes(candidates: Set<String>): HashMap<String, Int> {
        val voteResults = candidates.associateWith { candidate ->
            0
        } as HashMap<String, Int>
        members.forEach { mem ->
            val bestCandidate = candidates.maxBy { parent.getMutuality(mem, it) }
            voteResults[bestCandidate] = (voteResults[bestCandidate] ?: 0) + getMultiplier(mem)
        }
        return voteResults
    }


    /**
     * Kills anonymous members of the party.
     * The amount of killed members is distributed evenly among the anonymous members.
     * If the amount of killed members is not evenly divisible by the number of anonymous members,
     * the last anonymous member will receive the remaining amount.
     */
    private fun killAnonMembers(num: Int) {
        val anons = members.filter { it.contains("Anon") }
        var sum = num
        anons.forEachIndexed { index, string ->
            val char = parent.characters[string]!!
            if (index == anons.size - 1) {
                char.killReliant(sum)
            } else {
                val amount = clamp(num / anons.size, 0, char.reliant)
                sum -= amount
                char.killReliant(amount)

            }
        }
    }//Managers will have to rehire people after this.
}
