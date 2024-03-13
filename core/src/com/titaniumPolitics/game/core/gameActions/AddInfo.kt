package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.Information
import kotlin.math.max

class AddInfo(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{
    lateinit var infoKey: String
    var agendaIndex = 0

    override fun execute()
    {
        val meeting = parent.characters[tgtCharacter]!!.currentMeeting!!
        meeting.agendas[agendaIndex].informationKeys.add(infoKey)

        //The amount of attention spent can be modified here.
        meeting.currentAttention =
            max(
                meeting.currentAttention - 10,
                0
            ) //TODO: gain more attention when presenting an information that is not known to the characters in the meeting.
        //The information is known to the characters in the meeting.
        parent.informations[infoKey]!!.knownTo.addAll(meeting.currentCharacters)
        super.execute()
    }

    override fun isValid(): Boolean
    {
        val meeting = parent.characters[tgtCharacter]!!.currentMeeting!!
        return meeting.agendas.size > agendaIndex
    }

}