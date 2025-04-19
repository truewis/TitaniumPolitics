package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import kotlinx.serialization.Serializable

@Serializable
class Wait(override val sbjCharacter: String, override val tgtPlace: String) : GameAction()
{

    var count = ReadOnly.constInt("LongWaitTime")
    override fun execute()
    {
        parent.timeChanged.add(this::longWait)
        super.execute()
    }

    private fun longWait(o: Int, n: Int)
    {

        if (n - o == 1)
        {
            count--
            sbjCharObj.frozen += 1

        }
        oldInfoKeys = newInfoKeys
        newInfoKeys = parent.informations.filter {
            it.value.knownTo.contains(sbjCharacter) && it.value.creationTime >= parent.time - 1
        }.keys.toHashSet()
        if (count == 0 || waitInterruptCondition())
            parent.timeChanged -= this::longWait

    }

    var oldInfoKeys = hashSetOf<String>()
    var newInfoKeys = hashSetOf<String>()

    private fun waitInterruptCondition(): Boolean
    {
        //Interrupt if a character performs an action other than wait in this place.
        return (oldInfoKeys - newInfoKeys).any {
            val info = parent.informations[it]!!
            info.tgtPlace == tgtPlace && info.author != sbjCharacter &&
                    !(info.type == InformationType.ACTION && info.action is Wait)
        }
    }

    override fun isValid(): Boolean
    {
        return true
    }

}