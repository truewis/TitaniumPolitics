package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.gameActions.GameAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

//Trying implementing design pattern with function call stack is a bad idea because it is hard to debug.
//Routine was designed to be independent of the gameState, but it is not the case anymore.
@Serializable
sealed class Routine() {
    @Transient
    lateinit var gState: GameState
    var priority: Int = 0
    val subroutines = arrayListOf<Routine>()
    val variables: HashMap<String, String> = hashMapOf()
    val intVariables: HashMap<String, Int> = hashMapOf()
    val doubleVariables: HashMap<String, Double> = hashMapOf()
    var executeDone =
        false //This is used to check if the routine execution is successful. Otherwise, there is a problem executing the routine and the parent routine should be notified.

    fun injectParent(gState: GameState) {
        this.gState = gState
    }

    abstract fun newRoutineCondition(name: String, place: String, routines: List<Routine>): Routine?
    abstract fun execute(name: String, place: String): GameAction
    abstract fun endCondition(name: String, place: String): Boolean

    //The actions in this list are compared with GameEngine.availableActions() to see if the command is available.
    //Then, instances of the actions are created, their parameters are optimized for deltaWill, and their validity is checked.
    //If the action is valid, one with the highest deltaWill is executed.
    //Routines can switch to other routines in the meanwhile.
    @Transient
    open val availableActions: List<String> = listOf("Wait")

    //        get()
//        {
//            return when (name)
//            {
//                "work" -> listOf("Wait")
//                "rest" -> listOf("Eat", "Sleep", "Wait")
//                "attendMeeting" -> listOf("attendMeeting")
//                "supportAgenda" -> listOf("supportAgenda")
//                "attackAgenda" -> listOf("attackAgenda")
//                "attendConference" -> listOf("attendConference")
//                "findCharacter" -> listOf("Move")
//                "buy" -> listOf("buy")
//                else -> listOf()
//            }
//        }
    //TODO: it isn't clear at this moment how we pick between actions and routines. Shall we only pick between routines?
    //Just like the player pick actions at his will, NPC doesn't have to follow the gradient of will always. We just have to implement the penalty when the will is low in the game system.
    //Will based behaviour can be implemented in a different agent.
    fun pickAction(name: String, place: String): GameAction {

        return availableActions.intersect(GameEngine.availableActions(gState, place, name).toSet()).map {
            (Class.forName("com.titaniumPolitics.game.core.gameActions.$it")
                .getConstructor(String::class.java, String::class.java)
                .newInstance(name, place) as GameAction).apply { injectParent(gState);chooseParams() }

        }.filter { it.isValid() }.maxBy { it.optimizeWill() }
    }
}