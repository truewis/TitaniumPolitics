package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameState

class sleep(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {

    override fun execute() {
        tgtState.characters[tgtCharacter]!!.health+=100
        tgtState.characters[tgtCharacter]!!.frozen+=8
    }

}