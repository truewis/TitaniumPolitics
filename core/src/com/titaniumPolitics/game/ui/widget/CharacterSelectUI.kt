package com.titaniumPolitics.game.ui.widget

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.WindowUI
import com.titaniumPolitics.game.ui.map.PlaceSelectionUI
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.button
import ktx.scene2d.buttonGroup
import ktx.scene2d.container
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.scene2d
import ktx.scene2d.table

//This UI is used to select a character as a parameter for an action.
//It displays all characters in a table format, with division tab button.
//TODO: Perhaps only allow to select known characters to the player?
class CharacterSelectUI(val gameState: GameState) : WindowUI("CharacterSelectTitle"), KTable {

    val charactersTable = scene2d.table()
    val scrollPane = ScrollPane(charactersTable)
    val divisionSelectionBox = scene2d.buttonGroup(0, 1)
    var selectedCharacterCallback: (String) -> Unit = {}

    init {
        instance = this
        isVisible = false
        scrollPane.setScrollingDisabled(true, false)
        content.add(divisionSelectionBox).growX()
        content.row()
        content.add(scrollPane).grow()
        listOf(
            "infrastructure",
            "interior",
            "safety",
            "bioengineering",
            "mining",
            "education",
            "industry"
        ).forEach { tobj ->
            val t = scene2d.button {
                //TODO:Agenda Tooltip addListener(ActionTooltipUI(tobj))
                container {
                    it.size(150f)
                    it.fill(0.66f, 0.66f)
                    it.align(Align.center)
                    image("Help") {


                        when (tobj) {
                            //TODO: also make changes to NewAgendaUI.kt.
                            "infrastructure" -> {
                                this.setDrawable(Scene2DSkin.defaultSkin, "icon_traffic_39")
                            }

                            "interior" -> {
                                this.setDrawable(Scene2DSkin.defaultSkin, "icon_common_98")
                            }

                            "safety" -> {
                                this.setDrawable(Scene2DSkin.defaultSkin, "Shield2Grunge")
                            }

                            "bioengineering" -> {
                                this.setDrawable(Scene2DSkin.defaultSkin, "icon_activity_110")
                            }

                            "mining" -> {
                                this.setDrawable(Scene2DSkin.defaultSkin, "ShovelGrunge")
                            }

                            "education" -> {
                                this.setDrawable(Scene2DSkin.defaultSkin, "icon_tool_87")
                            }

                            "industry" -> {
                                this.setDrawable(Scene2DSkin.defaultSkin, "icon_tool_11")
                            }
                            //Default case for any other division, or if no division is selected.
                            else -> {
                                this.setDrawable(Scene2DSkin.defaultSkin, "Help")
                            }
                        }


                    }
                }
                this@button.addListener(object : ClickListener() {
                    override fun clicked(
                        event: InputEvent?,
                        x: Float,
                        y: Float
                    ) {
                        this@CharacterSelectUI.refresh(tobj)
                    }
                })
            }
            divisionSelectionBox.add(t).size(150f).fill()
        }
    }

    fun refresh(
        division: String = "",
        characters: Set<String> = gameState.characters.filter { it.value.alive && !it.key.contains("Anon") }.keys
    )//This function refreshes the character selection UI based on the selected division. If no division is selected, it shows all characters.
    {
        divisionSelectionBox.isVisible =
            characters.size > 10 //If there are more than 10 characters, show the division selection box.
        //This is a hack to avoid showing the division selection box when there are only a few characters.
        //This way, if the character set is not default, divisionSelectionBox would be hidden most of the time.

        charactersTable.clearChildren()
        val fcharacters = characters.filter { division == "" || gameState.characters[it]!!.division?.name == division }
        with(charactersTable) {
            if (fcharacters.isEmpty()) {
                label("No characters available", "trnsprtConsole") {
                    setFontScale(3f)
                    setAlignment(Align.center, Align.center)
                    color = Color.RED
                }
                return
            }
            fcharacters.forEach { character ->
                button {
                    it.fillX()
                    it.height(150f)
                    it.width(150f)
                    image("icon_app_1") {//portrait
                        it.fill()
                    }
                    row()
                    label(ReadOnly.prop(character), "trnsprtConsole") {
                        setFontScale(2f)
                        setAlignment(Align.center, Align.center)
                    }
                    addListener(object : ClickListener() {
                        override fun clicked(event: InputEvent?, x: Float, y: Float) {
                            this@CharacterSelectUI.selectedCharacterCallback(character)
                        }
                    })


                }
                //If the current row is full, add a new row.
                if (children.size % 6 == 0) {
                    row()
                }

            }
        }

    }

    companion object {
        lateinit var instance: CharacterSelectUI

    }
}