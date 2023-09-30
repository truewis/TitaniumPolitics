package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameState

class home(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {

    override fun execute() {

        tgtState.places[tgtPlace]!!.characters.remove(tgtCharacter)
        tgtState.places["home"]!!.characters.add(tgtCharacter)
        tgtState.characters[tgtCharacter]!!.frozen++
    }

}