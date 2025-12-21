package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Rescue
import com.titaniumPolitics.game.core.gameActions.Wait
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
class RescueRoutine(val rescuee: String) : Routine() {
    var timeout = ReadOnly.constInt("ExecuteCommandRoutineInvalidActionTimeout")


    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        if (subroutines.none { it is FindCharacterRoutine })
            return FindCharacterRoutine(rescuee)
        return null
    }

    override fun onSubroutineFail(subroutine: Routine) {
        if (subroutine is FindCharacterRoutine) {
            //Find Character routine failed, meaning that the character is not reachable.
            failed()
            return
        } else
            super.onSubroutineFail(subroutine)
    }

    override fun execute(name: String, place: String): GameAction {
        //Check if rescue is valid action
        Rescue(name, place, rescuee).let {
            if (it.isValid()) {
                Logger.write("$name: Rescue action is valid. Executing...", Logger.LogLevel.INFO)
                success()
                return it
            } else {
                timeout -= 1
                //Wait a bit to see if the action gets valid
                if (timeout <= 0) {
                    Logger.write(
                        "$name: Rescue action is still not valid after waiting. Terminating the routine...",
                        Logger.LogLevel.INFO
                    )
                    failed()
                }
            }
        }
        return Wait(name, place)
    }


}
