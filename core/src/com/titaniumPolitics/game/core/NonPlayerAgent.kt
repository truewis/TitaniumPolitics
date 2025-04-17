package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.NPCRoutines.*
import com.titaniumPolitics.game.core.gameActions.*
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
class NonPlayerAgent : Agent()
{

    private var routines =
        arrayListOf<Routine>()//Routines are sorted by priority. The first element is the current routine. All other routines are executed when the current routine is finished.

    override fun chooseAction(): GameAction
    {
        //1. High priority routine change
        selectRoutine()
        //2. Execute action according to the current routine. This includes low priority switching routines.
        return executeRoutine()
    }

    private fun selectRoutine()
    {

        //Force start meeting routing if the character is in a meeting. Note that the character will leave the meeting immediately if nothing interests it.
        //Note that even if the character has a higher priority routine, this block will not trigger.
        if (parent.ongoingMeetings.any { it.value.currentCharacters.contains(name) } && routines.none { it is AttendMeetingRoutine })
        {
            routines.add(
                AttendMeetingRoutine().apply {
                    priority = 800
                    intVariables["routineStartTime"] = parent.time
                }
            )
            return
        }
        //If there is almost no food or water, stop all activities and try to get some. ----------------------------------------------------------------------------
        if ((parent.characters[name]!!.resources["ration"]
                ?: 0) <= (parent.characters[name]!!.reliants.size + 1) || (parent.characters[name]!!.resources["water"]
                ?: 0) <= (parent.characters[name]!!.reliants.size + 1)
        )
        {
            val wantedResource = if ((parent.characters[name]!!.resources["ration"]
                    ?: 0) <= (parent.characters[name]!!.reliants.size + 1)
            ) "ration" else "water"
            if (parent.characters[name]!!.trait.contains("thief"))
            {
                //Find a place within my division with maximum res.
                if (routines.none { it is StealRoutine })
                {
                    routines.add(StealRoutine().apply {
                        priority = 1000
                        variables["stealResource"] = wantedResource
                        intVariables["routineStartTime"] = parent.time
                    })//Add a routine, priority higher than work.
                    return
                }

            } else if (parent.characters[name]!!.trait.contains("bargainer"))
            {
                if (routines.none { it is BuyRoutine })
                {
                    routines.add(BuyRoutine().apply {
                        priority = 1000
                        variables["wantedResource"] = wantedResource
                        intVariables["routineStartTime"] = parent.time
                    })//Add a routine, priority higher than work.
                    return
                }
            }
        }

        //If will is low, downTime.
        if (parent.getMutuality(name) < 30)
        {
            if (routines.none { it is DowntimeRoutine })
            {
                routines.add(DowntimeRoutine().apply {
                    priority = 800
                    intVariables["routineStartTime"] = parent.time
                })//Add a routine, priority higher than work.
                return
            }
        }
        //If there is a command that is within the set time window, issued party is trusted enough, and seems to be executable at the exact place the character is in right now,(AvailableActions), start execution routine.
        //Note that the command may not be valid even if it in AvailableActions list. For example, if the character is already at the place, move command is not valid.
        //We should not enter executeCommand routine if it is already in the routine list.
        if (routines.none { it is ExecuteCommandRoutine })
        {
            val request = parent.requests.values.firstOrNull {
                GameEngine.availableActions(
                    parent,
                    it.action.tgtPlace,
                    name
                ).contains(it.action.javaClass.simpleName) && it.action.tgtPlace == place
            }
            if (request != null)
            {
                routines.add(
                    ExecuteCommandRoutine().apply {
                        priority = routines[0].priority + 10
                        variables["request"] = request.name
                        intVariables["routineStartTime"] = parent.time
                    }
                )//Add the routine with higher priority.
                return
            }
        }


    }

    //This is a recursive function. It returns the action to be executed.
    private fun executeRoutine(): GameAction
    {
        if (routines.isEmpty())
        {
            whenIdle()
            if (routines.isEmpty())
            {
                Logger.warning("There is truly nothing to do for $name. This is likely a bug.")
                return Wait(name, place)
            }
        }
        routines.sortByDescending { it.priority }

        var nextRoutine = routines[0]
        nextRoutine.injectParent(parent)
        while (true)
        {
            val v = nextRoutine.newRoutineCondition(name, place)
            if (v != null)
            {
                v.injectParent(parent)
                nextRoutine = v.apply { priority = nextRoutine.priority + 10 }
                routines.add(nextRoutine)
                nextRoutine.intVariables["routineStartTime"] = parent.time
                routines.sortByDescending { it.priority }
                continue
            } else if (nextRoutine.endCondition(name, place))
            {
                routines.remove(nextRoutine)
                routines.sortByDescending { it.priority }
                if (routines.isEmpty())
                {
                    whenIdle()
                    if (routines.isEmpty())
                    {
                        println("Warning: No routine is available for $name. Waiting.")
                        return Wait(name, place)
                    }
                }
                nextRoutine = routines[0]
                nextRoutine.injectParent(parent)
                continue
            } else break
        }
        blockExecution()?.also { return it }
        return nextRoutine.execute(name, place)

    }

    //Any action that has to be executed before executing the current routine.
    fun blockExecution(): GameAction?
    {
        //Leave meeting or conference if the routine was changed.
        //This allows the character to leave the meeting if it has a higher priority routine.
        //In this case, attendMeetingRoutine is still alive in the queue,
        //but it will be removed immediately when it becomes the current routine, as the character is not in a meeting.
        if ((routines[0] !is IMeetingRoutine && character.currentMeeting != null))
        {
            return LeaveMeeting(name, place)
        }
        return null
    }


    private fun whenIdle()
    {
        //When work hours, work
        if (parent.hour in 8..18)
        {
            routines.add(WorkRoutine())
            return
        } else
        //When not work hours, rest
            routines.add(RestRoutine())
    }

    @Deprecated("This function is not used anymore because we don't have trade action anymore.")
    fun decideTrade(
        who: String,
        value: Double /*value of the items I am giving away*/,
        value2: Double/*value of the items I will receive*/,
        valuea: Double,
        valuea2: Double
    ): Boolean
    {
        val friendlinessFactor =
            0.5//TODO: this should be determined by the character's trait. More friendly characters are more likely to accept the trade which benefits the other character.
        return value >= value2 + (parent.getMutuality(
            name,
            who
        ) - 50) * (valuea - valuea2) * friendlinessFactor / 100
    }


}