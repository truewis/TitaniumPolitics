package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable
import kotlin.math.exp

@Serializable
data class Stat(var logos: Int, var ethos: Int, var pathos: Int, var riskTaking: Int = 10) {
    constructor() : this(10, 10, 10)

    val lScale get() = if (logos <= 10) logos / 10.0 else exp(logos / 10.0 - 1)
    val eScale get() = if (ethos <= 10) ethos / 10.0 else exp(ethos / 10.0 - 1)
    val pScale get() = if (pathos <= 10) pathos / 10.0 else exp(pathos / 10.0 - 1)
    val rScale get() = if (riskTaking <= 10) riskTaking / 10.0 else exp(riskTaking / 10.0 - 1)
}
