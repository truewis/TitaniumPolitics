package com.titaniumPolitics.game.core

class MutualityMatrix : ArrayList<MutualityMatrix.MutualityEntry>() {
    data class MutualityEntry(val a: String, val b: String, val value: Double, val reasonKey: String)

    fun addMutuality(a: String, b: String, delta: Double, reasonKey: String) {
        if (delta == 0.0) return
        this.add(MutualityEntry(a, b, delta, reasonKey))
    }

    fun addWill(a: String, delta: Double, reasonKey: String) {
        addMutuality(a, a, delta, reasonKey)
    }
}