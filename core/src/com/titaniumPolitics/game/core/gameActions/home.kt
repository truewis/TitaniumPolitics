package com.titaniumPolitics.game.core.gameActions

class home(override val tgtCharacter: String, override val tgtPlace: String) : GameAction() {

    override fun execute() {

        parent.places[tgtPlace]!!.characters.remove(tgtCharacter)
        parent.places["home"]!!.characters.add(tgtCharacter)
        parent.characters[tgtCharacter]!!.frozen++
    }

}