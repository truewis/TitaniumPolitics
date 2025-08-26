package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Move
import com.titaniumPolitics.game.core.gameActions.Wait
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.fail

/**
 * A routine that moves a character to a destination.
 * The routine ends when the character reaches the destination.
 * Shortest path is used to determine the next stop.
 * If there is no path to the destination, the routine ends with an error.
 */
@Serializable
class MoveRoutine(var destination: String) : Routine() {
    var nextStop = ""
    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        Move(name, place, gState).also {
            it.placeTo = nextStop
            if (it.isValid()) return it
        }
        //If move is not valid, wait and terminate the routine with an error.
        failed = true
        return Wait(name, place)
    }

    override fun endCondition(name: String, place: String): Boolean {
        if (place == destination) {
            return true
        } else {
            if (gState.places[place]!!.shortestPathAndTimeTo(destination)?.also {
                    nextStop = it.first[1]
                } == null) {

                Logger.write(
                    "There is no path from $place to ${destination}! Terminating moveRoutine...",
                    Logger.LogLevel.INFO
                )
                failed = true
            }

        }

        return false
    }
}