package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
class FinishNomination(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    val meeting get() = parent.characters[sbjCharacter]!!.currentMeeting
    override fun execute() {


        meeting!!.finishNomination()
        super.execute()

    }

    override fun isValid(): Boolean {
        return meeting != null &&
                meeting!!.currentCharacters.contains(sbjCharacter) &&
                meeting!!.currentSpeaker == sbjCharacter &&
                reason(meeting!!.type == Meeting.MeetingType.DIVISION_LEADER_ELECTION, "finishNomination-election") &&
                sbjCharacter == "ctrler" && //Only the controller can finish the nomination in a division leader election meeting.
                meeting!!.nominationFinishedTime == null //Nomination can only be finished if it hasn't been finished yet.
    }

}