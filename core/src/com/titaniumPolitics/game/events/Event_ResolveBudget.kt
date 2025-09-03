package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_ResolveBudget(val budgetMeeting: String) : EventObject("Resolve Budget: $budgetMeeting", true),
    IQuestEventObject {
    val party
        get() = parent.parties[parent.scheduledMeetings[budgetMeeting]!!
            .involvedParty]!!
    val partyName
        get() =
            if (party.type == Party.Type.DIVISION) ReadOnly.prop(party.name) else party.workplace.name + " Workplace"

    override val quest
            by lazy {
                Quest(
                    "Quarterly budget of %s is not resolved yet.".format(partyName),
                    description = "Attend the budget resolution meeting.",
                    tgtMeeting = budgetMeeting
                )
            }

    override fun exec(a: Int, b: Int) {
        if (parent.scheduledMeetings[budgetMeeting] == null)
            deactivate()
    }


}