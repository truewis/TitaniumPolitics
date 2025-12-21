package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SpeechUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_PrologueAlinaAccident2 : EventObject("Interrogation", true) {


    override fun exec(a: Int, b: Int) {
        //Move Rui and Alina to rescueStationWest.
        println("Executing Event_PrologueAlinaAccident2")
        //parent.characters["Rui"]!!.forceMoveToPlace("rescueStationWest")
        //parent.characters["Alina"]!!.forceMoveToPlace("rescueStationWest")
        //Disabled due to agent routine bug.
        //parent.characters["Peiyu"]!!.forceMoveToPlace("rescueStationWest")
        //parent.characters["Astinomis"]!!.forceMoveToPlace("rescueStationWest")
        onPlayDialogue("Prologue2")
        parent.eventSystem.add(Event_PrologueAlinaSpeech())
        deactivate()

    }

}
