package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.gameActions.GameAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Request(
    //This is the action to be executed.
    //IMPORTANT! tgtCharacter param of action is not used, as we want to support issuing requests to multiple characters.
    var action: GameAction,
    var issuedTo: HashSet<String>/*If unspecified, anyone can finish this request.*/
)
{
    var name = ""
        private set
    var executeTime = 0//The time the requester want the action to be executed. If 0, it can be executed anytime.
    var issuedBy: HashSet<String> = hashSetOf() //If unspecified, it is a system request.

    @Transient
    var completed = false
    var onComplete = arrayListOf<() -> Unit>()

    fun generateName(): String
    {
        if (this.name != "")
        {
            //println("Warning: name of an information is already set but you are trying to generate a new one. $name");
            return this.name

        }
        val name =
            "${action.javaClass.simpleName}-${action.tgtPlace}-$executeTime-${
                Math.random().toString().substring(8)
            }"
        this.name = name
        return name
    }

    fun refresh(gState: GameState)
    {
        if (completed) return
        //This function is called every turn.
        //Each time one of the issuedTo completes this request,
        //Add the key of this request to finishedRequests of the character.
        gState.informations.filter { it.value.type == InformationType.ACTION && it.value.action == action && (issuedTo.isEmpty() || it.value.tgtCharacter in issuedTo) }
            .forEach {
                gState.characters[it.value.tgtCharacter]!!.finishedRequests.add(name)
            }
        val tgt = gState.informations.filter {
            it.value.knownTo.containsAll(issuedBy) && it.value.type == InformationType.ACTION && it.value.action == action && (issuedTo.isEmpty() || issuedTo.contains(
                it.value.tgtCharacter
            ))

        }
        if ((executeTime in gState.time - 3..gState.time + 3 || executeTime == 0))
            if (tgt.isNotEmpty())
            {
                //Mutuality increases.
                issuedBy.forEach { issuedBy ->
                    if (gState.characters[issuedBy]!!.trait.contains("psychopath"))
                        issuedTo.forEach { issuedTo ->
                            gState.setMutuality(issuedBy, issuedTo, ReadOnly.const("RequestFinishDeltaMutuality").toDouble()/3)
                        }
                    else
                    {
                        issuedTo.forEach { issuedTo ->
                            gState.setMutuality(issuedBy, issuedTo, ReadOnly.const("RequestFinishDeltaMutuality").toDouble())
                        }
                    }
                }
                tgt.forEach {
                    gState.setMutuality(
                        it.value.tgtCharacter,
                        it.value.tgtCharacter,
                        deltaWill(it.value.tgtCharacter, gState)
                    )
                }
                onComplete.forEach { it() }
                completed = true
            }

    }

    fun deltaWill(tgtChar: String, gState: GameState): Double
    {
        return if ((executeTime in gState.time - 3..gState.time + 3 || executeTime == 0))
            issuedBy.sumOf { gState.getMutuality(tgtChar, it) * ReadOnly.const("RequestFinishDeltaWill") }
        else
            0.0
    }

    override fun toString(): String
    {
        return "Request(action=$action, issuedTo=$issuedTo, name='$name', executeTime=$executeTime, issuedBy=$issuedBy, completed=$completed)"
    }
}