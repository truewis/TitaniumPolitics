package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.ui.IActionUI
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.label
import ktx.scene2d.scene2d
import ktx.scene2d.stack
import ktx.scene2d.table

//Action descriptions displayed under AvailableActionsUI.
//Always used with ActionUI interface.
open class ActionSheetUI(titleKey: String, gameState: GameState) : Table(Scene2DSkin.defaultSkin), KTable, IActionUI {
    val titleLabel = scene2d.label(ReadOnly.prop(titleKey), "docTitle") {
        setFontScale(1f)
        setAlignment(Align.center)
        color = Color.BLACK
    }
    val onClose = ArrayList<() -> Unit>()
    val content = Table()
    override var actionCallback: (GameAction) -> Unit = {}
    override var subject = gameState.playerName
    override var tgtPlace = gameState.player.place.name

    init {
        stack {
            it.grow()
            table {
                add(this@ActionSheetUI.titleLabel).growX().fillX()
                row()
                add(this@ActionSheetUI.content).grow()
            }
        }


    }


}