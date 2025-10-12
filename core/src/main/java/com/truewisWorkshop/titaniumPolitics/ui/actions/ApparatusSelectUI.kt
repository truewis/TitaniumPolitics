package com.titaniumPolitics.game.ui.actions


import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.debugTools.Logger
import com.titaniumPolitics.game.ui.ApparatusPanelUI
import com.titaniumPolitics.game.ui.CapsuleStage
import com.titaniumPolitics.game.ui.widget.InformationSourceUI
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ktx.scene2d.*


class ApparatusSelectUI(val gameState: GameState, val callback: (Information) -> Unit) :
    Table(Scene2DSkin.defaultSkin), KTable {
    var selectedApp: Information? = null
        set(value) {
            if (value == null) throw Exception("")
            field = value
            callback(value)
        }
    private var appSelectBox: Table
    private val noSuitableAppLabel = table {
        label(ReadOnly.prop("ApparatusSelectUI-NoSuitableApparatus"), "docTitle") {
            it.fill()
            setAlignment(Align.center)
            setFontScale(0.5f)
        }
        row()
        label(ReadOnly.prop("ApparatusSelectUI-MakeSureInfo"), "description") {
            it.fill()
            setAlignment(Align.center)
            setFontScale(0.3f)
        }
    }
    val sp: ScrollPane
    val st = stack {
        it.grow()
        table {
            stack {
                it.size(900f, 400f)
                this@ApparatusSelectUI.sp = scrollPane {
                    setScrollingDisabled(true, false)
                    this@ApparatusSelectUI.appSelectBox =

                        buttonGroup(1, 1)
                }
                add(this@ApparatusSelectUI.noSuitableAppLabel)
            }

        }
    }


    fun refresh(tgtPlace: String) {
        appSelectBox.clear()
        refreshAvailableApparatusList(tgtPlace)
    }

    fun refreshAvailableApparatusList(tgtPlace: String) {
        val tgtPlaceObj = gameState.places[tgtPlace]!!
        tgtPlaceObj.apparatuses.forEach { app ->
            gameState.informations.values.filter {
                it.type == InformationType.APPARATUS
                it.tgtApparatusID == app.ID && gameState.playerName in it.knownTo
            }.maxByOrNull {
                it.tgtTime // Get the most recent information
            }?.also { appInfo ->
                val t = ApparatusPanelUI(appInfo)
                if (appSelectBox.hasChildren())
                    appSelectBox.row()
                appSelectBox.add(t).size(400f, 150f).fill()
                t.addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent?, actor: Actor?) {
                        selectedApp = appInfo
                    }
                }

                )
            }

        }
        if (appSelectBox.children.size == 0) {
            sp.isVisible = false
            noSuitableAppLabel.isVisible = true
        } else {
            sp.isVisible = true
            noSuitableAppLabel.isVisible = false
        }
    }


}