package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.MeetingAgenda
import kotlin.math.max

class NewAgenda(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{
    lateinit var agenda: MeetingAgenda

    override fun execute()
    {
        val meeting = parent.characters[tgtCharacter]!!.currentMeeting!!
        meeting.agendas.add(agenda)
        meeting.currentAttention = max(meeting.currentAttention - 10, 0)
        parent.characters[tgtCharacter]!!.frozen++
    }

    override fun isValid(): Boolean
    {
        //People will be more interested in agendas related to their interest. However, this is handled in NPC class.
        val mt = parent.characters[tgtCharacter]!!.currentMeeting!!
        when (agenda.subjectType)
        {
            "proofOfWork" -> return mt.involvedParty != "" && mt.subject == "divisionDailyConference"
            "budgetProposal" -> return mt.involvedParty == "cabinet" && !parent.isBudgetProposed
            "budgetResolution" -> return mt.involvedParty == "triumvirate" && !parent.isBudgetResolved
            "praise" -> return true
            "denounce" -> return true
            "workingHoursChange" -> return mt.involvedParty != "" && mt.subject == "divisionDailyConference"
            "reassignWorkersToApparatus" -> return mt.involvedParty != "" && mt.subject == "divisionDailyConference"
            "salary" -> return mt.involvedParty != "" && mt.subject == "divisionDailyConference" && !parent.parties[mt.involvedParty]!!.isSalaryPaid
            "appointMeeting" -> return mt.involvedParty != "" && mt.subject == "divisionDailyConference"


        }
        return false
    }

}