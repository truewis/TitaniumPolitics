package com.titaniumPolitics.game.core.gameActions

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.MutualityMatrix
import kotlinx.serialization.Serializable

@Serializable
data class AnnounceInfo(
    override val sbjCharacter: String, override val tgtPlace: String, var infoKey: String
) : GameAction() {
    constructor(
        sbjCharacter: String, tgtPlace: String, infoKey: String, gameState: GameState
    ) : this(sbjCharacter, tgtPlace, infoKey) {
        injectParent(gameState)
    }

    val info
        get() = parent.informations[infoKey]!!

    override fun execute() {
        //The information is known to all alive characters now.
        parent.informations[infoKey]!!.knownTo.addAll(parent.activeCharacters.keys)
        super.execute()
    }

    override fun deltaWill(): MutualityMatrix {
        val w = MutualityMatrix()
        //affect relation with the agenda author
        w.addWill(sbjCharacter, sbjCharObj.infoPreference(info), "AnnounceInfo")
        //The information is known to the characters in the meeting.
        val newsCharacters = parent.activeCharacters.filter { it.key !in info.knownTo }
        //Direct mutuality change caused by the information, if any.
        newsCharacters.forEach { listener ->
            //Affect mutualities based on inforPeference of the listener.
            w.addMutuality(listener.key, sbjCharacter, listener.value.infoPreference(info), "AnnounceInfo-listener")
        }
        return w
    }

    override fun isValid(): Boolean {
        if (sbjCharObj.currentMeeting != null) return false //Cannot announce when in a meeting.
        //Could only announce if I am a director of the interior division.
        if (!reason(
                parent.parties["interior"]!!.directorMembers.contains(sbjCharacter),
                "AnnounceInfo-Director"
            )
        ) return false
        //Check if the apparatus is functioning
        if (!reason(
                parent.places[tgtPlace]!!.apparatuses.none { it.name == "wiredBroadcastDevice" && it.netEfficiency > 0 },
                "AnnounceInfo-Apparatus"
            )
        ) return false
        if (!reason(isAnnounceable(info), "AnnounceInfo-Type")) return false
        return true //We are assuming that the information is always valid. Whether the information is effective or not is a different matter.
    }

    companion object {
        fun isAnnounceable(info: Information): Boolean {
            if (info.type !in listOf(InformationType.ACCIDENT, InformationType.CASUALTY)) return false
            return true
        }
    }

}
