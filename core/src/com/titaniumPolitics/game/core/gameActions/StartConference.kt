package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine

class StartConference(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{
    var meetingName = ""
    override fun chooseParams()
    {
        meetingName =
            GameEngine.acquire(parent.scheduledConferences.filter { it.value.time + 2 >= parent.time && parent.time + 2 >= it.value.time }
                .filter { !parent.ongoingMeetings.containsKey(it.key) }
                .filter { it.value.scheduledCharacters.contains(tgtCharacter) }.keys.toList())
        // Interrupt other required characters and add them to the meeting.
        val meeting = parent.ongoingMeetings[meetingName]!!
//        val requiredCharacters = meeting.scheduledCharacters.intersect(parent.places[tgtPlace]!!.characters)
//        requiredCharacters.forEach {
//            parent.characters[it]!!.frozen = 1 //Force them to join the meeting.
//            parent.ongoingMeetings[meetingName]!!.currentCharacters.add(it)
//            println("Interrupt: $it is forced to join the meeting.")
//        }
    }

    override fun execute()
    {
        val meeting = parent.scheduledConferences[meetingName]!!
        parent.ongoingConferences[meetingName] = meeting
        parent.scheduledConferences.remove(meetingName)
        meeting.currentCharacters.add(tgtCharacter)
        meeting.currentSpeaker = tgtCharacter
        meeting.currentAttention = parent.getPartyMutuality(meeting.involvedParty, meeting.involvedParty).toInt()
        parent.characters[tgtCharacter]!!.frozen++
    }

    override fun isValid(): Boolean
    {
        val targetMeeting =
            parent.scheduledConferences.filter { it.value.time + 2 >= parent.time && parent.time + 2 >= it.value.time }
                .filter { !parent.ongoingConferences.containsKey(it.key) }
                .filter { it.value.scheduledCharacters.contains(tgtCharacter) }.keys.firstOrNull()
        return if (targetMeeting == null) false else
        //We don't check if there are at least 2 characters to join. If this is the condition, we have to force other characters to join, which results in interrupting them.
        //Also, in a hypothetical situation when there is only one character in the party, the meeting still has to start.
        //  parent.scheduledConferences[targetMeeting]!!.scheduledCharacters.intersect(parent.places[tgtPlace]!!.characters).size >= 2
            true
    }

}