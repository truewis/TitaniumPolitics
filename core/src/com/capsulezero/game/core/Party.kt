package com.capsulezero.game.core

import kotlinx.serialization.Serializable
@Serializable
class Party {
    var name = ""
    var leader = ""
    var members = hashSetOf<String>()
    var anonymousMembers  = 0
    fun individualMutuality(name:String) = 0//TODO: implement this.
    var resources = hashMapOf<String, Int>()
    val integrity: Int
        get() = 0
}
