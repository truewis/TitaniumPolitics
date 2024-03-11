package com.titaniumPolitics.game.core

import com.badlogic.gdx.Gdx
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object ReadOnlyJsons
{
    val mapJson = Json.parseToJsonElement(Gdx.files.internal("json/map.json").readString()).jsonObject
    val constJson = Json.parseToJsonElement(Gdx.files.internal("json/consts.json").readString()).jsonObject
    fun getConst(constName: String): Float
    {
        return constJson[constName]!!.jsonPrimitive.float
    }
}