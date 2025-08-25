package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.gameActions.GameAction
import kotlinx.serialization.Serializable

@Serializable
class FindCharacterRoutine(val character: String) : Routine() {
    var time = 0
    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        //Stop if spent too much time
        if (time != 0) {
            if (gState.time - time > 10) {
                executeDone = true
                return null
            }
        } else {
            time = gState.time
        }

        if (endCondition(name, place)) {
            return null
        }

        if (subroutines.none { it is MoveRoutine })
            return MoveRoutine(gState.places.values.find { it.characters.contains(character) }!!.name)
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        TODO("Not supposed to be called.")
    }

    override fun endCondition(name: String, place: String): Boolean {
        //Stop if the character is at the same place
        return executeDone || place == gState.places.values.find { it.characters.contains(character) }!!.name
        //TODO: when pathfinding fails, return true.
    }
}