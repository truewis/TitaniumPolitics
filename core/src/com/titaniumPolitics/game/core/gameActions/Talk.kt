package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.Meeting
import kotlinx.serialization.Serializable

@Serializable
//Talk is considered as an on-the-fly meeting.
//If the target character is already in a meeting, join the meeting if possible. Otherwise, this action is invalid.
class Talk(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{
    var who = ""
    override fun chooseParams()
    {
        who =
            GameEngine.acquire(parent.places[tgtPlace]!!.characters.filter { it != tgtCharacter }.toList())
        if (parent.characters[who]!!.frozen > 1) println("Warning: $who is already busy.")
    }

    override fun execute()
    {
        if (parent.characters[tgtCharacter]!!.currentMeeting == null)
        {
            parent.ongoingMeetings["meeting-$tgtPlace-$tgtCharacter-${parent.time}"] =
                Meeting(parent.time, tgtPlace, scheduledCharacters = hashSetOf(who, tgtCharacter), tgtPlace)
            parent.ongoingMeetings["meeting-$tgtPlace-$tgtCharacter-${parent.time}"]!!.currentCharacters.add(
                tgtCharacter
            )
            super.execute()
        } else
        {
            parent.characters[tgtCharacter]!!.currentMeeting!!.currentCharacters.add(who)
            super.execute()
        }
    }

    override fun isValid(): Boolean
    {
        if (parent.characters[tgtCharacter]!!.currentMeeting == null)
            return parent.places[tgtPlace]!!.characters.filter { it != tgtCharacter }.toList()
                .isNotEmpty()
        else
        {
            //If the target character is already in a meeting, join the meeting if possible. Otherwise, this action is invalid.
            return parent.characters[tgtCharacter]!!.currentMeeting!!.scheduledCharacters.contains(who) &&
                    !parent.characters[tgtCharacter]!!.currentMeeting!!.currentCharacters.contains(who)
        }
    }

}