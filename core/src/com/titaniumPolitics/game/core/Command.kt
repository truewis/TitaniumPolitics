package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.gameActions.GameAction
import kotlinx.serialization.Serializable

@Serializable
class Command(var place: String, var action: GameAction)
{
    var executeTime = 0
    val compulsion = 0
    var issuedParty = ""
}