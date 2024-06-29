package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.Information
import kotlinx.serialization.Serializable
import kotlin.math.max

@Serializable
//This class is used to end a speech and nominate a new speaker. This action is used by the current speaker.
class EndSpeech(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{
    lateinit var nextSpeaker: String

    override fun execute()
    {
        val meeting = parent.characters[tgtCharacter]!!.currentMeeting!!

        //The amount of attention gained can be modified here.
        meeting.currentAttention += 10
        meeting.currentSpeaker = nextSpeaker
        super.execute()
    }

    override fun isValid(): Boolean
    {
        val meeting = parent.characters[tgtCharacter]!!.currentMeeting!!
        return meeting.currentSpeaker == tgtCharacter
    }

    override fun deltaWill(): Double
    {
        return parent.getMutuality(tgtCharacter, nextSpeaker) * 0.1
    }

}