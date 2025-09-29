package com.titaniumPolitics.game.core

import com.badlogic.gdx.Gdx
import com.titaniumPolitics.game.core.CharacterGenerator.Companion.generatedCharJson
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.json.*
import java.io.File
import java.io.FileInputStream
import java.util.*

object ReadOnly {
    //Boltzmann constant in J/K
    val KB = 1.380649e-23

    val NA = 6.02214076e23 //Avogadro's number in mol^-1

    //Gravitational acceleration in m/s^2
    //TODO: depends on the coordinate
    val GA = 9.8
    val mapJson = Json.parseToJsonElement(
        Gdx.files?.internal("json/map.json")?.readString() ?: File("../assets/json/map.json").readText()
    ).jsonObject
    val charJson get() = generatedCharJson
    val actionJson = Json.parseToJsonElement(
        Gdx.files?.internal("json/action.json")?.readString() ?: File("../assets/json/action.json").readText()
    ).jsonObject
    val constJson = Json.parseToJsonElement(
        Gdx.files?.internal("json/consts.json")?.readString() ?: File("../assets/json/consts.json").readText()
    ).jsonObject
    val gasJson = Json.parseToJsonElement(
        Gdx.files?.internal("json/gas.json")?.readString() ?: File("../assets/json/gas.json").readText()
    ).jsonObject

    val appJson = Json.parseToJsonElement(
        Gdx.files?.internal("json/apparatus.json")?.readString() ?: File("../assets/json/apparatus.json").readText()
    ).jsonObject

    val resJson = Json.parseToJsonElement(
        Gdx.files?.internal("json/resources.json")?.readString() ?: File("../assets/json/resources.json").readText()
    ).jsonObject

    val props = javaClass.classLoader.getResourceAsStream("texts/ui.properties")?.use {
        Properties().apply { load(it) }
    } ?: Properties().apply { load(FileInputStream(File("../assets/texts/ui.properties"))) }

    val appProps = javaClass.classLoader.getResourceAsStream("texts/apparatus.properties")?.use {
        Properties().apply { load(it) }
    } ?: Properties().apply { load(FileInputStream(File("../assets/texts/apparatus.properties"))) }

    val placeProps = javaClass.classLoader.getResourceAsStream("texts/place.properties")?.use {
        Properties().apply { load(it) }
    } ?: Properties().apply { load(FileInputStream(File("../assets/texts/place.properties"))) }

    val charProps = javaClass.classLoader.getResourceAsStream("texts/character.properties")?.use {
        Properties().apply { load(it) }
    } ?: Properties().apply { load(FileInputStream(File("../assets/texts/character.properties"))) }

    val itemProps = javaClass.classLoader.getResourceAsStream("texts/resources.properties")?.use {
        Properties().apply { load(it) }
    } ?: Properties().apply { load(FileInputStream(File("../assets/texts/resources.properties"))) }

    val script = javaClass.classLoader.getResourceAsStream("texts/DefaultCharacter.properties")?.use {
        Properties().apply { load(it) }
    } ?: Properties().apply { load(FileInputStream(File("../assets/texts/DefaultCharacter.properties"))) }


    fun const(constName: String): Double {
        return constJson[constName]?.jsonPrimitive?.double
            ?: .0.also { throw Exception("Could not find constant $constName") }
    }

    fun constInt(constName: String): Int {
        return constJson[constName]?.jsonPrimitive?.int
            ?: 0.also { throw Exception("Could not find constant $constName") }
    }

    fun charName(charId: String): String {
        return charProp(charId)
    }

    /**A timestep in seconds.
     *
     */
    val DT = (86400 / const("lengthOfDay")).toInt()

    /**A timestep in hours.
     *
     */
    val DTH = (24 / const("lengthOfDay"))

    /**An hour in timestep units.
     *
     */
    val IDTH = (const("lengthOfDay") / 24.0).toInt()

    const val S_PER_HR = 3600 //How many seconds in an hour.


