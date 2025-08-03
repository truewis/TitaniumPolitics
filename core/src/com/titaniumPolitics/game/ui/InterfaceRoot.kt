package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.map.PlaceSelectionUI
import com.titaniumPolitics.game.ui.meeting.MeetingUI
import com.titaniumPolitics.game.ui.widget.ActionSelectButton
import com.titaniumPolitics.game.ui.widget.ActionSelectUI
import com.titaniumPolitics.game.ui.widget.CharacterSelectUI
import ktx.scene2d.*

class InterfaceRoot(val gameState: GameState) : Table(Scene2DSkin.defaultSkin), KTable {
    val stack: Stack
    val avAUI = AvailableActionsUI(this@InterfaceRoot.gameState)
    val charactersView = CharacterPortraitsUI(gameState)
    val meetingUI = MeetingUI(gameState)

    init {
        instance = this
        gameState.updateUI += {
            if (it.player.currentMeeting != null) {
                meetingUI.isVisible = true
                meetingUI.newMeeting(it.player.currentMeeting!!)
                charactersView.isVisible = false
            } else {
                meetingUI.isVisible = false
                charactersView.isVisible = true
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
                    add(CharStatusUI(this@InterfaceRoot.gameState)).align(Align.bottomRight).expandY()
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
                    add(AssistantUI(this@InterfaceRoot.gameState)).align(Align.bottomLeft).expandY().fill()
                }

                val centerSeparator = table {
                    it.grow()

                }
                add().fill()

            }
            add(ResourceInfoUI())
            add(HumanResourceInfoUI())
            add(ApparatusInfoUI())

            //TODO: Place UI here


            add(ProgressBackgroundUI(this@InterfaceRoot.gameState, this@InterfaceRoot.skin))

            //We draw the following UIs above any other UIs, as they have to appear on top of everything else.
            add(PlaceSelectionUI(this@InterfaceRoot.gameState))
            add(CharacterSelectUI(this@InterfaceRoot.gameState))
            add(ActionSelectUI(this@InterfaceRoot.gameState))

            //We draw the following UIs above any other UIs.
            add(DialogueUI(this@InterfaceRoot.gameState))

            add(SystemUI(this@InterfaceRoot.gameState))

        }


    }

    companion object {
        //Singleton
        lateinit var instance: InterfaceRoot
    }

}