package com.titaniumPolitics.game.ui

import com.titaniumPolitics.game.core.gameActions.GameAction


interface ActionUI
{
    var actionCallback: (GameAction) -> Unit
    fun changeSubject(charName: String)
}