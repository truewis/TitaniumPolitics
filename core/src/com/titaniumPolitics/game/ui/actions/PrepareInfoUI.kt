package com.titaniumPolitics.game.ui.actions


import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.PrepareInfo
import com.titaniumPolitics.game.ui.InformationViewMode
import com.titaniumPolitics.game.ui.InformationViewUI
import com.titaniumPolitics.game.ui.widget.ActionSheetUI


class PrepareInfoUI(val gameState: GameState, actionCallback: (GameAction) -> Unit) :
    ActionSheetUI("PrepareInfoTitle", gameState, actionCallback) {
    init {
        content.add(InformationViewUI(gameState).apply {
            refresh("creationTime", InformationViewMode.SELECT) { keys ->
                actionCallback(
                    PrepareInfo(
                        gameState.playerName,
                        gameState.player.place.name
                    ).also {
                        it.newSetOfPrepInfoKeys = ArrayList(keys)
                    })
            }
        }).size(900f, 600f).fill()
    }


}