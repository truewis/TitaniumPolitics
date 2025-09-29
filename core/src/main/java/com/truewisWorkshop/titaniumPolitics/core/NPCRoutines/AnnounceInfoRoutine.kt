package com.titaniumPolitics.game.core.NPCRoutines

import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.gameActions.AnnounceInfo
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.PrepareInfo
import com.titaniumPolitics.game.core.gameActions.Wait
import com.titaniumPolitics.game.debugTools.Logger
import kotlinx.serialization.Serializable

/**
 * Routine to announce information to the public.
 * The NPC will move to the nearest place with a wiredBroadcastDevice with netEfficiency > 0.
 * If there is no such place, the routine will fail.
 * If the NPC is already at home, it will prepare the information to announce.
 * If the NPC is not at home, it will move to home first.
 */
@Serializable
class AnnounceInfoRoutine(val infoKey: String) : Routine() {


    override fun newRoutineCondition(name: String, place: String, subroutines: List<Routine>): Routine? {
        nearestPlaceWithApparatus(place, gState)?.let { announcePl ->
            //If AnnounceInfo is in availableActions, announce it myself. Otherwise, fail the routine.
            if ("AnnounceInfo" in GameEngine.availableActions(gState, announcePl, name)) {
                if (place != announcePl) {
                    return MoveRoutine(announcePl)//Add a move routine with higher priority.
                } else {
                    return null //I am already at the place with apparatus, just execute the action.
                }
            } else {
                failed()
            }
        }
        return null

    }

    override fun execute(name: String, place: String): GameAction {
        if (place == nearestPlaceWithApparatus(place, gState)) {
            AnnounceInfo(name, place, infoKey, gState).also {
                if (it.isValid()) {
                    success()
                    return it
                }
            }
        }
        failed()
        return Wait(name, place)
    }

    companion object {
        fun nearestPlaceWithApparatus(place: String, gState: GameState) = gState.places.values.filter {
            it.apparatuses.any {
                it.name == "wiredBroadcastDevice" && it.netEfficiency > 0
            }
        }
            .minByOrNull { gState.places[place]!!.distanceTo(it.name) ?: Int.MAX_VALUE }?.name
    }
}