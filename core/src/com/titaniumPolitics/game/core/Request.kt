package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.gameActions.GameAction
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Request(
    //IMPORTANT! tgtCharacter param of action is not used, as we want to support issuing requests to multiple characters.
    var action: GameAction,
    var issuedTo: HashSet<String>/*If unspecified, anyone can finish this request.*/
)
{
    var name = ""
        private set
    var executeTime = 0//If unspecified, it can be executed anytime.
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
        //If one of the issuedTo completes this request, call onComplete.
        val tgt = gState.informations.filter {
            it.value.knownTo.containsAll(issuedBy) && it.value.type == "action" && it.value.action == action && (issuedTo.isEmpty() || issuedTo.contains(
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
                            gState.setMutuality(issuedBy, issuedTo, 2.0)
                        }
                    else
                    {
                        issuedTo.forEach { issuedTo ->
                            gState.setMutuality(issuedBy, issuedTo, 7.0)
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
            issuedBy.sumOf { gState.getMutuality(tgtChar, it) * ReadOnly.const("requestFinishDeltaWill") }
        else
            0.0
    }
}