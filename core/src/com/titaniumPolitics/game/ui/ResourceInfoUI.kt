package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.widget.InformationSourceUI
import com.titaniumPolitics.game.ui.widget.ResourceDisplayUI
import com.titaniumPolitics.game.ui.widget.WindowUI
import ktx.scene2d.label
import ktx.scene2d.table


class ResourceInfoUI : WindowUI("ResourceInfoTitle") {
    private val dataTable = Table()

    init {
        isVisible = false
        instance = this
        val informationPane = ScrollPane(dataTable)
        informationPane.setScrollingDisabled(false, false)
        content.add(informationPane).grow()


    }

    fun refresh(information: Information) {
        dataTable.clear()
        dataTable.apply {
            add(label("Target Place: ${ReadOnly.placeProp(information.tgtPlace)}") { setAlignment(Align.center) })
            row()
            add(ResourceDisplayUI(information.resources)).size(500f, 300f).fill()
            row()
            add(InformationSourceUI(information)).fill()
        }

    }

    companion object {
        //Singleton
        lateinit var instance: ResourceInfoUI
    }


}