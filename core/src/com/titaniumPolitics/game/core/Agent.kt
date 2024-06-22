package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Wait
import kotlinx.serialization.Serializable

@Serializable
sealed class Agent : GameStateElement()
{
    override val name: String
        get() = parent.nonPlayerAgents.filter { it.value == this }.keys.first()
    val character: Character
        get() = parent.characters[name]!!
    val place
        get() = parent.places.values.find { it.characters.contains(name) }!!.name

    open fun chooseAction(): GameAction
    {
        return Wait(character.name, place)
    }
}