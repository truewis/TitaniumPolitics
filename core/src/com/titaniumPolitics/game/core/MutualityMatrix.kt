package com.titaniumPolitics.game.core

class MutualityMatrix : HashMap<String, HashMap<String, Pair<Double, String>>>() {
    fun addMutuality(a: String, b: String, delta: Double, reasonKey: String) {
        if (!this.containsKey(a)) this[a] = hashMapOf()
        if (!this.containsKey(b)) this[b] = hashMapOf()
        if (!this[a]!!.containsKey(b)) this[a]!![b] = Pair(0.0, "")
        if (!this[b]!!.containsKey(a)) this[b]!![a] = Pair(0.0, "")
        this[a]!![b] = Pair(this[a]!![b]!!.first + delta, reasonKey)
        this[b]!![a] = Pair(this[b]!![a]!!.first + delta, reasonKey)
    }

    fun addWill(a: String, delta: Double, reasonKey: String) {
        addMutuality(a, a, delta, reasonKey)
    }
}