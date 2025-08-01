package com.titaniumPolitics.game.ui


import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable

import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState

import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.widget.WindowUI
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

import ktx.scene2d.*


class SystemUI(val gameState: GameState) : WindowUI("SystemUITitle")
{
    private val dataTable = scene2d.table {
        add(QuickSave(this@SystemUI.gameState))
        add(QuickLoad())
    }

    init
    {
        instance = this
        isVisible = false
        val informationPane = ScrollPane(dataTable)
        informationPane.setScrollingDisabled(true, false)
        content.add(informationPane).grow()


    }

    companion object
    {
        lateinit var instance: SystemUI
    }

}