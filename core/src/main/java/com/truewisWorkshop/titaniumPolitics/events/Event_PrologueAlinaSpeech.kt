package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SpeechUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_PrologueAlinaSpeech : EventObject(ReadOnly.questProp("PrologueAlinaSpeech-name"), true), IQuestEventObject {


    @Transient
    override val quest = Quest(
        ReadOnly.questProp("PrologueAlinaSpeech-title"),
        ReadOnly.questProp("PrologueAlinaSpeech-desc"),
        tgtMeeting = "conference-outerBarrierWest-1980",
        tgtPlace = "infrastructureHeadquarters"
    )

    override fun exec(a: Int, b: Int) {
        if (parent.parties["infrastructure"]!!.leader == "Alina" && parent.player.currentMeeting?.currentCharacters?.containsAll(
                listOf("Alina", "Krailin", "Rui")
            )
            ?: false
        ) {
            parent.places["outerBarrierEast"]!!.workplaceParty?.addMember("Rui", Party.Role.NONE)
            parent.places["outerBarrierEast"]!!.workplaceParty?.changeLeader("Rui")
            onPlayDialogue("PrologueInfDivLeaderSpeech")
            parent.eventSystem.add(Event_AlinaResign())
            parent.eventSystem.add(Event_ObserverIntroAfterMeeting1())
            parent.eventSystem.add(Event_FirstWorkplaceMeeting())
            deactivate()
        }
    }

}