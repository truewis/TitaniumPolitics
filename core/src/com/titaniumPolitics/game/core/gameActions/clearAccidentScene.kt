package com.titaniumPolitics.game.core.gameActions

class clearAccidentScene(override val tgtCharacter: String, override val tgtPlace: String) : GameAction() {

    override fun execute() {
        parent.places[tgtPlace]!!.isAccidentScene = false
        parent.places[tgtPlace]!!.accidentInformations.clear()//Remove all accident information from the place.
        parent.characters[tgtCharacter]!!.frozen+=3
    }

}