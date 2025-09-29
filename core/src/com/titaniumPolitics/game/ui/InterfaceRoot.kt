package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.debugTools.Logger
import com.titaniumPolitics.game.ui.map.PlaceSelectionUI
import com.titaniumPolitics.game.ui.meeting.MeetingUI
import com.titaniumPolitics.game.ui.widget.ActionSelectUI
import com.titaniumPolitics.game.ui.widget.CharacterSelectUI
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ktx.scene2d.KTable
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.stack
import ktx.scene2d.table

class InterfaceRoot(val gameState: GameState) : Table(defaultSkin), KTable {
    val stack: Stack
    val avAUI = AvailableActionsUI(this@InterfaceRoot.gameState)
    val charactersView = CharactersInPlaceUI(gameState)
    val meetingUI = MeetingUI(gameState)
    val assistantUI = AssistantUI(gameState)

    init {
        instance = this
        gameState.updateUI += {
            if (it.player.currentMeeting != null) {
                meetingUI.isVisible = true
                meetingUI.displayMeeting(it.player.currentMeeting!!)
                charactersView.isVisible = false
                //assistantUI.cabinetWindowUIs.firstOrNull()?.changeOpenState(false) TODO: Close the cabinet if open. This generates sound so we comment it out for now.
                assistantUI.isVisible = false
            } else {
                meetingUI.isVisible = false
                charactersView.isVisible = true
                assistantUI.isVisible = true
            }
        }
        stack = stack { cell ->
            cell.size(1920f, 1080f)

            //We draw the following UIs under any other UIs.

            add(this@InterfaceRoot.charactersView)
            add(this@InterfaceRoot.meetingUI)
            table {
                add().fill()
                add().grow()
                val rightSeparator = table {
                    it.fill()
                    add(AlertUI(this@InterfaceRoot.gameState)).align(Align.topLeft).expandY().fill()
                    row()
                    add(PlayerStatusUI(this@InterfaceRoot.gameState)).align(Align.bottomRight).expandY()
                }
            }
            table {
                addActor(CharacterInteractionWindowUI(gameState = this@InterfaceRoot.gameState))

                addActor(this@InterfaceRoot.avAUI)
                this@InterfaceRoot.avAUI.setPosition(
                    1920f / 2 - this@InterfaceRoot.avAUI.width / 2,
                    -350f,
                    Align.bottomLeft
                )
            }
            table {
                val leftSeparator = table {
                    it.fill()
                    add(TasksUI(this@InterfaceRoot.gameState)).align(Align.topLeft).fill()
                    row()
                    add(this@InterfaceRoot.assistantUI).align(Align.bottomLeft).expandY().fill()
                }

                val centerSeparator = table {
                    it.grow()

                }
                add().fill()

            }
            add(ResourceInfoUI())
            add(HumanResourceInfoUI())
            add(ApparatusInfoUI())
            add(CharacterDetailUI())
            add(GraphInfoUI())

            //TODO: Place UI here


            add(ProgressBackgroundUI(this@InterfaceRoot.gameState))
            //add(OtherCharacterProgressBackgroundUI(this@InterfaceRoot.gameState))


            add(ActionSelectUI(this@InterfaceRoot.gameState, {
                val actionName = it::class.simpleName
                ActionSelectUI.instance.isVisible = false
                ActionSelectUI.instance.buttonOwner?.apply {
                    actionIcon.setDrawable(
                        defaultSkin,
                        ReadOnly.actionJson[actionName]?.jsonObject?.get("image")?.jsonPrimitive?.content ?: "Help"
                    )
                    setLabel(it)
                    callback(it)
                }
                    ?: Logger.write("No button owner found for action: $actionName")
            }))
            //ActionSelectUI may use PlaceSelection and CharacterSelectUI, so we add it before them.
            //We draw the following UIs above any other UIs, as they have to appear on top of everything else.
            add(PlaceSelectionUI(this@InterfaceRoot.gameState))
            add(CharacterSelectUI(this@InterfaceRoot.gameState))
            add(BlockingWarningUI(this@InterfaceRoot.gameState))
            //We draw the following UIs above any other UIs.
            add(DialogueUI(this@InterfaceRoot.gameState))

            add(SystemUI(this@InterfaceRoot.gameState))
            add(GameOverUI(this@InterfaceRoot.gameState))

        }


    }

    companion object {
        //Singleton
        lateinit var instance: InterfaceRoot
    }

}