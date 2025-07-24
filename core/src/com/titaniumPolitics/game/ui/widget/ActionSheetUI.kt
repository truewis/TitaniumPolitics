package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.button
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.scene2d
import ktx.scene2d.stack
import ktx.scene2d.table

//Action descriptions displayed under AvailableActionsUI.
open class ActionSheetUI(titleKey: String) : Table(Scene2DSkin.defaultSkin), KTable {
    val titleLabel = scene2d.label(ReadOnly.prop(titleKey), "docTitle") {
        setFontScale(1f)
        setAlignment(Align.center)
    }
    val onClose = ArrayList<() -> Unit>()
    val content = Table()

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