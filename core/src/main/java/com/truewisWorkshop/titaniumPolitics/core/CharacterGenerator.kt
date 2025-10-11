package com.titaniumPolitics.game.core

import com.badlogic.gdx.Gdx
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.io.File

class CharacterGenerator {
    companion object {
        private var count = 0

        // Removed manual property loading; name lists are now handled by ReadOnly.

        // JSON loading remains the same
        val charJson = Json.parseToJsonElement(
            Gdx.files?.internal("json/characters.json")?.readString()
                ?: File("../assets/json/characters.json").readText()
        ).jsonObject
        var generatedCharJson = charJson.toMutableMap()

        val generatedNames = mutableSetOf<String>()

        /**
         * Generates a unique, localized character name based on the current locale
         * and stores it in the ReadOnly character properties for runtime access.
         */
        fun generateName(isMale: Boolean): String {
            // Retrieve localized name bundles using the new ReadOnly helper
            val maleNameProps = ReadOnly.getAllPropsFromBundle("maleNames")
            val femaleNameProps = ReadOnly.getAllPropsFromBundle("femaleNames")
            val nameModifierProps = ReadOnly.getAllPropsFromBundle("nameModifier")

            // Determine the base name source and ensure values are present
            val baseNameValues = if (isMale) maleNameProps.values else femaleNameProps.values
            val modifierValues = nameModifierProps.values

            if (baseNameValues.isEmpty() || modifierValues.isEmpty()) {
                // Fallback or error if essential localization data is missing
                return "Unknown Character"
            }

            // Pick random values (Properties.values() returns Collection<Any> so we cast to String)
            val baseName = baseNameValues.random().toString()
            val modifier = modifierValues.random().toString()

            // Format the name using the localized modifier template (e.g., "{0} son" vs. "{0} fils")
            val fullName = modifier.format(baseName)

            val charId = "c$count"

            // Uniqueness check for the full name. If not unique, try again recursively.
            return if (fullName in generatedNames) {
                // If not unique, discard this ID attempt and recurse (the original logic's behavior)
                generateName(isMale)
            } else {
                // If unique, finalize the character creation
                generatedNames.add(fullName)

                // Store the dynamically generated name using the new setCharacterProp accessor.
                // This keeps the generated name available via ReadOnly.charProp(charId).
                ReadOnly.setCharacterProp(charId, fullName)

                // Image generation logic
                if (isMale) {
                    val random = count % 5 + 1
                    generatedCharJson[charId] = JsonObject(
                        mapOf(
                            "image" to JsonPrimitive("portraits/generated/man${random}.png"),
                            "headImage" to JsonPrimitive("portraits/generated/man${random}Head.png")
                        )
                    )
                } else {
                    val random = count % 5 + 1
                    generatedCharJson[charId] = JsonObject(
                        mapOf(
                            "image" to JsonPrimitive("portraits/generated/woman${random}.png"),
                            "headImage" to JsonPrimitive("portraits/generated/woman${random}Head.png")
                        )
                    )
                }

                // Increment count for the next character generation attempt
                count++

                charId
            }
        }
    }
}
