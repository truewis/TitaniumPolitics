package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.Place
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.ReadOnly.const
import com.titaniumPolitics.game.core.gameActions.BuyDrink
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Talk
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class DowntimeRoutine() : Routine() {
    init {
        priority = PRIORITY_REST
    }

    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        if (condition(name, place)) return success()
        //If I am in a talk meeting, engage in the talk.
        gState.characters[name]!!.currentMeeting?.let {
            if (it.type == Meeting.MeetingType.TALK)
                return AttendPrivateMeetingRoutine(scheduledMeetingName = gState.meetingName(it))
        }
        val char = gState.characters[name]!!
        if (char.trait.contains("extrovert")) {
            if (place !in Place.publicPlaces)
                return MoveRoutine((Place.publicPlaces + "tavern").random())//Add a move routine with higher priority.
            else {
                //Engage in a random talk if possible.
                val potentialTalkTargets = gState.places[place]!!.characters.filter {
                    it != name && gState.getMutNorm(name, it) > 0
                }
                if (potentialTalkTargets.isNotEmpty()) {
                    val target = potentialTalkTargets.random()
                    return AttendPrivateMeetingRoutine(target)
                }
            }

        } else {
            //Otherwise, go home
            if (place != "home_$name")
                return MoveRoutine("home_$name")//Add a move routine with higher priority.
        }
        return null
    }

    override fun execute(name: String, place: String): GameAction {
        BuyDrink(name, place, gState).also {
            if (it.isValid())
                return it
        }
        Wait(name, place, gState).also {
            if (it.isValid())
                return it
        }
        throw Exception("No valid action found for DowntimeRoutine")
    }

    private fun condition(name: String, place: String): Boolean {
        //Pay attention to the condition checking order.
        if (gState.characters[name]!!.health <= const("CriticalHealth")
            || gState.characters[name]!!.hunger >= const("hungerThreshold")
            || gState.characters[name]!!.thirst >= const("thirstThreshold")
        ) return true //Need to take care of life support first.
        if (gState.getMutuality(name) < const("DowntimeWill")) return false
        if (variables["workplace"] == null)
            return false //Jobless = downtime forever.
        else
            return isWorkHourWithETA(gState, place, variables["workplace"]!!)
    }
}