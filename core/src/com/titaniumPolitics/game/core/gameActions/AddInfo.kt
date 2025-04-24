package com.titaniumPolitics.game.core.gameActions

import com.badlogic.gdx.math.MathUtils.clamp
import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class AddInfo(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    lateinit var infoKey: String
    var agendaIndex = 0
    val agenda
        get() = sbjCharObj.currentMeeting!!.agendas[agendaIndex]
    val info
        get() = parent.informations[infoKey]!!
    val meeting
        get() = sbjCharObj.currentMeeting!!


    //Unit: Mutuality
    fun effectivity(): Double
    {
        val newsPeople = meeting.currentCharacters.intersect(info.knownTo)
        when (agenda.type)
        {
            AgendaType.PROOF_OF_WORK ->
            {
                //if there is any supporting information, add it.

                if (info.type == InformationType.ACTION
                    && sbjCharObj.finishedRequests.any {
                        parent.requests[it]!!.action == info.action &&
                                parent.requests[it]!!.issuedBy.any {
                                    meeting.currentCharacters.contains(
                                        it
                                    )
                                }
                    }
                ) return 5.0


                //If there are any interesting (to this character) news about the division, share it.

                if (info.tgtTime in parent.day * ReadOnly.constInt("lengthOfDay")..(parent.day * ReadOnly.constInt(
                        "lengthOfDay"
                    ) + ReadOnly.constInt("lengthOfDay") - 1)
                )
                    return newsPeople.sumOf { parent.characters[it]!!.infoPreference(info) } / newsPeople.size//Share the most interesting news.

            }

            AgendaType.NOMINATE, AgendaType.PRAISE ->
            {
                return parent.characters[agenda.subjectParams["character"]]!!.infoPreference(info)
            }

            AgendaType.REQUEST -> return meeting.currentCharacters.sumOf { parent.characters[it]!!.actionValue(agenda.attachedRequest!!.action) }
            AgendaType.DENOUNCE ->
            {
                return -parent.characters[agenda.subjectParams["character"]]!!.infoPreference(info)
            }

            AgendaType.PRAISE_PARTY ->
            {
                val pt = parent.parties[agenda.subjectParams["party"]]!!
                return pt.members.sumOf { parent.characters[it]!!.infoPreference(info) } / pt.members.size

            }

            AgendaType.DENOUNCE_PARTY ->
            {
                val pt = parent.parties[agenda.subjectParams["party"]]!!
                return pt.members.sumOf { -parent.characters[it]!!.infoPreference(info) } / pt.members.size
            }

            AgendaType.BUDGET_PROPOSAL -> TODO()
            AgendaType.BUDGET_RESOLUTION -> TODO()
            AgendaType.APPOINT_MEETING -> return 0.0
            else -> return 0.0
        }
        return 0.0
    }

    override fun execute()
    {

        agenda.informationKeys.add(infoKey)

        //The amount of attention spent can be modified here.
        //TODO: each prepared information can only be presented once in a meeting.
        //Attention is consumed.
        val newsDegree =
            1.0 - meeting.currentCharacters.intersect(info.knownTo).size / (.0 + meeting.currentCharacters.size)
        meeting.currentAttention = clamp(
            meeting.currentAttention + (10 * effectivity() * newsDegree * sbjCharObj.will / ReadOnly.const("mutualityMax")).toInt() - 20,
            0, 100
        )
        //The information is known to the characters in the meeting.
        parent.informations[infoKey]!!.knownTo.addAll(meeting.currentCharacters)
        //affect mutuality based on the information.
        NewAgenda.extracted(effectivity(), meeting, agenda, sbjCharacter, parent)
        super.execute()
    }

    override fun isValid(): Boolean
    {
        if (meeting.agendas.size <= agendaIndex)
            return false
        //If the information is already presented in the meeting, it cannot be presented again.
        if (meeting.agendas.any { it.informationKeys.contains(infoKey) })
            return false
        return true //We are assuming that the information is always valid. Whether the information is effective or not is a different matter.
    }

}