package com.capsulezero.game.core.gameActions

class salary(override val tgtCharacter: String, override val tgtPlace: String) : GameAction() {
    var amount = 2
    var what1 = "ration"
    var what2 = "water"
    override fun chooseParams() {
    }
    override fun execute() {
        val who =
            (parent.ongoingMeetings.filter {it.value.currentCharacters.contains(tgtCharacter)}.flatMap { it.value.currentCharacters }+parent.ongoingConferences.filter {it.value.currentCharacters.contains(tgtCharacter)}.flatMap { it.value.currentCharacters }).toHashSet()

        val party = parent.parties.values.find { it.members.containsAll(who+tgtCharacter) }!!
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
            (parent.places[guildHall]!!.resources[what1]?:0)>=amount && (parent.places[guildHall]!!.resources[what2]?:0)>=amount
        ) {
            parent.places[guildHall]!!.resources[what1] = (parent.places[guildHall]!!.resources[what1]?:0) - amount
            parent.characters[tgtCharacter]!!.resources[what1] =
                (parent.characters[tgtCharacter]!!.resources[what1]?:0) + amount

            parent.places[guildHall]!!.resources[what2] = (parent.places[guildHall]!!.resources[what2]?:0) - amount
            parent.characters[tgtCharacter]!!.resources[what2] =
                (parent.characters[tgtCharacter]!!.resources[what2]?:0) + amount
            party.isDailySalaryPaid[tgtCharacter]=true
            println( "$tgtCharacter is paid $amount $what1 and $amount $what2 from $party.")
            parent.characters[tgtCharacter]!!.frozen++

        }
        else{
            println("Not enough resources to pay salary to $tgtCharacter: $tgtPlace, ${parent.places[tgtPlace]!!.resources}")
            //Party integrity decreases
            parent.setPartyMutuality(party.name, party.name, -1.0)
            //Opinion of the leader of the party decreases
            parent.setMutuality(tgtCharacter, party.leader, -1.0)
            party.isDailySalaryPaid[tgtCharacter]=true//TODO: this is a hack to prevent infinite loop. This is a lie, but who would be able to complain?
        }

    }

}