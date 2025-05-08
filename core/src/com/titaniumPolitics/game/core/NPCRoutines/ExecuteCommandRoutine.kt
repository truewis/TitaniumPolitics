package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable

@Serializable
class ExecuteCommandRoutine() : Routine()
{
    var err = false
    val executableRequest get() = gState.requests[variables["request"]!!]!!
    var timeout = ReadOnly.const("ExecuteCommandRoutineInvalidActionTimeout")

    override fun newRoutineCondition(name: String, place: String): Routine?
    {
        println("$name is executing the command ${executableRequest}.")

        if (place != executableRequest.action.tgtPlace)
        {
            return MoveRoutine().apply {
                variables["movePlace"] = executableRequest.action.tgtPlace
            }//Add a move routine with higher priority.
        }
        return null
    }

    override fun execute(name: String, place: String): GameAction
    {
        if (place == executableRequest.action.tgtPlace)
        {
            executableRequest.action.injectParent(gState)
            if (executableRequest.action.isValid())
            {
                println("$name: The request ${executableRequest.action} is valid. Executing...")
                executeDone = true
                return executableRequest.action
            } else
            {
                timeout -= 1
                //Wait a bit to see if the action gets valid
                if (timeout <= 0)
                {
                    err = true
                    //TODO: executableRequest callback
                }
                return Wait(name, place)

            }
        }

        println("$name: Cannot move to ${executableRequest.action.tgtPlace} to execute the request ${executableRequest.action}. Terminating the routine......")
        err = true
        return Wait(name, place)
    }

    override fun endCondition(name: String, place: String): Boolean
    {
        return executeDone || err
    }

}