package com.capsulezero.game.core

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

@Serializable
class Information(//If there is no author, it is a rumor.
    var author: String = "", var creationTime: Int = 0, var type: String = "", var tgtTime: Int = 0,
    var tgtPlace: String = "", var tgtApparatus: String = "", var tgtCharacter: String = "",
    var tgtResource: String = "", var amount: Int = 0, var action: String = ""
) {
    constructor(info: Information) : this(info.author, info.creationTime, info.type, info.tgtTime, info.tgtPlace, info.tgtApparatus, info.tgtCharacter, info.tgtResource, info.amount, info.action)

    var publicity = hashMapOf<String, Int>()//Number of people knowing this info in the party.
    var credibility: Int = 0
    var life: Int = 100//How long this information will last.
    //We try to keep track of every aspect of our lives, but we can't. They eventually fade away.
    var knownTo = hashSetOf<String>()
    var supporters = hashSetOf<String>()
    fun compatibility(other: Information): Double {//Two information with low compatibility fight each other.
        if(tgtCharacter==other.tgtCharacter && tgtCharacter!=""){//alibi
            if(tgtTime - other.tgtTime !in -6..6)//If time does not overlap
                return .0
            if(tgtPlace == other.tgtPlace && action==other.action) {//If exactly the same
                if (amount == other.amount)
                    return 0.0
                else//TODO: compatibility of unofficial resource transfer.
                    return 1.0
            }
            return .0 //TODO: the two information should be merged..?

        }
        if(type =="casualty" && other.type=="casualty"){
            if(tgtTime - other.tgtTime !in -6..6)//If time does not overlap
                return 1.0
            if (tgtPlace != other.tgtPlace)
                return 1.0
            val tmp = max(amount, other.amount)
            if(tmp==0)
                return 1.0
            if(min(amount, other.amount)==0)
                return 0.0 //One info says zero, other says not.
            return 0.0/*TODO: continuous compatibility change*///min(amount, other.amount)/tmp.toDouble()
        }
        if(type =="action" && other.type=="action"){
            //This case is dealt in 'alibi' case above.
            return 0.0
        }
    return 1.0
    }
    fun generateName():String{
        return "$author-$type-$creationTime-$tgtTime-$tgtPlace-$tgtApparatus-$tgtCharacter-$tgtResource-$amount-$action"
    }
}