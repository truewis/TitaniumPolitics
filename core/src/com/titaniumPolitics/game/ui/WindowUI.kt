package com.titaniumPolitics.game.ui


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

import com.badlogic.gdx.utils.Align

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly

import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin


open class WindowUI(titleKey: String) : Table(defaultSkin), KTable
{
    val titleLabel = scene2d.label(ReadOnly.prop(titleKey), "trnsprtConsole") {
        setFontScale(4f)
        setAlignment(Align.center)
    }
    val content = Table()

    init
    {
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
                    addListener(object : ClickListener()
                    {
                        override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                        {
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