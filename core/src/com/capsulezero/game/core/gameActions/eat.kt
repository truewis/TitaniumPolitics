package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameState

class eat(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {

    override fun execute() {
        if((tgtState.characters[tgtCharacter]!!.resources["ration"] ?:0) >0 && (tgtState.characters[tgtCharacter]!!.resources["water"] ?:0) >0) {
            tgtState.characters[tgtCharacter]!!.resources["ration"] = tgtState.characters[tgtCharacter]!!.resources["ration"]!! - 1
            tgtState.characters[tgtCharacter]!!.resources["water"] = tgtState.characters[tgtCharacter]!!.resources["water"]!! - 1
            tgtState.setMutuality(tgtCharacter, tgtCharacter, 10.0)//Increase will.
            tgtState.characters[tgtCharacter]!!.hunger-=50
            tgtState.characters[tgtCharacter]!!.thirst-=50
            tgtState.characters[tgtCharacter]!!.frozen++
            println("$tgtCharacter ate a ration and drank some water.")
        }
        else {
            println("$tgtCharacter tried to eat, but there is nothing to eat.")
            tgtState.characters[tgtCharacter]!!.frozen++
        }
    }

}