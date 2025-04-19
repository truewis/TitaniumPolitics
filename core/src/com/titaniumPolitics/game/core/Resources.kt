package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable

@Serializable
class Resources : HashMap<String, Double>()
{
    constructor(vararg pairs: Pair<String, Double>) : this()
    {
        putAll(pairs)
    }

    override fun get(key: String): Double
    {
        return super[key] ?: .0
    }

    override fun put(key: String, value: Double): Double?
    {
        return if (this[key] + value > 0)
        {
            super.put(key, value)
        } else
        {
            throw Exception("Resource value must be positive: $key, $value")
        }
    }

    fun plus(r1: Resources, r2: Resources): Resources
    {
        val result = Resources()
        r1.forEach { (key, value) ->
            result[key] = value + r2[key]
        }
        r2.forEach { (key, value) ->
            if (!result.containsKey(key))
            {
                result[key] = value
            }
        }
        return result
    }
}