package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.label

class SubmitButton(private var action: GameAction? = null, var actionCallback: (GameAction) -> Unit) :
    Button(Scene2DSkin.defaultSkin),
    KTable {
    private var tooltip: ActionTooltipUI = ActionTooltipUI("Wait")
    var checkValidity = true
        set(value) {
            field = value
            action?.let { refresh(it) }
        }

    init {
        isDisabled = true
        isVisible = false
        addListener(tooltip)
        label(ReadOnly.prop("SubmitButton-Submit"), "docTitle") {
            color = Color.WHITE
            setAlignment(Align.center)
            setFontScale(0.5f)
        }
        addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent?, actor: Actor?) {
                actionCallback(
                    action!!
                )
            }
        })
    }

    fun refresh(action: GameAction) {
        removeListener(tooltip)
        tooltip = ActionTooltipUI(action::class.simpleName!!)
        addListener(tooltip)
        isVisible = true
        if (checkValidity) {
            this.isDisabled = !action.isValid()
            if (this.isDisabled) {
                this.tooltip.displayInvalidReason(action.invalidReason)
            }
        } else {
            this.isDisabled = false
        }
        this.action = action

    }

    fun refresh() {
        action?.let { refresh(it) }
    }
}