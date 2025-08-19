package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.rafaskoberg.gdx.typinglabel.TypingLabel
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.widget.CharacterSelectButton
import com.titaniumPolitics.game.ui.widget.PlaceSelectButton
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.button
import ktx.scene2d.checkBox
import ktx.scene2d.label
import ktx.scene2d.scene2d
import ktx.scene2d.table

enum class InformationViewMode {
    SIMPLE, SELECT
}

class InformationViewUI(var gameState: GameState) : Table(defaultSkin) {
    private val informationTable = Table()
    private var mode = InformationViewMode.SIMPLE

    //Only used in SELECT mode.
    val selectedInfos = arrayListOf<String>()

    var selectedCharacter: String? = null
    var selectedPlace: String? = null
    var filterPredicate = { info: Information ->
        selectedCharacter?.let { it == info.tgtCharacter } ?: true && selectedPlace?.let { it == info.tgtPlace } ?: true
    } //Default predicate, shows all information.

    val filters = scene2d.table {
        val charFilter = CharacterSelectButton {
            //Filter the information by character.
            selectedCharacter = it
            refresh("creationTime", mode)
        }
        val placeFilter = PlaceSelectButton {
            selectedPlace = it
            refresh("creationTime", mode)
        }
        val clearFilterButton = scene2d.button {
            label("Clear Filter", "docTitle") {
                setAlignment(Align.center)
                setFontScale(0.5f)
            }
            addListener(object : ClickListener() {
                override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                    charFilter.clearSelection()
                    selectedCharacter = null
                    placeFilter.clearSelection()
                    selectedPlace = null
                    refresh("creationTime", mode)
                }
            })
        }
        add(charFilter).size(150f).fill()
        add(placeFilter).size(300f, 150f).fill()
        add(clearFilterButton).size(200f, 150f).fill()
    }
    private var submitCallback = { selectedInfos: List<String> ->
        //Default callback, does nothing.
    }

    val submitButton = scene2d.button {
        label(ReadOnly.prop("InformationViewUI-SubmitSelected")) {
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
        val informationPane = ScrollPane(informationTable)
        informationPane.setScrollingDisabled(false, false)
        add(filters).align(Align.right).pad(10f).fill().size(650f, 150f)
        row()
        add(informationPane).grow().pad(100f)
        row()
        add(submitButton).align(Align.right).size(400f, 75f)
        //Add a toggle button to show/hide the simple information view.

    }

    fun refresh(
        sortBy: String,
        mode: InformationViewMode = InformationViewMode.SIMPLE,
        callback: (List<String>) -> Unit = submitCallback
    ) {
        informationTable.clear()
        selectedInfos.clear()
        this.mode = mode
        submitButton.isVisible = mode == InformationViewMode.SELECT
        submitCallback = callback
        val informationList: List<Information>
        val knownInfos = gameState.informations.values.filter { it.knownTo.contains(gameState.playerName) }
        if (knownInfos.isEmpty()) {
            informationTable.add(scene2d.label(ReadOnly.prop("InformationViewUI-NoInfo"), "docTitle") {
                setFontScale(0.75f)
                setAlignment(Align.center)
            }).grow()
            return
        }
        if (mode == InformationViewMode.SELECT) {
            informationTable.add(scene2d.label(ReadOnly.prop("InformationViewUI-Prep"), "docTitle") {
                setFontScale(0.5f)
                setAlignment(Align.left)
            }).size(100f, 100f).left()
        }
        informationTable.add(scene2d.label(ReadOnly.prop("InformationViewUI-Time"), "docTitle") {
            setFontScale(0.5f)
            setAlignment(Align.left)
        }).size(200f, 100f).left()
        informationTable.add(scene2d.label(ReadOnly.prop("InformationViewUI-Description"), "docTitle") {
            setFontScale(0.5f)
            setAlignment(Align.left)
        }).growX().left()
        informationTable.row()
        informationList = knownInfos.sortedBy { -it.creationTime }
        informationList.forEach { information ->
            if (!filterPredicate(information)) return@forEach //Skip the information if it does not match the filter.
            val timeLabel = scene2d.label(
                GameState.formatTime(information.creationTime),
                "docTitle"
            ).also {
                it.setAlignment(Align.left)
                it.setFontScale(0.5f)
                if (gameState.player.preparedInfoKeys.contains(information.name))
                    it.color = com.badlogic.gdx.graphics.Color.GREEN
                else
                    it.color = com.badlogic.gdx.graphics.Color.WHITE
            }
            val label = TypingLabel(
                information.simpleDescription(),
                Scene2DSkin.defaultSkin,
                "docTitle"
            ).also {
                it.setAlignment(Align.left)
                it.setFontScale(0.5f)
                if (gameState.player.preparedInfoKeys.contains(information.name))
                    it.setFontScale(0.6f)
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

                            InformationType.APPARATUS -> {
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

    }

}