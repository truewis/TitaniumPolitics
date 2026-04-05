package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.Arrest
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Wait
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
class ArrestRoutine(val suspect: String) : Routine() {
    var timeout = ReadOnly.constInt("ExecuteCommandRoutineInvalidActionTimeout")

    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        if (gState.characters[suspect] == null) {
            failed()
            return null
        }
        if (place == gState.characters[suspect]!!.place.name) {
            return null // Ready to execute arrest
        }
        if (subroutines.none { it is FindCharacterRoutine })
            return FindCharacterRoutine(suspect)
        return null
    }

    override fun onSubroutineFail(subroutine: Routine) {
        if (subroutine is FindCharacterRoutine) {
            failed()
            return
        } else
            super.onSubroutineFail(subroutine)
    }

    override fun execute(name: String, place: String): GameAction {
        if (gState.characters[suspect] == null) {
            failed()
            return Wait(name, place)
        }
        Arrest(name, place, suspect, gState).let {
            if (it.isValid()) {
                Logger.write("$name: Arrest action on $suspect is valid. Executing...", Logger.LogLevel.INFO)
                success()
                return it
            } else {
                timeout -= 1
                if (timeout <= 0) {
                    Logger.write(
                        "$name: Arrest action on $suspect is still not valid after waiting. Terminating the routine...",
                        Logger.LogLevel.INFO
                    )
                    failed()
                }
            }
        }
        return Wait(name, place)
    }
}
