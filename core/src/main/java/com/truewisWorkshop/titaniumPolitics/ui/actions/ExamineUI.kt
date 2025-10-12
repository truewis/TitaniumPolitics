package com.titaniumPolitics.game.ui.actions

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.Examine
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.ui.ProgressBackgroundUI
import com.titaniumPolitics.game.ui.widget.ActionSheetUI
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import ktx.scene2d.*

class ExamineUI(var gameState: GameState, actionCallback: (GameAction) -> Unit) :
    ActionSheetUI("examineUI", gameState, actionCallback), KTable {
    val button1 = scene2d.button("document") {
        image("UserGrunge") {
            it.size(70f)
            this@button.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    if (this@button.isChecked)
                        this@ExamineUI.submitButton.refresh(
                            Examine(
                                this@ExamineUI.subject,
                                this@ExamineUI.tgtPlace,
                                InformationType.HUMAN_RESOURCES,
                                this@ExamineUI.gameState
                            )
                        )
                }
            }
            )
        }
        row()
        label(ReadOnly.prop("Examine-HR"), "docTitle") {
            setAlignment(Align.center)
            color = Color.WHITE
            setFontScale(0.15f)
        }
    }
    val button2 = scene2d.button("document") {
        image("CogGrunge") {
            it.size(70f)
            val action =
                this@button.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent?, actor: Actor?) {
                        if (this@button.isChecked)
                            this@ExamineUI.submitButton.refresh(
                                Examine(
                                    this@ExamineUI.subject,
                                    this@ExamineUI.tgtPlace,
                                    InformationType.APPARATUS,
                                    this@ExamineUI.gameState
                                )
                            )
                    }
                }
                )

        }
        row()
        label(ReadOnly.prop("Examine-Apparatus"), "docTitle") {
            setAlignment(Align.center)
            color = Color.WHITE
            setFontScale(0.15f)
        }
    }
    val button3 = scene2d.button("document") {
        image("TilesGrunge") {
            it.size(70f)
            this@button.addListener(object : ChangeListener() {
                override fun changed(event: ChangeEvent?, actor: Actor?) {
                    if (this@button.isChecked)
                        this@ExamineUI.submitButton.refresh(
                            Examine(
                                this@ExamineUI.subject,
                                this@ExamineUI.tgtPlace,
                                InformationType.RESOURCES,
                                this@ExamineUI.gameState
                            )
                        )
                }
            }
            )
        }
        row()
        label(ReadOnly.prop("Examine-Resources"), "docTitle") {
            setAlignment(Align.center)
            color = Color.WHITE
            setFontScale(0.15f)
        }
    }
    private val docList = ButtonGroup<Button>(button1, button2, button3)

    init {

        content.add(
            scene2d.table {
                container(this@ExamineUI.button1) {
                    size(100f, 100f)
                }
                container(this@ExamineUI.button2) {
                    size(100f, 100f)
                }
                container(this@ExamineUI.button3) {
                    size(100f, 100f)
                }
            }

        ).size(300f, 100f)
        content.row()
        content.add(submitButton).size(200f, 75f)
    }


}
