package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class InformationViewUI : Table(defaultSkin), KTable
{
    private val informationTable = Table()

    init
    {
        isVisible = false
        instance = this
        val informationPane = ScrollPane(informationTable)
        informationPane.setScrollingDisabled(false, false)
        stack {
            it.grow()
            image("capsuleDevLabel1") {
            }
            add(informationPane)

        }

    }

    fun populateInformation(gameState: GameState, sortBy: String)
    {
        informationTable.clear()
        val informationList: List<Information>
        when (sortBy)
        {
            //All major fields of informations.
            "name" ->
            {
                informationList = gameState.informations.values.sortedBy { it.name }
            }

            "tgtPlace" ->
            {
                informationList = gameState.informations.values.sortedBy { it.tgtPlace }
            }

            "author" ->
            {
                informationList = gameState.informations.values.sortedBy { it.author }
            }

            "creationTime" ->
            {
                informationList = gameState.informations.values.sortedBy { it.creationTime }
            }

            "type" ->
            {
                informationList = gameState.informations.values.sortedBy { it.type }
            }

            "tgtTime" ->
            {
                informationList = gameState.informations.values.sortedBy { it.tgtTime }
            }

            else ->
            {
                informationList = gameState.informations.values.toList()
            }
        }
        if (informationList.isNotEmpty())
        {
            val firstInformation = informationList.first()
            firstInformation::class.java.declaredFields.forEach { field ->
                val button = button {
                    label(field.name, "trnsprtConsole") {
                        setFontScale(2f)
                    }
                    addListener(object : ClickListener()
                    {
                        override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                        {
                            this@InformationViewUI.populateInformation(gameState, field.name)
                        }
                    })
                }
                informationTable.add(button)
            }
            informationTable.row()
            informationList.forEach { information ->
                information::class.java.declaredFields.forEach { field ->
                    field.isAccessible = true
                    val label = Label(field.get(information).toString(), defaultSkin, "trnsprtConsole").also {
                        it.setFontScale(2f)
                    }
                    informationTable.add(label)
                }
                informationTable.row()
            }
        }
    }

    companion object
    {
        lateinit var instance: InformationViewUI
    }

}