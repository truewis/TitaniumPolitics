package com.titaniumPolitics.game.core

import com.badlogic.gdx.math.MathUtils.clamp
import com.titaniumPolitics.game.core.gameActions.Salary
import kotlinx.serialization.Serializable
import java.util.Collections
import kotlin.collections.set

@Serializable
class Party : GameStateElement() {
    private var _name: String? = null
    override val name: String
        get() = _name ?: parent.parties.filter { it.value == this }.keys.first().also { _name = it }

    /**
     * Roles for employees in the party. Does not include leader or directors.
     */
    enum class Role {
        SECRETARY, ADMINISTRATOR, TREASURER, OVERSEER, GUARD, NONE
    }

    var leader: String? = null
        private set
    var administrator: String? = null //The person who manages the party.
        private set
    var treasurer: String? = null //The person who manages the party's finances.
        private set
    var overseer: String? = null //The person who oversees worker's activities.
        private set
    var guard: String? = null //The person who is responsible for the party's security.
        private set
    var secretary: String? = null //The person who helps the leader with paperwork and communication.
        private set

    fun getRole(char: String): Role {
        return when (char) {
            administrator -> Role.ADMINISTRATOR
            treasurer -> Role.TREASURER
            overseer -> Role.OVERSEER
            guard -> Role.GUARD
            secretary -> Role.SECRETARY
            else -> Role.NONE
        }
    }

    fun getCharByRole(role: Role): String? {
        return when (role) {
            Role.ADMINISTRATOR -> administrator
            Role.TREASURER -> treasurer
            Role.OVERSEER -> overseer
            Role.GUARD -> guard
            Role.SECRETARY -> secretary
            Role.NONE -> null
        }
    }

    fun changeLeader(char: String) {
        if (char !in parent.characters.keys)
            throw Exception("Character $char does not exist.")
        if (char !in members)
            throw Exception("Character $char is not a member of the party.")
        leader = char
        getRole(char).let { role ->
            when (role) {
                Role.ADMINISTRATOR -> administrator = null
                Role.TREASURER -> treasurer = null
                Role.OVERSEER -> overseer = null
                Role.GUARD -> guard = null
                Role.SECRETARY -> secretary = null
                Role.NONE -> {}
            }
        }
    }

    fun addMember(char: String, role: Role) {
        if (char !in parent.characters.keys)
            throw Exception("Character $char does not exist.")
        _members.add(char)
        when (role) {
            Role.ADMINISTRATOR -> administrator = char
            Role.TREASURER -> treasurer = char
            Role.OVERSEER -> overseer = char
            Role.GUARD -> guard = char
            Role.SECRETARY -> secretary = char
            Role.NONE -> {}
        }
    }

    fun removeMember(char: String) {
        _members.remove(char)
        if (leader == char) leader = null
        if (administrator == char) administrator = null
        if (treasurer == char) treasurer = null
        if (overseer == char) overseer = null
        if (guard == char) guard = null
        if (secretary == char) secretary = null
    }

    var type: Type = Type.OTHER

    enum class Type {
        CABINET, DIVISION, WORKPLACE, TRIUMVIRATE, OTHER
    }

    var home: String? = null //The place where the party is based.
    private val _members = hashSetOf<String>()
    val members: Set<String> = Collections.unmodifiableSet<String>(_members)
    var isSalaryPaid = false //This variable is reset every quarter.
    val anonMembers: HashSet<String>
        get() = members.filter { parent.characters[it]!!.type == Character.Type.ANON }
            .toHashSet() //Anonymous members are those who are not registered in the party.
    val numAnonMembers: Int
        get() = anonMembers.sumOf { parent.characters[it]!!.reliant }
    val realMembers: HashSet<String>
        get() = (members - anonMembers).toHashSet()
    val directorMembers: HashSet<String>
        get() = members.filter { char -> parent.characters[char]!!.type == Character.Type.DIRECTOR }
            .toHashSet() //Directors are the ones who can make decisions in the party.
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
                Type.CABINET -> {
                    val divisions = parent.parties.filter { it.value.type == Type.DIVISION }.values
                    val resMap = hashMapOf<String, Resources>()
                    divisions.forEach { division ->
                        val directorWage =
                            Salary.standardQuarterlyRate(Type.DIVISION) * (division.directorMembers - division.leader).size
                        resMap[division.name] = division.standardBudget.sum() + directorWage
                    }
                    return Budget(resMap)
                }

                Type.DIVISION -> {
                    val workplaces = parent.places.filter { it.value.responsibleDivision == name }.values
                    val resMap = hashMapOf<String, Resources>()
                    workplaces.forEach { place ->
                        place.workplaceParty?.let { party ->
                            resMap[party.name] = party.standardBudget.sum()
                        }

                    }
                    return Budget(resMap)
                }

                Type.WORKPLACE -> {
                    val employeeWage =
                        Salary.standardQuarterlyRate(Type.WORKPLACE) * (realMembers - directorMembers).size
                    //Workplace directors are paid from the division budget, not workplace budget.
                    //Laborer salary is included in apparatus operation cost.

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
    val integrity: Double
        get() = parent.getPartyMutuality(this.name, this.name)

    val divisionPlaces: Collection<Place>
        get() = parent.places.filter { it.value.responsibleDivision == name }.values.also {
            assert(it.isNotEmpty() && type == Type.DIVISION)
        }

    val workplace
        get() = parent.places.values.firstOrNull { place -> place.workplaceParty == this }
            ?: throw Exception("Party $name has no workplace.")

    val currentWorker: Int
        get() = divisionPlaces.sumOf { it.currentWorker }

    val plannedWorker: Int
        get() = divisionPlaces.sumOf { it.plannedWorker }

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

    fun vacancyRole(): Role? {
        return Role.entries.firstOrNull {
            getCharByRole(it) == null
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
