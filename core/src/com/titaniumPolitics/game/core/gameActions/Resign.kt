package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

@Serializable
//Called when a character resigns from a party, in a daily party meeting
class Resign(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    override fun chooseParams() {
    }

    override fun execute() {
        val party =
            parent.ongoingMeetings.filter { it.value.currentCharacters.contains(sbjCharacter) }.values.first().involvedParty
        if (parent.parties[party]!!.leader != sbjCharacter) {
            Logger.write("Warning: $sbjCharacter is not the leader of $party.", Logger.LogLevel.INFO)
            return
        }
        parent.parties[party]!!.members.remove(sbjCharacter)
        parent.parties[party]!!.leader = null
        Logger.write("$sbjCharacter resigns from $party.", Logger.LogLevel.INFO)
        //If member of cabinet, also leave the cabinet
        if (parent.parties["cabinet"]!!.members.contains(sbjCharacter)) {
            parent.parties["cabinet"]!!.members.remove(sbjCharacter)
            Logger.write("$sbjCharacter resigns from cabinet.", Logger.LogLevel.INFO)
        }
        //Should immediately leave the party meeting if it is ongoing
        if (parent.ongoingMeetings.any { it.value.currentCharacters.contains(sbjCharacter) && it.value.involvedParty == party }) {
            LeaveMeeting(sbjCharacter, tgtPlace).also {
                it.injectParent(parent)
                it.execute()
            }
        }
        super.execute()

    }

    override fun isValid(): Boolean {
        try {
            val party =
                parent.ongoingMeetings.filter { it.value.currentCharacters.contains(sbjCharacter) }.values.first().involvedParty
            return parent.parties[party]!!.leader == sbjCharacter
        } catch (e: Exception) {
            return false
        }
    }


}