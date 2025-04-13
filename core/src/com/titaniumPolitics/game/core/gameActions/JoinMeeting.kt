package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import kotlinx.serialization.Serializable

@Serializable
class JoinMeeting(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    var meetingName = ""
    override fun chooseParams()
    {
        meetingName =
            GameEngine.acquire(parent.ongoingMeetings.filter { it.value.scheduledCharacters.contains(sbjCharacter) && it.value.place == tgtPlace }.keys.toList())
    }

    override fun execute()
    {
        parent.ongoingMeetings[meetingName]!!.currentCharacters.add(sbjCharacter)
        println("$sbjCharacter joined the meeting $meetingName")
        super.execute()
    }

    override fun isValid(): Boolean
    {
        return parent.ongoingMeetings.any {
            it.value.scheduledCharacters.contains(sbjCharacter) && !it.value.currentCharacters.contains(
                sbjCharacter
            ) && it.value.place == tgtPlace
        }
    }

}