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
        val buttonSize = 100f
        button {
            it.fill()
            it.size(buttonSize)
            stack {
                it.size(50f)
                it.fill()
                image("CrownGrunge") {
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
        button {
            it.fill()
            it.size(buttonSize)
            stack {
                it.size(50f)
                it.fill()
                image("icon_app_140") {
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
        button {
            it.fill()
            it.size(buttonSize)
            stack {
                it.size(50f)
                it.fill()
                image("icon_app_119") {
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

        button {
            it.fill()
            it.size(buttonSize)
            image("icon_app_195") {
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
            Gdx.app.postRunnable { calendarLabel.setText((gameState.day).toString()) } // Current Date.
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
        NewAgendaUI.instance.isVisible = false
    }


}
