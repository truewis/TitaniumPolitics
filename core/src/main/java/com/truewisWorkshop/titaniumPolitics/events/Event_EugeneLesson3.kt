package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Eugene's third and final political practice lesson.
 * Requires even higher mutual trust with Eugene (mutuality >= 65).
 * Completing this lesson unlocks the "NewAgenda" progression,
 * allowing the player to propose meeting agendas beyond simple praise.
 */
@Serializable
class Event_EugeneLesson3 : EventObject(ReadOnly.questProp("EugeneLesson3-name"), true), IQuestEventObject {

    @Transient
    override val quest = Quest(
        ReadOnly.questProp("EugeneLesson3-title"),
        ReadOnly.questProp("EugeneLesson3-desc"),
        tgtPlace = "educationHeadquarters",
        tgtCharacters = listOf("Eugene"),
    )

    override fun exec(a: Int, b: Int) {
        if (parent.player.place.name == "educationHeadquarters" &&
            parent.player.currentMeeting?.currentCharacters?.contains("Eugene") == true &&
            parent.getMutuality("Eugene", parent.playerName) >= 65.0
        ) {
            onPlayDialogue("EugeneAndPoliticalPractices3")
            parent.unlockProgression("NewAgenda")
            deactivate()
        }
    }

}
