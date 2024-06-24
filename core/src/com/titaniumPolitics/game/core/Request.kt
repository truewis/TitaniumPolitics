package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.gameActions.GameAction
import kotlinx.serialization.Serializable

@Serializable
class Request(
    var action: GameAction,
    var issuedTo: HashSet<String>/*If unspecified, anyone can finish this request.*/
)
{
    var name = ""
        private set
    var executeTime = 0//If unspecified, it can be executed anytime.
    var issuedBy: HashSet<String> = hashSetOf() //If unspecified, it is a system request.

    fun generateName(): String
    {
        if (this.name != "")
        {
            //println("Warning: name of an information is already set but you are trying to generate a new one. $name");
            return this.name

        }
        val name =
            "${action.javaClass.simpleName}-${action.tgtPlace}-$executeTime-${
                Math.random().toString().substring(8)
            }"
        this.name = name
        return name
    }
}