package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import kotlinx.serialization.Serializable

@Serializable
class Event_ThreatFromBoss(val charName: String) :
    EventObject(ReadOnly.questProp("ThreatFromBoss-name").format(charName), false),
    IQuestEventObject {

    val character get() = parent.characters[charName]

    override val quest
            by lazy {
                val charDisplayName = ReadOnly.charProp(charName)
                Quest(
                    ReadOnly.questProp("ThreatFromBoss-title").format(charDisplayName),
                    description = ReadOnly.questProp("ThreatFromBoss-desc"),
                    tgtCharacters = listOf(charName),
                    getTooltip = {
                        SimpleTextTooltipUI("Your boss is threatening to fire you. This is serious.")
                    }
                )
            }

    override fun exec(a: Int, b: Int) {
        // Deactivate if character dies or character's dislike diminishes
        if (character == null || !character!!.alive) {
            deactivate(false)
            return
        }
        if (parent.getMutNorm(charName, parent.playerName) > -0.25) {
            deactivate()
            return
        }
    }
}
