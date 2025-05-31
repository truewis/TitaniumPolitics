package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.Meeting
import kotlinx.serialization.Serializable

@Serializable
class AppointMeeting(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    var meetingTime = 0
    var where = ""
    var who = hashSetOf<String>()
    override fun chooseParams()
    {

        meetingTime = parent.time + GameEngine.acquire(arrayListOf("3", "6", "9", "12", "18", "21", "24")).toInt()
        where = GameEngine.acquire(parent.places.map { it.value.name })
        who.add(GameEngine.acquire(parent.characters.map { it.value.name }))//TODO: meeting with multiple people
    }

    override fun execute()
    {
        parent.addScheduledMeeting(
            Meeting(meetingTime, "informal", who, where))

    }

    override fun isValid(): Boolean
    {
        return parent.characters[sbjCharacter]!!.currentMeeting != null
    }

}