package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine

class JoinMeeting(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{
    var meetingName = ""
    override fun chooseParams()
    {
        meetingName =
            GameEngine.acquire(parent.ongoingMeetings.filter { it.value.scheduledCharacters.contains(tgtCharacter) && it.value.place == tgtPlace }.keys.toList())
    }

    override fun execute()
    {
        parent.ongoingMeetings[meetingName]!!.currentCharacters.add(tgtCharacter)
        parent.characters[tgtCharacter]!!.frozen++
    }

    override fun isValid(): Boolean
    {
        return parent.ongoingMeetings.any { it.value.scheduledCharacters.contains(tgtCharacter) && it.value.place == tgtPlace }
    }

}