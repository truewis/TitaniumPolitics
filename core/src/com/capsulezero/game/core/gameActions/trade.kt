package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.*


//TODO: because of a no interrupt polity, the trade action should be executed immediately, not after the current action is finished.
//The player character accepts the trade if the value of the offered item is higher than the value of the requested item.
//The player cannot affect this decision.
class trade(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {
    var who = ""
    var item  = ""
    var amount = 0
    var item2  = ""
    var amount2 = 0
    var action : Command? = null
    var action2 : Command? = null
    var info : Information? = null
    var info2 : Information? = null
    var onFinished : (Boolean)->Unit = {} //This is called when the trade is accepted or rejected.
    override fun chooseParams() {
        who =
            tgtState.ongoingMeetings.filter {it.value.currentCharacters.contains(tgtCharacter)}.flatMap { it.value.currentCharacters }.first {it!=tgtCharacter}//Trade can happen only when there is exactly one other character in the meeting.

        println("What do you want to trade?")
        val type = GameEngine.acquire(listOf("resource", "information", "action"))
        when (type){
            "resource"->{
                //TODO: Can only request information that you know the existence.
                item = GameEngine.acquire(tgtState.characters[tgtCharacter]!!.resources.keys.toList())
                amount = GameEngine.acquire(arrayListOf("1","2","3","4","5","6","7","8","9","10")).toInt()
            }
            "action"-> {
                //println("When do you want the action to be executed?") TODO: this is not implemented yet.
                //val meetingTime = tgtState.time+GameEngine.acquire(arrayListOf("3","6","9","12","18","21","24")).toInt()
                println("Where do you want the action to be executed?")
                val where = GameEngine.acquire(tgtState.places.map { it.value.name })
                println("What action do you want to execute?")
                val name = GameEngine.acquire(GameEngine.availableActions(tgtState, where, who))
                action = Command(where, name, 0)
            }
            "information"->{
                info = tgtState.informations[GameEngine.acquire(tgtState.informations.filter { it.value.knownTo.contains(tgtCharacter) }.map { it.key })] //You can give information that opponent has no clue about.
            }
        }
        println("What do you want to trade it for?")
        val type2 = GameEngine.acquire(listOf("resource", "information", "action"))
        when (type2){
            "resource"->{
                //TODO: Can only request information that you know the existence.

                item2 = GameEngine.acquire(tgtState.characters[who]!!.resources.keys.toList())
                amount2 = GameEngine.acquire(arrayListOf("1","2","3","4","5","6","7","8","9","10")).toInt()
            }
            "action"-> {
                //println("When do you want the action to be executed?") TODO: this is not implemented yet.
                //val meetingTime = tgtState.time+GameEngine.acquire(arrayListOf("3","6","9","12","18","21","24")).toInt()
                println("Where do you want the action to be executed?")
                val where = GameEngine.acquire(tgtState.places.map { it.value.name })
                println("What action do you want to execute?")
                val name = GameEngine.acquire(GameEngine.availableActions(tgtState, where, who))
                action2 = Command(where, name, 0)
            }
            "information"->{
                info2 = tgtState.informations[GameEngine.acquire(tgtState.informations.filter { it.value.knownTo.contains(who) and it.value.knowExistence.contains(tgtCharacter) }.map { it.key })] //You can only request information that you know the existence.
            }
        }
    }
    override fun execute() {
        var success= false
        val value = tgtState.characters[who]!!.itemValue(item)+(action?.let { tgtState.characters[who]!!.actionValue(it) } ?:.0)+ (info?.let { tgtState.characters[who]!!.infoValue(it) }?:.0)//Value is calculated based on how the opponent values the item, not how the tgtCharacter values it.
        val value2 = tgtState.characters[who]!!.itemValue(item2)+(action2?.let {tgtState.characters[who]!!.actionValue(it)} ?:.0)+ (info2?.let { tgtState.characters[who]!!.infoValue(it) } ?:.0)
        val valuea = tgtState.characters[tgtCharacter]!!.itemValue(item)+(action?.let { tgtState.characters[tgtCharacter]!!.actionValue(it) } ?:.0)+ (info?.let { tgtState.characters[tgtCharacter]!!.infoValue(it) }?:.0)
        val valuea2 = tgtState.characters[tgtCharacter]!!.itemValue(item2)+(action2?.let {tgtState.characters[tgtCharacter]!!.actionValue(it)} ?:.0)+ (info2?.let { tgtState.characters[tgtCharacter]!!.infoValue(it) } ?:.0)
        success = if(tgtState.nonPlayerAgents.keys.contains(who)) {
            tgtState.nonPlayerAgents[who]!!.decideTrade(tgtCharacter, value, value2, valuea, valuea2)
        } else//If player, acquires the decision from the player.
        {
            println("$tgtCharacter offers $item x $amount for $item2 x $amount2,\n and $action for $action2,\n and $info for $info2.")
            println("Do $who accept the trade?")
            GameEngine.acquire(listOf("yes", "no"))=="yes"
        }

        if(success) {
            if(item!=""){
                if(tgtState.characters[tgtCharacter]!!.resources[item]!!<amount){
                    println("You don't have enough $item to trade.")
                    onFinished(false)
                    return
                }
                tgtState.characters[tgtCharacter]!!.resources[item] = (tgtState.characters[tgtCharacter]!!.resources[item] ?: 0) - amount
                tgtState.characters[who]!!.resources[item] = (tgtState.characters[who]!!.resources[item] ?: 0) + amount
            }
            if (item2 != "") {
                if(tgtState.characters[who]!!.resources[item2]!!<amount){
                    println("They don't have enough $item2 to trade.")
                    onFinished(false)
                    return
                }
                tgtState.characters[who]!!.resources[item2] = (tgtState.characters[who]!!.resources[item2] ?: 0) - amount2
                tgtState.characters[tgtCharacter]!!.resources[item2] = (tgtState.characters[tgtCharacter]!!.resources[item2] ?: 0) + amount2
            }
            action?.let{ tgtState.nonPlayerAgents[tgtCharacter]!!.commands.add(it)}
            action2?.let{ tgtState.nonPlayerAgents[who]!!.commands.add(it)}
            info?.let{ tgtState.informations[it.name]!!.knownTo.add(who)}
            info2?.let{ tgtState.informations[it.name]!!.knownTo.add(tgtCharacter)}
            //Increase mutualities of each character with the other. Value is proportional to the value of the traded item.
            tgtState.setMutuality(who, tgtCharacter, value+value2)
            tgtState.setMutuality(tgtCharacter, who, valuea+valuea2)

            println("$who trades with $tgtCharacter.")
            onFinished(true)
        }
        else{
            println("$who refuses to trade with $tgtCharacter.")
            onFinished(false)
            //TODO: this should come with consequences.
        }
        tgtState.characters[tgtCharacter]!!.frozen++

    }


}