package com.capsulezero.game.core

import kotlinx.serialization.Serializable

@Serializable
class Command (var place: String, var action: String, var amount: Int){
    var executeTime = 0
    val compulsion = 0
    var issuedParty = ""
}