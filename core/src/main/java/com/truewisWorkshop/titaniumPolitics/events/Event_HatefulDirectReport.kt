package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import kotlinx.serialization.Serializable

@Serializable
class Event_HatefulDirectReport(val charNames: List<String>, val partyKey: String) :
    EventObject(ReadOnly.questProp("HatefulDirectReport-name").format(partyKey), true),
    IQuestEventObject {
    val party get() = parent.parties[partyKey]!!
    val partyName
        get() =
            if (party.type == Party.Type.DIVISION) ReadOnly.prop(party.name) else party.workplace.name + " Workplace"

    override val quest
            by lazy {
                Quest(
                    ReadOnly.questProp("HatefulDirectReport-title").format(partyName),
                    description = ReadOnly.questProp("HatefulDirectReport-desc"),
                    tgtCharacters = charNames,
                    getTooltip = {
                        SimpleTextTooltipUI(charNames.joinToString {
                            "You must improve the relationship with %s.\n".format(
                                ReadOnly.charProp(
                                    it
                                )
                            )
                        })
                    }
                )
            }

    override fun exec(a: Int, b: Int) {
        if (charNames.all {
                parent.getMutNorm(it, parent.playerName) > 0
            })
            deactivate()
    }


}