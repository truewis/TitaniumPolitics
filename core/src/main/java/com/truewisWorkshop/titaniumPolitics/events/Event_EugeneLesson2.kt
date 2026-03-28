package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Eugene's second political practice lesson.
 * Requires the player to have built mutual trust with Eugene (mutuality >= 55).
 * This represents the "mutuality" condition for this questline step.
 */
@Serializable
class Event_EugeneLesson2 : EventObject(ReadOnly.questProp("EugeneLesson2-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("EugeneLesson2-title"),
        ReadOnly.questProp("EugeneLesson2-desc"),
        tgtPlace = "educationHeadquarters",
        tgtCharacters = listOf("Eugene"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "educationHeadquarters" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Eugene") == true &&
            parent.getMutuality("Eugene", parent.playerName) >= 55.0
        ) {
            onPlayDialogue("EugeneAndPoliticalPractices2")
            parent.eventSystem.add(Event_EugeneLesson3())
            deactivate()
        }
    }

}
