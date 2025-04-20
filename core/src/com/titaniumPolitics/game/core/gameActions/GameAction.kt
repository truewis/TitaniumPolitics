package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
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
    abstract val sbjCharacter: String

    //This can be different from the current place of the subject, in case of a hypothetical action.
    abstract val tgtPlace: String

    val sbjCharObj get() = parent.characters[sbjCharacter]!!
    val tgtPlaceObj get() = parent.places[tgtPlace]!!

    @Transient
    lateinit var parent: GameState
    fun injectParent(parent: GameState)
    {
        this.parent = parent
    }

    open fun chooseParams()
    {
    }

    //Return all declared properties.
    fun returnParams()
    {

    }

    //This is a test function to check if the action is valid. It is called before execute. You can insert conditions to check here.
    //The execute function is still called even if this function returns false, but the engine throws a warning.
    open fun isValid(): Boolean
    {
        return true
    }

    open fun execute()
    {
        sbjCharObj.frozen += ReadOnly.constInt(this::class.simpleName!! + "Duration")
    }

    open fun deltaWill(): Double
    {
        return .0
    }

    //This function is used by agents to pick the best action they want.
    open fun optimizeWill(): Double
    {
        return deltaWill()
    }

}