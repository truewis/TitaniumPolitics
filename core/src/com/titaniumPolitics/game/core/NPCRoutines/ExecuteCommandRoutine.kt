package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Move
import kotlinx.serialization.Serializable

@Serializable
class ExecuteCommandRoutine() : Routine()
{
    val executableRequest = gState.requests[variables["request"]!!]!!

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
            if (executableRequest.action.isValid())
            {
                println("$name: The request ${executableRequest.action} is valid. Executing...")
                executeDone = true
                return executableRequest.action
            }
        }
        throw Exception("The request ${executableRequest.action} is invalid.")
    }

    override fun endCondition(name: String, place: String): Boolean
    {
        return place == executableRequest.action.tgtPlace && !executableRequest.action.isValid()
        //TODO: if execution is too late, return true.
    }

}