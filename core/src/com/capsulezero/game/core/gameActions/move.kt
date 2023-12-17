package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameEngine

class move(override val tgtCharacter: String, override val tgtPlace: String) : GameAction() {
    var placeTo = ""
    override fun chooseParams() {
        placeTo = if(tgtPlace=="home")
            GameEngine.acquire(arrayListOf(parent.characters[tgtCharacter]!!.home)+"cancel")
        else
            GameEngine.acquire(parent.places[tgtPlace]!!.connectedPlaces+"cancel")
    }
    override fun isValid():Boolean = placeTo != "cancel"
    override fun execute() {

        parent.places[tgtPlace]!!.characters.remove(tgtCharacter)
        parent.places[placeTo]!!.characters.add(tgtCharacter)
        parent.characters[tgtCharacter]!!.frozen++
    }

}