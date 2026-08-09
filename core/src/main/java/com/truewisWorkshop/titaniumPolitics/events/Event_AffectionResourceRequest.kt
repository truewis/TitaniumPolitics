package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.ReadOnly.IDTH
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import kotlinx.serialization.Serializable

@Serializable
class Event_AffectionResourceRequest(
    val charName: String,
    val infoKey: String
) : EventObject(ReadOnly.questProp("AffectionResourceRequest-name").format(charName), true),
    IQuestEventObject {

    val character get() = parent.characters[charName]
    val info get() = parent.informations[infoKey]

    override val quest
            by lazy {
                val charDisplayName = ReadOnly.charProp(charName)
                Quest(
                    ReadOnly.questProp("AffectionResourceRequest-title").format(charDisplayName),
                    description = ReadOnly.questProp("AffectionResourceRequest-desc").format(charDisplayName),
                    tgtCharacters = listOf(charName),
                    getTooltip = {
                        val resType = info?.let {
                            val place = it.tgtPlace
                            "Resources needed at ${ReadOnly.placeProp(place)}"
                        } ?: "Unknown resource needs"
                        SimpleTextTooltipUI(resType)
                    }
                )
            }

    override fun exec(a: Int, b: Int) {
        // Deactivate if the character no longer likes the player, or info is gone
        if (character == null || parent.informations[infoKey] == null) {
            deactivate(false)
            return
        }
        if (parent.getMutNorm(charName, parent.playerName) < -0.1) {
            deactivate(false)
            return
        }
        // Deactivate if information is too old
        val info = info
        if (info != null && info.type == InformationType.RESOURCES) {
            val hasNewerInfo = parent.informations.values.any {
                it.type == InformationType.RESOURCES &&
                        it.tgtPlace == info.tgtPlace &&
                        it.tgtTime > info.tgtTime
            }
            if (hasNewerInfo) {
                deactivate()
            }
            // Also deactivate if info is older than 1 week
            if (parent.time - info.tgtTime > 168 * IDTH) {
                deactivate()
            }
        }
    }
}
