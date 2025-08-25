package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Wait
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
class ExecuteCommandRoutine() : Routine() {
    var err = false
    val executableRequest get() = gState.requests[variables["request"]!!]!!
    var timeout = ReadOnly.const("ExecuteCommandRoutineInvalidActionTimeout")
    var delegationAttemptCount = ReadOnly.const("ExecuteCommandRoutineDelegationAttemptCount")

    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        Logger.write("$name is executing the command ${executableRequest}.", Logger.LogLevel.INFO)
        gState.characters.filter {
            it.key != name &&
                    gState.getMutuality(
                        name,
                        it.key
                    ) > executableRequest.difficulty() //Someone I trust, does not matter if they trust me or not
        }
        //Check if the action is delegatable
        if (executableRequest.action.isProofOfWork(
                Information(
                    action = executableRequest.action
                )
            )
        )

            if (place != executableRequest.action.tgtPlace) {
                if (subroutines.none { it is MoveRoutine })
                    return MoveRoutine(executableRequest.action.tgtPlace)//Add a move routine with higher priority.
            }
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        if (place == executableRequest.action.tgtPlace) {
            executableRequest.action.injectParent(gState)
            if (executableRequest.action.isValid()) {
                Logger.write(
                    "$name: The request ${executableRequest.action} is valid. Executing...",
                    Logger.LogLevel.INFO
                )
                executeDone = true
                return executableRequest.action
            } else {
                timeout -= 1
                //Wait a bit to see if the action gets valid
                if (timeout <= 0) {
                    err = true
                    //TODO: executableRequest callback
                }
                return Wait(name, place)

            }
        }

        Logger.write(
            "$name: Cannot move to ${executableRequest.action.tgtPlace} to execute the request ${executableRequest.action}. Terminating the routine......",
            Logger.LogLevel.INFO
        )
        err = true
        return Wait(name, place)
    }

    override fun endCondition(name: String, place: String): Boolean {
        return executeDone || err
    }

}