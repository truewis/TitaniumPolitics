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
    var isSalaryPaid = false
    var commands = hashSetOf<String>()//The commands that the party can issue. Currently only used in CommandUI.
    var anonymousMembers = 0
    val size: Int
        get() = members.size + anonymousMembers

    //This is average person to person mutuality of all members.
    fun individualMutuality(name: String): Double = members.sumOf { parent.getMutuality(it, name) } / members.size

    var resources = hashMapOf<String, Int>()
    val integrity: Double
        get() = parent.getPartyMutuality(this.name, this.name)

    fun causeDeaths(num: Int)
    {
        if (anonymousMembers >= num)
            anonymousMembers -= num
        else
        {
            //kill members
            for (i in 0..<num - anonymousMembers)
                if (members.count { parent.characters[it]!!.alive } > num - anonymousMembers)
                    members.filter { parent.characters[it]!!.alive }.random()
                        .let { parent.characters[it]!!.alive = false }//kill num - anonymousMembers members
                else
                    members.filter { parent.characters[it]!!.alive }
                        .forEach { parent.characters[it]!!.alive = false }//kill all members

            anonymousMembers = 0
        }

    }
}
