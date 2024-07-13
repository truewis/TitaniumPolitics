package com.titaniumPolitics.game.ui


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.*

import com.badlogic.gdx.utils.Align

import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin


open class FloatingWindowUI : Window("", defaultSkin), KTable
{
    val inContent = Table()
    val nameLabel = Label("", defaultSkin, "consoleWhite")
    val shadow = scene2d.image("TooltipShadow10p") {
        it.width = 450f
        it.height = 450f
        it.x = -50f
        it.y = -50f
        setColor(0f, 0f, 0f, 0.7f)
        touchable = Touchable.disabled//This is a shadow outside the tooltip
    }

    init
    {
        setSize(350f, 350f)
        addActor(shadow)
        nameLabel.apply {
            setFontScale(2f)
            setAlignment(Align.center)
        }
        add(this@FloatingWindowUI.nameLabel).growX()
        row()
        image("Stroke5pxHorizontal") {
            it.size(350f, 5f).fill()
            color = Color.WHITE
        }
        row()
        stack {
            it.grow()
            image("BlackPx")

            image("NoiseBackground") {
                setColor(1f, 1f, 1f, 0.1f)
            }
            image("PanelDottedShade700x700") {
                setColor(0f, 0f, 0f, 1f)
            }
            table {
                add(this@FloatingWindowUI.inContent).grow()
            }
        }


    }

    override fun layout()
    {
        super.layout()
        shadow.setSize(width + 100, height + 100)
    }


}