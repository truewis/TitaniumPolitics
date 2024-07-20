package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.scene2d
import ktx.scene2d.*

class AlertPanelUI(var type: String, action: () -> Unit, val docList: Group) : Table(), KTable
{

    init
    {
        stack {
            it.size(400f, 75f)
            image("GradientBottom") {
                color = Color.BLACK
            }
            image("BackgroundNoiseHD")


            table {
                when (this@AlertPanelUI.type)
                {
                    "newInfo" -> image("icon_activity_66") {
                        it.size(36f).fill()
                    }

                    "vital" -> image("icon_activity_105") {
                        it.size(36f).fill()
                    }

                    "accident" -> image("skull_white") {
                        it.size(36f).fill()
                    }

                    "hunger" -> image("HandLeftGrunge") {
                        it.size(36f).fill()
                    }

                    "thrist" -> image("HandLeftGrunge") {
                        it.size(36f).fill()
                    }

                    "meeting" -> image("ChatGrunge") {
                        it.size(36f).fill()
                    }
                }
                label(ReadOnly.prop(this@AlertPanelUI.type), "trnsprtConsole") {
                    it.growX()
                    setFontScale(2f)
                    if (this@AlertPanelUI.type in listOf("hunger", "thrist", "vital"))
                        color = Color.RED
                    this@label.addListener(object : ClickListener()
                    {
                        override fun clicked(event: InputEvent?, x: Float, y: Float)
                        {
                            super.clicked(event, x, y)
                            action()
                        }
                    })
                }
                button {
                    it.size(50f)
                    image("XGrunge") {
                        it.size(36f)
                    }
                    this@button.addListener(object : ClickListener()
                    {
                        override fun clicked(event: InputEvent?, x: Float, y: Float)
                        {
                            super.clicked(event, x, y)
                            this@AlertPanelUI.docList.removeActor(this@AlertPanelUI)
                            if (this@AlertPanelUI.docList.children.isEmpty)
                                this@AlertPanelUI.isVisible = false
                        }

                    }
                    )

                }
            }
        }
    }


}