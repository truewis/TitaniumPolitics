package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import kotlinx.serialization.Serializable

@Serializable
class Event_ThreatFromPeer(val charName: String) :
    EventObject(ReadOnly.questProp("ThreatFromPeer-name").format(charName), false),
    IQuestEventObject {

    val character get() = parent.characters[charName]

    override val quest
            by lazy {
                val charDisplayName = ReadOnly.charProp(charName)
                val threatType = when {
                    "psychopath" in character?.trait.orEmpty() -> "a psychological threat"
                    "soldier" in character?.trait.orEmpty() -> "a physical threat"
                    "engineer" in character?.trait.orEmpty() -> "sabotage of your work"
                    else -> "serious consequences"
                }
                Quest(
                    ReadOnly.questProp("ThreatFromPeer-title").format(charDisplayName),
                    description = ReadOnly.questProp("ThreatFromPeer-desc").format(threatType),
                    tgtCharacters = listOf(charName),
                    getTooltip = {
                        SimpleTextTooltipUI("A colleague is threatening you. You should address this.")
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
