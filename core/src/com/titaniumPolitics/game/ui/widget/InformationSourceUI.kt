package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.graphics.Color.LIGHT_GRAY
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.Party
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.container
import ktx.scene2d.image
import ktx.scene2d.label

class InformationSourceUI(info: Information) : Table(Scene2DSkin.defaultSkin), KTable {
    init {
        align(Align.topLeft)
        label(
            ReadOnly.prop("InformationSourceUI")
                .format(ReadOnly.charProp(info.author ?: "Someone"), GameState.formatTime(info.creationTime)),
            "docTitle"
        ) {
            it.fill()
            color = LIGHT_GRAY
            setAlignment(Align.left)
            setFontScale(0.2f)
        }
    }
}