package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine

class Move(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{
    var placeTo = ""
    override fun chooseParams()
    {
        GameEngine.acquire(parent.places[tgtPlace]!!.connectedPlaces + "cancel")
    }

    override fun isValid(): Boolean = placeTo != ""
    override fun execute()
    {

        parent.places[tgtPlace]!!.characters.remove(tgtCharacter)
        parent.places[placeTo]!!.characters.add(tgtCharacter)
        parent.characters[tgtCharacter]!!.frozen++
    }

}