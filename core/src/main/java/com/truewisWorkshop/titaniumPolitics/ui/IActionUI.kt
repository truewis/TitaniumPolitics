package com.titaniumPolitics.game.ui

import com.titaniumPolitics.game.core.gameActions.GameAction


interface IActionUI {
    val actionCallback: (GameAction) -> Unit//This is set immutable to prevent callback hell, especially combined with singletons. Set it properly once in the constructor.
    var subject: String
    var tgtPlace: String
    fun setCheckValidity(checkValidity: Boolean)
}