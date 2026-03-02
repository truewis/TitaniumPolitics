package com.titaniumPolitics.game.events

import com.titaniumPolitics.game.core.Character
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.Request
import com.titaniumPolitics.game.core.gameActions.Resign
import com.titaniumPolitics.game.debugTools.Logger

/**
 * Shared utilities for all party drama events.
 */
internal object PartyDramaUtils {

    /** Normalised will threshold below which an EMPLOYEE character will resign from their workplace party. */
    const val LEAVE_THRESHOLD = -0.7

    /**
     * Issues a system resign request (like Event_AlinaResign) for each EMPLOYEE member whose
     * will has fallen critically low.  Party leaders and the player character are never affected.
     */
    fun checkMembersLeaving(party: Party, gameState: GameState) {
        val partyHome = party.home ?: return
        party.realMembers.filter { char ->
            char != gameState.playerName &&
                party.leader != char &&
                gameState.characters[char]!!.type == Character.Type.EMPLOYEE &&
                gameState.getMutNorm(char, char) < LEAVE_THRESHOLD &&
                // Avoid duplicate resign requests for the same character in the same party.
                gameState.requests.values.none { req ->
                    !req.completed && char in req.issuedTo && req.action is Resign &&
                        (req.action as Resign).tgtPlace == partyHome
                }
        }.forEach { char ->
            // System request (issuedBy empty) – same pattern as Event_AlinaResign.
            Request(
                action = Resign(char, partyHome),
                issuedTo = hashSetOf(char),
            ).apply {
                gameState.requests[generateName()] = this
            }
            Logger.write(
                "$char has critically low morale and will resign from ${party.name}.",
                Logger.LogLevel.INFO
            )
        }
    }

    /** Returns true when the time step [a] → [b] crosses a day boundary. */
    fun isNewDay(a: Int, b: Int) = ReadOnly.toDays(b) > ReadOnly.toDays(a)
}
