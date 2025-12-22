package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.ReadOnly.IDTH
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

//Trying implementing design pattern with function call stack is a bad idea because it is hard to debug.
//Routine was designed to be independent of the gameState, but it is not the case anymore.
@Serializable
sealed class Routine() {
    @Transient
    lateinit var gState: GameState
    val ID = UUID.randomUUID().toString()

    @Deprecated("Only one subroutine per routine is allowed, so priority must not be set.")
    var priority: Int = 0
    val subroutines = arrayListOf<String>() //Store the IDs of subroutines that are currently running.
    var routineStartTime: Int = 0 //The time when the routine starts, used to calculate the duration of the routine.
    val variables: HashMap<String, String> = hashMapOf()
    val PRIORITY_WORK = 1000
    val PRIORITY_MEETING = 1500
    val PRIORITY_REST = 0
    val PRIORITY_LIFE_SUPPORT = 2000

    /**This is used to check if the routine execution is successful. Otherwise, there is a problem executing the routine and the parent routine should be notified.
     */
    var success =
        false
        private set

    /**This is used to check if the routine has failed.
     * Once set to true, the routine will be killed regardless of the success variable, and onSubroutineFail method of the parent routine will be called.
     * */
    var failed = false
        private set
    private var ended = false
    fun success(): Routine? {
        if (!ended)
            success = true
        else {
            if (success)
                Logger.write("Routine Control Flow warning: success called for an already successful routine: $this")
            if (failed)
                Logger.write("Routine Control Flow warning: success called for an already failed routine: $this")
        }
        ended = true
        return null
    }

    fun failed(): Routine? {
        if (!ended)
            failed = true
        else {
            if (success)
                Logger.write("Routine Control Flow warning: failed called for an already successful routine: $this")
            if (failed)
                Logger.write("Routine Control Flow warning: failed called for an already failed routine: $this")
        }
        ended = true
        return null
    }

    fun injectParent(gState: GameState) {
        this.gState = gState
    }

    abstract fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine?
    abstract fun execute(name: String, place: String): GameAction

    /**
     * This function is called when a subroutine ends with error.
     * The default implementation is to fail the current routine as well.
     * Override this function to implement custom behaviour.
     */
    open fun onSubroutineFail(subroutine: Routine) {
        failed()
    }


    override fun toString(): String {
        return "${this::class.simpleName}(routineStartTime=$routineStartTime, variables=$variables)"
    }

    companion object {
        fun isWorkHourWithETA(
            gState: GameState,
            name: String,
            place: String,
            workplace: String,
            padding: Int = 0
        ): Boolean {
            //Consider the estimated time to workplace, if the character is not at home.
            val eta = gState.places[place]!!.shortestPathAndTimeTo(workplace, name)?.second ?: 0
            val extendedWorkHours =
                gState.places[workplace]!!.workHours.first * IDTH - eta - padding..gState.places[workplace]!!.workHours.last * IDTH + eta + padding
            return (gState.timeInDay in extendedWorkHours)
        }

        fun isWorkCondition(name: String, place: String, workplace: String, gState: GameState): Boolean {
            return (isWorkHourWithETA(gState, name, place, workplace, IDTH)
                && gState.characters[name]!!.health > ReadOnly.const("CriticalHealth")
                && gState.characters[name]!!.hunger < ReadOnly.const("hungerThreshold")
                && gState.characters[name]!!.thirst < ReadOnly.const("thirstThreshold")
                )
        }
    }
}
