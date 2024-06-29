package com.titaniumPolitics.game.core.gameActions

import kotlinx.serialization.Serializable

@Serializable
class PrepareInfo(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{
    var newSetOfPrepInfoKeys = arrayListOf<String>()
    fun recommendedKeys()
    {
        newSetOfPrepInfoKeys.clear()
        //If you have executed a command, you know the result. Add the result to the prepared information.
        parent.requests.values.filter { it.issuedBy.contains(tgtCharacter) }.forEach { command ->
            //If you have the corresponding action information.
            parent.informations.filter { it.value.knownTo.contains(tgtCharacter) && it.value.type == "action" && it.value.action!!.javaClass.simpleName == command.action.javaClass.simpleName }
                .forEach {
                    newSetOfPrepInfoKeys.add(it.key)
                }
        }

        //If you have done wrongdoings, you know the result. Add the result to the prepared information.
        parent.informations.filter { it.value.knownTo.contains(tgtCharacter) && it.value.type == "action" && it.value.action!!.javaClass.simpleName == "unofficialResourceTransfer" }
            .forEach {
                newSetOfPrepInfoKeys.add(it.key)
            }
    }

    override fun execute()
    {
        tgtCharObj.preparedInfoKeys.clear()
        tgtCharObj.preparedInfoKeys.addAll(newSetOfPrepInfoKeys)

        super.execute()
    }

    override fun isValid(): Boolean
    {
        return parent.informations.filter { it.value.knownTo.contains(tgtCharacter) }.keys.containsAll(
            newSetOfPrepInfoKeys
        )
    }

}