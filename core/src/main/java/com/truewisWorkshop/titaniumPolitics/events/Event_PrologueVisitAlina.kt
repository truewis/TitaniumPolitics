package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_PrologueVisitAlina : EventObject(ReadOnly.questProp("PrologueVisitAlina-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("PrologueVisitAlina-title"),
        ReadOnly.questProp("PrologueVisitAlina-desc"),
        tgtCharacters = listOf("Alina"),
        tgtPlace = "welfareStationEast"
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.currentMeeting?.currentCharacters?.contains("Alina") == true &&
            parent.player.place.name in listOf(
                "welfareStationEast", "welfareStationWest",
                "rescueStationEast", "rescueStationWest", "rescueStationSouth"
            )
        ) {
            onPlayDialogue("Prologue5")
            parent.eventSystem.add(Event_PrologueAlinaSpeech())
            deactivate()
        }
    }

}
