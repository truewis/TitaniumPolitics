package com.titaniumPolitics.game.core

import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Assertions.*
import kotlinx.serialization.json.*
import java.io.File

class GameStateTest
{
    @org.junit.jupiter.api.Test
    fun transformJson()
    {
        val file = File("../assets/json/init.json")
        val jsonString = file.readText()
        val json = Json.parseToJsonElement(jsonString).jsonObject

        fun processObject(obj: JsonObject): JsonObject
        {
            val updatedEntries = obj.mapValues { (key, value) ->
                when (value)
                {
                    is JsonObject -> processObject(value)
                    is JsonArray -> JsonArray(value.map { if (it is JsonObject) processObject(it) else it })
                    else -> value
                }
            }.toMutableMap()

            // Check and rename "resources" or similar keys

            return JsonObject(updatedEntries)
        }

        val transformedJson = processObject(json)
        //Write into file.
        file.writeText(Json.encodeToString(JsonObject.serializer(), transformedJson))
    }
}
