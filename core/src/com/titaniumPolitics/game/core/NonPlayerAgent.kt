package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.NPCRoutines.*
import com.titaniumPolitics.game.core.ReadOnly.DTH
import com.titaniumPolitics.game.core.ReadOnly.IDTH
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
    /**Routines are sorted by priority. The first element is the current routine. All other routines are executed when the current routine is finished.
     *
     */
    var routines =
        arrayListOf<Routine>()

    /**
     * Routines that are to be removed after the current routine is executed.
     */
    private val removeList =
        arrayListOf<Routine>()

    /**
     * Routines that are to be added after the current routine is executed.
     */
    private val addList =
        arrayListOf<Routine>()

    /**
     * Several events will set this flag to true, indicating that the character wants to talk to the player.
     * This will change the character's routine to talk to the player when possible.
     */
    var wantsToTalkToPlayer = false

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
                    routines.add(StealRoutine(wantedResource).apply {
                        priority = pri
                        routineStartTime = parent.time
                    })//Add a routine, priority higher than work.
                    return
                }

            } else if (parent.characters[name]!!.trait.contains("bargainer")) {
                if (routines.none { it is BuyRoutine }) {
                    routines.add(BuyRoutine(wantedResource).apply {
                        priority = pri
                        routineStartTime = parent.time
                    })//Add a routine, priority higher than work.
                    return
                }
            }
        }
        //If health is low, rest
        if (character.health < ReadOnly.const("TiredHealth")) {
            if (routines.none { it is RestRoutine }) {
                routines.add(RestRoutine(parent.getWorkplace(name)?.name).apply {
                    priority = pri
                    routineStartTime = parent.time
                })//Add a routine, priority higher than work.
                return
            }
        }

        //If will is low, downTime.
        if (parent.getMutuality(name) < ReadOnly.const("DowntimeWill")) {
            if (routines.none { it is DowntimeRoutine }) {
                routines.add(DowntimeRoutine().apply {

                    priority = pri
                    routineStartTime = parent.time
                })//Add a routine, priority higher than work.
                return
            }
        }

    }

    //This is a recursive function. It returns the action to be executed.
    private fun executeRoutine(): GameAction {
        routines.sortByDescending { it.priority }

        var routineSettled = false
        var loopCounter = 0
        var maxLoopCounter = 20
        while (!routineSettled) {
            loopCounter++
            if (loopCounter == maxLoopCounter) {
                Logger.write(
                    "//////////////////////Routine loop counter exceeded for $name. Collecting Trace.//////////////////////",
                    Logger.LogLevel.INFO
                )
            }
            if (loopCounter > maxLoopCounter + 5) {
                throw RuntimeException("Routine loop counter exceeded for $name.")
            }
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
            if (loopCounter > maxLoopCounter)
                Logger.write("Routines $removeList is being removed.", Logger.LogLevel.INFO)
            removeList.clear()
            routines.forEach {
                it.newRoutineCondition(name, place, it.subroutines.map { routines.first { rt -> rt.ID == it } })
                    ?.let { v ->
                        v.routineStartTime = parent.time
                        if (v.priority == 0)//Initial priority
                            v.priority = it.priority + 10 //Set the priority to be higher than the current routine.
                        it.subroutines += v.ID
                        addList += v
                        if (loopCounter > maxLoopCounter)
                            Logger.write("Adding new routine $v from $it", Logger.LogLevel.INFO)
                        routineSettled = false
                    }
            }
            routines += addList
            addList.clear()
        }

        if (routines.isEmpty()) {
            whenIdle()
            if (routines.isEmpty()) {
                Logger.write("There is truly nothing to do for $name. This is likely a bug.")
                return Wait(name, place)
            }
        }
        routines.forEach {
            it.injectParent(parent)
        }
        routines.sortByDescending { routine -> routine.priority }//WARNING: Soring must be done here, after the routines are updated and before the blockExecution.
        blockExecution(routines)?.also { return it }
        return routines[0].execute(name, place)

    }

    //Recursively stop the routine and all its subroutines.
    fun endRoutine(routine: Routine) {
        routine.subroutines.forEach { id -> routines.firstOrNull { it.ID == id }?.let { endRoutine(it) } }
        removeList += (routine)
    }


    private fun whenIdle() {
        //When work hours, work
        parent.getWorkplace(name)?.let { wkplace ->
            if (Routine.isWorkHourWithETA(parent, place, wkplace.name, IDTH)) {
                routines.add(WorkRoutine(wkplace.name).also {
                    it.routineStartTime = parent.time
                })
                return
            } else
            //When not work hours, rest
                routines.add(RestRoutine(wkplace.name).also {
                    it.routineStartTime = parent.time
                })
        }
            ?:
            //When no workplace, play
            routines.add(DowntimeRoutine().also {
                it.routineStartTime = parent.time
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