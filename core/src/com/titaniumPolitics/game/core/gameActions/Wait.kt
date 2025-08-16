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

}