package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.ui.*

import com.badlogic.gdx.utils.Align

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information

import ktx.scene2d.*


class ResourceInfoUI(skin: Skin?, var gameState: GameState) : Table(skin), KTable
{
    init
    {

// Assuming you have an instance of Information like this
        val information = Information(
            author = "Author",
            creationTime = 0,
            type = "resources",
            tgtTime = 0,
            tgtPlace = "Place",
            tgtResource = "Resource",
            amount = 10
        ).also { it.knownTo.add("Author"); it.credibility = 100 }

        val dataTable = scene2d.table {
            label("Resource") { setAlignment(Align.center) }
            label(information.tgtResource) { setAlignment(Align.center) }
            row()
            label("Amount") { setAlignment(Align.center) }
            label(information.amount.toString()) { setAlignment(Align.center) }
            setFillParent(true)
        }


        val authorLabel = label("Author: ${information.author}") { setAlignment(Align.center) }
        val creationTimeLabel = label("Creation Time: ${information.creationTime}") { setAlignment(Align.center) }
        val typeLabel = label("Type: ${information.type}") { setAlignment(Align.center) }
        val tgtTimeLabel = label("Target Time: ${information.tgtTime}") { setAlignment(Align.center) }
        val tgtPlaceLabel = label("Target Place: ${information.tgtPlace}") { setAlignment(Align.center) }
        row()
        val scrollPane = scrollPane {
            addActor(dataTable)
            setFillParent(true)
            setScrollingDisabled(false, false)
        }


    }


}