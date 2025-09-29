package com.titaniumPolitics.game.ui


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Color.BLACK
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.widget.TitleLabel
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin


open class FloatingWindowUI : Window("", defaultSkin), KTable {
    val content = Table()
    val shadow = scene2d.image("TooltipShadow10p") {
        it.width = 450f
        it.height = 450f
        it.x = -50f
        it.y = -50f
        setColor(0f, 0f, 0f, 0.7f)
        touchable = Touchable.disabled//This is a shadow outside the tooltip
    }
    val closeButton = scene2d.button {
        label(ReadOnly.prop("closeUI"), "description") {
            setFontScale(0.3f)
        }

        addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                this@FloatingWindowUI.isVisible = false

            }
        })
    }

    val overTitleLabel = TitleLabel("", 0.5f, BLACK)

    override fun getTitleLabel(): Label {
        return overTitleLabel.label
    }

    init {
        setSize(350f, 350f)
        addActor(shadow)
        stack {
            it.grow()
            image("Stroke500x500") {
                setColor(0f, 0f, 0f, 1f)
            }
            table {
                add(this@FloatingWindowUI.overTitleLabel).growX()
                row()
                stack {
                    it.grow()
                    image("BlackPx")

                    image("NoiseBackground") {
                        setColor(0.5f, 0.5f, 0.5f, 0.1f)
                    }
                    image("PanelDottedShade700x700") {
                        setColor(0f, 0f, 0f, 1f)
                    }
                    table {
                        add(this@FloatingWindowUI.content).grow()
                    }
                }
            }
        }


    }

    override fun layout() {
        super.layout()
        shadow.setSize(width + 100, height + 100)
    }


}