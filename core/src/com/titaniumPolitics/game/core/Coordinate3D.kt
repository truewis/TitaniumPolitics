package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable
import kotlin.math.sqrt

@Serializable
data class Coordinate3D(var x: Int, var y: Int, var z: Int) {
    val amplitude: Double
        get() = sqrt((x * x + y * y + z * z).toDouble())

    override fun toString(): String {
        return "($x, $y, $z)"
    }

    operator fun plus(other: Coordinate3D): Coordinate3D {
        return Coordinate3D(x + other.x, y + other.y, z + other.z)
    }

    operator fun minus(other: Coordinate3D): Coordinate3D {
        return Coordinate3D(x - other.x, y - other.y, z - other.z)
    }

    operator fun times(scalar: Int): Coordinate3D {
        return Coordinate3D(x * scalar, y * scalar, z * scalar)
    }

    operator fun div(scalar: Int): Coordinate3D {
        if (scalar == 0) throw ArithmeticException("Division by zero")
        return Coordinate3D(x / scalar, y / scalar, z / scalar)
    }

    operator fun unaryMinus(): Coordinate3D {
        return Coordinate3D(-x, -y, -z)
    }
}
