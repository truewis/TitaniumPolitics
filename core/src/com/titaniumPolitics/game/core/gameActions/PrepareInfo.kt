package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.InformationType
import kotlinx.serialization.Serializable

@Serializable
class PrepareInfo(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{
    var newSetOfPrepInfoKeys = arrayListOf<String>()
    fun recommendedKeys()
    {
        newSetOfPrepInfoKeys.clear()
        //If you have executed a command, you know the result. Add the result to the prepared information.
        parent.requests.values.filter { it.issuedBy.contains(sbjCharacter) }.forEach { command ->
            //If you have the corresponding action information.
            parent.informations.filter { it.value.knownTo.contains(sbjCharacter) && it.value.type == InformationType.ACTION && it.value.action!!.javaClass.simpleName == command.action.javaClass.simpleName }
                .forEach {
                    newSetOfPrepInfoKeys.add(it.key)
                }
        }

        //If you have seen wrongdoings, you know the result. Add the result to the prepared information.
        parent.informations.filter { it.value.knownTo.contains(sbjCharacter) && it.value.type == InformationType.ACTION && it.value.action!!.javaClass.simpleName == "unofficialResourceTransfer" }
            .forEach {
                newSetOfPrepInfoKeys.add(it.key)
            }
    }

    override fun execute()
    {
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

    override fun isValid(): Boolean
    {
        return parent.informations.filter { it.value.knownTo.contains(sbjCharacter) }.keys.containsAll(
            newSetOfPrepInfoKeys
        )
    }

}