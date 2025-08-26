package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable

/**
 * A class representing a collection of in-game resources, where each resource is identified by a string key and has a double value.
 * The class supports various operations such as addition, subtraction, and scaling of resources.
 * It also includes functionality to ensure that resource values remain non-negative if specified.
 *
 * @property positive A boolean flag indicating whether the resources should be non-negative.
 */
@Serializable
class Resources(var positive: Boolean = false) {
    private val _resources = hashMapOf<String, Double>()

    constructor(map: Map<String, Double>, positive: Boolean = false) : this(positive) {
        map.forEach {
            _resources[it.key] = it.value
        }
    }

    constructor(vararg pairs: Pair<String, Double>, positive: Boolean = false) : this(positive) {
        pairs.forEach {
            _resources[it.first] = it.second
        }
    }

    operator fun get(key: String): Double {
        return _resources[key] ?: .0
    }

    operator fun set(key: String, value: Double): Double? {
        if (value.isNaN() || value.isInfinite()) {
            throw Exception("Resource value must be finite: $key, $value")
        }
        return if (this[key] + value >= 0 || !positive) {
            _resources.put(key, value)
        } else {
            throw Exception("Resource value must be nonNegative: $key, $value")
        }
    }

    fun contains(r1: Resources): Boolean {
        return _resources.all { it.value >= r1[it.key] }
    }

    operator fun plus(r1: Resources): Resources {
        val result = Resources()
        r1._resources.forEach { (key, value) ->
            result[key] = value
        }
        _resources.forEach { (key, value) ->
            result[key] += value
        }
        return result
    }

    operator fun plusAssign(r1: Resources) {
        r1._resources.forEach { (key, value) ->
            this[key] += value
        }
    }

    operator fun times(r: Double): Resources {
        val result = Resources()
        _resources.forEach { (key, value) ->
            result[key] += value * r
        }
        return result
    }

    operator fun times(r: Int): Resources {
        val result = Resources()
        _resources.forEach { (key, value) ->
            result[key] += value * r
        }
        return result
    }

    operator fun timesAssign(r: Double) {
        _resources.forEach { (key, value) ->
            this[key] = value * r
        }
    }

    operator fun timesAssign(r: Int) {
        _resources.forEach { (key, value) ->
            this[key] = value * r
        }
    }

    operator fun minus(r1: Resources): Resources {
        if (!contains(r1) && positive) throw Exception("Tried to subtract more resources than available: $this - $r1")
        val result = Resources()
        _resources.forEach { (key, value) ->
            result[key] = value - r1[key]
        }
        return result
    }

    operator fun minusAssign(r1: Resources) {
        if (!contains(r1) && positive) throw Exception("Tried to subtract more resources than available: $this - $r1")
        r1._resources.forEach { (key, value) ->
            this[key] -= value
        }
    }

    fun containsKey(key: String): Boolean {
        return _resources.containsKey(key)
    }

    fun toHashMap(): HashMap<String, Double> {
        return HashMap(_resources)
    }

    fun forEach(function: (Map.Entry<String, Double>) -> Unit) {
        _resources.forEach {
            function(it)
        }
    }

    fun filter(function: (Map.Entry<String, Double>) -> Boolean): Resources {
        val result = Resources()
        _resources.forEach {
            if (function(it)) result[it.key] = it.value
        }
        return result
    }

    fun all(function: (Map.Entry<String, Double>) -> Boolean): Boolean {
        _resources.forEach {
            if (!function(it)) return false
        }
        return true
    }

    val keys: Set<String>
        get() {
            return _resources.keys
        }
}