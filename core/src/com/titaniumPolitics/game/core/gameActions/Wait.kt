package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Wait(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    override fun execute() {
        //Not affected by the will of the character, so no need to call super.execute()
        sbjCharObj.frozen += ReadOnly.constInt("WaitDuration")
    }

    override fun isValid(): Boolean {
        return true
    }

}