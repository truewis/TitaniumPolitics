package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine

class StartMeeting(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{
    var meetingName = ""
    override fun chooseParams()
    {
        meetingName =
            GameEngine.acquire(parent.scheduledMeetings.filter { it.value.time + 2 > parent.time && parent.time + 2 > it.value.time && it.value.place == tgtPlace }
                .filter { !parent.ongoingMeetings.containsKey(it.key) }
                .filter { it.value.scheduledCharacters.contains(tgtCharacter) }.keys.toList())
    }

    override fun execute()
    {
        parent.ongoingMeetings[meetingName] = parent.scheduledMeetings[meetingName]!!
        parent.scheduledMeetings.remove(meetingName)
        parent.ongoingMeetings[meetingName]!!.currentCharacters.add(tgtCharacter)
        parent.characters[tgtCharacter]!!.frozen++
        //TODO: Interrupt other required characters and add them to the meeting.
    }

    override fun isValid(): Boolean
    {
        val targetMeeting =
            parent.scheduledMeetings.filter { it.value.time + 2 > parent.time && parent.time + 2 > it.value.time && it.value.place == tgtPlace }
                .filter { !parent.ongoingMeetings.containsKey(it.key) }
                .filter { it.value.scheduledCharacters.contains(tgtCharacter) }.keys.firstOrNull()
        return if (targetMeeting == null) false else
        //Check if there are at least 2 characters to join.
            parent.scheduledMeetings[targetMeeting]!!.scheduledCharacters.intersect(parent.places[tgtPlace]!!.characters).size >= 2
    }

}