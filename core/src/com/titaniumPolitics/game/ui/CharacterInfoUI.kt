package com.titaniumPolitics.game.ui


import com.badlogic.gdx.graphics.Color.BLACK
import com.badlogic.gdx.graphics.Color.DARK_GRAY
import com.badlogic.gdx.graphics.Color.LIGHT_GRAY
import com.badlogic.gdx.graphics.Color.WHITE
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable

import com.badlogic.gdx.utils.Align
import com.rafaskoberg.gdx.typinglabel.TypingLabel
import com.titaniumPolitics.game.core.Character

import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.debugTools.Logger
import com.titaniumPolitics.game.ui.widget.DescriptionLabel
import com.titaniumPolitics.game.ui.widget.SimplePortraitUI
import com.titaniumPolitics.game.ui.widget.StatRadarGraph
import com.titaniumPolitics.game.ui.widget.WindowUI
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

import ktx.scene2d.*


class CharacterInfoUI : WindowUI("CharacterInfoTitle") {
    private val dataTable = scene2d.table()

    init {
        isVisible = false
        instance = this
        val informationPane = ScrollPane(dataTable)
        informationPane.setScrollingDisabled(false, false)
        content.add(informationPane).grow()


    }

    fun refresh(character: Character) {
        dataTable.clear()
        dataTable.apply {
            table {
                it.fill()
                label(ReadOnly.charProp(character.name), "description") {
                    setAlignment(Align.center)
                    setFontScale(0.5f)
                }
                row()
                add(
                    DescriptionLabel(ReadOnly.charProp(character.name + "-desc"))
                ).size(500f, 200f).fill()
                row()
                add(StatRadarGraph(character.stats)).pad(50f)
                row()
                //Only add the mutuality meter if the character is not the player.
                if (character.name != character.parent.playerName) {
                    add(
                        MutualityMeter(
                            character.parent,
                            tgtCharacter = character.name,
                            who = character.parent.playerName
                        ).also {
                            it.remove() //Do not refresh the meter, since this window is not persistent.
                        })
                }

            }

            stack {

                it.size(500f, 1000f)
                character.division?.let { div ->
                    table {
                        align(Align.topLeft)
                        container {
                            image(div.name + "Division") {
                                color = LIGHT_GRAY
                            }
                            size(200f)
                            align(Align.topLeft)
                        }
                        row()
                        label(ReadOnly.prop(div.name), "docTitle") {
                            it.padTop(-15f) /*Division name closer to the logo for aesthetics*/
                            setAlignment(Align.top)
                            setFontScale(0.2f)
                            color = LIGHT_GRAY
                        }
                    }
                }
                add(SimplePortraitUI(character.name, scale = 1f, interactable = false))
            }
        }

    }

    companion object {
        //Singleton
        lateinit var instance: CharacterInfoUI
    }


}