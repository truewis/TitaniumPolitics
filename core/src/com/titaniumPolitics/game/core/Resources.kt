package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

@Serializable
class Resources()
{
    private val _resources = hashMapOf<String, Double>()

    constructor(hashMap: HashMap<String, Double>) : this()
    {
        hashMap.forEach {
            _resources[it.key] = it.value
        }
    }

    constructor(vararg pairs: Pair<String, Double>) : this()
    {
        pairs.forEach {
            _resources[it.first] = it.second
        }
    }

    operator fun get(key: String): Double
    {
        return _resources[key] ?: .0
    }

    operator fun set(key: String, value: Double): Double?
    {
        return if (this[key] + value >= 0)
        {
            _resources.put(key, value)
        } else
        {
            throw Exception("Resource value must be nonNegative: $key, $value")
        }
    }

    fun contains(r1: Resources): Boolean
    {
        return _resources.all { it.value >= r1[it.key] }
    }

    fun plus(r1: Resources, r2: Resources): Resources
    {
        val result = Resources()
        r1._resources.forEach { (key, value) ->
            result[key] = value + r2[key]
        }
        r2._resources.forEach { (key, value) ->
            if (!result.containsKey(key))
            {
                result[key] = value
            }
        }
        return result
    }

    operator fun minus(r1: Resources): Resources
    {
        if (!contains(r1)) throw Exception("OHH!")
        val result = Resources()
        _resources.forEach { (key, value) ->
            result[key] = value - r1[key]
        }
        return result
    }

    fun containsKey(key: String): Boolean
    {
        return _resources.containsKey(key)
    }

    fun toHashMap(): HashMap<String, Double>
    {
        return HashMap(_resources)
    }

    fun forEach(function: (Map.Entry<String, Double>) -> Unit)
    {
        _resources.forEach {
            function(it)
        }
    }

    fun all(function: (Map.Entry<String, Double>) -> Boolean): Boolean
    {
        _resources.forEach {
            if (!function(it)) return false
        }
        return true
    }

    val keys: Set<String>
        get()
        {
            return _resources.keys
        }
}