package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameState

class clearAccidentScene(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {

    override fun execute() {
        tgtState.places[tgtPlace]!!.isAccidentScene = false
        tgtState.places[tgtPlace]!!.accidentInformations.clear()//Remove all accident information from the place.
        tgtState.characters[tgtCharacter]!!.frozen+=3
    }

}