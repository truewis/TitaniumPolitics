package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Log : GameStateElement()
{
    override val name: String
        get() = "Log" //There is only one log in the game.
    var dataBase = hashMapOf<Int, String>()

    @Transient
    var newItemAdded = ArrayList<(String, Int, Int) -> Unit>()
    fun appendLog(new: String): Int
    {
        if (dataBase.containsKey(parent.time))
            dataBase[parent.time] = dataBase[parent.time] + "\t" + new
        else
            dataBase[parent.time] = new
        newItemAdded.forEach { it(new, parent.time, dataBase.size - 1) }
        return dataBase.size - 1
    }
}