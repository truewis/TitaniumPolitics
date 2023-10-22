package com.capsulezero.game.core

import kotlinx.serialization.Serializable
@Serializable
class Party: GameStateElement() {
    var name = ""
    var leader = ""
    var members = hashSetOf<String>()
    var commands = hashSetOf<String>()
    var anonymousMembers  = 0
    fun individualMutuality(name:String) = 0//TODO: implement this.
    var resources = hashMapOf<String, Int>()
    val integrity: Int
        get() = 0
    fun causeDeaths(num:Int){
        if(anonymousMembers>=num)
        anonymousMembers-=num
        else
        {
            //kill members
            for (i in 0..<num - anonymousMembers)
                if(members.filter { parent.characters[it]!!.alive }.count()>num - anonymousMembers)
                members.filter { parent.characters[it]!!.alive }.random().let { parent.characters[it]!!.alive = false }//kill num - anonymousMembers members
                else
                    members.filter { parent.characters[it]!!.alive }.forEach { parent.characters[it]!!.alive = false}//kill all members

            anonymousMembers = 0
        }

    }
}
