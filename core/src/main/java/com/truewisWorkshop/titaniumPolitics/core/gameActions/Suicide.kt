package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
data class Suicide(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    override fun execute() {
        Logger.write("$sbjCharacter killed themselves.", Logger.LogLevel.ACTION_VERBOSE)
        sbjCharObj.kill()
    }

    override fun isValid(): Boolean {
        return tgtPlace == "home_$sbjCharacter" && sbjCharObj.will < ReadOnly.const("CriticalWill")
    }

}