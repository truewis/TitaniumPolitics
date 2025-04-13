package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable

@Serializable
data class Coordinate3D(var x: Int, var y: Int, var z: Int)
{
    override fun toString(): String
    {
        return "($x, $y, $z)"
    }
}
