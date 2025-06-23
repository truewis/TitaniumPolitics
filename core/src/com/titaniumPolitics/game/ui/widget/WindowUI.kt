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

open class WindowUI(titleKey: String) : Table(Scene2DSkin.defaultSkin), KTable {
    val titleLabel = scene2d.label(ReadOnly.prop(titleKey), "docTitle") {
        setFontScale(1f)
        setAlignment(Align.center)
    }
    val onClose = ArrayList<() -> Unit>()
    val content = Table()

    init {
        stack {
            it.grow()
            image("GradientBottom") {
                color = Color.BLACK
            }
            image("BackgroundNoiseHD")
            table {
                add(this@WindowUI.titleLabel).growX().fillX()
                button {
                    it.fill()
                    it.size(70f)
                    image("XGrunge")
                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            this@WindowUI.onClose.forEach { it() }
                            this@WindowUI.isVisible = false
                        }
                    })
                }

                row()
                add(this@WindowUI.content).colspan(2).grow()
            }
        }


    }


}