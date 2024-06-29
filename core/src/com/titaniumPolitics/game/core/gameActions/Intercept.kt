package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable
import kotlin.math.max

@Serializable
//This class is used to end a speech and nominate a new speaker. This action is used by the current speaker.
class Intercept(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{

    override fun execute()
    {
        val meeting = parent.characters[tgtCharacter]!!.currentMeeting!!

        //The amount of attention gained can be modified here.
        meeting.currentAttention += 20
        meeting.currentSpeaker = tgtCharacter
        super.execute()
    }

    override fun isValid(): Boolean
    {
        val meeting = parent.characters[tgtCharacter]!!.currentMeeting!!
        return meeting.currentSpeaker != tgtCharacter && meeting.currentAttention <= ReadOnly.const("maxAttentionIntercept")
    }

    override fun deltaWill(): Double
    {
        val meeting = parent.characters[tgtCharacter]!!.currentMeeting!!
        val factor = if (parent.characters[tgtCharacter]!!.trait.contains("provoker")) -0.05 else -0.1
        return super.deltaWill() + parent.getMutuality(tgtCharacter, meeting.currentSpeaker) * factor
    }


}