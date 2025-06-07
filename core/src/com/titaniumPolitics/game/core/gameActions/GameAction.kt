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
sealed class GameAction() {
    abstract val sbjCharacter: String

    //This can be different from the current place of the subject, in case of a hypothetical action.
    abstract val tgtPlace: String

    val sbjCharObj get() = parent.characters[sbjCharacter]!!
    val tgtPlaceObj get() = parent.places[tgtPlace]!!


    //This is used to store why the action is invalid, used by the UI elements to display the reason why the action cannot be performed.
    @Transient
    var invalidReason = ""

    @Transient
    lateinit var parent: GameState
    fun injectParent(parent: GameState) {
        this.parent = parent
    }

    open fun chooseParams() {
    }

    //Return all declared properties.
    fun returnParams() {

    }

    //This is a test function to check if the action is valid. It is called before execute. You can insert conditions to check here.
    //The execute function is still called even if this function returns false, but the engine throws a warning.
    open fun isValid(): Boolean {
        return true
    }

    //Some gameActions have more complicated freezing mechanism, so they don't call this function.
    open fun execute() {

        //Execution time penalty when the will is low.
        if (parent.getMutuality(sbjCharacter) < ReadOnly.const("CriticalWill")) {
            if (this is NewAgenda || this is Intercept || this is InvestigateAccidentScene || this is ClearAccidentScene || this is PrepareInfo)
                sbjCharObj.frozen += 3 * ReadOnly.constInt(this::class.simpleName!! + "Duration")
            else if (this is Sleep || this is Move)
                sbjCharObj.frozen += 2 * ReadOnly.constInt(this::class.simpleName!! + "Duration")
            //TODO: Reduce health every turn?
        } else if (parent.getMutuality(sbjCharacter) < ReadOnly.const("DowntimeWill")) {
            sbjCharObj.frozen += 3 * ReadOnly.constInt(this::class.simpleName!! + "Duration") / 2

        } else
            sbjCharObj.frozen += ReadOnly.constInt(this::class.simpleName!! + "Duration")
    }

    open fun deltaWill(): Double {
        return .0
    }

    //This function is used by agents to pick the best action they want.
    open fun optimizeWill(): Double {
        return deltaWill()
    }

}