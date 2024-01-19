package com.titaniumPolitics.game.core

import com.badlogic.gdx.Gdx
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

object ReadOnlyJsons
{
    val mapJson = Json.parseToJsonElement(Gdx.files.internal("json/map.json").readString()).jsonObject
}