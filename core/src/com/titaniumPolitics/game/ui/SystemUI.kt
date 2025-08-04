package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.widget.WindowUI
import ktx.scene2d.scene2d
import ktx.scene2d.table


class SystemUI(val gameState: GameState) : WindowUI("SystemUITitle") {
    private val dataTable = scene2d.table {
        add(QuickSave(this@SystemUI.gameState))
        row()
        add(QuickLoad())
    }

    init {
        instance = this
        isVisible = false
        val informationPane = ScrollPane(dataTable)
        informationPane.setScrollingDisabled(true, false)
        content.add(informationPane).grow()


    }

    companion object {
        lateinit var instance: SystemUI
    }

}