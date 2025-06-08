package com.titaniumPolitics.game.core.gameActions

import kotlinx.serialization.Serializable

@Serializable
//This class is used to end a speech and nominate a new speaker. This action is used by the current speaker.
class EndSpeech(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    lateinit var nextSpeaker: String

    override fun execute() {
        val meeting = parent.characters[sbjCharacter]!!.currentMeeting!!

        //The amount of attention gained can be modified here.
        meeting.currentAttention += 10
        meeting.currentSpeaker = nextSpeaker
        println("$sbjCharacter ended their speech and nominated $nextSpeaker as the next speaker.")
        super.execute()
    }

    override fun isValid(): Boolean {
        val meeting = parent.characters[sbjCharacter]!!.currentMeeting!!
        return meeting.currentSpeaker == sbjCharacter
    }

    override fun deltaWill(): Double {
        return parent.getMutuality(sbjCharacter, nextSpeaker) * 0.1
    }


}