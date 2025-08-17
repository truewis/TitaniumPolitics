package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.*

open class WindowUI(titleKey: String) : Table(Scene2DSkin.defaultSkin), KTable {
    val titleLabel = TitleLabel(ReadOnly.prop(titleKey)).apply {
        label.setAlignment(Align.center)
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
                stack { cell ->
                    cell.size(1920f, 70f)
                    add(this@WindowUI.titleLabel)
                    container {
                        right()
                        size(70f)
                        button {
                            image("XGrunge")
                            addListener(object : ClickListener() {
                                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                                    this@WindowUI.onClose.forEach { it() }
                                    this@WindowUI.isVisible = false
                                }
                            })
                        }
                    }
                }

                row()
                add(this@WindowUI.content).grow()
            }
        }


    }


}