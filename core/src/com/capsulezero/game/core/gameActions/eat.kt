package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameState

class eat(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {

    override fun execute() {
        if(tgtState.characters[tgtCharacter]!!.resources["ration"]!! >0 && tgtState.characters[tgtCharacter]!!.resources["water"]!! >0) {
            tgtState.characters[tgtCharacter]!!.resources["ration"] = tgtState.characters[tgtCharacter]!!.resources["ration"]!! - 1
            tgtState.characters[tgtCharacter]!!.resources["water"] = tgtState.characters[tgtCharacter]!!.resources["water"]!! - 1
            tgtState.characters[tgtCharacter]!!.will+=10
            tgtState.characters[tgtCharacter]!!.health+=10
            tgtState.characters[tgtCharacter]!!.frozen++
        }
    }

}