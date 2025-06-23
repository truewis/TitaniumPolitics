package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.ui.*

import com.badlogic.gdx.utils.Align

import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.ui.widget.WindowUI

import ktx.scene2d.*


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

            add(label("Author: ${information.author}") { setAlignment(Align.center) })
            row()
            add(label("Creation Time: ${information.creationTime}") { setAlignment(Align.center) })

            row()
            add(label("Type: ${information.type}") { setAlignment(Align.center) })
            row()
            add(label("Target Time: ${information.tgtTime}") { setAlignment(Align.center) })
            row()
            add(label("Target Place: ${information.tgtPlace}") { setAlignment(Align.center) })
            row()
            add(table {
                information.resources.forEach { (resourceName, resourceAmount) ->
                    label("$resourceName: $resourceAmount") { setAlignment(Align.center) }
                    row()
                }
            })
        }

    }

    companion object {
        //Singleton
        lateinit var instance: ResourceInfoUI
    }


}