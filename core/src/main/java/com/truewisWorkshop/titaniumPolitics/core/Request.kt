package com.titaniumPolitics.game.core

import com.titaniumPolitics.game.core.ReadOnly.IDTH
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.UnofficialResourceTransfer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * @param action This is the action to be executed. IMPORTANT! sbjCharacter param of action is used, as we don't support issuing requests to multiple characters, and sbjCharacter variable is immutable.
 * @param issuedTo If unspecified, anyone can finish this request.
 * @param issuedBy If unspecified, it is a system request.
 * @param executeTime The time the requester want the action to be executed. If 0, it can be executed anytime.
 */
@Serializable
class Request(
    var action: GameAction,
    var issuedTo: HashSet<String>,
    var issuedBy: HashSet<String> = hashSetOf(),
    var executeTime: Int? = null
) {
    var name = ""
        private set

    var completed = false

    //This is not serialized, which means that if the game is saved and loaded, onComplete will be lost.
//    @Transient
//    var onComplete = arrayListOf<() -> Unit>()

    fun generateName(): String {
        if (this.name != "") {
            //Logger.write("Warning: name of an information is already set but you are trying to generate a new one. $name", Logger.LogLevel.INFO);
            return this.name

        }
        val name =
            "${action.javaClass.simpleName}-${action.tgtPlace}-$executeTime-${
                Math.random().toString().substring(8)
            }"
        this.name = name
        return name
    }

    val proofOfExecutionInfos = arrayListOf<String>()
    fun onNewInfo(info: Information) {
        if ((executeTime == null || executeTime in info.tgtTime - IDTH..info.tgtTime + IDTH) && action.isProofOfWork(
                info
            )
        )
            proofOfExecutionInfos.add(info.name)
    }

    fun refresh(gState: GameState) {
        if (completed) return
        //This function is called every turn.
        //Each time one of the issuedTo completes this request,
        //Add the key of this request to finishedRequests of the character.
        val proofOfExecutionInfoObjs = proofOfExecutionInfos.map { gState.informations[it] }.filter { it != null }
            .map { it!! }
        proofOfExecutionInfoObjs.forEach {
            gState.characters[it.tgtCharacter]?.executedRequests?.add(name) //Works only for Action Information
            gState.characters[it.author]?.executedRequests?.add(name) //Works for other information which proves work.
        }
        val proofOfExecutionInfosHaveBeenShared = proofOfExecutionInfoObjs.filter {
            it.knownTo.containsAll(issuedBy)
        }
        if (proofOfExecutionInfosHaveBeenShared.isNotEmpty()) {
            //Mutuality increases.
            issuedBy.forEach { issuedBy ->
                if (gState.characters[issuedBy]!!.trait.contains("psychopath"))
                    issuedTo.forEach { issuedTo ->
                        gState.setMutuality(
                            issuedBy,
                            issuedTo,
                            ReadOnly.const("RequestFinishDeltaMutuality") / 3,
                            "RequestFinish-Psychopath"
                        )
                    }
                else {
                    issuedTo.forEach { issuedTo ->
                        gState.setMutuality(
                            issuedBy,
                            issuedTo,
                            ReadOnly.const("RequestFinishDeltaMutuality"),
                            "RequestFinish"
                        )
                    }
                }
            }
            proofOfExecutionInfosHaveBeenShared.forEach {
                it.tgtCharacter?.run {
                    gState.setMutuality(
                        this,
                        delta = deltaWill(this, gState),
                        reasonKey = "RequestFinishWill"
                    )
                }

            }
            completed = true
        }
    }

    fun deltaWill(tgtChar: String, gState: GameState): Double {
        return if ((executeTime in gState.time - 3..gState.time + 3 || executeTime == null))
            issuedBy.sumOf { gState.getMutNorm(tgtChar, it) * ReadOnly.const("RequestFinishDeltaWill") }
        else
            0.0
    }

    fun difficulty(gState: GameState): Double {
        var base = ReadOnly.const("RequestRejectAverageMutuality")
        if (issuedBy.isEmpty()) return base//System request, average difficulty.

        //If action is unofficialTransfer, difficulty is proportional to the value of the resources.
        if (action is UnofficialResourceTransfer && issuedTo.isNotEmpty()) {
            val urt = action as UnofficialResourceTransfer
            val value = gState.characters[issuedTo.first()]!!.itemValue(urt.resources)
            base += value
        }
        //If issuedBy contains my boss, it is easier.
        if (issuedBy.any { issuedByChar ->
                gState.parties.any {
                    it.value.leader == issuedByChar && it.value.members.intersect(
                        issuedTo
                    ).isNotEmpty()
                }
            }) {
            return base / 2
        }
        return base
    }

    override fun toString(): String {
        return "Request(action=$action, issuedTo=$issuedTo, name='$name', executeTime=$executeTime, issuedBy=$issuedBy, completed=$completed)"
    }
}