package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.GameAction
import ktx.scene2d.KTable
import ktx.scene2d.image
import ktx.scene2d.label

class ActionSelectButton(skin: Skin, callback: (GameAction) -> Unit) : Button(skin, "default"), KTable {
    val actionIcon: Image
    val actionNameLabel: Label
    var availableActions: Set<String>? = null

    init {
        actionIcon = image("") {
            it.size(100f)
        }
        row()
        actionNameLabel = label("", "docTitle") { setFontScale(3f) }
        addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                ActionSelectUI.instance.isVisible = true
                availableActions?.also {
                    ActionSelectUI.instance.refreshList(it.toList())
                }
                ActionSelectUI.instance.actionCallback = {
                    ActionSelectUI.instance.isVisible = false
                    setLabel(it)
                    callback(it)
                }
            }
        })

    }

    fun setLabel(action: GameAction) {
        actionNameLabel.setText(ReadOnly.prop(action::class.simpleName!!))
    }
}