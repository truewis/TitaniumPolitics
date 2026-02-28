package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.Character
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.MutualityMatrix
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
data class Feast(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    val meeting get() = parent.characters[sbjCharacter]!!.currentMeeting!!
    val foodAmount get() = meeting.currentCharacters.size * 1.0 // 1 kg of fineFood per attendee

    override fun execute() {
        sbjCharObj.resources["fineFood"] -= foodAmount
        // Good food boosts attention and prevents higher-ups from losing interest
        meeting.currentAttention += meeting.currentCharacters.size * 5
        Logger.write("$sbjCharacter held a feast for the meeting attendees.", Logger.LogLevel.ACTION_VERBOSE)
        super.execute()
    }

    override fun isValid(): Boolean {
        val mt = parent.characters[sbjCharacter]!!.currentMeeting ?: return false
        return mt.currentSpeaker == sbjCharacter && reason(
            sbjCharObj.resources["fineFood"] >= foodAmount,
            "feast-resources"
        )
    }

    override fun deltaWill(): MutualityMatrix {
        val w = MutualityMatrix()
        meeting.currentCharacters.forEach { attendee ->
            val attendeeObj = parent.characters[attendee]!!
            // Workers benefit more from a good meal
            val willBoost = when {
                attendeeObj.trait.contains("gourmand") -> 15.0
                attendeeObj.type == Character.Type.EMPLOYEE -> 12.0
                else -> 10.0
            }
            w.addWill(attendee, willBoost, "Feast")
            // Attendees appreciate the host's generosity
            if (attendee != sbjCharacter) {
                w.addMutuality(attendee, sbjCharacter, 5.0, "Feast")
            }
        }
        return w
    }
}
