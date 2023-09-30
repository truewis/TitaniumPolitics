package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameAction
import com.capsulezero.game.core.GameEngine
import com.capsulezero.game.core.GameState

class move(targetState: GameState, targetCharacter: String, targetPlace: String) : GameAction(targetState, targetCharacter,
    targetPlace
) {
    var placeTo = ""
    override fun chooseParams() {
        placeTo = if(tgtPlace=="home")
            GameEngine.acquire(arrayListOf(tgtState.characters[tgtCharacter]!!.home)+"cancel")
        else
            GameEngine.acquire(tgtState.places[tgtPlace]!!.connectedPlaces+"cancel")
    }
    override fun isValid():Boolean = placeTo != "cancel"
    override fun execute() {

        tgtState.places[tgtPlace]!!.characters.remove(tgtCharacter)
        tgtState.places[placeTo]!!.characters.add(tgtCharacter)
        tgtState.characters[tgtCharacter]!!.frozen++
    }

}