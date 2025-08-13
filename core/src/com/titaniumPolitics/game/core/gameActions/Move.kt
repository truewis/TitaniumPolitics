package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import kotlinx.serialization.Serializable

@Serializable
class Move(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    var placeTo = ""
    override fun chooseParams() {
        GameEngine.acquire(tgtPlaceObj.connectedPlaces + "cancel")
    }

    override fun isValid(): Boolean =
        tgtPlaceObj.connectedPlaces.contains(placeTo) && sbjCharObj.currentMeeting == null //You cannot move during meeting; you have to end meeting first.
                && (tgtPlaceObj.whoseHome?.let { it == sbjCharacter }
            ?: true) //You can only move to your home place or places that are not home to anyone.

    override fun execute() {

        tgtPlaceObj.characters.remove(sbjCharacter)
        parent.places[placeTo]!!.characters.add(sbjCharacter)
        super.execute()
    }

}