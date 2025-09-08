package com.titaniumPolitics.game.core

import com.badlogic.gdx.Gdx
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.io.FileInputStream
import java.util.Properties

class CharacterGenerator {
    companion object {
        private var count = 0
        val maleNameProps = javaClass.classLoader.getResourceAsStream("texts/maleNames.properties")?.use {
            Properties().apply { load(it) }
        } ?: Properties().apply { load(FileInputStream(File("../assets/texts/maleNames.properties"))) }
        val femaleNameProps = javaClass.classLoader.getResourceAsStream("texts/femaleNames.properties")?.use {
            Properties().apply { load(it) }
        } ?: Properties().apply { load(FileInputStream(File("../assets/texts/femaleNames.properties"))) }
        val nameModifierProps = javaClass.classLoader.getResourceAsStream("texts/nameModifier.properties")?.use {
            Properties().apply { load(it) }
        } ?: Properties().apply { load(FileInputStream(File("../assets/texts/nameModifier.properties"))) }

        val charJson = Json.parseToJsonElement(
            Gdx.files?.internal("json/characters.json")?.readString()
                ?: File("../assets/json/characters.json").readText()
        ).jsonObject
        var generatedCharJson = charJson.toMutableMap()

        val generatedNames = mutableSetOf<String>()
        fun generateName(isMale: Boolean): String {
            val baseName = if (isMale) {
                maleNameProps.values.random().toString()
            } else {
                femaleNameProps.values.random().toString()
            }
            val modifier = nameModifierProps.values.random().toString()
            val fullName = modifier.format(baseName)
            ReadOnly.charProps.setProperty("c$count", fullName)
            if (isMale) {
                val random = count % 5 + 1
                generatedCharJson.apply {
                    put(
                        "c$count",
                        JsonObject(
                            mapOf(
                                "image" to JsonPrimitive("portraits/generated/man${random}.png"),
                                "headImage" to JsonPrimitive("portraits/generated/man${random}Head.png")
                            )
                        )
                    )
                }
            } else {
                val random = count % 5 + 1
                generatedCharJson.apply {
                    put(
                        "c$count",
                        JsonObject(
                            mapOf(
                                "image" to JsonPrimitive("portraits/generated/woman${random}.png"),
                                "headImage" to JsonPrimitive("portraits/generated/woman${random}Head.png")
                            )
                        )
                    )
                }
            }

            count++
            return if (fullName in generatedNames) generateName(isMale) else {
                generatedNames.add(fullName)
                "c$count"
            }
        }

    }
}