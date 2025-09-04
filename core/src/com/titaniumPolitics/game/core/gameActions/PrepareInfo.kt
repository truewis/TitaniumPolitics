package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.InformationType
import kotlinx.serialization.Serializable

@Serializable
data class PrepareInfo(override val sbjCharacter: String, override val tgtPlace: String) : GameAction() {
    constructor(sbjCharacter: String, tgtPlace: String, gameState: GameState) : this(sbjCharacter, tgtPlace) {
        injectParent(gameState)
    }

    var newSetOfPrepInfoKeys = arrayListOf<String>()
    fun recommendKeys() {
        newSetOfPrepInfoKeys.clear()
        //If you have executed a command, you know the result. Add the result to the prepared information.
        parent.requests.values.filter { it.issuedBy.contains(sbjCharacter) }.forEach { command ->
            //If you have the corresponding action information.
            parent.informations.filter { it.value.knownTo.contains(sbjCharacter) && command.action.isProofOfWork(it.value) }
                .forEach {
                    newSetOfPrepInfoKeys.add(it.key)
                }
            //Order them in time and add the most recent five ones to the prepared information.
        }

        //If you have seen wrongdoings, you know the result. Add the result to the prepared information.
        parent.informations.filter { it.value.knownTo.contains(sbjCharacter) && it.value.type == InformationType.ACTION && it.value.action!!.javaClass.simpleName == "unofficialResourceTransfer" }
            .forEach {
                newSetOfPrepInfoKeys.add(it.key)
            }

        //If you hate someone in your party, prepare information about them which they hate.
        parent.parties.filter { (_, value) -> sbjCharacter in value.members }.forEach {
            val party = it.value
            party.members.filter { it != sbjCharacter && parent.getMutNorm(sbjCharacter, it) < -0.5 }
                .forEach { hatedChar ->
                    val hatedCharObj = parent.characters[hatedChar]!!
                    parent.informations.filter {
                        it.value.knownTo.contains(sbjCharacter) && hatedCharObj.infoPreference(
                            it.value
                        ) < 0
                    }
                        .forEach {
                            newSetOfPrepInfoKeys.add(it.key)
                        }
                }
        }

        //If you like some information, prepare it.
        parent.informations.filter { it.value.knownTo.contains(sbjCharacter) && sbjCharObj.infoPreference(it.value) > 1e-2 }
            .forEach {
                newSetOfPrepInfoKeys.add(it.key)
            }
    }

    override fun execute() {
        sbjCharObj.preparedInfoKeys.forEach {
            parent.informations[it]!!.rememberedBy -= sbjCharacter
        }

        sbjCharObj.preparedInfoKeys.clear()
        sbjCharObj.preparedInfoKeys.addAll(newSetOfPrepInfoKeys)

        //rememberedBy prevents the information from disappearing, as long as some character has it in their prepared information list.
        sbjCharObj.preparedInfoKeys.forEach {
            parent.informations[it]!!.rememberedBy += sbjCharacter
        }

        super.execute()
    }

    override fun isValid(): Boolean {
        return parent.informations.filter { it.value.knownTo.contains(sbjCharacter) }.keys.containsAll(
            newSetOfPrepInfoKeys
        )
    }

}