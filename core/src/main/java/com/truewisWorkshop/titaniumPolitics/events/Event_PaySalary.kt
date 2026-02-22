package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_PaySalary(val partyKey: String) : EventObject(ReadOnly.questProp("PaySalary-name").format(partyKey), true), IQuestEventObject {
    val party
        get() = parent.parties[partyKey]!!
    val partyName
        get() =
            if (party.type == Party.Type.DIVISION) ReadOnly.prop(party.name) else party.workplace.name + " Workplace"

    override val quest
            by lazy {
                Quest(
                    ReadOnly.questProp("PaySalary-title").format(partyName),
                    description = ReadOnly.questProp("PaySalary-desc"),
                    tgtPlace = party.home
                )
            }

    override fun exec(a: Int, b: Int) {
        if (party.isSalaryPaid)
            deactivate()
    }


}