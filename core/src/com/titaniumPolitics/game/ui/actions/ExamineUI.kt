package com.titaniumPolitics.game.ui.actions

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
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
    ActionSheetUI(ReadOnly.prop("examineUI"), gameState, actionCallback), KTable {
    private val docList = HorizontalGroup()
    val invalidReasonLabel = scene2d.label("", "docTitle") {
        setAlignment(Align.center)
        color = Color.RED
        setFontScale(0.5f)
    }

    init {
        docList.grow()
        docList.addActor(scene2d.container {
            button("document") {
                image("UserGrunge") {
                    it.size(70f)
                    val action = Examine(
                        this@ExamineUI.subject,
                        this@ExamineUI.tgtPlace,
                        InformationType.HUMAN_RESOURCES
                    ).also {
                        it.injectParent(this@ExamineUI.gameState)
                    }
                    if (!action.isValid()) {
                        this@button.isDisabled = true
                        this@button.addListener(SimpleTextTooltipUI(action.invalidReason))
                    }
                    this@button.addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent?, actor: Actor?) {
                            actionCallback(
                                action
                            )
                            ProgressBackgroundUI.Companion.instance.setVisibleWithFade(true, "Examine")
                            this@ExamineUI.onClose.forEach { it() }
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
            size(100f, 100f)
        })
        docList.addActor(scene2d.container {
            button("document") {
                image("CogGrunge") {
                    it.size(70f)
                    val action = Examine(
                        this@ExamineUI.subject,
                        this@ExamineUI.tgtPlace,
                        InformationType.APPARATUS
                    ).also {
                        it.injectParent(this@ExamineUI.gameState)
                    }
                    if (!action.isValid()) {
                        this@button.isDisabled = true
                        this@button.addListener(SimpleTextTooltipUI(action.invalidReason))
                    }
                    this@button.addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent?, actor: Actor?) {
                            actionCallback(
                                action
                            )
                            ProgressBackgroundUI.Companion.instance.setVisibleWithFade(true, "Examine")
                            this@ExamineUI.onClose.forEach { it() }
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
            size(100f, 100f)

        })
        docList.addActor(scene2d.container {
            button("document") {
                image("TilesGrunge") {
                    it.size(70f)
                    val action = Examine(
                        this@ExamineUI.subject,
                        this@ExamineUI.tgtPlace,
                        InformationType.RESOURCES
                    ).also {
                        it.injectParent(this@ExamineUI.gameState)
                    }
                    if (!action.isValid()) {
                        this@button.isDisabled = true
                        this@button.addListener(SimpleTextTooltipUI(action.invalidReason))
                    }
                    this@button.addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent?, actor: Actor?) {
                            actionCallback(
                                action
                            )
                            ProgressBackgroundUI.Companion.instance.setVisibleWithFade(true, "Examine")
                            this@ExamineUI.onClose.forEach { it() }
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
            size(100f, 100f)
        })
        content.add(docList).size(300f, 100f)
    }


}