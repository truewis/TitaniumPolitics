package com.titaniumPolitics.game.core.gameActions

import kotlinx.serialization.Serializable

@Serializable
//Called when a character resigns from a party, in a daily party meeting
class Resign(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    override fun chooseParams()
    {
    }

    override fun execute()
    {
        val party =
            parent.ongoingMeetings.filter { it.value.currentCharacters.contains(sbjCharacter) }.values.first().involvedParty
        if (parent.parties[party]!!.leader != sbjCharacter)
        {
            println("Warning: $sbjCharacter is not the leader of $party.")
            return
        }
        parent.parties[party]!!.members.remove(sbjCharacter)
        parent.parties[party]!!.leader = ""
        println("$sbjCharacter resigns from $party.")
        //If member of cabinet, also leave the cabinet
        if (parent.parties["cabinet"]!!.members.contains(sbjCharacter))
        {
            parent.parties["cabinet"]!!.members.remove(sbjCharacter)
            println("$sbjCharacter resigns from cabinet.")
        }
        //Should immediately leave the party meeting if it is ongoing
        if (parent.ongoingMeetings.any { it.value.currentCharacters.contains(sbjCharacter) && it.value.involvedParty == party })
        {
            LeaveMeeting(sbjCharacter, tgtPlace).also {
                it.injectParent(parent)
                it.execute()
            }
        }
        super.execute()

    }

    override fun isValid(): Boolean
    {
        try
        {
            val party =
                parent.ongoingMeetings.filter { it.value.currentCharacters.contains(sbjCharacter) }.values.first().involvedParty
            return parent.parties[party]!!.leader == sbjCharacter
        } catch (e: Exception)
        {
            return false
        }
    }


}