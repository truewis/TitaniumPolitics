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
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.Repair
import com.titaniumPolitics.game.debugTools.Logger
import com.titaniumPolitics.game.ui.CapsuleStage
import com.titaniumPolitics.game.ui.widget.ActionSheetUI
import com.titaniumPolitics.game.ui.widget.DescriptionLabel
import com.titaniumPolitics.game.ui.widget.InformationSourceUI
import com.titaniumPolitics.game.ui.widget.ResourceDisplayUI
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ktx.scene2d.*


class RepairUI(val gameState: GameState, actionCallback: (GameAction) -> Unit) :
    ActionSheetUI("RepairTitle", gameState, actionCallback) {
    val sbjChar = gameState.characters[subject]!!
    val agendaDetailStack: Stack
    val onUpdateSelectedApp = arrayListOf<(Information) -> Unit>()
    var selectedApp: Information? = null
        set(value) {
            if (value == null) throw Exception("")
            field = value
            onUpdateSelectedApp.forEach { it(value) }
            action = Repair(sbjChar.name, sbjChar.place.name, value.tgtApparatus!!, gameState)
            submitButton.refresh(action)
        }
    var action =
        Repair(sbjChar.name, sbjChar.place.name, gameState.places[tgtPlace]!!.apparatuses.first().ID, gameState)

    private var agendaSelectBox: Table
    private val noSuitableAppLabel = table {
        label("No suitable apparatus to repair.", "docTitle") {
            it.fill()
            setAlignment(Align.center)
            setFontScale(0.5f)
        }
        row()
        label("Make sure there is an apparatus that is damaged and you have the information of it.", "description") {
            it.fill()
            setAlignment(Align.center)
            setFontScale(0.3f)
        }
    }
    val apparatusDetailTable = scene2d.table {

        val requiredRes = ResourceDisplayUI()
        table {
            it.size(400f, 200f)
            it.fill()
            val name = label("Apparatus Name:", "docTitle") {
                it.size(400f, 50f)
                it.fill()
                setAlignment(Align.center)
                setFontScale(0.5f)
            }
            row()
            val desc = DescriptionLabel("")
            add(desc).size(400f, 100f).fill()
            row()
            val dur = label("Durability:", "docTitle") {
                it.size(400f, 50f)
                it.fill()
                setAlignment(Align.center)
                setFontScale(0.5f)
            }
            this@RepairUI.onUpdateSelectedApp += {
                name.setText(ReadOnly.appProp(it.name))
                desc.label.setText(ReadOnly.appProp(it.name + "-desc"))
                dur.setText(ReadOnly.prop("durability") + ": " + String.format("%.1f", it.variables["durability"]!!))
                val realApp = this@RepairUI.gameState.getApparatus(it.tgtApparatus!!)
                requiredRes.current =
                    realApp.requiredResourcePerRepair[Repair.checkRepairLevel(realApp).first]

                requiredRes.refresh()

            }
        }
        add(requiredRes)


    }
    val sp: ScrollPane
    val st = scene2d.stack {
        table {
            stack {
                it.size(900f, 400f)
                this@RepairUI.sp = scrollPane {
                    setScrollingDisabled(false, true)
                    this@RepairUI.agendaSelectBox =

                        buttonGroup(1, 1)
                }
                add(this@RepairUI.noSuitableAppLabel)
            }

            row()
            //Fill in agenda details.
            this@RepairUI.agendaDetailStack = stack {
                it.size(800f, 200f)
                add(this@RepairUI.apparatusDetailTable)
                //TODO: also make changes to NewAgenda.kt.
            }
            row()
            add(this@RepairUI.submitButton).size(200f, 75f).fill()
//            button {
//                it.fill().size(300f, 100f)
//                label("Cancel") {
//                    setFontScale(3f)
//                    setAlignment(Align.center)
//
//                }
//                addListener(object : ClickListener()
//                {
//                    override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float)
//                    {
//                        this@NewAgendaUI.isVisible = false
//                    }
//                })
//            }
        }
    }

    init {

        content.add(st).grow()

    }


    fun refresh(gameState: GameState) {
        agendaSelectBox.clear()
        refreshAvailableApparatusList(gameState)
    }

    fun refreshAvailableApparatusList(gameState: GameState) {
        val tgtPlaceObj = gameState.places[tgtPlace]!!
        tgtPlaceObj.apparatuses.forEach { app ->
            gameState.informations.values.firstOrNull {
                it.type == InformationType.APPARATUS
                it.tgtApparatusID == app.ID && gameState.playerName in it.knownTo
            }?.also { appInfo ->
                val t = scene2d.button("check") {
                    //TODO:Agenda Tooltip addListener(ActionTooltipUI(tobj))
                    container {
                        it.size(400f)
                        it.fill()
                        it.align(Align.center)
                        image("CogGrunge") {
                            try {
                                drawable = TextureRegionDrawable(
                                    CapsuleStage.Companion.instance.assetManager.get( //TODO: Temporary solution for portrait image loading. PortraitUI does not have a stage.
                                        ReadOnly.appJson[app.name]!!.jsonObject["image"]!!.jsonPrimitive.content,
                                        Texture::class.java
                                    )!!
                                )
                            } catch (e: Exception) {
                                Logger.write("Portrait Image Error: ${app.name}", Logger.LogLevel.INFO)
                            }

                        }
                    }
                    row()
                    add(InformationSourceUI(appInfo)).fill()
                    addListener(object : ChangeListener() {
                        override fun changed(event: ChangeEvent?, actor: Actor?) {
                            this@RepairUI.selectedApp = appInfo
                        }
                    })

                }
                agendaSelectBox.add(t).size(400f).fill()
            }

        }
        if (agendaSelectBox.children.size == 0) {
            sp.isVisible = false
        } else
            sp.isVisible = true
    }


}