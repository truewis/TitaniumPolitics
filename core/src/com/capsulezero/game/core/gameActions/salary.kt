package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameState

class salary(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {
    var amount = 2
    var what1 = "ration"
    var what2 = "water"
    override fun chooseParams() {
    }
    override fun execute() {
        val who =
            (tgtState.ongoingMeetings.filter {it.value.currentCharacters.contains(tgtCharacter)}.flatMap { it.value.currentCharacters }+tgtState.ongoingConferences.filter {it.value.currentCharacters.contains(tgtCharacter)}.flatMap { it.value.currentCharacters }).toHashSet()

        val party = tgtState.parties.values.find { it.members.containsAll(who+tgtCharacter) }!!
        val guildHall = party.home
        if(party.isDailySalaryPaid.keys.none { it==tgtCharacter  }){
            println("Warning: $tgtCharacter is not eligible to be paid from $party.")
            return
        }
        if(party.isDailySalaryPaid[tgtCharacter]==true){
            println("Warning: $tgtCharacter has already been paid from $party today.")
            return
        }
        if(
            (tgtState.places[guildHall]!!.resources[what1]?:0)>=amount && (tgtState.places[guildHall]!!.resources[what2]?:0)>=amount
        ) {
            tgtState.places[guildHall]!!.resources[what1] = (tgtState.places[guildHall]!!.resources[what1]?:0) - amount
            tgtState.characters[tgtCharacter]!!.resources[what1] =
                (tgtState.characters[tgtCharacter]!!.resources[what1]?:0) + amount

            tgtState.places[guildHall]!!.resources[what2] = (tgtState.places[guildHall]!!.resources[what2]?:0) - amount
            tgtState.characters[tgtCharacter]!!.resources[what2] =
                (tgtState.characters[tgtCharacter]!!.resources[what2]?:0) + amount
            party.isDailySalaryPaid[tgtCharacter]=true
            println( "$tgtCharacter is paid $amount $what1 and $amount $what2 from $party.")
            tgtState.characters[tgtCharacter]!!.frozen++

        }
        else{
            println("Not enough resources to pay salary to $tgtCharacter: $tgtPlace, ${tgtState.places[tgtPlace]!!.resources}")
            //Party integrity decreases
            tgtState.setPartyMutuality(party.name, party.name, -1.0)
            //Opinion of the leader of the party decreases
            tgtState.setMutuality(tgtCharacter, party.leader, -1.0)
            party.isDailySalaryPaid[tgtCharacter]=true//TODO: this is a hack to prevent infinite loop. This is a lie, but who would be able to complain?
        }

    }

}