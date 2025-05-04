package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable

@Serializable
class Party : GameStateElement()
{
    override val name: String
        get() = parent.parties.filter { it.value == this }.keys.first()
    var leader = ""
    var type = ""
    var home = "" //The place where the party is based.
    var members = hashSetOf<String>()
    var isSalaryPaid = false //This variable is reset every quarter.
    var anonymousMembers = arrayListOf<Int>()
    val size: Int
        get() = members.size + anonymousMembers.sum()

    //This is average person to person mutuality of all members.
    fun individualMutuality(name: String): Double = members.sumOf { parent.getMutuality(it, name) } / members.size

    var resources = hashMapOf<String, Int>()
    val integrity: Double
        get() = parent.getPartyMutuality(this.name, this.name)

    fun causeDeaths(num: Int)
    {
        if (anonymousMembers.sum() >= num)
        {
            reduceAnonMembers(num) //If there are anon members left, kill them first.
        } else if (num >= size)
        {
            reduceAnonMembers(anonymousMembers.sum())
            members.forEach { parent.characters[it]!!.alive = false }
        } else
        {
            //kill members
            for (i in 0..<num - anonymousMembers.sum())
                members.filter { parent.characters[it]!!.alive }.random()
                    .let { parent.characters[it]!!.alive = false }//kill num - anonymousMembers members

            reduceAnonMembers(anonymousMembers.sum())
        }
        parent.popChanged.forEach { it() }

    }

    //Used in mutuality calculation
    fun getMultiplier(char: String): Int
    {
        return if (members.contains(char))
            1
        else if (char.startsWith("$name-Anon"))
        {
            val index = char.split("-")[2].toInt()
            if (index < anonymousMembers.size)
                anonymousMembers[index]
            else
                0
        } else
            0
    }

    fun reduceAnonMembers(num: Int)
    {
        var remaining = num
        var index = anonymousMembers.size - 1

        while (remaining > 0 && index >= 0)
        {
            val currentValue = anonymousMembers[index]
            if (currentValue > remaining)
            {
                anonymousMembers[index] = currentValue - remaining
                remaining = 0
            } else
            {
                remaining -= currentValue
                anonymousMembers[index] = 0
                index--
            }
        }
        if (remaining > 0)
            println("There are not enough anonymous members to kill in $name.")
    }
}
