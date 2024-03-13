package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.Information

class PrepareInfo(override val tgtCharacter: String, override val tgtPlace: String) : GameAction()
{
    var newSetOfPrepInfoKeys = arrayListOf<String>()

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