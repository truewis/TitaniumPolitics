package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
data class JoinMeeting(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    var meetingName = ""
    override fun chooseParams() {
        meetingName =
            GameEngine.acquire(parent.ongoingMeetings.filter { it.value.scheduledCharacters.contains(sbjCharacter) && it.value.place == tgtPlace }.keys.toList())
    }

    override fun execute() {
        parent.ongoingMeetings[meetingName]!!.currentCharacters.add(sbjCharacter)
        Logger.write("$sbjCharacter joined the meeting $meetingName", Logger.LogLevel.INFO)
        super.execute()
    }

    override fun isValid(): Boolean {
        if (sbjCharObj.currentMeeting != null) {
            Logger.write(
                "Cannot join a meeting $meetingName while already in one: ${sbjCharObj.currentMeeting}",
                Logger.LogLevel.ERROR
            )
            return false
        }

        return parent.ongoingMeetings.any {
            it.value.scheduledCharacters.contains(sbjCharacter) && !it.value.currentCharacters.contains(
                sbjCharacter
            ) && it.value.place == tgtPlace
        }
    }

}