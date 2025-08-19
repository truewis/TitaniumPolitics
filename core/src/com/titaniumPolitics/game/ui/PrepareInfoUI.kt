package com.titaniumPolitics.game.ui


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.gameActions.AddInfo
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.PrepareInfo
import com.titaniumPolitics.game.ui.meeting.AgendaBubbleUI
import com.titaniumPolitics.game.ui.widget.ActionSheetUI
import ktx.scene2d.*


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