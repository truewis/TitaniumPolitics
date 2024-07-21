package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.NPCRoutines.*
import com.titaniumPolitics.game.core.gameActions.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.math.min

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
class AnonAgent : Agent()
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
                    routines.add(StealRoutine().apply {
                        priority = 1000
                        variables["stealResource"] = wantedResource
                    })//Add a routine, priority higher than work.

            } else if (parent.characters[name]!!.trait.contains("bargainer"))
            {
                if (routines.none { it is BuyRoutine })
                    routines.add(BuyRoutine().apply {
                        priority = 1000
                        variables["wantedResource"] = wantedResource
                    })//Add a routine, priority higher than work.
            }
        }

        //Anonymous characters does not have downtime when they are not working. They are always having downtime.


    }

    //This is a recursive function. It returns the action to be executed.
    private fun executeRoutine(): GameAction
    {
        if (routines.isEmpty())
        {
            whenIdle()
            if (routines.isEmpty())
                return Wait(name, place)
        }
        routines.sortByDescending { it.priority }
        //Leave meeting or conference if the routine was changed.
        //This allows the character to leave the meeting if it has a higher priority routine.
        //In this case, attendMeetingRoutine is still alive in the queue,
        //but it will be removed immediately when it becomes the current routine, as the character is not in a meeting.
        if ((routines[0] !is IMeetingRoutine && character.currentMeeting != null))
        {
            return LeaveMeeting(name, place)
        }
        //Don't start meetings
        //Don't execute commands
        var nextRoutine = routines[0]
        while (true)
        {
            nextRoutine.injectParent(parent)
            val v = nextRoutine.newRoutineCondition(name, place)
            if (v != null)
            {
                nextRoutine = v.apply { priority = nextRoutine.priority + 10 }
            } else if (nextRoutine.endCondition(name, place))
            {
                routines.remove(nextRoutine)
                routines.sortByDescending { it.priority }
                if (routines.isEmpty())
                {
                    whenIdle()
                    if (routines.isEmpty())
                        return Wait(name, place)
                }
                nextRoutine = routines[0]
            } else break
        }
        return nextRoutine.execute(name, place)

    }


    private fun whenIdle()
    {
        //When work hours, work
        if (parent.hour in 8..18)
        {
            routines.add(WanderRoutine())
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