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
            image("panel") {
            }
            add(informationPane)

        }

    }

    fun refresh(gameState: GameState, sortBy: String)
    {
        informationTable.clear()
        val informationList: List<Information>
        val knownInfos = gameState.informations.values.filter { it.knownTo.contains(gameState.playerName) }
        when (sortBy)
        {
            //All major fields of informations.
            "name" ->
            {
                informationList = knownInfos.sortedBy { it.name }
            }

            "tgtPlace" ->
            {
                informationList = knownInfos.sortedBy { it.tgtPlace }
            }

            "author" ->
            {
                informationList = knownInfos.sortedBy { it.author }
            }

            "creationTime" ->
            {
                informationList = knownInfos.sortedBy { it.creationTime }
            }

            "type" ->
            {
                informationList = knownInfos.sortedBy { it.type }
            }

            "tgtTime" ->
            {
                informationList = knownInfos.sortedBy { it.tgtTime }
            }

            else ->
            {
                informationList = knownInfos.toList()
            }
        }
        if (informationList.isNotEmpty())
        {
            val firstInformation = informationList.first()
            firstInformation::class.java.declaredFields.forEach { field ->
                if (field.name == "Companion" || field.name == "knownTo" || field.name == "\$childSerializers")
                { //We don't want to show these fields.
                } else
                {
                    val button = button {
                        label(field.name, "trnsprtConsole") {
                            setFontScale(2f)
                        }
                        addListener(object : ClickListener()
                        {
                            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
                            {
                                this@InformationViewUI.refresh(gameState, field.name)
                            }
                        })
                    }
                    informationTable.add(button)
                }
            }
            informationTable.row()
            informationList.forEach { information ->
                information::class.java.declaredFields.forEach { field ->
                    if (field.name == "Companion" || field.name == "knownTo" || field.name == "\$childSerializers")
                    { //We don't want to show these fields.
                    } else
                    {
                        field.isAccessible = true
                        val label = Label(field.get(information).toString(), defaultSkin, "trnsprtConsole").also {
                            it.setFontScale(2f)
                            it.addListener(object : ClickListener()
                            {
                                //When clicked, open the information in a new window, depending on the type of information.
                                override fun clicked(
                                    event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                    x: Float,
                                    y: Float
                                )
                                {
                                    when (information.type)
                                    {
                                        "resources" ->
                                        {
                                            //Open resource window
                                            ResourceInfoUI.instance.isVisible = true
                                            ResourceInfoUI.instance.refresh(information)
                                        }

                                        "apparatusDurability" ->
                                        {
                                            //Open apparatus window
                                            ApparatusInfoUI.instance.isVisible = true
                                            ApparatusInfoUI.instance.refresh(information)
                                        }
                                    }
                                }
                            })
                        }
                        informationTable.add(label)
                    }
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