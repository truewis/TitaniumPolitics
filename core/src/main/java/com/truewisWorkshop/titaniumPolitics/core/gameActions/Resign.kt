package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.Character
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
//Called when a character resigns from a party, in a daily party meeting
data class Resign(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    override fun chooseParams() {
    }

    override fun execute() {
        val partyKey =
            parent.ongoingMeetings.values.find { it.currentCharacters.contains(sbjCharacter) }?.involvedParty
                ?: return
        val party = parent.parties[partyKey]!!
        val isLeader = party.leader == sbjCharacter
        if (!isLeader && parent.characters[sbjCharacter]!!.type != Character.Type.EMPLOYEE) {
            Logger.write("Warning: $sbjCharacter cannot resign from $partyKey.", Logger.LogLevel.INFO)
            return
        }
        party.removeMember(sbjCharacter)
        Logger.write("$sbjCharacter resigns from $partyKey.", Logger.LogLevel.INFO)
        //If member of cabinet, also leave the cabinet (leaders only)
        if (isLeader && parent.parties["cabinet"]!!.members.contains(sbjCharacter)) {
            parent.parties["cabinet"]!!.removeMember(sbjCharacter)
            Logger.write("$sbjCharacter resigns from cabinet.", Logger.LogLevel.INFO)
        }
        super.execute()

    }

    override fun isValid(): Boolean {
        try {
            val partyKey =
                parent.ongoingMeetings.values.find { it.currentCharacters.contains(sbjCharacter) }?.involvedParty
                    ?: return false
            val party = parent.parties[partyKey]!!
            return party.leader == sbjCharacter ||
                (parent.characters[sbjCharacter]!!.type == Character.Type.EMPLOYEE &&
                    party.members.contains(sbjCharacter) &&
                    party.home == tgtPlace)
        } catch (e: Exception) {
            return false
        }
    }


}