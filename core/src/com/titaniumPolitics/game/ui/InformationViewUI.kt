package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.KTable
import ktx.scene2d.table

class InformationViewUI : Table(defaultSkin), KTable
{
    private val informationTable = Table()

    init
    {
        val informationPane = ScrollPane(informationTable)
        informationPane.setScrollingDisabled(false, false)
        add(informationPane).expand().fill()
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
                val button = TextButton(field.name, defaultSkin)
                button.addListener(object : ClickListener()
                {
                    override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                    {
                        populateInformation(gameState, field.name)
                    }
                })
                informationTable.add(button)
            }
            informationTable.row()
            informationList.forEach { information ->
                information::class.java.declaredFields.forEach { field ->
                    field.isAccessible = true
                    val label = Label(field.get(information).toString(), defaultSkin)
                    informationTable.add(label)
                }
                informationTable.row()
            }
        }
    }

}