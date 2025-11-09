package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.widget.SpeechUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Event_PrologueAlinaAccident : EventObject("Introduction of Alina.", true) {


    override fun exec(a: Int, b: Int) {
        //Move Rui and Alina to outerBarrierEast.
        parent.characters["Rui"]!!.forceMoveToPlace("outerBarrierEast")
        parent.characters["Alina"]!!.forceMoveToPlace("outerBarrierEast")
        parent.characters["Alina"]!!.health = 0.0 //Alina is unconscious.
        onPlayDialogue("Prologue")
        parent.eventSystem.add(Event_PrologueAlinaSpeech())
        deactivate()

    }

}
