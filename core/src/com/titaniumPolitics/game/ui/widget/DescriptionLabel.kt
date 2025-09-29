package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Color.BLACK
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.rafaskoberg.gdx.typinglabel.TypingLabel
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.gameActions.GameAction
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.container
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.scene2d
import ktx.scene2d.scrollPane
import ktx.scene2d.stack

class DescriptionLabel(text: String) : Table(Scene2DSkin.defaultSkin), KTable {
    val label = TypingLabel(text, Scene2DSkin.defaultSkin, "description").apply {
        setAlignment(Align.left)
        setFontScale(0.2f)
        color = Color.WHITE
        setWrap(true)
        restart()
    }
    val cont = container(this@DescriptionLabel.label) {
        pad(10f)
    }

    init {
        stack {
            it.grow()
            image("Stroke500x500") {
                color = BLACK
            }
            container(ScrollPane(this@DescriptionLabel.cont).apply { setScrollingDisabled(true, false) }) {
                pad(5f)
            }
        }
    }

    /**
     * We set the preferred width of the container here because the label does not have a preferred width when wrap is enabled.
     * This way, the width of the container is set based on the width of the cell this entire widget is placed in.
     */
    override fun validate() {
        super.validate()
        cont.width(width - 40f)

    }
}