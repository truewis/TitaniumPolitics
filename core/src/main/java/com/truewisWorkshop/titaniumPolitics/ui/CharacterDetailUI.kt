package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.ui.*

import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.Character

import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.widget.DescriptionLabel
import com.titaniumPolitics.game.ui.widget.DivisionBannerUI
import com.titaniumPolitics.game.ui.widget.SimplePortraitUI
import com.titaniumPolitics.game.ui.widget.StatRadarGraph
import com.titaniumPolitics.game.ui.widget.WindowUI

import ktx.scene2d.*


class CharacterDetailUI : WindowUI("CharacterInfoTitle") {
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
                    add(DivisionBannerUI(div))
                }
                add(SimplePortraitUI(character.name, interactable = false))
            }
        }

    }

    companion object {
        //Singleton
        lateinit var instance: CharacterDetailUI
    }


}
