package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.gameActions.GameAction
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/*
* Everything agent knows about the world is an information.
* Information can be true or false. It can be shared with others or sold.
* Information can be used to make a decision. It can be used to blame or blackmail someone.
* */
@Serializable
data class Information(//If there is no author, it is a rumor.
    var author: String = "",
    var creationTime: Int = 0,
    var type: InformationType = InformationType.ACTION,
    var tgtTime: Int = 0,
    var tgtPlace: String = "",
    var tgtApparatus: String = "",
    var tgtCharacter: String = "",
    var amount: Int = 0,
    var action: GameAction? = null,
    var tgtParty: String = "",
    var auxParty: String = "",
    var resources: Resources = Resources()
) {
    //Do not copy the name. It is unique.
    constructor(info: Information) : this(
        info.author,
        info.creationTime,
        info.type,
        info.tgtTime,
        info.tgtPlace,
        info.tgtApparatus,
        info.tgtCharacter,
        info.amount,
        info.action,
        info.tgtParty,
        info.auxParty,
        info.resources
    )

    var name: String = ""
        private set

    init {
        if (name == "")
            generateName()
    }

    var life: Double = ReadOnly.const("InfoLifetime")//How long this information will last in seconds

    //We try to keep track of every aspect of our lives, but we can't. They eventually fade away.
    //But these characters has prepared this information. As far as rememberedBy is not empty, this information does not expire.
    val rememberedBy = hashSetOf<String>()

    var knownTo = hashSetOf<String>()

    //TODO: NPCs should do this instead.
    fun compatibility(other: Information): Double {//Two information with low compatibility fight each other.
        if (tgtCharacter == other.tgtCharacter && tgtCharacter != "") {//alibi
            if (tgtTime - other.tgtTime !in -6..6)//If time does not overlap
                return .0
            if (tgtPlace == other.tgtPlace && action == other.action) {//If exactly the same
                if (amount == other.amount)
                    return 0.0
                else//TODO: compatibility of unofficial resource transfer.
                    return 1.0
            }
            return .0 //TODO: the two information should be merged..?

        }
        if (type == InformationType.CASUALTY && other.type == InformationType.CASUALTY) {
            if (tgtTime - other.tgtTime !in -6..6)//If time does not overlap
                return 1.0
            if (tgtPlace != other.tgtPlace)
                return 1.0
            val tmp = max(amount, other.amount)
            if (tmp == 0)
                return 1.0
            if (min(amount, other.amount) == 0)
                return 0.0 //One info says zero, other says not.
            return 0.0/*TODO: continuous compatibility change*///min(amount, other.amount)/tmp.toDouble()
        }
        if (type == InformationType.ACTION && other.type == InformationType.ACTION) {
            //This case is dealt in 'alibi' case above.
            return 0.0
        }
        return 1.0
    }

    fun generateName(): String {
        if (this.name != "") {
            //println("Warning: name of an information is already set but you are trying to generate a new one. $name");
            return this.name

        }
        val name =
            "$author-$type-$creationTime-${
                Math.random().toString().substring(8)
            }"
        this.name = name
        return name
    }

    fun simpleDescription(): String {
        return when (type) {
            InformationType.ACTION -> {
                val actor = if (author.isNotEmpty()) author else "Someone"
                val actionStr = action!!::class.simpleName
                val target = if (tgtCharacter.isNotEmpty()) "to $tgtCharacter" else ""
                val place = if (tgtPlace.isNotEmpty()) "at $tgtPlace" else ""
                "$actor performed $actionStr $target $place"
            }

            InformationType.RESOURCES -> {
                val who = if (tgtCharacter.isNotEmpty()) tgtCharacter else "Someone"
                "$who has $amount resources at $tgtPlace"
            }

            InformationType.CASUALTY -> {
                val who = if (tgtCharacter.isNotEmpty()) tgtCharacter else "Someone"
                "$who suffered $amount casualties at $tgtPlace"
            }

            InformationType.LOST_RESOURCES -> {
                val who = if (tgtCharacter.isNotEmpty()) tgtCharacter else "Someone"
                "$who lost $amount resources at $tgtPlace"
            }

            InformationType.DAMAGED_APPARATUS -> {
                val apparatus = if (tgtApparatus.isNotEmpty()) tgtApparatus else "an apparatus"
                "$apparatus was damaged at $tgtPlace"
            }

            InformationType.APPARATUS_DURABILITY -> {
                val apparatus = if (tgtApparatus.isNotEmpty()) tgtApparatus else "an apparatus"
                "$apparatus durability is $amount at $tgtPlace"
            }
        }
    }
}

@Serializable
enum class InformationType {
    ACTION, RESOURCES, CASUALTY, LOST_RESOURCES, DAMAGED_APPARATUS, APPARATUS_DURABILITY
}