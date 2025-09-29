package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.ui.Quest
import kotlinx.serialization.Transient

interface IQuestEventObject {
    @Transient
    val quest: Quest

}
