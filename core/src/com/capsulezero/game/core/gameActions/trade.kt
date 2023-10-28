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
    override fun chooseParams() {
        who =
            GameEngine.acquire(tgtState.ongoingMeetings.filter {it.value.currentCharacters.contains(tgtCharacter)}.flatMap { it.value.currentCharacters })
        println("What do you want to trade?")
        val type = GameEngine.acquire(listOf("resource", "information", "action"))
        when (type){
            "resource"->{
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
                info = tgtState.informations[GameEngine.acquire(tgtState.informations.filter { it.value.knownTo.contains(tgtCharacter) }.map { it.key })]
            }
        }
        println("What do you want to trade it for?")//TODO: the list of available stuff should not be visible by default.
        val type2 = GameEngine.acquire(listOf("resource", "information", "action"))
        when (type2){
            "resource"->{
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
                info2 = tgtState.informations[GameEngine.acquire(tgtState.informations.filter { it.value.knownTo.contains(who) }.map { it.key })]
            }
        }
    }
    override fun execute() {

            //TODO: trade
        if (tgtCharacter == who) {println("You trade with yourself.")
            return}

        val value = itemValue(who, item)+(action?.let { actionValue(who, it) } ?:.0)+ (info?.let { infoValue(who, it) }?:.0)//Value is calculated based on how the opponent values the item, not how the tgtCharacter values it.
        val value2 = itemValue(who, item2)+(action2?.let {actionValue(who, it)} ?:.0)+ (info2?.let { infoValue(who, it) } ?:.0)
        if(value>=value2) {
            if(item!=""){
                if(tgtState.characters[tgtCharacter]!!.resources[item]!!<amount){
                    println("You don't have enough $item to trade.")
                    return
                }
                tgtState.characters[tgtCharacter]!!.resources[item] = (tgtState.characters[tgtCharacter]!!.resources[item] ?: 0) - amount
                tgtState.characters[who]!!.resources[item] = (tgtState.characters[who]!!.resources[item] ?: 0) + amount
            }
            if (item2 != "") {
                if(tgtState.characters[who]!!.resources[item2]!!<amount){
                    println("They don't have enough $item2 to trade.")
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
            tgtState.setMutuality(tgtCharacter, who, value+value2)
            //tgtState.characters[tgtCharacter]!!.frozen++ TODO: trading does not take time right now, but it should
            println("$who trades with $tgtCharacter.")
        }
        else{
            println("$who refuses to trade with $tgtCharacter.")
        }


    }

    fun itemValue(char:String, item:String):Double{
        return when(item){
            //Value of ration and water is based on the current need of the character.
            "ration"->5.0*(tgtState.characters[char]!!.reliants.size+1.0) / ((tgtState.characters[char]!!.resources["ration"]?:0)+1.0)
            "water"->(tgtState.characters[char]!!.reliants.size+1.0) / ((tgtState.characters[char]!!.resources["water"]?:0)+1.0)
            "hydrogen"->1.0
            "organics"->5.0
            "lightMetal"->1.0
            "heavyMetal"->1.0
            "rareMetal"->5.0
            "silicon"->1.0
            "plastic"->10.0
            "glass"->1.0
            "ceramic"->1.0
            "diamond"->3.0
            "helium"->1.0
            "glassClothes"->1.0
            "cottonClothes"->10.0

            else->0.0
        }

    }
    fun actionValue(char:String, action:Command):Double{
        //TODO: the value of the action should be calculated based on the expected outcome.
        //TODO: Action to remove rivals is more valuable.
        //TODO: Action to acquire resources is more valuable.

        //Action to repair the character's apparatus is more valuable.
        if(action.action=="repair" && tgtState.parties[tgtState.places[action.place]!!.responsibleParty]?.members?.contains(char)==true)
        {
            val urgency = 100.0 - tgtState.places[action.place]!!.apparatuses.sumOf { it.durability } / tgtState.places[action.place]!!.apparatuses.size
            return urgency
        }

        return 1.0
    }
    fun infoValue(char:String, info:Information):Double{
        //Known information is less valuable.
        if(info.knownTo.contains(char))
            return 0.0
        //Information about the character itself is more valuable.
        if(info.tgtCharacter==char)
            return 2.0
        //Information about the character's party is more valuable.
        if(tgtState.parties[info.tgtParty]?.members?.contains(char) == true)
            return 2.0
        //Information about valuable resource is more valuable.
        if(info.type=="resource")
            return itemValue(char, info.tgtResource) * info.amount
        //UnofficialTransfer is more valuable if it is not known to the other character.
        if(info.type=="action" && info.action=="unofficialResourceTransfer" && !info.knownTo.contains(char))
            return 10.0

        return 1.0
    }

}