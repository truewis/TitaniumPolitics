package com.titaniumPolitics.game.ui


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable

import com.badlogic.gdx.utils.Align
import com.rafaskoberg.gdx.typinglabel.TypingLabel
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState

import com.titaniumPolitics.game.core.Information
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.debugTools.Logger
import com.titaniumPolitics.game.ui.widget.DescriptionLabel
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


            label(ReadOnly.appProp(information.tgtApparatus!!), "description") {
                setAlignment(Align.center)
                setFontScale(0.5f)
            }
            row()
            add(DescriptionLabel(ReadOnly.appProp(information.tgtApparatus!! + "-desc"))).size(400f, 100f).fill()
            row()
            add(TypingLabel("", Scene2DSkin.defaultSkin, "description").apply {
                val text1 = if ((information.variables["durability"] ?: 1.0) > .0) {
                    ReadOnly.appProp("status-running")
                        .format(
                            ReadOnly.appProp(information.tgtApparatus ?: ReadOnly.appProp("unknownApparatus")),
                            information.variables["durability"]
                        )
                } else {
                    ReadOnly.appProp("status-broken")
                }
                val text2 = if ((information.variables["temperature"] ?: 300.0) > (information.variables["maxTemp"]
                        ?: Double.POSITIVE_INFINITY)
                ) ReadOnly.appProp("status-highTemp").format(
                    information.variables["maxTemp"] ?: Double.POSITIVE_INFINITY,
                    information.variables["temperature"]
                )
                else if ((information.variables["temperature"] ?: 300.0) < (information.variables["minTemp"]
                        ?: .0)
                ) ReadOnly.appProp("status-lowTemp").format(
                    information.variables["minTemp"] ?: 0.0,
                    information.variables["temperature"]
                )
                else ""
                val text3 =
                    ReadOnly.appProp("status-worker")
                        .format(
                            (information.variables["efficiency"] ?: .0) * 100,
                            information.variables["currentWorker"]?.toInt() ?: 0
                        )
                val text4 = if ((information.variables["currentWorker"]?.toInt()
                        ?: 0) > (information.variables["idealWorker"]?.toInt() ?: 0)
                )
                    ReadOnly.appProp("status-overWorked")
                else if ((information.variables["currentWorker"]?.toInt()
                        ?: 0) < (information.variables["idealWorker"]?.toInt() ?: 0)
                ) ReadOnly.appProp("status-underWorked")
                else ""
                val text5 = if ((information.variables["graveDanger"] ?: 0.0) > 0.0) {
                    ReadOnly.appProp("status-highDanger")
                } else if ((information.variables["danger"] ?: 0.0) > 0.0) {
                    ReadOnly.appProp("status-mediumDanger")
                } else ReadOnly.appProp("status-minimumDanger")
                setText(
                    text1 + "\n" + text2 + "\n" + text3 + "\n" + text4 + "\n" + text5
                )
                setAlignment(Align.left)
                setFontScale(0.2f)
                skipToTheEnd() //Skip the typing effect for this label.
            })

            row()
            label("${ReadOnly.prop("author")}: ${information.author}", "docTitle") {
                setAlignment(Align.center)
                setFontScale(0.3f)
            }
            row()
            label("${ReadOnly.prop("reportTime")}: ${GameState.formatTime(information.creationTime)}", "docTitle") {
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