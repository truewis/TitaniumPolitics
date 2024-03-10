package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.Information
import kotlin.math.max

//This class is used to end a speech and nominate a new speaker. This action is used by the current speaker.
class Intercept(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{

    override fun execute()
    {
        val meeting = parent.characters[tgtCharacter]!!.currentMeeting!!

        //The amount of attention gained can be modified here.
        meeting.currentAttention += 20
        meeting.currentSpeaker = tgtCharacter
        parent.characters[tgtCharacter]!!.frozen++
    }

    override fun isValid(): Boolean
    {
        val meeting = parent.characters[tgtCharacter]!!.currentMeeting!!
        return meeting.currentSpeaker != tgtCharacter && meeting.currentAttention <= 30
    }

}