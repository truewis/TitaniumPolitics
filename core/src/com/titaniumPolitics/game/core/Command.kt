package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.gameActions.GameAction
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
class Command(var place: String, var action: GameAction)
{
    var ID = UUID.randomUUID().toString()
    var executeTime = 0
    val compulsion = 0
    var issuedBy: HashSet<String> = hashSetOf()
    var issuedTo: HashSet<String> = hashSetOf()
}