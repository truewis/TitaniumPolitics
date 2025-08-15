package com.titaniumPolitics.game.ui


import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable

import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.Character

import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.debugTools.Logger
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
            image("CogGrunge") {
                it.size(200f, 200f)
                try {
                    drawable = TextureRegionDrawable(
                        CapsuleStage.instance.assetManager.get( //TODO: Temporary solution for portrait image loading. PortraitUI does not have a stage.
                            ReadOnly.charJson[character.name]!!.jsonObject["image"]!!.jsonPrimitive.content,
                            Texture::class.java
                        )!!
                    )
                } catch (e: Exception) {
                    Logger.write("Portrait Image Error: ${character.name}", Logger.LogLevel.INFO)
                }
            }
            row()


            label(ReadOnly.charProp(character.name), "description") {
                setAlignment(Align.center)
                setFontScale(0.5f)
            }
            row()
            label(ReadOnly.charProp(character.name + "-desc"), "description") {
                setAlignment(Align.center)
                setFontScale(0.3f)
            }
            row()
            label("Stats: ${character.stats}", "description") {
                setAlignment(Align.center)
                setFontScale(0.5f)
            }
            row()
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

    companion object {
        //Singleton
        lateinit var instance: CharacterInfoUI
    }


}