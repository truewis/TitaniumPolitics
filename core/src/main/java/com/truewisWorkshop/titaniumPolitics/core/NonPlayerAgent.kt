package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.NPCRoutines.*
import com.titaniumPolitics.game.core.ReadOnly.IDTH
import com.titaniumPolitics.game.core.ReadOnly.const
import com.titaniumPolitics.game.core.gameActions.GameAction
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

    override fun printStatus(): String {
        return "Routines: $routines"
    }

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

        if (!routines.isEmpty())
            pri = routines[0].priority + 10
        //If there is almost no food or water, stop all activities and try to get some. ----------------------------------------------------------------------------
        if (character.resources["ration"] <= (character.reliant) || character.resources["water"] <= (character.reliant)
        ) {
            val wantedResource =
                if (character.resources["ration"] <= (character.reliant)
                ) "ration" else "water"
            if (character.trait.contains("thief")) {
                //Find a place within my division with maximum res.
                if (routines.none { it is StealRoutine }) {
                    routines.clear()
                    routines.add(
                        StealRoutine(
                            wantedResource,
                            (character.reliant + 1/*Numerically incorrect for anon agents, but ensure non zero value.*/) * const(
                                "StealAmountMultiplier"
                            )
                        ).apply {
                            priority = PRIORITY_LIFE_SUPPORT
                            routineStartTime = parent.time
                        })//Add a routine, priority higher than work.
                    return
                }

            } else if (character.trait.contains("bargainer")) {
                if (routines.none { it is BuyRoutine }) {
                    routines.clear()
                    routines.add(BuyRoutine(wantedResource, character.reliant * 10.0).apply {
                        priority = PRIORITY_LIFE_SUPPORT
                        routineStartTime = parent.time
                    })//Add a routine, priority higher than work.
                    return
                }
            }
        }
        //If health is low, rest
        if (character.health < const("TiredHealth") ||
            (character.hunger > const("hungerThreshold")) ||
            (character.thirst > const("thirstThreshold"))
        ) {
            if (routines.none { it is RestRoutine }) {
                routines.clear()
                routines.add(RestRoutine(parent.getWorkplace(name)?.name).apply {
                    priority = pri
                    routineStartTime = parent.time
                })//Add a routine, priority higher than work.
                return
            }
        }

        //If will is low, downTime.
        if (parent.getMutuality(name) < const("DowntimeWill")) {
            if (routines.none { it is DowntimeRoutine }) {
                routines.clear()
                routines.add(DowntimeRoutine(parent.getWorkplace(name)?.name).apply {

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
            if (loopCounter > maxLoopCounter + 10) {
                throw RuntimeException("Routine loop counter exceeded for $name.")
            }
            routineSettled = true
            routines.forEach {
                it.injectParent(parent)
            }
            routines.forEach {
                it.newRoutineCondition(name, place, it.subroutines.map { routines.first { rt -> rt.ID == it } })
                    ?.let { v ->
                        if (!it.subroutines.isEmpty()) return@let//Only support one subroutine for now.
                        v.routineStartTime = parent.time
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
            routines.forEach {
                if (it.success) {
                    routineSettled = false
                    endRoutine(it)
                }
            }
            routines.forEach {
                if (it.failed) {
                    routineSettled = false
                    failRoutine(it)
                }
            }
            //////////////////////////////////
            if (loopCounter > maxLoopCounter)
                Logger.write("Routines $removeList is being removed.", Logger.LogLevel.INFO)
            routines.removeAll(removeList)
            routines.forEach { routine -> routine.subroutines.removeIf { s -> routines.none { it.ID == s } } } //Remove the subroutines that were removed.
            removeList.clear()
            //////////////////////////////////
            if (routines.isEmpty()) {
                routineSettled = false
                whenIdle()
                if (routines.isEmpty()) {
                    Logger.write("There is truly nothing to do for $name. This is likely a bug.")
                    return Wait(name, place)
                }
            }
        }

        routines.forEach {
            it.injectParent(parent)
        }
        routines.sortByDescending { routine -> routine.priority }//WARNING: Soring must be done here, after the routines are updated and before the blockExecution.
        blockExecution(routines)?.also { return it }
        return routines[0].execute(name, place)

    }

    /**Recursively stop the routine and all its subroutines.
     *
     */
    fun endRoutine(routine: Routine) {
        routine.subroutines.forEach { id -> routines.firstOrNull { it.ID == id }?.let { endRoutine(it) } }
        removeList += (routine)
    }

    /**Recursively stop the routine and all its subroutines.
     * Also call the parent routine's onSubroutineFail if exists.
     */
    fun failRoutine(routine: Routine) {
        routine.subroutines.forEach { id -> routines.firstOrNull { it.ID == id }?.let { endRoutine(it) } }
        removeList += (routine)
        //Call parent routine's onSubroutineFail if exists.
        routines.filter { it.subroutines.contains(routine.ID) }.forEach { it.onSubroutineFail(routine) }
    }


    private fun whenIdle() {
        //If life support is needed, do it first.
        if (character.health < const("TiredHealth") ||
            (character.hunger > const("hungerThreshold")) ||
            (character.thirst > const("thirstThreshold"))
        ) {
            routines.add(RestRoutine(parent.getWorkplace(name)?.name).also {
                it.routineStartTime = parent.time
            })
            return
        }
        //When work hours, work
        parent.getWorkplace(name)?.let { wkplace ->
            if (Routine.isWorkCondition(name, place, wkplace.name, parent)) {
                if (name.contains("Anon")) {
                    routines.add(WorkAnonRoutine(wkplace.name).also {
                        it.routineStartTime = parent.time
                    })
                } else if (wkplace.responsibleDivision == null && StoreWorkRoutine.isPrivateStore(wkplace.name)) {
                    routines.add(StoreWorkRoutine(wkplace.name).also {
                        it.routineStartTime = parent.time
                    })
                } else {
                    routines.add(WorkRoutine(wkplace.name).also {
                        it.routineStartTime = parent.time
                    })
                }
                return
            }
        }

        //When no work and no life support, play
        routines.add(DowntimeRoutine(parent.getWorkplace(name)?.name).also {
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
