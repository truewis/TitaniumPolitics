package com.capsulezero.game.core.gameActions

import com.capsulezero.game.core.GameState
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient


@Serializable
sealed class GameAction() {
    abstract val tgtCharacter: String
    abstract val tgtPlace: String
    @Transient
    lateinit var parent: GameState
    fun injectParent(parent: GameState){
        this.parent = parent
    }
    open fun chooseParams(){}
    open fun isValid():Boolean{return true}
    abstract fun execute()
}