package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable

@Serializable
class Party : GameStateElement() {
    override val name: String
        get() = parent.parties.filter { it.value == this }.keys.first()
    var leader = ""
    var type = ""
    var home = "" //The place where the party is based.
    var members = hashSetOf<String>()
    var isSalaryPaid = false //This variable is reset every quarter.
    val numAnonymousMembers: Int
        get() = members.filter { it.contains("Anon") }.sumOf { parent.characters[it]!!.reliant }
    val realMembers: HashSet<String>
        get() = members.filter { !it.contains("Anon") }.toHashSet()
    val size: Int
        get() = members.sumOf { getMultiplier(it) }

    //This is average person to person mutuality of all members.
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
        if (numAnonymousMembers >= num) {
            killAnonMembers(num) //If there are anon members left, kill them first.
        } else if (num >= size) {
            killAnonMembers(numAnonymousMembers)
            members.forEach { parent.characters[it]!!.alive = false }
        } else {
            //kill members
            for (i in 0..<num - numAnonymousMembers)
                members.filter { parent.characters[it]!!.alive }.random()
                    .let { parent.characters[it]!!.alive = false }//kill num - anonymousMembers members

            killAnonMembers(numAnonymousMembers)
        }
        parent.popChanged.forEach { it() }

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

    private fun killAnonMembers(num: Int) {
        val anons = members.filter { it.contains("Anon") }
        anons.forEachIndexed { index, string ->
            val char = parent.characters[string]!!
            if (index == anons.size - 1) {
                char.killReliant(num - (num / anons.size) * (anons.size - 1))
            } else {
                char.killReliant(num / anons.size)
            }
        }
    }//Managers will have to rehire people after this.
}
