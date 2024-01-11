package com.titaniumPolitics.game.core

import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonObject

/**Interface to dependenty inject gameState to its elements*/
sealed class GameStateElement {
    @Transient
    lateinit var parent: GameState
    var params = JsonObject(mapOf())
    fun add(key: String){
        params.plus(key to "")
    }
    fun add(key: String, value: JsonObject){
        params = JsonObject(this.params.plus(key to value))
    }
    fun remove(key: String){
        this.params = JsonObject(this.params.minus(key))
    }
    open fun injectParent(gameState:GameState){
        parent = gameState
    }
}