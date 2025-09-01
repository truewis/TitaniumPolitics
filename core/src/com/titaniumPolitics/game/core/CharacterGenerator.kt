package com.titaniumPolitics.game.core

import java.io.File
import java.io.FileInputStream
import java.util.Properties

class CharacterGenerator {
    companion object {
        var count = 0
        val maleNameProps = javaClass.classLoader.getResourceAsStream("texts/maleNames.properties")?.use {
            Properties().apply { load(it) }
        } ?: Properties().apply { load(FileInputStream(File("../assets/texts/maleNames.properties"))) }
        val femaleNameProps = javaClass.classLoader.getResourceAsStream("texts/femaleNames.properties")?.use {
            Properties().apply { load(it) }
        } ?: Properties().apply { load(FileInputStream(File("../assets/texts/femaleNames.properties"))) }
        val nameModifierProps = javaClass.classLoader.getResourceAsStream("texts/nameModifier.properties")?.use {
            Properties().apply { load(it) }
        } ?: Properties().apply { load(FileInputStream(File("../assets/texts/nameModifier.properties"))) }

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
            count++
            return if (fullName in generatedNames) generateName(isMale) else {
                generatedNames.add(fullName)
                "c$count"
            }
        }

    }
}