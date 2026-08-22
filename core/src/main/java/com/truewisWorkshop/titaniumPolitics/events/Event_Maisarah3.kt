package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Maisarah3(val searchFrom: Int) : EventObject(ReadOnly.questProp("Maisarah3-name"), true), IQuestEventObject {
    private fun recentMiningEvidenceCount() =
        parent.player.preparedInfoKeys.mapNotNull { parent.informations[it] }.count { info ->
            info.type in listOf(InformationType.ACCIDENT, InformationType.CASUALTY) &&
                info.creationTime > searchFrom &&
                (info.tgtPlace == "miningHeadquarters" || info.tgtPlace.startsWith("mine"))
        }

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Maisarah3-title"),
        ReadOnly.questProp("Maisarah3-desc"),
        tgtPlace = "miningHeadquarters",
        tgtCharacters = listOf("Maisarah"),
        getTooltip = {
            SimpleTextTooltipUI("Recent mining evidence: ${recentMiningEvidenceCount()}/2")
        }
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "miningHeadquarters" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Maisarah") == true &&
            recentMiningEvidenceCount() >= 2 &&
            parent.getMutuality("Maisarah", parent.playerName) >= 20.0
        ) {
            onPlayDialogue("Maisarah3")
            parent.eventSystem.add(Event_Maisarah4(parent.time))
            deactivate()
        }
    }
}
