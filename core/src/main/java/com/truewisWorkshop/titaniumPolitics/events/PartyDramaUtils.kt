package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Character
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.debugTools.Logger

/**
 * Shared utilities for all party drama events.
 */
internal object PartyDramaUtils {

    /** Normalised will threshold below which an EMPLOYEE character leaves their workplace party. */
    const val LEAVE_THRESHOLD = -0.8

    /**
     * Removes EMPLOYEE members whose will has fallen critically low from [party].
     * Party leaders and the player character are never removed.
     */
    fun checkMembersLeaving(party: Party, gameState: GameState) {
        party.realMembers.filter { char ->
            char != gameState.playerName &&
                party.leader != char &&
                gameState.characters[char]!!.type == Character.Type.EMPLOYEE &&
                gameState.getMutNorm(char, char) < LEAVE_THRESHOLD
        }.forEach { char ->
            party.removeMember(char)
            Logger.write(
                "$char leaves ${party.name} due to workplace drama.",
                Logger.LogLevel.INFO
            )
        }
    }

    /** Returns true when the time step [a] → [b] crosses a day boundary. */
    fun isNewDay(a: Int, b: Int) = ReadOnly.toDays(b) > ReadOnly.toDays(a)
}
