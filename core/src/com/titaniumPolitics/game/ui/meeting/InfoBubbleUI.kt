package com.titaniumPolitics.game.ui.meeting

import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.ui.ApparatusInfoUI
import com.titaniumPolitics.game.ui.InformationViewUI
import com.titaniumPolitics.game.ui.ResourceInfoUI
import ktx.scene2d.*

class InfoBubbleUI(val info: Information) : Table(), KTable {
    init {
        with(info) {
            stack {
                it.size(WIDTH, HEIGHT).fill()
                if (tgtCharacter != "") {
                    //TODO: want to display the character's image here, but we don't have space for it.
//                    image(tgtCharacter) {
//                    }
                }
                table {
                    when (type) {
                        InformationType.ACTION -> {
                            image("HelpGrunge") {
                            }
                            label("Action:$tgtCharacter", "description") {
                                setFontScale(0.2f)
                            }
                        }

                        InformationType.RESOURCES -> {
                            image("LightGrunge") {
                            }
                            label("Resources:$tgtPlace", "description") {
                                setFontScale(0.2f)
                            }
                        }

                        InformationType.CASUALTY -> {
                            image("HeartGrunge") {
                            }
                            label("Casualty:$tgtPlace", "description") {
                                setFontScale(0.2f)
                            }

                        }

                        else -> {
                            label(type.name, "description") {
                                setFontScale(0.2f)
                            }
                        }
                    }
                }
                addListener(
                    object : ClickListener() {
                        override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                            super.clicked(event, x, y)
                            InformationViewUI.displayInformation(this@InfoBubbleUI.info)
                        }
                    }
                )
                //Add a blinking effect to draw attention.
                addAction(
                    Actions.forever(
                        Actions.sequence(
                            Actions.alpha(0.5f, 0.5f),
                            Actions.alpha(1f, 0.5f)
                        )
                    )
                )
            }
        }
    }

    companion object {
        const val WIDTH = 150f
        const val HEIGHT = 50f
    }
}