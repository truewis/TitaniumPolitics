package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_HatefulDirectReport(val charNames: List<String>, val partyKey: String) :
    EventObject("A direct report hates you: $partyKey", true),
    IQuestEventObject {
    val party get() = parent.parties[partyKey]!!
    val partyName
        get() =
            if (party.type == Party.Type.DIVISION) ReadOnly.prop(party.name) else party.workplace.name + " Workplace"

    override val quest
        get() = Quest(
            "Your authority is being questioned in %s.".format(partyName),
            description = "Improve relations with your direct reports",
            tgtCharacters = charNames,
            tooltip = SimpleTextTooltipUI(charNames.joinToString {
                "You must improve the relationship with %s.\n".format(
                    ReadOnly.charProp(
                        it
                    )
                )
            })
        )

    override fun exec(a: Int, b: Int) {
        if (charNames.all {
                parent.getMutNorm(it, parent.playerName) > 0
            })
            deactivate()
    }


}