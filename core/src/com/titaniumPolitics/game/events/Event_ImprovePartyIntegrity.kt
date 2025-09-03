package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Meeting
import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.PartyMutualityMeter
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_ImprovePartyIntegrity(val partyKey: String) : EventObject("Improve Party Integrity: $partyKey", true),
    IQuestEventObject {
    val party get() = parent.parties[partyKey]!!
    val partyName
        get() =
            if (party.type == Party.Type.DIVISION) ReadOnly.prop(party.name) else party.workplace.name + " Workplace"

    override val quest
        get() = Quest(
            "Members of %s are not getting along.".format(partyName),
            description = "You must improve the integrity of %s".format(partyName),
            tgtPlace = party.home,
            display = {
                with(it) {
                    add(PartyMutualityMeter(this@Event_ImprovePartyIntegrity.parent, partyKey))
                }
            }
        )

    override fun exec(a: Int, b: Int) {
        if (party.integrity > 0.5)
            deactivate()
    }


}