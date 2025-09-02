package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.*

class AlertPanelUI(var type: String, action: () -> Unit, val docList: Group, vararg params: String) : Table(), KTable {

    init {
        stack {
            it.size(500f, 75f)
            image("GradientBottom") {
                color = Color.BLACK
            }
            image("BackgroundNoiseHD")


            table {
                when (this@AlertPanelUI.type) {
                    "newInfo" -> image("icon_activity_66") {
                        it.size(36f).fill()
                    }

                    "moved" -> image("StatsGrunge") {
                        it.size(36f).fill()
                    }

                    "vital" -> image("icon_activity_105") {
                        it.size(36f).fill()
                    }

                    "will" -> image("icon_activity_105") {
                        it.size(36f).fill()
                    }

                    "accident" -> image("skull_white") {
                        it.size(36f).fill()
                    }

                    "hunger" -> image("HandLeftGrunge") {
                        it.size(36f).fill()
                    }

                    "thirst" -> image("HandLeftGrunge") {
                        it.size(36f).fill()
                    }

                    "meeting" -> image("ChatGrunge") {
                        it.size(36f).fill()
                    }

                    "apparatus" -> image("CogGrunge") {
                        it.size(36f).fill()
                    }

                    "alarm" -> image("ClockGrunge") {
                        it.size(36f).fill()
                    }

                    "interrupt" -> image("Help") {
                        it.size(36f).fill()
                    }

                    "budgetProposed" -> image("StatsGrunge") {
                        it.size(36f).fill()
                    }

                    "budgetResolved" -> image("StatsGrunge") {
                        it.size(36f).fill()
                    }

                    "budgetFailed" -> image("StatsGrunge") {
                        it.size(36f).fill()
                    }

                    "death" -> image("skull_white") {
                        it.size(36f).fill()
                    }

                    "electionFinished" -> image("StatsGrunge") {
                        it.size(36f).fill()
                    }

                    else -> image("Help") {
                        it.size(36f).fill()
                    }
                }
                when (this@AlertPanelUI.type) {
                    "moved" ->
                        label(ReadOnly.prop(this@AlertPanelUI.type).format(params[0], params[1]), "description") {
                            it.growX()
                            setFontScale(0.2f)
                            wrap = true
                            this@label.addListener(object : ClickListener() {
                                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                                    super.clicked(event, x, y)
                                    action()
                                }
                            })
                        }

                    "hunger", "thirst", "vital", "will", "accident", "death" ->
                        label(ReadOnly.prop("AlertPanelUI-" + this@AlertPanelUI.type).format(*params), "description") {
                            it.growX()
                            setFontScale(0.3f)
                            color = Color.RED
                            this@label.addListener(object : ClickListener() {
                                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                                    super.clicked(event, x, y)
                                    action()
                                }
                            })
                            this@AlertPanelUI.color = Color.RED
                            //Add blinking action to indicate urgency
                            this@AlertPanelUI.addAction(
                                Actions.forever(
                                    Actions.sequence(
                                        Actions.alpha(1f, 0.5f), Actions.alpha(0.5f, 0.5f)
                                    )
                                )
                            )
                        }

                    "interrupted" -> {
                        label(
                            ReadOnly.prop("AlertPanelUI-" + this@AlertPanelUI.type).format(params[0]),
                            "description"
                        ) {
                            it.growX()
                            setFontScale(0.2f)
                            wrap = true
                            this@label.addListener(object : ClickListener() {
                                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                                    super.clicked(event, x, y)
                                    action()
                                }
                            })
                        }
                    }

                    else ->
                        label(ReadOnly.prop("AlertPanelUI-" + this@AlertPanelUI.type).format(*params), "description") {
                            it.growX()
                            setFontScale(0.2f)
                            this@label.addListener(object : ClickListener() {
                                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                                    super.clicked(event, x, y)
                                    action()
                                }
                            })
                        }
                }
                button {
                    it.size(50f)
                    image("XGrunge") {
                        it.size(36f)
                    }
                    this@button.addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
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