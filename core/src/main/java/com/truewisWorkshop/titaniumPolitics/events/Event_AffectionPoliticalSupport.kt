package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.ReadOnly.IDTH
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import kotlinx.serialization.Serializable

@Serializable
class Event_AffectionPoliticalSupport(
    val charName: String,
    val partyKey: String,
    val infoKey: String
) : EventObject(
    ReadOnly.questProp("AffectionPoliticalSupport-name").format(charName, partyKey),
    true
),
    IQuestEventObject {

    val character get() = parent.characters[charName]
    val party get() = parent.parties[partyKey]
    val info get() = parent.informations[infoKey]

    val partyDisplayName
        get() = party?.let {
            if (it.type == Party.Type.DIVISION) ReadOnly.prop(it.name) else it.workplace.name + " Workplace"
        } ?: partyKey

    override val quest
            by lazy {
                val charDisplayName = ReadOnly.charProp(charName)
                Quest(
                    ReadOnly.questProp("AffectionPoliticalSupport-title").format(
                        charDisplayName,
                        partyDisplayName
                    ),
                    description = ReadOnly.questProp("AffectionPoliticalSupport-desc").format(
                        charDisplayName,
                        partyDisplayName
                    ),
                    tgtCharacters = listOf(charName),
                    tgtPlace = party?.home ?: "",
                    getTooltip = {
                        val partyInfo = "Support needed for ${partyDisplayName}"
                        SimpleTextTooltipUI(partyInfo)
                    }
                )
            }

    override fun exec(a: Int, b: Int) {
        // Deactivate if the character no longer likes the player, or parties are gone
        if (character == null || party == null || parent.informations[infoKey] == null) {
            deactivate(false)
            return
        }
        if (parent.getMutNorm(charName, parent.playerName) < -0.1) {
            deactivate(false)
            return
        }
        // Deactivate if party relationships have improved or info is too old
        val info = info
        if (info != null && info.type == InformationType.PARTY_MUTUALITY) {
            if (party!!.integrity > 0.5) {
                deactivate()
            }
            // Also deactivate if info is older than 1 week
            if (parent.time - info.tgtTime > 168 * IDTH) {
                deactivate()
            }
        }
    }
}

