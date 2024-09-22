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

    fun prop(key: String): String
    {

        return (props.getProperty(key)) ?: "Unknown".also { println("Warning: Could not find property $key") }
    }

    fun script(key: String): String?
    {
        return (script.getProperty(key)) ?: return null

    }

}