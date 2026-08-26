package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Maisarah4(val searchFrom: Int) : EventObject(ReadOnly.questProp("Maisarah4-name"), true), IQuestEventObject {
    private fun recentMiningEvidenceCount() =
        parent.player.preparedInfoKeys.mapNotNull { parent.informations[it] }.count { info ->
            info.type in listOf(InformationType.ACCIDENT, InformationType.CASUALTY) &&
                info.creationTime > searchFrom &&
                (info.tgtPlace == "miningHeadquarters" || info.tgtPlace.startsWith("mine"))
        }

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Maisarah4-title"),
        ReadOnly.questProp("Maisarah4-desc"),
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
            parent.getMutuality("Maisarah", parent.playerName) >= 40.0
        ) {
            onPlayDialogue("Maisarah4")
            parent.unlockProgression("NewAgenda")
            deactivate()
        }
    }
}
