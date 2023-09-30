package com.capsulezero.game.core

import kotlinx.serialization.Serializable

@Serializable
class Command (var place: String, var action: String, var amount: Int){

}