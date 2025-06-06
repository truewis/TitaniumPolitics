package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import kotlinx.serialization.Serializable

@Serializable
class StartMeeting(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    var meetingName = ""
    override fun chooseParams()
    {
        meetingName =
            GameEngine.acquire(parent.scheduledMeetings.filter { it.value.time + 2 >= parent.time && parent.time + 2 >= it.value.time && it.value.place == tgtPlace }
                .filter { !parent.ongoingMeetings.containsKey(it.key) }
                .filter { it.value.scheduledCharacters.contains(sbjCharacter) }.keys.toList())
    }

    //Also refer to Talk.execute()
    override fun execute()
    {
        parent.ongoingMeetings[meetingName] = parent.scheduledMeetings[meetingName]!!
        parent.scheduledMeetings.remove(meetingName)
        parent.ongoingMeetings[meetingName]!!.currentCharacters.add(sbjCharacter)
        // Interrupt other required characters and add them to the meeting.
        val meeting = parent.ongoingMeetings[meetingName]!!
        meeting.currentSpeaker = sbjCharacter
        meeting.currentAttention = 100
        val requiredCharacters = meeting.scheduledCharacters.intersect(tgtPlaceObj.characters)
        requiredCharacters.forEach {
            parent.characters[it]!!.frozen = 1 //Force them to join the meeting.
            parent.ongoingMeetings[meetingName]!!.currentCharacters.add(it)
            println("Interrupt: $it is forced to join the meeting.")
        }
        super.execute()

    }

    override fun isValid(): Boolean
    {
        val targetMeeting =
            parent.scheduledMeetings.filter { it.value.time + 2 >= parent.time && parent.time + 2 >= it.value.time && it.value.place == tgtPlace }
                .filter { !parent.ongoingMeetings.containsKey(it.key) }
                .filter { it.value.scheduledCharacters.contains(sbjCharacter) }.keys.firstOrNull()
        return if (targetMeeting == null) false else
        //Check if there are at least 2 characters to join.
            parent.scheduledMeetings[targetMeeting]!!.scheduledCharacters.intersect(parent.places[tgtPlace]!!.characters).size >= 2
    }

}