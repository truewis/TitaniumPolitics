package com.titaniumPolitics.game.core

import com.badlogic.gdx.Gdx
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.*

object ReadOnly
{
    val mapJson = Json.parseToJsonElement(Gdx.files.internal("json/map.json").readString()).jsonObject
    val charJson = Json.parseToJsonElement(Gdx.files.internal("json/characters.json").readString()).jsonObject
    val actionJson = Json.parseToJsonElement(Gdx.files.internal("json/action.json").readString()).jsonObject
    val constJson = Json.parseToJsonElement(Gdx.files.internal("json/consts.json").readString()).jsonObject
    val props = javaClass.classLoader.getResourceAsStream("texts/ui.properties").use {
        Properties().apply { load(it) }
    }
    val script = javaClass.classLoader.getResourceAsStream("texts/DefaultCharacter.properties").use {
        Properties().apply { load(it) }
    }

    fun const(constName: String): Float
    {
        return constJson[constName]?.jsonPrimitive?.float
            ?: 0f.also { println("Warning: Could not find constant $constName") }
    }

    fun prop(key: String, obj: Any? = null): String
    {

        return if (obj != null)
                (props.getProperty(key)?.replacePlaceholders(obj)) ?: "Unknown".also { println("Warning: Could not find property $key") }
        else
                (props.getProperty(key)) ?: "Unknown".also { println("Warning: Could not find property $key") }
    }

    fun script(key: String, obj: Any? = null): String
    {
        return if(obj != null)
            (script.getProperty(key).replacePlaceholders(obj))
        else
            (script.getProperty(key))

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