package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.Resources
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.UnofficialResourceTransfer
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
        val charactersDelegatableTo = gState.aliveCharacters.filter {
            it.key != name &&
                    gState.getMutuality(
                        name,
                        it.key
                    ) > executableRequest.difficulty() //Someone I trust, does not matter if they trust me or not
                    &&
                    executableRequest.action.isProofOfWork(
                        Information(
                            action = executableRequest.action.copy(it.key)
                        )
                    )
        }
        if (charactersDelegatableTo.isNotEmpty()) {
            charactersDelegatableTo.keys.intersect(
                gState.places[place]!!.characters
            ).firstOrNull()?.let { executor ->
                return TalkRoutine(
                    executor, MeetingAgenda(
                        AgendaType.REQUEST, name, attachedRequest = Request(
                            executableRequest.action.copy(executor),
                            issuedTo = hashSetOf(executor),
                            issuedBy = hashSetOf(name),
                            executeTime = gState.time
                        )
                    )
                )
            }
            //If there isn't anyone here to delegate the job, but am aware that someone exists,
            val delegator = charactersDelegatableTo.keys.first()
            return FindCharacterRoutine(delegator)

        }

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