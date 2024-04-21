package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

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
            image("panel") {
            }
            add(informationPane)

        }
        row()
        button {
            it.fill()
            label("Close") {
                setAlignment(Align.center)
                setFontScale(2f)
            }
            addListener(object : ClickListener()
            {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                {
                    this@ResourceInfoUI.isVisible = false
                }
            })
        }


    }

    fun refresh(information: Information)
    {
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

    companion object
    {
        //Singleton
        lateinit var instance: ResourceInfoUI
    }


}