package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import kotlinx.serialization.Serializable

@Serializable
class Move(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    var placeTo = ""
    override fun chooseParams()
    {
        GameEngine.acquire(tgtPlaceObj.connectedPlaces + "cancel")
    }

    override fun isValid(): Boolean =
        tgtPlaceObj.connectedPlaces.contains(placeTo) && sbjCharObj.currentMeeting == null //You cannot move during meeting; you have to end meeting first.

    override fun execute()
    {

        tgtPlaceObj.characters.remove(sbjCharacter)
        parent.places[placeTo]!!.characters.add(sbjCharacter)
        super.execute()
    }

}