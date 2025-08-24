package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable

@Serializable
data class Budget(var value: HashMap<String, Resources>) {
    fun sum(): Resources {
        val total = Resources()
        value.forEach { (_, resources) ->
            total += resources
        }
        return total
    }

    fun sum(type: String): Double {
        return value.values.sumOf { it[type] }
    }
}
