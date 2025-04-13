package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.Meeting
import kotlinx.serialization.Serializable

@Serializable
//Talk is considered as an on-the-fly meeting.
//If the object (who) is already in a meeting, join the meeting if possible. Otherwise, create a new meeting with me(tgtCharacter) and the object (who).
//Note that if the me(tgtCharacter) is in the meeting, this action is invalid.
class Talk(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    var who = ""
    override fun chooseParams()
    {
        who =
            GameEngine.acquire(tgtPlaceObj.characters.filter { it != sbjCharacter }.toList())
        if (parent.characters[who]!!.frozen > 1) println("Warning: $who is already busy.")
    }

    //Also refer to StartMeeting.execute()
    override fun execute()
    {
        if (sbjCharObj.currentMeeting == null)
        {
            parent.ongoingMeetings["meeting-$tgtPlace-$sbjCharacter-${parent.time}"] =
                Meeting(parent.time, tgtPlace, scheduledCharacters = hashSetOf(who, sbjCharacter), tgtPlace)
            parent.ongoingMeetings["meeting-$tgtPlace-$sbjCharacter-${parent.time}"]!!.currentCharacters.add(
                sbjCharacter
            )

            super.execute()
        } else
        {
            sbjCharObj.currentMeeting!!.currentCharacters.add(who)
            super.execute()
        }
    }

    override fun isValid(): Boolean
    {
        //The subject character must not be in any meeting, otherwise they are too busy to talk.
        if (sbjCharObj.currentMeeting != null)
            return false

        if (parent.characters[who]!!.currentMeeting == null)
            return tgtPlaceObj.characters.contains(who)
        else
        {
            //If the object character is already in a meeting, join the meeting. Note that this bypasses the scheduledCharacters condition.
            return !parent.characters[who]!!.currentMeeting!!.currentCharacters.contains(sbjCharacter)
        }
    }

}