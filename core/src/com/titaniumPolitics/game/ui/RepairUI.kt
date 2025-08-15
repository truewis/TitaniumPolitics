package com.titaniumPolitics.game.ui


import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.titaniumPolitics.game.core.*
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.NewAgenda
import com.titaniumPolitics.game.core.gameActions.Repair
import com.titaniumPolitics.game.debugTools.Logger
import com.titaniumPolitics.game.ui.widget.ActionSelectButton
import com.titaniumPolitics.game.ui.widget.ActionSheetUI
import com.titaniumPolitics.game.ui.widget.CharacterSelectButton
import com.titaniumPolitics.game.ui.widget.PlaceSelectButton
import com.titaniumPolitics.game.ui.widget.ResourceDisplayUI
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin
import kotlin.collections.get


class RepairUI(val gameState: GameState, actionCallback: (GameAction) -> Unit) :
    ActionSheetUI("RepairTitle", gameState, actionCallback) {
    val sbjChar = gameState.characters[subject]!!
    val submitButton: Button
    val agendaDetailStack: Stack
    val onUpdateSelectedApp = arrayListOf<(Apparatus) -> Unit>()
    val tooltip = ActionTooltipUI("Repair")
    var selectedApp: Apparatus? = null
        set(value) {
            if (value == null) throw Exception("")
            field = value
            onUpdateSelectedApp.forEach { it(value) }
            action = Repair(sbjChar.name, sbjChar.place.name).apply {
                injectParent(gameState)
                apparatusID = value.ID
            }
            if (!action.isValid()) {
                submitButton.isDisabled = true
                tooltip.displayInvalidReason(this@RepairUI.action.invalidReason)
            }
        }
    lateinit var action: Repair

    private lateinit var agendaSelectBox: Table
    val apparatusDetailTable = scene2d.table {
        val name = label("Apparatus Name:", "docTitle") {
            setAlignment(Align.center)
            setFontScale(0.5f)
        }
        row()
        val desc = label("Apparatus Description:", "docTitle") {
            setAlignment(Align.center)
            setFontScale(0.3f)
        }
        row()
        val dur = label("Durability:", "docTitle") {
            setAlignment(Align.center)
            setFontScale(0.5f)
        }

        row()
        val requiredRes = ResourceDisplayUI()
        add(requiredRes)

        this@RepairUI.onUpdateSelectedApp += {
            name.setText(it.name)
            desc.setText(ReadOnly.appProp(it.name))
            dur.setText(it.durability.toString())
            requiredRes.current = (
                    it.requiredResourcePerRepair[Repair.checkRepairLevel(it).first]
                    )
            requiredRes.refresh()

        }
    }
    val st = scene2d.stack {
        table {
            scrollPane {
                it.size(1000f, 400f)
                setScrollingDisabled(false, true)
                this@RepairUI.agendaSelectBox =

                    buttonGroup(1, 1)
            }

            row()
            //Fill in agenda details.
            this@RepairUI.agendaDetailStack = stack {
                it.grow()
                add(this@RepairUI.apparatusDetailTable)
                //TODO: also make changes to NewAgenda.kt.
            }
            row()
            this@RepairUI.submitButton = button {
                addListener(this@RepairUI.tooltip)
                it.size(300f, 100f).fill()
                label("Submit", "docTitle") {
                    color = Color.BLACK
                    setAlignment(Align.center)

                }
                addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent?, actor: Actor?) {
                        this@RepairUI.actionCallback(
                            this@RepairUI.action
                        )
                        this@RepairUI.onClose.forEach { it() }
                    }
                })
            }
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
        refreshAvailableAgendaList(gameState)
    }

    fun refreshAvailableAgendaList(gameState: GameState) {
        val tgtPlaceObj = gameState.places[tgtPlace]!!
        tgtPlaceObj.apparatuses.forEach { app ->
            val t = scene2d.button("check") {
                //TODO:Agenda Tooltip addListener(ActionTooltipUI(tobj))
                container {
                    it.size(400f)
                    it.fill()
                    it.align(Align.center)
                    image("CogGrunge") {
                        try {
                            drawable = TextureRegionDrawable(
                                CapsuleStage.instance.assetManager.get( //TODO: Temporary solution for portrait image loading. PortraitUI does not have a stage.
                                    ReadOnly.appJson[app.name]!!.jsonObject["image"]!!.jsonPrimitive.content,
                                    Texture::class.java
                                )!!
                            )
                        } catch (e: Exception) {
                            Logger.write("Portrait Image Error: ${app.name}", Logger.LogLevel.INFO)
                        }

                    }
                }
                addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent?, actor: Actor?) {
                        this@RepairUI.selectedApp = app
                    }
                })

            }
            agendaSelectBox.add(t).size(400f).fill()
        }
    }


}