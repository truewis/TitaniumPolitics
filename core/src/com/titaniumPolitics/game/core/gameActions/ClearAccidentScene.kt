package com.titaniumPolitics.game.core.gameActions

import kotlinx.serialization.Serializable

@Serializable
class ClearAccidentScene(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{

    override fun execute()
    {
        parent.places[tgtPlace]!!.isAccidentScene = false
        parent.places[tgtPlace]!!.accidentInformationKeys.clear()//Remove all accident information from the place.
        super.execute()
    }

    override fun isValid(): Boolean
    {
        return parent.places[tgtPlace]!!.isAccidentScene
    }

}