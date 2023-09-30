package com.capsulezero.game.core

abstract class GameAction(val tgtState: GameState, val tgtCharacter: String, val tgtPlace: String) {
    open fun chooseParams(){}
    open fun isValid():Boolean{return true}
    abstract fun execute()
}