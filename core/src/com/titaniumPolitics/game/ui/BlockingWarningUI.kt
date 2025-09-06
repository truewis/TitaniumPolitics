package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.widget.TitleLabel
import ktx.scene2d.*

class BlockingWarningUI(var gameState: GameState) : Table(Scene2DSkin.defaultSkin), KTable {
    private var warningKey = ""
    private val titleLabel = TitleLabel("")
    private val descLabel = scene2d.label("", "docTitle") {
        setFontScale(0.3f)
        setAlignment(Align.center)
        wrap = true
        color = Color.WHITE
    }
    private val yesButton = scene2d.button {
        label(ReadOnly.prop("BlockingWarningUI-Proceed"), "docTitle") {
            setFontScale(0.4f)
            setAlignment(Align.center)
            color = Color.WHITE
        }
    }
    private val noButton = scene2d.button {
        label(ReadOnly.prop("BlockingWarningUI-Rescind"), "docTitle") {
            setFontScale(0.4f)
            setAlignment(Align.center)
            color = Color.WHITE
        }
    }
    private val optionTable: Table = table {}
    val bkg: Actor

    init {
        instance = this
        isVisible = false
        stack {
            it.grow()
            this@BlockingWarningUI.bkg = image("white-pixel") {
                color = Color.BLACK
            }
            table {
                stack {
                    it.size(800f, 400f).growY()
                    image("BlackPx")

                    image("NoiseBackground") {
                        setColor(1f, 1f, 1f, 0.1f)
                    }
                    image("PanelDottedShade700x700") {
                        setColor(0f, 0f, 0f, 1f)
                    }
                    image("Stroke500x500") {
                        setColor(0f, 0f, 0f, 1f)
                    }
                    table {
                        val PADDING = 3f
                        pad(PADDING)
                        stack {
                            it.size(350f - PADDING * 2, 50f) /*With the padding, they add up to 350.*/
                            add(this@BlockingWarningUI.titleLabel)
                        }
                        row()
                        add(this@BlockingWarningUI.descLabel).size(800f, 400f).growY()
                        row()
                        add(this@BlockingWarningUI.optionTable)
                    }
                }
            }
        }

    }

    //set visibility with fade in and out
    fun display(warningKey: String, callBack: (() -> Unit)?) {
        isVisible = true
        bkg.color = Color(0f, 0f, 0f, 0.6f) // Semi-transparent black background
        addAction(Actions.fadeIn(0f))// No fade in, just show it immediately, but still need to change alpha to 1f here.
        this@BlockingWarningUI.warningKey = warningKey
        val displayText = ReadOnly.prop("BlockingWarningUI-" + this@BlockingWarningUI.warningKey + "-desc")
        descLabel.setText(displayText)
        titleLabel.label.setText(ReadOnly.prop("BlockingWarningUI-Warning"))
        yesButton.clearListeners()
        yesButton.addListener(
            object : ClickListener() {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                    callBack?.invoke()
                    isVisible = false
                    yesButton.clearListeners()
                    noButton.clearListeners()
                }
            }
        )
        noButton.clearListeners()
        noButton.addListener(
            object : ClickListener() {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                    isVisible = false
                    yesButton.clearListeners()
                    noButton.clearListeners()
                }
            }
        )
        if (callBack != null) {
            optionTable.clear()
            optionTable.add(yesButton).size(200f, 75f).pad(20f)
            optionTable.add(noButton).size(200f, 75f).pad(20f)
        } else {
            optionTable.clear()
            optionTable.add(yesButton).size(200f, 75f).pad(20f)
        }

    }


    companion object {
        lateinit var instance: BlockingWarningUI
    }

}