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
        //TODO: each prepared information can only be presented once in a meeting.
        meeting.currentAttention =
            max(
                meeting.currentAttention - 10,
                0
            ) //TODO: gain more attention when presenting an information that is not known to the characters in the meeting.
        //The information is known to the characters in the meeting.
        parent.informations[infoKey]!!.knownTo.addAll(meeting.currentCharacters)
        //TODO: affect mutuality based on the information.
        super.execute()
    }

    override fun isValid(): Boolean
    {
        val meeting = parent.characters[tgtCharacter]!!.currentMeeting!!
        if (meeting.agendas.size <= agendaIndex)
            return false
        return true //We are assuming that the information is always valid. Whether the information is effective or not is a different matter.
    }

}