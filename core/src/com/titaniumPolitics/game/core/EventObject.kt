package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable

/**
 *
 */
@Serializable
open class EventObject(override var name: String, val oneTime: Boolean) : GameStateElement()
{
    open val isTriggered: Boolean
        get() = false
}