    /**
     * Show total minutes corresponding to the given time. (not modulo 60).
     */
    fun toTotalMinutes(time: Int): Int =
        (time / (const("lengthOfDay") / 1440.0)).toInt()

    /**
     * Show minute in clock format (0-59).
     */
    fun toMinutes(time: Int): Int =
        (time % constInt("lengthOfDay") / (const("lengthOfDay") / 1440.0)).toInt()

    /**
     * Show total hours corresponding to the given time. (not modulo 24).
     */
    fun toTotalHours(time: Int): Int =
        (time / (const("lengthOfDay") / 24.0)).toInt()

    /**
     * Show hour in clock format (0-23).
     */
    fun toHours(time: Int): Int =
        (time % constInt("lengthOfDay") / (const("lengthOfDay") / 24.0)).toInt()

    fun toDays(time: Int): Int =
        (time / const("lengthOfDay")).toInt()


    val mutualityScale = const("mutualityMax") - const("mutualityMin")

    fun prop(key: String, obj: Any? = null): String {
        return if (obj != null)
            (props.getProperty(key)?.replacePlaceholders(obj))
                ?: "Unknown".also { Logger.write("Warning: Could not find property $key", Logger.LogLevel.INFO) }
        else
            (props.getProperty(key)) ?: "Unknown".also {
                Logger.write(
                    "Warning: Could not find property $key",
                    Logger.LogLevel.INFO
                )
            }
    }

    fun appProp(key: String, obj: Any? = null): String {

        return if (obj != null)
            (appProps.getProperty(key)?.replacePlaceholders(obj))
                ?: "Unknown".also { Logger.write("Warning: Could not find property $key", Logger.LogLevel.INFO) }
        else
            (appProps.getProperty(key)) ?: "Unknown".also {
                Logger.write(
                    "Warning: Could not find property $key",
                    Logger.LogLevel.INFO
                )
            }
    }

    fun placeProp(key: String, obj: Any? = null): String {
        if (key.startsWith("home_"))
            if (key.contains("desc"))
                return placeProps.getProperty("home-desc")
            else
                return placeProps.getProperty("home").format(key.substringAfter("home_"))
        return if (obj != null)
            (placeProps.getProperty(key)?.replacePlaceholders(obj))
                ?: "Unknown".also { Logger.write("Warning: Could not find property $key", Logger.LogLevel.INFO) }
        else
            (placeProps.getProperty(key)) ?: "Unknown".also {
                Logger.write(
                    "Warning: Could not find property $key",
                    Logger.LogLevel.INFO
                )
            }
    }

    fun charProp(key: String, obj: Any? = null): String {

        return if (obj != null)
            (charProps.getProperty(key)?.replacePlaceholders(obj))
                ?: "Unknown".also { Logger.write("Warning: Could not find property $key", Logger.LogLevel.INFO) }
        else
            (charProps.getProperty(key)) ?: "Unknown".also {
                Logger.write(
                    "Warning: Could not find property $key",
                    Logger.LogLevel.INFO
                )
            }
    }

    fun itemProp(key: String, obj: Any? = null): String {

        return if (obj != null)
            (itemProps.getProperty(key)?.replacePlaceholders(obj))
                ?: "Unknown".also { Logger.write("Warning: Could not find property $key", Logger.LogLevel.INFO) }
        else
            (itemProps.getProperty(key)) ?: "Unknown".also {
                Logger.write(
                    "Warning: Could not find property $key",
                    Logger.LogLevel.INFO
                )
            }
    }

    fun script(key: String, obj: Any? = null): String {
        return if (obj != null)
            (script.getProperty(key)?.replacePlaceholders(obj))
                ?: "Unknown".also { Logger.write("Warning: Could not find property $key", Logger.LogLevel.INFO) }
        else
            (script.getProperty(key)) ?: "Unknown".also {
                Logger.write(
                    "Warning: Could not find property $key",
                    Logger.LogLevel.INFO
                )
            }

    }

    private fun String.replacePlaceholders(source: Any): String {
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