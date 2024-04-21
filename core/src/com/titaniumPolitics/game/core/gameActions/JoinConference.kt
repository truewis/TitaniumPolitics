package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import kotlinx.serialization.Serializable

@Serializable
class JoinConference(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{
    var meetingName = ""
    override fun chooseParams()
    {
        meetingName =
            GameEngine.acquire(parent.ongoingConferences.filter { it.value.scheduledCharacters.contains(tgtCharacter) }.keys.toList())
    }

    override fun execute()
    {
        parent.ongoingConferences[meetingName]!!.currentCharacters.add(tgtCharacter)
        super.execute()
    }

    override fun isValid(): Boolean
    {
        return parent.ongoingConferences.any {
            it.value.scheduledCharacters.contains(tgtCharacter) && !it.value.currentCharacters.contains(
                tgtCharacter
            ) && it.value.place == tgtPlace
        }
    }

}