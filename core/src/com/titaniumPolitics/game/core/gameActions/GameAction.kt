package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/*
*  This is the base class for all game actions. It is used to represent actions that characters can take.
*  It is also used to represent actions that are taken by the game itself.
*
*  Game actions are serialized to JSON and sent to the client. The client then displays the action to the user.
*  The user then chooses the parameters for the action. The client then sends the action back to the server.
*
*  The server then checks the parameters for validity and then executes the action.
* */
@Serializable
sealed class GameAction()
{
    abstract val tgtCharacter: String
    abstract val tgtPlace: String

    @Transient
    lateinit var parent: GameState
    fun injectParent(parent: GameState)
    {
        this.parent = parent
    }

    open fun chooseParams()
    {
    }

    open fun isValid(): Boolean
    {
        return true
    }

    abstract fun execute()
}