package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class InformationViewUI(var gameState: GameState) : WindowUI("InformationViewTitle") {
    private val informationTable = Table()
    val toggleButton = scene2d.checkBox("Advanced") {
        isChecked = false
        label.setFontScale(2f)
        addListener(object : ClickListener() {
            //When clicked, toggle the view between simple and advanced.
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                this@InformationViewUI.refresh(sortBy = "creationTime")
            }
        })
    }

    init {
        isVisible = false
        instance = this
        val informationPane = ScrollPane(informationTable)
        informationPane.setScrollingDisabled(false, false)
        content.add(toggleButton).align(Align.right).pad(10f).size(200f, 50f)
        content.row()
        content.add(informationPane).grow().pad(100f)
        //Add a toggle button to show/hide the simple information view.

    }

    fun refresh(sortBy: String) {
        informationTable.clear()
        val informationList: List<Information>
        val knownInfos = gameState.informations.values.filter { it.knownTo.contains(gameState.playerName) }
        if (knownInfos.isEmpty()) {
            informationTable.add(scene2d.label("No information available", "trnsprtConsole") {
                setFontScale(3f)
                setAlignment(Align.center)
            }).grow()
            return
        }
        if (!toggleButton.isChecked)//Simple view
        {
            informationTable.add(scene2d.label("Time", "trnsprtConsole") {
                setFontScale(2f)
                setAlignment(Align.left)
            }).size(200f, 100f).left()
            informationTable.add(scene2d.label("Description", "trnsprtConsole") {
                setFontScale(2f)
                setAlignment(Align.left)
            }).growX().left()
            informationTable.row()
            informationList = knownInfos.sortedBy { -it.creationTime }
            informationList.forEach { information ->
                val timeLabel = scene2d.label(
                    GameState.formatTime(information.creationTime),
                    "trnsprtConsole"
                ).also {
                    it.setAlignment(Align.left)
                    it.setFontScale(2f)
                }
                val label = scene2d.label(
                    information.simpleDescription(),
                    "trnsprtConsole"
                ).also {
                    it.setAlignment(Align.left)
                    it.setFontScale(2f)
                    it.addListener(object : ClickListener() {
                        //When clicked, open the information in a new window, depending on the type of information.
                        override fun clicked(
                            event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                            x: Float,
                            y: Float
                        ) {
                            when (information.type) {
                                InformationType.RESOURCES -> {
                                    //Open resource window
                                    ResourceInfoUI.instance.isVisible = true
                                    ResourceInfoUI.instance.refresh(information)
                                }

                                InformationType.APPARATUS_DURABILITY -> {
                                    //Open apparatus window
                                    ApparatusInfoUI.instance.isVisible = true
                                    ApparatusInfoUI.instance.refresh(information)
                                }

                                else -> {
                                    //Do nothing
                                }
                            }
                        }
                    })
                }
                informationTable.add(timeLabel).size(200f, 100f).left()
                informationTable.add(label).growX().left()
                informationTable.row()
            }
        } else {//Advanced view
            when (sortBy) {
                //All major fields of informations.
                "tgtPlace" -> {
                    informationList = knownInfos.sortedBy { it.tgtPlace }
                }

                "author" -> {
                    informationList = knownInfos.sortedBy { it.author }
                }

                "creationTime" -> {
                    informationList = knownInfos.sortedBy { -it.creationTime }
                }

                "type" -> {
                    informationList = knownInfos.sortedBy { it.type }
                }

                "tgtTime" -> {
                    informationList = knownInfos.sortedBy { -it.tgtTime }
                }

                else -> {
                    informationList = knownInfos.toList()
                }
            }
            if (informationList.isNotEmpty()) {

                val firstInformation = informationList.first()
                firstInformation::class.java.declaredFields.forEach { field ->
                    if (field.name == "author" || field.name == "creationTime" || field.name == "type" || field.name == "tgtTime" || field.name == "tgtPlace" || field.name == "tgtCharacter" || field.name == "amount") {
                        val button = scene2d.button {
                            label(field.name, "trnsprtConsole") {
                                setFontScale(2f)
                            }
                            addListener(object : ClickListener() {
                                override fun clicked(
                                    event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                    x: Float,
                                    y: Float
                                ) {
                                    this@InformationViewUI.refresh(field.name)
                                }
                            })
                        }
                        informationTable.add(button).size(200f, 100f)
                    }
                }
                informationTable.row()
                informationList.forEach { information ->
                    information::class.java.declaredFields.forEach { field ->
                        //Only show fields that are relevant to the information.
                        if (field.name == "author" || field.name == "creationTime" || field.name == "type" || field.name == "tgtTime" || field.name == "tgtPlace" || field.name == "tgtCharacter" || field.name == "amount") {
                            field.isAccessible = true
                            val label = Label(
                                "${field.get(information)?.toString() ?: "null"}",
                                defaultSkin,
                                "trnsprtConsole"
                            ).also {
                                it.setAlignment(Align.center)
                                it.setFontScale(2f)
                                it.addListener(object : ClickListener() {
                                    //When clicked, open the information in a new window, depending on the type of information.
                                    override fun clicked(
                                        event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                        x: Float,
                                        y: Float
                                    ) {
                                        when (information.type) {
                                            InformationType.RESOURCES -> {
                                                //Open resource window
                                                ResourceInfoUI.instance.isVisible = true
                                                ResourceInfoUI.instance.refresh(information)
                                            }

                                            InformationType.APPARATUS_DURABILITY -> {
                                                //Open apparatus window
                                                ApparatusInfoUI.instance.isVisible = true
                                                ApparatusInfoUI.instance.refresh(information)
                                            }

                                            else -> {
                                                //Do nothing
                                            }
                                        }
                                    }
                                })
                            }
                            informationTable.add(label).growX()
                        }
                    }
                    informationTable.row()
                }
            }
        }
    }

    companion object {
        lateinit var instance: InformationViewUI
    }

}