package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.gameActions.GameAction
import kotlinx.serialization.Serializable

@Serializable
class Request(var place: String, var action: GameAction)
{
    var name = ""
    var executeTime = 0
    val compulsion = 0
    var issuedBy: HashSet<String> = hashSetOf()
    var issuedTo: HashSet<String> = hashSetOf()
    fun generateName(): String
    {
        if (this.name != "")
        {
            //println("Warning: name of an information is already set but you are trying to generate a new one. $name");
            return this.name

        }
        val name =
            "$action-$place-$executeTime-${
                Math.random().toString().substring(8)
            }"
        this.name = name
        return name
    }
}