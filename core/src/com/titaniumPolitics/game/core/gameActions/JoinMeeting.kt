package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.Character
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
data class JoinMeeting(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    val targetMeeting
        get() =
            parent.ongoingMeetings
                .filter {
                    parent.ongoingMeetings.containsKey(it.key) &&
                            sbjCharacter in it.value.scheduledCharacters &&
                            sbjCharacter !in it.value.currentCharacters &&
                            it.value.place == tgtPlace
                }.keys.firstOrNull()

    override fun execute() {
        parent.ongoingMeetings[targetMeeting]!!.currentCharacters.add(sbjCharacter)
        Logger.write("$sbjCharacter joined the meeting $targetMeeting", Logger.LogLevel.INFO)
        super.execute()
    }

    override fun isValid(): Boolean {
        if (sbjCharObj.type == Character.Type.ANON)
            return false
        if (sbjCharObj.currentMeeting != null) {
            Logger.write(
                "Cannot join a meeting $targetMeeting while already in one: ${sbjCharObj.currentMeeting}",
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