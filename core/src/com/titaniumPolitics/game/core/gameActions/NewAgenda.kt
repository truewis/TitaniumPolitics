package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.MeetingAgenda
import kotlinx.serialization.Serializable
import kotlin.math.max

@Serializable
class NewAgenda(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{
    lateinit var agenda: MeetingAgenda

    override fun execute()
    {
        val meeting = parent.characters[tgtCharacter]!!.currentMeeting!!
        meeting.agendas.add(agenda)
        meeting.currentAttention = max(meeting.currentAttention - 10, 0)
        super.execute()
        //affect mutuality based on the agenda.
        parent.setMutuality(tgtCharacter, tgtCharacter, deltaWill())
        when (agenda.subjectType)
        {
            "proofOfWork" ->
            {
            }

            "budgetProposal" ->
            {
            }

            "budgetResolution" ->
            {
            }

            "praise" ->
            {
                parent.setMutuality(agenda.subjectParams["character"]!!, tgtCharacter, 3.0)
            }

            "denounce" ->
            {
                parent.setMutuality(agenda.subjectParams["character"]!!, tgtCharacter, -10.0)
            }

            "praiseParty" ->
            {
                parent.setPartyMutuality(meeting.involvedParty, agenda.subjectParams["party"]!!, 3.0)
            }

            "denounceParty" ->
            {
                parent.setPartyMutuality(meeting.involvedParty, agenda.subjectParams["party"]!!, -10.0)
                //Increase party integrity
                parent.setPartyMutuality(meeting.involvedParty, meeting.involvedParty, 5.0)
            }

            "nomination" ->
            {

            }
        }
    }

    override fun isValid(): Boolean
    {
        //People will be more interested in agendas related to their interest. However, this is handled in NPC class.
        val mt = parent.characters[tgtCharacter]!!.currentMeeting!!
        when (agenda.subjectType)
        {
            "proofOfWork" -> return mt.involvedParty != "" && mt.type == "divisionDailyConference" //TODO: how do we handle command issued?
            //You have to choose which command you are responding to. The character who issued the command must be present in the meeting.
            //Other people may add supporting or disapproving information.
            "budgetProposal" -> return mt.involvedParty == "cabinet" && !parent.isBudgetProposed
            //TODO: this is done by the mandatory cabinet election, which is separate from meetings.
            "budgetResolution" -> return mt.involvedParty == "triumvirate" && !parent.isBudgetResolved
            //TODO: this is done by the mandatory triumvirate election, which is separate from meetings.
            "praise" -> return true

            "denounce" -> return true
            "praiseParty" -> return true

            "denounceParty" -> return true
            "request" -> return true
            "nomination" -> return mt.type == "divisionLeaderElection" && agenda.subjectParams["character"]!! in parent.parties[mt.involvedParty]!!.members
            //You can choose the person to request, and one of the actions that the person can do. The command is issued immediately, and other people can opt in.
            //The below actions are executed by the leader. Party members can request the leader to do these actions.
            //"workingHoursChange" -> return mt.involvedParty != "" && mt.type == "divisionDailyConference" && parent.places[agenda.subjectParams["where"]]!!.responsibleParty == mt.involvedParty
            //"reassignWorkersToApparatus" -> return mt.involvedParty != "" && mt.type == "divisionDailyConference" && parent.places[agenda.subjectParams["where"]]!!.responsibleParty == mt.involvedParty //TODO: check apparatus key.
            //"salary" -> return mt.involvedParty != "" && mt.type == "divisionDailyConference" && !parent.parties[mt.involvedParty]!!.isSalaryPaid
            "appointMeeting" -> return true
            //TODO: impeach, fire
            //TODO: Also update NewAgendaUI.kt


        }
        return false
    }

    override fun deltaWill(): Double
    {
        val mt = parent.characters[tgtCharacter]!!.currentMeeting!!
        when (agenda.subjectType)
        {
            "praise" -> return parent.getMutuality(tgtCharacter, agenda.subjectParams["character"]!!) * 0.1
            "denounce" -> return parent.getMutuality(tgtCharacter, agenda.subjectParams["character"]!!) * -0.1
            "nomination" -> return parent.getMutuality(tgtCharacter, agenda.subjectParams["character"]!!) * 0.1
            "praiseParty" -> return parent.getPartyMutuality(mt.involvedParty, agenda.subjectParams["party"]!!) * 0.1
            "denounceParty" -> return parent.getPartyMutuality(mt.involvedParty, agenda.subjectParams["party"]!!) * -0.1
        }
        return .0
    }

}