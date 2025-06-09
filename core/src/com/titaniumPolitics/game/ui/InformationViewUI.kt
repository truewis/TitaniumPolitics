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

enum class InformationViewMode {
    SIMPLE, SELECT
}

class InformationViewUI(var gameState: GameState) : WindowUI("InformationViewTitle") {
    private val informationTable = Table()
    private var mode = InformationViewMode.SIMPLE

    //Only used in SELECT mode.
    val selectedInfos = arrayListOf<String>()

    val toggleButton = scene2d.checkBox("Advanced") {
        isChecked = false
        label.setFontScale(2f)
        addListener(object : ClickListener() {
            //When clicked, toggle the view between simple and advanced.
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                this@InformationViewUI.refresh(
                    sortBy = "creationTime", this@InformationViewUI.mode,
                    this@InformationViewUI.submitCallback
                )
            }
        })
    }
    private var submitCallback = { selectedInfos: List<String> ->
        //Default callback, does nothing.
    }

    val submitButton = scene2d.button {
        label("Submit Selected") {
            setAlignment(Align.center)
            setFontScale(2f)
        }
        addListener(object : ClickListener() {
            //When clicked, submit the selected informations.
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                this@InformationViewUI.submitCallback(this@InformationViewUI.selectedInfos)
                this@InformationViewUI.submitCallback = { selectedInfos: List<String> ->
                    //Default callback, does nothing.
                }
                this@InformationViewUI.isVisible = false
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
        content.row()
        content.add(submitButton).align(Align.right).size(200f, 50f)
        //Add a toggle button to show/hide the simple information view.

    }

    fun refresh(
        sortBy: String,
        mode: InformationViewMode = InformationViewMode.SIMPLE,
        callback: (List<String>) -> Unit = { }
    ) {
        informationTable.clear()
        selectedInfos.clear()
        this.mode = mode
        submitButton.isVisible = mode == InformationViewMode.SELECT
        submitCallback = callback
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
            if (mode == InformationViewMode.SELECT) {
                informationTable.add(scene2d.label("Prep", "trnsprtConsole") {
                    setFontScale(2f)
                    setAlignment(Align.left)
                }).size(100f, 100f).left()
            }
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
                    if (gameState.player.preparedInfoKeys.contains(information.name))
                        it.color = com.badlogic.gdx.graphics.Color.GREEN
                    else
                        it.color = com.badlogic.gdx.graphics.Color.WHITE
                }
                val label = scene2d.label(
                    information.simpleDescription(),
                    "trnsprtConsole"
                ).also {
                    it.setAlignment(Align.left)
                    it.setFontScale(2f)
                    if (gameState.player.preparedInfoKeys.contains(information.name))
                        it.color = com.badlogic.gdx.graphics.Color.GREEN
                    else
                        it.color = com.badlogic.gdx.graphics.Color.WHITE
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
                if (mode == InformationViewMode.SELECT) {
                    informationTable.add(scene2d.button("check") {
                        isChecked = this@InformationViewUI.selectedInfos.contains(information.name)
                        addListener(object : ClickListener() {
                            override fun clicked(
                                event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                x: Float,
                                y: Float
                            ) {
                                //Toggle the selection of the information.
                                if (isChecked)
                                    this@InformationViewUI.selectedInfos.add(information.name)
                                else
                                    this@InformationViewUI.selectedInfos.remove(information.name)
                            }
                        })
                    }).size(100f, 100f).left()
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

                if (mode == InformationViewMode.SELECT) {
                    informationTable.add(scene2d.label("Prep", "trnsprtConsole") {
                        setFontScale(2f)
                        setAlignment(Align.left)
                    }).size(100f, 100f).left()
                }

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
                                    this@InformationViewUI.refresh(field.name, mode, callback)
                                }
                            })
                        }
                        informationTable.add(button).size(200f, 100f)
                    }
                }
                informationTable.row()
                informationList.forEach { information ->
                    if (mode == InformationViewMode.SELECT) {
                        informationTable.add(scene2d.button("check") {
                            isChecked = this@InformationViewUI.selectedInfos.contains(information.name)
                            addListener(object : ClickListener() {
                                override fun clicked(
                                    event: com.badlogic.gdx.scenes.scene2d.InputEvent?,
                                    x: Float,
                                    y: Float
                                ) {
                                    //Toggle the selection of the information.
                                    if (isChecked)
                                        this@InformationViewUI.selectedInfos.add(information.name)
                                    else
                                        this@InformationViewUI.selectedInfos.remove(information.name)
                                }
                            })
                        }).size(100f, 100f).left()
                    }
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

                                if (gameState.player.preparedInfoKeys.contains(information.name))
                                    it.color = com.badlogic.gdx.graphics.Color.GREEN
                                else
                                    it.color = com.badlogic.gdx.graphics.Color.WHITE
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