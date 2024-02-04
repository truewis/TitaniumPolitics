package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine

class StartConference(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{
    var meetingName = ""
    override fun chooseParams()
    {
        meetingName =
            GameEngine.acquire(parent.scheduledConferences.filter { it.value.time + 2 > parent.time && parent.time + 2 > it.value.time }
                .filter { !parent.ongoingMeetings.containsKey(it.key) }
                .filter { it.value.scheduledCharacters.contains(tgtCharacter) }.keys.toList())
        // Interrupt other required characters and add them to the meeting.
        val meeting = parent.ongoingMeetings[meetingName]!!
        val requiredCharacters = meeting.scheduledCharacters.intersect(parent.places[tgtPlace]!!.characters)
        requiredCharacters.forEach {
            parent.characters[it]!!.frozen = 1 //Force them to join the meeting.
            parent.ongoingMeetings[meetingName]!!.currentCharacters.add(it)
            println("Interrupt: $it is forced to join the meeting.")
        }
    }

    override fun execute()
    {
        parent.ongoingConferences[meetingName] = parent.scheduledConferences[meetingName]!!
        parent.scheduledConferences.remove(meetingName)
        parent.ongoingConferences[meetingName]!!.currentCharacters.add(tgtCharacter)
        parent.characters[tgtCharacter]!!.frozen++
    }

    override fun isValid(): Boolean
    {
        val targetMeeting =
            parent.scheduledConferences.filter { it.value.time + 2 > parent.time && parent.time + 2 > it.value.time }
                .filter { !parent.ongoingMeetings.containsKey(it.key) }
                .filter { it.value.scheduledCharacters.contains(tgtCharacter) }.keys.firstOrNull()
        return if (targetMeeting == null) false else
        //Check if there are at least 2 characters to join.
            parent.scheduledConferences[targetMeeting]!!.scheduledCharacters.intersect(parent.places[tgtPlace]!!.characters).size >= 2
    }

}