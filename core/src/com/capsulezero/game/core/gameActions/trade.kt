package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameEngine
import com.capsulezero.game.core.GameState

class trade(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {
    var who = ""
    override fun chooseParams() {
        who =
            GameEngine.acquire(tgtState.ongoingMeetings.filter {it.value.currentCharacters.contains(tgtCharacter)}.flatMap { it.value.currentCharacters })
    }
    override fun execute() {
        //TODO: trade
        if (tgtCharacter == who) {println("You trade with yourself.")
            return}
        println("What do you want to trade?")
        val item = GameEngine.acquire(tgtState.characters[tgtCharacter]!!.resources.keys.toList())
        println("What do you want to trade it for?")
        val item2 = GameEngine.acquire(tgtState.characters[who]!!.resources.keys.toList())//TODO: this should not be visible
        val value = itemValue(who, item)
        val value2 = itemValue(who, item2)
        if(value>value2) {
            tgtState.characters[who]!!.resources[item2] = tgtState.characters[who]!!.resources[item2]!! - 1
            tgtState.characters[tgtCharacter]!!.resources[item] =
                tgtState.characters[tgtCharacter]!!.resources[item]!! - 1
            tgtState.characters[who]!!.resources[item] = tgtState.characters[who]!!.resources[item]!! + 1
            tgtState.characters[tgtCharacter]!!.resources[item2] =
                tgtState.characters[tgtCharacter]!!.resources[item2]!! + 1
        }

        //tgtState.characters[tgtCharacter]!!.frozen++ TODO: trading does not take time right now, but it should
    }

    fun itemValue(char:String, item:String):Double{
        return when(item){
            "ration"->1.0
            "water"->1.0
            "hydrogen"->1.0
            "organics"->1.0
            "lightMetal"->1.0
            "heavyMetal"->1.0
            "rareMetal"->1.0
            "silicon"->1.0
            "plastic"->1.0
            "glass"->1.0
            "ceramic"->1.0
            "diamond"->1.0
            "helium"->1.0
            "glassClothes"->1.0
            "cottonClothes"->1.0

            else->0.0
        }

    }

}