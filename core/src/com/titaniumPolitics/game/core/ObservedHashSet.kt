package com.titaniumPolitics.game.core

import kotlinx.serialization.Serializable

@Serializable
class ObservedHashSet<E>(val event: (E) -> Unit) : HashSet<E>() {
    override fun add(e: E): Boolean {
        event(e)
        return super.add(e)
    }

    override fun addAll(elements: Collection<E>): Boolean {
        elements.forEach { event(it) }
        return super.addAll(elements)
    }
}