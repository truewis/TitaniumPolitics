package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
//This class is used to end a speech and nominate a new speaker. This action is used by the current speaker.
class Intercept(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{

    override fun execute()
    {
        val meeting = parent.characters[sbjCharacter]!!.currentMeeting!!

        //The amount of attention gained can be modified here.
        meeting.currentAttention += 20
        meeting.currentSpeaker = sbjCharacter
        super.execute()
    }

    override fun isValid(): Boolean
    {
        val meeting = parent.characters[sbjCharacter]!!.currentMeeting!!
        return meeting.currentSpeaker != sbjCharacter && meeting.currentAttention <= ReadOnly.const("maxAttentionIntercept")
    }

    override fun deltaWill(): Double
    {
        val meeting = parent.characters[sbjCharacter]!!.currentMeeting!!
        val factor = if (parent.characters[sbjCharacter]!!.trait.contains("provoker")) -0.05 else -0.1
        return super.deltaWill() + parent.getMutuality(sbjCharacter, meeting.currentSpeaker) * factor
    }


}