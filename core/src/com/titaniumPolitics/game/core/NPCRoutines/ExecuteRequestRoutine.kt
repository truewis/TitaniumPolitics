package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Wait
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
class ExecuteRequestRoutine() : Routine() {
    val executableRequest get() = gState.requests[variables["request"]!!]!!
    var timeout = ReadOnly.constInt("ExecuteCommandRoutineInvalidActionTimeout")
    var delegationAttemptCount = 0


    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        //If there are subroutines, it means that the character is already doing something else, so do not create new subroutines.
        if (subroutines.isEmpty()) {

            //Try to delegate the command to someone else if possible.

            if (delegationAttemptCount < ReadOnly.constInt("ExecuteCommandRoutineDelegationAttemptCount")) {
                val charactersDelegatableTo = gState.aliveCharacters.filter {
                    it.key != name && it.key !in executableRequest.issuedBy && it.key !in executableRequest.issuedTo &&
                            gState.getMutuality(
                                name,
                                it.key
                            ) > executableRequest.difficulty(gState) //Someone I trust, does not matter if they trust me or not
                            &&
                            //But if I am an Employee, do not delegate the command, who would do the actual work then? Everyone else is just my boss.
                            gState.parties.any {
                                name == it.value.leader && it.value.members.contains(it.key)
                            }
                            &&
                            executableRequest.action.isProofOfWork(
                                Information(
                                    action = executableRequest.action.copyRef(it.key)
                                )
                            )
                }
                if (charactersDelegatableTo.isEmpty()) {
                    //No one to delegate to, stop trying.
                    delegationAttemptCount = ReadOnly.constInt("ExecuteCommandRoutineDelegationAttemptCount")
                } else {
                    delegationAttemptCount++
                    charactersDelegatableTo.keys.intersect(
                        gState.places[place]!!.characters
                    ).firstOrNull()?.let { executor ->
                        //If there is someone to delegate to in the same place, try to have a private meeting with them to delegate the job.
                        if (subroutines.none { it is IMeetingRoutine })
                            return AttendPrivateMeetingRoutine(
                                executor, MeetingAgenda(
                                    AgendaType.REQUEST, name, attachedRequest = Request(
                                        executableRequest.action.copyRef(executor),
                                        issuedTo = hashSetOf(executor),
                                        issuedBy = hashSetOf(name),
                                        executeTime = gState.time
                                    )
                                )
                            )
                    }
                    //If there isn't anyone here to delegate the job, but am aware that someone exists,
                    val delegator = charactersDelegatableTo.keys.random()
                    return FindCharacterRoutine(delegator)

                }
            }
            //If I cannot delegate the command to anyone else, try to move to the target place to execute it myself.
            else {
                if (place != executableRequest.action.tgtPlace) {
                    if (subroutines.none { it is MoveRoutine })
                        return MoveRoutine(executableRequest.action.tgtPlace)//Add a move routine with higher priority.
                }
            }
        }

        return null
    }

    override fun onSubroutineFail(subroutine: Routine) {
        if (subroutine is FindCharacterRoutine) {
            //Find Character routine failed, meaning that the character is not reachable.
            //Delegation attempt counter has already been incremented in newRoutineCondition, so just return.
            return
        } else
            super.onSubroutineFail(subroutine)
    }

    override fun execute(name: String, place: String): GameAction {
        if (place == executableRequest.action.tgtPlace) {
            val copy = executableRequest.action.copyRef(name, place)
            copy.injectParent(gState)
            if (copy.isValid()) {
                Logger.write(
                    "$name: The request ${executableRequest.action} is valid. Executing...",
                    Logger.LogLevel.INFO
                )
                success()
                return copy
            } else {
                timeout -= 1
                //Wait a bit to see if the action gets valid
                if (timeout <= 0) {
                    failed()
                    //TODO: executableRequest callback
                }
                return Wait(name, place)

            }
        }

        Logger.write(
            "$name: Cannot move to ${executableRequest.action.tgtPlace} to execute the request ${executableRequest.action}. Terminating the routine......",
            Logger.LogLevel.INFO
        )
        failed()
        return Wait(name, place)
    }

}