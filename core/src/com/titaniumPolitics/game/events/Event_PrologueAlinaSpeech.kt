package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SpeechUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_PrologueAlinaSpeech : EventObject("Introduction of Alina.", true), IQuestEventObject {


    @Transient
    override val quest = Quest(
        "Alina's speech",
        "Alina is giving a speech to the Infrastructure Division.",
        tgtMeeting = "conference-outerBarrierWest-1980",
        tgtPlace = "outerBarrierWest"
    )

    override fun exec(a: Int, b: Int) {
        if (parent.parties["infrastructure"]!!.leader == "Alina" && parent.player.currentMeeting?.currentCharacters?.containsAll(
                listOf("Alina", "Krailin", "Rui")
            )
            ?: false
        ) {
            onPlayDialogue("PrologueInfDivLeaderSpeech")
            parent.eventSystem.add(Event_AlinaResign())
            parent.eventSystem.add(Event_ObserverIntroAfterMeeting1())
            deactivate()
        }
    }

}