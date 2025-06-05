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
class NonPlayerAgent : Agent() {

    var routines =
        arrayListOf<Routine>()//Routines are sorted by priority. The first element is the current routine. All other routines are executed when the current routine is finished.
    private val removeList =
        arrayListOf<Routine>() //Routines that are to be removed after the current routine is executed.
    private val addList =
        arrayListOf<Routine>() //Routines that are to be added after the current routine is executed.

    override fun chooseAction(): GameAction {
        //1. High priority routine change
        selectRoutine()
        //2. Execute action according to the current routine. This includes low priority switching routines.
        return executeRoutine()
    }

    //Also Check AnonAgent.kt
    private fun selectRoutine() {
        var pri = 10
        routines.sortByDescending { it.priority }

        //Remove all meeting routines if the character is not in a meeting.
        if (character.currentMeeting == null)
            routines.removeAll { it is IMeetingRoutine }

        if (!routines.isEmpty())
            pri = routines[0].priority + 10
        //If there is almost no food or water, stop all activities and try to get some. ----------------------------------------------------------------------------
        if (parent.characters[name]!!.resources["ration"] <= (parent.characters[name]!!.reliant) || parent.characters[name]!!.resources["water"] <= (parent.characters[name]!!.reliant)
        ) {
            val wantedResource =
                if (parent.characters[name]!!.resources["ration"] <= (parent.characters[name]!!.reliant)
                ) "ration" else "water"
            if (parent.characters[name]!!.trait.contains("thief")) {
                //Find a place within my division with maximum res.
                if (routines.none { it is StealRoutine }) {
                    routines.add(StealRoutine().apply {

                        priority = pri
                        variables["stealResource"] = wantedResource
                        intVariables["routineStartTime"] = parent.time
                    })//Add a routine, priority higher than work.
                    return
                }

            } else if (parent.characters[name]!!.trait.contains("bargainer")) {
                if (routines.none { it is BuyRoutine }) {
                    routines.add(BuyRoutine().apply {

                        priority = pri
                        variables["wantedResource"] = wantedResource
                        intVariables["routineStartTime"] = parent.time
                    })//Add a routine, priority higher than work.
                    return
                }
            }
        }
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
        //If there is a command that is within the set time window, issued party is trusted enough, and seems to be executable at the exact place the character is in right now,(AvailableActions), start execution routine.
        //Note that the command may not be valid even if it in AvailableActions list. For example, if the character is already at the place, move command is not valid.
        //We should not enter executeCommand routine if it is already in the routine list.
        if (routines.none { it is ExecuteCommandRoutine }) {
            val request = parent.requests.values.firstOrNull {
                GameEngine.availableActions(
                    parent,
                    it.action.tgtPlace,
                    name
                ).contains(it.action.javaClass.simpleName) && it.action.tgtPlace == place
            }
            if (request != null) {
                routines.add(
                    ExecuteCommandRoutine().apply {

                        priority = pri
                        variables["request"] = request.name
                        intVariables["routineStartTime"] = parent.time
                    }
                )//Add the routine with higher priority.
                return
            }
        }


    }

    //This is a recursive function. It returns the action to be executed.
    private fun executeRoutine(): GameAction {
        routines.sortByDescending { it.priority }

        var routineSettled = false
        while (!routineSettled) {
            routineSettled = true
            routines.forEach {
                it.injectParent(parent)
            }
            routines.forEach {
                if (it.endCondition(name, place)) {
                    routineSettled = false
                    endRoutine(it)
                }
            }
            routines.removeAll(removeList)
            routines.forEach { routine -> routine.subroutines.removeIf { s -> routines.none { it.ID == s } } } //Remove the subroutines that were removed.
            removeList.clear()
            routines.forEach {
                it.newRoutineCondition(name, place, routines)?.let { v ->
                    if (v.priority == 0)//Initial priority
                        v.priority = it.priority + 10 //Set the priority to be higher than the current routine.
                    it.subroutines += v.ID
                    addList += v
                    v.intVariables["routineStartTime"] = parent.time
                    routineSettled = false
                }
            }
            routines += addList
            addList.clear()
        }

        if (routines.isEmpty()) {
            whenIdle()
            if (routines.isEmpty()) {
                Logger.warning("There is truly nothing to do for $name. This is likely a bug.")
                return Wait(name, place)
            }
        }
        routines.forEach {
            it.injectParent(parent)
        }
        blockExecution()?.also { return it }

        routines.sortByDescending { routine -> routine.priority }
        return routines[0].execute(name, place)

    }

    //Recursively stop the routine and all its subroutines.
    fun endRoutine(routine: Routine) {
        routine.subroutines.forEach { id -> endRoutine(routines.first { it.ID == id }) }
        removeList += (routine)
    }

    //Any action that has to be executed before executing the current routine.
    fun blockExecution(): GameAction? {
        //Leave meeting or conference if the routine was changed.
        //This allows the character to leave the meeting if it has a higher priority routine.
        //In this case, attendMeetingRoutine is still alive in the queue,
        //but it will be removed immediately when it becomes the current routine, as the character is not in a meeting.
        if (routines.isEmpty()) return null
        if ((routines[0] !is IMeetingRoutine && character.currentMeeting != null)) {
            return LeaveMeeting(name, place)
        }
        return null
    }


    private fun whenIdle() {
        //When work hours, work
        if (parent.hour in 8..18) {
            routines.add(WorkRoutine().also {
                it.intVariables["routineStartTime"] = parent.time
            })
            return
        } else
        //When not work hours, rest
            routines.add(RestRoutine().also {
                it.intVariables["routineStartTime"] = parent.time
            })
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