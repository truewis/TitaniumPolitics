package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.ui.*

import com.badlogic.gdx.utils.Align

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information

import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin


class ResourceInfoUI : Table(defaultSkin), KTable
{
    private val dataTable = Table()

    init
    {
        isVisible = false
        instance = this
        val informationPane = ScrollPane(dataTable)
        informationPane.setScrollingDisabled(false, false)
        stack {
            it.grow()
            image("capsuleDevLabel1") {
            }
            add(informationPane)

        }


    }

    fun refresh(information: Information)
    {
        dataTable.clear()
        dataTable.apply {
            val authorLabel = label("Author: ${information.author}") { setAlignment(Align.center) }
            row()
            val creationTimeLabel = label("Creation Time: ${information.creationTime}") { setAlignment(Align.center) }
            row()
            val typeLabel = label("Type: ${information.type}") { setAlignment(Align.center) }
            row()
            val tgtTimeLabel = label("Target Time: ${information.tgtTime}") { setAlignment(Align.center) }
            row()
            val tgtPlaceLabel = label("Target Place: ${information.tgtPlace}") { setAlignment(Align.center) }
            row()
            table {
                information.resources.forEach { (resourceName, resourceAmount) ->
                    val resourceLabel = label("$resourceName: $resourceAmount") { setAlignment(Align.center) }
                    row()
                }
            }
        }

    }

    companion object
    {
        //Singleton
        lateinit var instance: ResourceInfoUI
    }


}