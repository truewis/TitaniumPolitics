package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

import com.badlogic.gdx.utils.Align

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information

import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin


class ApparatusInfoUI : WindowUI("ApparatusInfoTitle")
{
    private val dataTable = Table()

    init
    {
        isVisible = false
        instance = this
        val informationPane = ScrollPane(dataTable)
        informationPane.setScrollingDisabled(false, false)
        content.add(informationPane).grow()


    }

    fun refresh(information: Information)
    {
        dataTable.clear()
        dataTable.apply {

            add(label("Author: ${information.author}") { setAlignment(Align.center) })
            row()
            add(label("Creation Time: ${information.creationTime}") { setAlignment(Align.center) })

            row()
            add(label("Apparatus Name: ${information.tgtApparatus}") { setAlignment(Align.center) })
            row()
            add(label("Durability: ${information.amount}") { setAlignment(Align.center) })
        }

    }

    companion object
    {
        //Singleton
        lateinit var instance: ApparatusInfoUI
    }


}