package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_ProposeBudget(val budgetMeeting: String) : EventObject(ReadOnly.questProp("ProposeBudget-name").format(budgetMeeting), true),
    IQuestEventObject {
    val party
        get() = parent.parties[parent.scheduledMeetings[budgetMeeting]!!
            .involvedParty]!!
    val partyName
        get() =
            if (party.type == Party.Type.DIVISION) ReadOnly.prop(party.name) else party.workplace.name + " Workplace"

    override val quest by lazy {
        Quest(
            ReadOnly.questProp("ProposeBudget-title").format(partyName),
            description = ReadOnly.questProp("ProposeBudget-desc"),
            tgtMeeting = budgetMeeting
        )
    }

    override fun exec(a: Int, b: Int) {
        if (parent.scheduledMeetings[budgetMeeting] == null || party.isBudgetProposed)
            deactivate()
    }


}