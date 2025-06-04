package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.NPCRoutines.*
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.LeaveMeeting
import com.titaniumPolitics.game.core.gameActions.Wait
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

/*
*  NonPlayerAgent is a character that is not controlled by the player.
* It has a list of routines, which are executed in order of priority.
* The first routine is the current routine.
* When the current routine is finished, the next routine is executed.
* Routines ultimately return GameAction, which is executed by the GameEngine.
* This logic is called by GameEngine.chooseAction(), once per character per turn.
*
* */
@Serializable
class AnonAgent : Agent() {

    var workPlace = ""
    private var routines =
        arrayListOf<Routine>()//Routines are sorted by priority. The first element is the current routine. All other routines are executed when the current routine is finished.

    override fun chooseAction(): GameAction {
        //1. High priority routine change
        selectRoutine()
        //2. Execute action according to the current routine. This includes low priority switching routines.
        return executeRoutine()
    }

    //Also check NonPlayerAgent.kt
    private fun selectRoutine() {
        var pri = 10
        if (!routines.isEmpty())
            pri = routines[0].priority + 10
        //If there is almost no food or water, stop all activities and try to get some. ----------------------------------------------------------------------------
        if (parent.characters[name]!!.resources["ration"]
            <= (parent.characters[name]!!.reliant) || parent.characters[name]!!.resources["water"] <= (parent.characters[name]!!.reliant)
        ) {
            val wantedResource =
                if (parent.characters[name]!!.resources["ration"] <= (parent.characters[name]!!.reliant)
                ) "ration" else "water"
            if (parent.characters[name]!!.trait.contains("thief")) {
                //Find a place within my division with maximum res.
                if (routines.none { it is StealRoutine })
                    routines.add(StealRoutine().apply {
                        priority = pri
                        variables["stealResource"] = wantedResource
                        intVariables["routineStartTime"] = parent.time
                    })//Add a routine, priority higher than work.

            } else if (parent.characters[name]!!.trait.contains("bargainer")) {
                if (routines.none { it is BuyRoutine })
                    routines.add(BuyRoutine().apply {
                        priority = pri
                        variables["wantedResource"] = wantedResource
                        intVariables["routineStartTime"] = parent.time
                    })//Add a routine, priority higher than work.
            }
        }

        //Anonymous characters does not have downtime when they are not working. They are always having downtime.
        //If health is low, rest
        if (character.health < ReadOnly.const("TiredHealth")) {
            if (routines.none { it is RestRoutine }) {
                routines.add(RestRoutine().apply {
                    priority = pri
                    intVariables["routineStartTime"] = parent.time
                })//Add a routine, priority higher than work.
                return
            }
        }

        //If will is low, downTime.
        if (parent.getMutuality(name) < ReadOnly.const("DowntimeWill")) {
            if (routines.none { it is DowntimeRoutine }) {
                routines.add(DowntimeRoutine().apply {
                    priority = pri
                    intVariables["routineStartTime"] = parent.time
                })//Add a routine, priority higher than work.
                return
            }
        }

    }

    //This is a recursive function. It returns the action to be executed.
    private fun executeRoutine(): GameAction {
        if (routines.isEmpty()) {
            whenIdle()
            if (routines.isEmpty()) {
                Logger.warning("There is truly nothing to do for $name. This is likely a bug.")
                return Wait(name, place)
            }
        }
        routines.sortByDescending { it.priority }

        var routineSettled = false
        while (!routineSettled) {
            routineSettled = true
            routines.forEach {
                if (it.endCondition(name, place))
                    endRoutine(it)
                it.newRoutineCondition(name, place, routines)?.let { v ->
                    v.injectParent(parent)
                    it.subroutines += v
                    routines += v
                    v.intVariables["routineStartTime"] = parent.time
                    routineSettled = false
                }
            }
        }
        blockExecution()?.also { return it }

        routines.sortByDescending { routine -> routine.priority }
        return routines[0].execute(name, place)

    }

    //Recursively stop the routine and all its subroutines.
    fun endRoutine(routine: Routine) {
        routine.subroutines.forEach { endRoutine(it) }
        routines.remove(routine)
    }

    //Any action that has to be executed before executing the current routine.
    fun blockExecution(): GameAction? {
        //Leave meeting or conference if the routine was changed.
        //This allows the character to leave the meeting if it has a higher priority routine.
        //In this case, attendMeetingRoutine is still alive in the queue,
        //but it will be removed immediately when it becomes the current routine, as the character is not in a meeting.
        if ((routines[0] !is IMeetingRoutine && character.currentMeeting != null)) {
            return LeaveMeeting(name, place)
        }
        return null
    }

    private fun whenIdle() {
        //When work hours, work
        if (parent.hour in parent.places[workPlace]!!.workHoursStart..parent.places[workPlace]!!.workHoursEnd) {
            routines.add(WorkAnonRoutine().also { it.variables["workPlace"] = workPlace })
            return
        } else
        //When not work hours, rest
            routines.add(RestRoutine().also { it.variables["workPlace"] = workPlace })
    }

    @Deprecated("This function is not used anymore because we don't have trade action anymore.")
    fun decideTrade(
        who: String,
        value: Double /*value of the items I am giving away*/,
        value2: Double/*value of the items I will receive*/,
        valuea: Double,
        valuea2: Double
    ): Boolean {
        val friendlinessFactor =
            0.5//TODO: this should be determined by the character's trait. More friendly characters are more likely to accept the trade which benefits the other character.
        return value >= value2 + (parent.getMutuality(
            name,
            who
        ) - 50) * (valuea - valuea2) * friendlinessFactor / 100
    }


}