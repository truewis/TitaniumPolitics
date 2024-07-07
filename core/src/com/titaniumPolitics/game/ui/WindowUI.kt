package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

import com.badlogic.gdx.utils.Align

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly

import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin


class WindowUI : Table(defaultSkin), KTable
{
    val titleLabel = scene2d.label("Title", "trnsprtConsole") {
        setFontScale(4f)
    }
    val content = Table()

    init
    {
        stack {
            it.grow()
            image("GradientBottom")
            image("BackgroundNoise")
            table {
                add(this@WindowUI.titleLabel).growX().fillX()
                button {
                    it.fill()
                    label("Close") {
                        setAlignment(Align.center)
                        setFontScale(2f)
                    }
                    addListener(object : ClickListener()
                    {
                        override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                        {
                            this@WindowUI.isVisible = false
                        }
                    })
                }
                row()
                add().growY()
            }
            add(this@WindowUI.content)

        }
        row()


    }


}