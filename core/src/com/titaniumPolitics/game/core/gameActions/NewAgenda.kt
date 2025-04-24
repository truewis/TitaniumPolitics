package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable
import kotlin.collections.set
import kotlin.math.max

@Serializable
class NewAgenda(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    lateinit var agenda: MeetingAgenda

    override fun execute()
    {
        val meeting = parent.characters[sbjCharacter]!!.currentMeeting!!
        meeting.agendas.add(agenda)
        val effectivity = meeting.currentAttention / 100.0 * sbjCharObj.will / ReadOnly.const("mutualityMax")

        //Attention is consumed.
        meeting.currentAttention = max(
            meeting.currentAttention + (10 * sbjCharObj.will / ReadOnly.const("mutualityMax")).toInt() - 20,
            0
        )
        super.execute()
        //affect mutuality based on the agenda.
        parent.setMutuality(sbjCharacter, sbjCharacter, deltaWill())
        extracted(effectivity, meeting, agenda, sbjCharacter, parent)
    }


    override fun isValid(): Boolean
    {
        //People will be more interested in agendas related to their interest. However, this is handled in NPC class.
        val mt = parent.characters[sbjCharacter]!!.currentMeeting!!
        when (agenda.type)
        {
            AgendaType.PROOF_OF_WORK -> return mt.involvedParty != "" && mt.type == "divisionDailyConference" //TODO: how do we handle command issued?
            //You have to choose which command you are responding to. The character who issued the command must be present in the meeting.
            //Other people may add supporting or disapproving information.
            AgendaType.BUDGET_PROPOSAL -> return mt.involvedParty == "cabinet" && !parent.isBudgetProposed
            //TODO: this is done by the mandatory cabinet election, which is separate from meetings.
            AgendaType.BUDGET_RESOLUTION -> return mt.involvedParty == "triumvirate" && !parent.isBudgetResolved
            //TODO: this is done by the mandatory triumvirate election, which is separate from meetings.
            AgendaType.PRAISE -> return true

            AgendaType.DENOUNCE -> return true
            AgendaType.PRAISE_PARTY -> return true

            AgendaType.DENOUNCE_PARTY -> return true
            AgendaType.REQUEST -> return agenda.attachedRequest != null
            AgendaType.NOMINATE -> return mt.type == "divisionLeaderElection" && agenda.subjectParams["character"]!! in parent.parties[mt.involvedParty]!!.members
            //You can choose the person to request, and one of the actions that the person can do. The command is issued immediately, and other people can opt in.
            //The below actions are executed by the leader. Party members can request the leader to do these actions.
            //"workingHoursChange" -> return mt.involvedParty != "" && mt.type == "divisionDailyConference" && parent.places[agenda.subjectParams["where"]]!!.responsibleParty == mt.involvedParty
            //"reassignWorkersToApparatus" -> return mt.involvedParty != "" && mt.type == "divisionDailyConference" && parent.places[agenda.subjectParams["where"]]!!.responsibleParty == mt.involvedParty //TODO: check apparatus key.
            //"salary" -> return mt.involvedParty != "" && mt.type == "divisionDailyConference" && !parent.parties[mt.involvedParty]!!.isSalaryPaid
            AgendaType.APPOINT_MEETING -> return true
            //TODO: impeach, fire
            //TODO: Also update NewAgendaUI.kt


        }
        return false
    }

    override fun deltaWill(): Double
    {
        val mt = parent.characters[sbjCharacter]!!.currentMeeting!!
        when (agenda.type)
        {
            AgendaType.PRAISE -> return parent.getMutuality(sbjCharacter, agenda.subjectParams["character"]!!) * 0.1
            AgendaType.DENOUNCE -> return parent.getMutuality(sbjCharacter, agenda.subjectParams["character"]!!) * -0.1
            AgendaType.NOMINATE -> return parent.getMutuality(sbjCharacter, agenda.subjectParams["character"]!!) * 0.5
            AgendaType.PRAISE_PARTY -> return parent.getPartyMutuality(
                mt.involvedParty,
                agenda.subjectParams["party"]!!
            ) * 0.1

            AgendaType.DENOUNCE_PARTY -> return parent.getPartyMutuality(
                mt.involvedParty,
                agenda.subjectParams["party"]!!
            ) * -0.1

            else -> return .0
        }
    }

    companion object
    {

        fun extracted(
            effectivity: Double,
            meeting: Meeting,
            agenda: MeetingAgenda,
            sbjCharacter: String,
            parent: GameState
        )
        {
            when (agenda.type)
            {
                AgendaType.PROOF_OF_WORK ->
                {
                }

                AgendaType.REQUEST ->
                {
                    agenda.attachedRequest!!.also { parent.requests[it.generateName()] = it }
                }

                AgendaType.BUDGET_PROPOSAL ->
                {
                }

                AgendaType.BUDGET_RESOLUTION ->
                {
                }

                AgendaType.PRAISE ->
                {
                    parent.setMutuality(agenda.subjectParams["character"]!!, sbjCharacter, 3.0 * effectivity)
                }

                AgendaType.DENOUNCE ->
                {
                    parent.setMutuality(agenda.subjectParams["character"]!!, sbjCharacter, -10.0)
                }

                AgendaType.PRAISE_PARTY ->
                {
                    parent.setPartyMutuality(meeting.involvedParty, agenda.subjectParams["party"]!!, 3.0 * effectivity)
                }

                AgendaType.DENOUNCE_PARTY ->
                {
                    parent.setPartyMutuality(meeting.involvedParty, agenda.subjectParams["party"]!!, -10.0)
                    //Increase party integrity
                    parent.setPartyMutuality(meeting.involvedParty, meeting.involvedParty, 5.0 * effectivity)
                }

                AgendaType.NOMINATE ->
                {
                    parent.setMutuality(agenda.subjectParams["character"]!!, sbjCharacter, 10.0 * effectivity)
                }
                //request is not executed until the end of the meeting. Check Meeting.kt
                else ->
                {
                }
            }
        }
    }

}