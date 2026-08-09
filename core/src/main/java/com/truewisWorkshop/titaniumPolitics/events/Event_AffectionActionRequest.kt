package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.ReadOnly.IDTH
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import kotlinx.serialization.Serializable

@Serializable
class Event_AffectionActionRequest(
    val charName: String,
    val infoKey: String
) : EventObject(ReadOnly.questProp("AffectionActionRequest-name").format(charName), true),
    IQuestEventObject {

    val character get() = parent.characters[charName]
    val info get() = parent.informations[infoKey]

    override val quest
            by lazy {
                val charDisplayName = ReadOnly.charProp(charName)
                Quest(
                    ReadOnly.questProp("AffectionActionRequest-title").format(charDisplayName),
                    description = ReadOnly.questProp("AffectionActionRequest-desc").format(charDisplayName),
                    tgtCharacters = listOf(charName),
                    tgtPlace = info?.tgtPlace ?: "",
                    getTooltip = {
                        val actionInfo = info?.let {
                            val place = it.tgtPlace
                            "Task needed at ${ReadOnly.placeProp(place)}"
                        } ?: "Unknown task needed"
                        SimpleTextTooltipUI(actionInfo)
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
        // Deactivate if information is too old or newer info exists
        val info = info
        if (info != null) {
            if (info.type == InformationType.ACTION) {
                val hasNewerInfo = parent.informations.values.any {
                    it.type == InformationType.ACTION &&
                            it.tgtPlace == info.tgtPlace &&
                            it.tgtTime > info.tgtTime
                }
                if (hasNewerInfo) {
                    deactivate()
                }
            } else if (info.type == InformationType.APPARATUS) {
                val hasNewerInfo = parent.informations.values.any {
                    it.type == InformationType.APPARATUS &&
                            it.tgtApparatusName == info.tgtApparatusName &&
                            it.tgtTime > info.tgtTime
                }
                if (hasNewerInfo) {
                    deactivate()
                }
            }
            // Also deactivate if info is older than 1 week
            if (parent.time - info.tgtTime > 168 * IDTH) {
                deactivate()
            }
        }
    }
}

