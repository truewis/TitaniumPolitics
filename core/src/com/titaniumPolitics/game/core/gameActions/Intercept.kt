package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.MutualityMatrix
import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
//This class is used to end a speech and nominate a new speaker. This action is used by the current speaker.
data class Intercept(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    override fun execute() {
        val meeting = parent.characters[sbjCharacter]!!.currentMeeting!!

        //The amount of attention gained can be modified here.
        meeting.currentAttention += 20
        meeting.currentSpeaker = sbjCharacter



        super.execute()
    }

    override fun isValid(): Boolean {
        val meeting = parent.characters[sbjCharacter]!!.currentMeeting!!
        return meeting.currentSpeaker != sbjCharacter && reason(
            meeting.currentAttention <= ReadOnly.const("maxAttentionIntercept"),
            "intercept-attention"
        )
    }

    override fun deltaWill(): MutualityMatrix {
        val meeting = parent.characters[sbjCharacter]!!.currentMeeting!!
        val factor = if (parent.characters[sbjCharacter]!!.trait.contains("provoker")) -15 else -10
        return MutualityMatrix().apply {
            this.addWill(
                sbjCharacter,
                parent.getMutuality(
                    sbjCharacter,
                    meeting.currentSpeaker!!
                ) * factor * sbjCharObj.stats.pScale, "InterceptWill"
            )

            //Mutuality decreases before changing speaker.
            this.addMutuality(
                meeting.currentSpeaker!!,
                sbjCharacter,
                parent.getMutuality(
                    meeting.currentSpeaker!!,
                    sbjCharacter
                ) * factor * parent.characters[meeting.currentSpeaker!!]!!.stats.pScale, "Intercept"
            )

        }

    }


}