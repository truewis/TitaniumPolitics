package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_Maisarah2(val searchFrom: Int) : EventObject(ReadOnly.questProp("Maisarah2-name"), true), IQuestEventObject {
    private fun recentMiningEvidenceCount() =
        parent.player.preparedInfoKeys.mapNotNull { parent.informations[it] }.count { info ->
            info.type in listOf(InformationType.ACCIDENT, InformationType.CASUALTY) &&
                info.creationTime > searchFrom &&
                (info.tgtPlace == "miningHeadquarters" || info.tgtPlace.startsWith("mine"))
        }

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("Maisarah2-title"),
        ReadOnly.questProp("Maisarah2-desc"),
        tgtPlace = "miningHeadquarters",
        tgtCharacters = listOf("Maisarah"),
        getTooltip = {
            SimpleTextTooltipUI("Recent mining evidence: ${recentMiningEvidenceCount()}/1")
        }
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "miningHeadquarters" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Maisarah") == true &&
            recentMiningEvidenceCount() >= 1 &&
            parent.getMutuality("Maisarah", parent.playerName) >= 10.0
        ) {
            onPlayDialogue("Maisarah2")
            parent.eventSystem.add(Event_Maisarah3(parent.time))
            deactivate()
        }
    }
}
