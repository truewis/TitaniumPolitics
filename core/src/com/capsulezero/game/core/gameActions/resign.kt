package com.capsulezero.game.core.gameActions

import kotlinx.serialization.Serializable

@Serializable
//Called when a character resigns from a party, in a daily party meeting
class resign(override val tgtCharacter: String, override val tgtPlace: String) : GameAction() {
    override fun chooseParams() {
    }
    override fun execute() {
        val party = parent.ongoingConferences.filter {it.value.currentCharacters.contains(tgtCharacter)}.values.first().involvedParty
        if(parent.parties[party]!!.leader!= tgtCharacter){
            println("Warning: $tgtCharacter is not the leader of $party.")
            return
        }
        parent.parties[party]!!.members.remove(tgtCharacter)
        parent.parties[party]!!.leader = ""
        println("$tgtCharacter resigns from $party.")


    }

    override fun isValid(): Boolean {
        try {
            val party =
                parent.ongoingConferences.filter { it.value.currentCharacters.contains(tgtCharacter) }.values.first().involvedParty
            return parent.parties[party]!!.leader == tgtCharacter
        }
        catch (e:Exception){
            return false
        }
    }


}