package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.map.MapUI
import com.titaniumPolitics.game.ui.map.PlaceSelectionUI
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class AssistantUI(gameState: GameState) : Table(defaultSkin), KTable
{

    init
    {
        val PoliticiansViewButton = button {
            it.fill()
            stack {
                it.size(50f)
                it.fill()
                image("raincoat-icon") {
                }
            }
            addListener(object : ClickListener()
            {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                {
                    if (PoliticiansInfoUI.instance.isVisible) this@AssistantUI.closeAll()
                    else
                    {
                        PoliticiansInfoUI.instance.isVisible = !PoliticiansInfoUI.instance.isVisible
                        PoliticiansInfoUI.instance.refresh(gameState)
                    }
                }
            }
            )
        }
        row()
        val informationViewButton = button {
            it.fill()
            stack {
                it.size(50f)
                it.fill()
                image("binder-file-icon") {
                }
            }
            addListener(object : ClickListener()
            {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                {
                    if (InformationViewUI.instance.isVisible) this@AssistantUI.closeAll()
                    else
                    {
                        InformationViewUI.instance.isVisible = !InformationViewUI.instance.isVisible
                        InformationViewUI.instance.refresh(gameState, "creationTime")
                    }
                }
            }
            )
        }
        row()
        val calendarLabel = Label("0", defaultSkin, "trnsprtConsole").also {
            it.setFontScale(3f)
            it.setAlignment(Align.center, Align.center)
        }
        val calendarButton = button {
            it.fill()
            stack {
                it.size(50f)
                it.fill()
                image("calendar-blank-line-icon") {
                }
                add(calendarLabel)
            }
            addListener(object : ClickListener()
            {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                {
                    //Open Calendar UI
                    if (HeadUpInterface.instance.calendarUI.isVisible) this@AssistantUI.closeAll()
                    else
                    {
                        HeadUpInterface.instance.calendarUI.refresh(gameState)
                        HeadUpInterface.instance.calendarUI.isVisible = !HeadUpInterface.instance.calendarUI.isVisible
                    }
                }
            }
            )
        }
        add(ClockUI(gameState)).growX().align(Align.left)

        row()

        button { cell ->
            cell.fill()
            image("map-icon") {
                it.size(50f)
            }
            addListener(object : ClickListener()
            {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                {

                    //Open Map UI
                    if (HeadUpInterface.instance.mapUI.isVisible) this@AssistantUI.closeAll()
                    else
                    {
                        HeadUpInterface.instance.mapUI.refresh()
                        HeadUpInterface.instance.mapUI.isVisible = !HeadUpInterface.instance.mapUI.isVisible
                    }
                }
            }
            )
        }
        add(PlaceAndCoordUI(gameState)).growX()


        gameState.timeChanged += { _, y ->
            Gdx.app.postRunnable { calendarLabel.setText((y / 48).toString()) } // Current Date.
        }
    }

    fun closeAll()
    {
        InformationViewUI.instance.isVisible = false
        HeadUpInterface.instance.mapUI.isVisible = false
        HeadUpInterface.instance.calendarUI.isVisible = false
        HeadUpInterface.instance.politiciansInfoUI.isVisible = false
        PlaceSelectionUI.instance.isVisible = false
        ResourceInfoUI.instance.isVisible = false
        ApparatusInfoUI.instance.isVisible = false
        HumanResourceInfoUI.instance.isVisible = false
        ResourceTransferUI.instance.isVisible = false
    }


}
