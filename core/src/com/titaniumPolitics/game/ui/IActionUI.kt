package com.titaniumPolitics.game.ui

import com.titaniumPolitics.game.core.gameActions.GameAction


interface IActionUI {
    var actionCallback: (GameAction) -> Unit
    var subject: String
    var tgtPlace: String
}