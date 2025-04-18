package com.titaniumPolitics.game.core

import com.badlogic.gdx.Gdx
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.*
import kotlin.jvm.javaClass

object ReadOnly
{
    //Boltzmann constant in J/K
    val KB = 1.380649e-23

    val NA = 6.02214076e23 //Avogadro's number in mol^-1

    //Gravitational acceleration in m/s^2
    //TODO: depends on the coordinate
    val GA = 9.8
    val mapJson = Json.parseToJsonElement(
        Gdx.files?.internal("json/map.json")?.readString() ?: File("../assets/json/map.json").readText()
    ).jsonObject
    val charJson = Json.parseToJsonElement(
        Gdx.files?.internal("json/characters.json")?.readString() ?: File("../assets/json/characters.json").readText()
    ).jsonObject
    val actionJson = Json.parseToJsonElement(
        Gdx.files?.internal("json/action.json")?.readString() ?: File("../assets/json/action.json").readText()
    ).jsonObject
    val constJson = Json.parseToJsonElement(
        Gdx.files?.internal("json/consts.json")?.readString() ?: File("../assets/json/consts.json").readText()
    ).jsonObject
    val gasJson = Json.parseToJsonElement(
        Gdx.files?.internal("json/gas.json")?.readString() ?: File("../assets/json/gas.json").readText()
    ).jsonObject

    val props = javaClass.classLoader.getResourceAsStream("texts/ui.properties")?.use {
        Properties().apply { load(it) }
    } ?: Properties().apply { load(FileInputStream(File("../assets/texts/ui.properties"))) }

    val script = javaClass.classLoader.getResourceAsStream("texts/DefaultCharacter.properties")?.use {
        Properties().apply { load(it) }
    } ?: Properties().apply { load(FileInputStream(File("../assets/texts/DefaultCharacter.properties"))) }


    fun const(constName: String): Double
    {
        return constJson[constName]?.jsonPrimitive?.double
            ?: .0.also { println("Warning: Could not find constant $constName") }
    }

    fun constInt(constName: String): Int
    {
        return constJson[constName]?.jsonPrimitive?.int
            ?: 0.also { println("Warning: Could not find constant $constName") }
    }

    //A timestep in seconds.
    val dt = (86400 / const("lengthOfDay")).toInt()

    val mutualityScale = const("mutualityMax") - const("mutualityMin")

    fun prop(key: String, obj: Any? = null): String
    {

        return if (obj != null)
            (props.getProperty(key)?.replacePlaceholders(obj))
                ?: "Unknown".also { println("Warning: Could not find property $key") }
        else
            (props.getProperty(key)) ?: "Unknown".also { println("Warning: Could not find property $key") }
    }

    fun script(key: String, obj: Any? = null): String
    {
        return if (obj != null)
            (script.getProperty(key)?.replacePlaceholders(obj))
                ?: "Unknown".also { println("Warning: Could not find property $key") }
        else
            (script.getProperty(key)) ?: "Unknown".also { println("Warning: Could not find property $key") }

    }

    private fun String.replacePlaceholders(source: Any): String
    {
        val regex = "\\{VAR=([A-Za-z0-9_]+)}".toRegex()
        val kClass = source::class
        val propsByName = kClass.members.associateBy { it.name }

        return regex.replace(this) { matchResult ->
            val varName = matchResult.groupValues[1]
            val prop = propsByName[varName]
            prop?.call(source)?.toString() ?: matchResult.value // leave as-is if not found
        }
    }

}