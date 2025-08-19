package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Wait(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    override fun execute() {
        //Not affected by the will of the character, so no need to call super.execute()
        sbjCharObj.frozen += ReadOnly.constInt("WaitDuration")
    }

    override fun isValid(): Boolean {
        //If there is a meeting which I am the current speaker, I cannot wait.
        if (sbjCharObj.currentMeeting != null && sbjCharObj.currentMeeting!!.currentSpeaker == sbjCharacter)
            return false
        return true

    }

    override fun deltaWill(): Double {
        val ret = super.deltaWill()
        // If in one of the public places, the will is increased.
        if (tgtPlace in listOf("market", "squareSouth", "squareNorth")) {
            // Unless the character has trait "agoraphobia", in which case the will is decreased.
            if ("agoraphobia" in sbjCharObj.trait) {
                parent.setMutuality(sbjCharacter, delta = -expectedDuration * 0.5, reasonKey = "publicPlaceAgoraphobia")
            } else {
                parent.setMutuality(sbjCharacter, delta = +expectedDuration * 1.0, reasonKey = "publicPlace")
            }
        }
        // If in one of the remote places, the will is increased.
        else if (tgtPlace in listOf("reservoirEast", "reservoirWest", "observatory", "cemetery", "spaceport")) {
            if ("introvert" in sbjCharObj.trait) {
                parent.setMutuality(sbjCharacter, delta = expectedDuration * 1.5, reasonKey = "remotePlaceIntrovert")
            } else {
                parent.setMutuality(sbjCharacter, delta = expectedDuration * 1.0, reasonKey = "remotePlace")
            }

        }
        return ret
    }

}