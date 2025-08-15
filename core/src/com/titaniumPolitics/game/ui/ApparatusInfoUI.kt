package com.titaniumPolitics.game.ui


import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable

import com.badlogic.gdx.utils.Align

import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.debugTools.Logger
import com.titaniumPolitics.game.ui.widget.WindowUI
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

import ktx.scene2d.*


class ApparatusInfoUI : WindowUI("ApparatusInfoTitle") {
    private val dataTable = scene2d.table()

    init {
        isVisible = false
        instance = this
        val informationPane = ScrollPane(dataTable)
        informationPane.setScrollingDisabled(false, false)
        content.add(informationPane).grow()


    }

    fun refresh(information: Information) {
        dataTable.clear()
        dataTable.apply {
            image("CogGrunge") {
                it.size(200f, 200f)
                try {
                    drawable = TextureRegionDrawable(
                        CapsuleStage.instance.assetManager.get( //TODO: Temporary solution for portrait image loading. PortraitUI does not have a stage.
                            ReadOnly.appJson[information.tgtApparatus]!!.jsonObject["image"]!!.jsonPrimitive.content,
                            Texture::class.java
                        )!!
                    )
                } catch (e: Exception) {
                    Logger.write("Portrait Image Error: ${information.tgtApparatus}", Logger.LogLevel.INFO)
                }
            }
            row()


            label("Apparatus Name: ${information.tgtApparatus}", "description") {
                setAlignment(Align.center)
                setFontScale(0.5f)
            }
            row()
            label(ReadOnly.appProp(information.tgtApparatus!!), "description") {
                setAlignment(Align.center)
                setFontScale(0.3f)
            }
            row()
            label("Durability: ${information.amount}", "description") {
                setAlignment(Align.center)
                setFontScale(0.5f)
            }

            row()
            label("Author: ${information.author}", "description") {
                setAlignment(Align.center)
                setFontScale(0.3f)
            }
            row()
            label("Creation Time: ${information.creationTime}", "description") {
                setAlignment(Align.center)
                setFontScale(0.3f)
            }

        }

    }

    companion object {
        //Singleton
        lateinit var instance: ApparatusInfoUI
    }


}