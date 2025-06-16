package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.map.MapUI
import com.titaniumPolitics.game.ui.map.PlaceSelectionUI
import com.titaniumPolitics.game.ui.widget.CharacterSelectUI
import ktx.scene2d.*

class InterfaceRoot(val gameState: GameState) : Table(Scene2DSkin.defaultSkin), KTable {
    val mapUI = MapUI(gameState = this@InterfaceRoot.gameState)
    val calendarUI = CalendarUI(gameState)
    val politiciansInfoUI = PoliticiansInfoUI(gameState)
    val stack: Stack

    init {
        instance = this
        addActor(CharacterInteractionWindowUI(gameState = this@InterfaceRoot.gameState))
        val actions = AvailableActionsUI(this@InterfaceRoot.gameState)
        addActor(actions)
        actions.setPosition(1920f / 2 - actions.width / 2, -350f, Align.bottomLeft)
        stack = stack { cell ->
            cell.size(1920f, 1080f)

            //We draw the following UIs above any other UIs.
            table {
                val leftSeparator = table {
                    it.fill()
                    add(QuestUI(this@InterfaceRoot.gameState)).align(Align.bottomLeft).expandY().fill()
                    row()
                    add(AlertUI(this@InterfaceRoot.gameState)).align(Align.bottomLeft).expandY().fill()
                    row()
                    add(AssistantUI(this@InterfaceRoot.gameState)).align(Align.bottomLeft)
                }

                val centerSeparator = table {
                    it.grow()

                }
                val rightSeparator = table {
                    it.fill()
                    add(CharStatusUI(this@InterfaceRoot.gameState)).align(Align.bottomRight).expandY()
                }
            }
            add(this@InterfaceRoot.mapUI)
            add(this@InterfaceRoot.calendarUI)
            add(this@InterfaceRoot.politiciansInfoUI)
            add(InformationViewUI(this@InterfaceRoot.gameState))
            add(ResourceInfoUI())
            add(HumanResourceInfoUI())
            add(ApparatusInfoUI())
            add(ResourceTransferUI(this@InterfaceRoot.gameState, {}).also {
                ResourceTransferUI.primary = it
            })
            add(NewAgendaUI(this@InterfaceRoot.gameState, {}).also {
                NewAgendaUI.primary = it
            })
            add(AddInfoUI(this@InterfaceRoot.gameState, {}).also {
                AddInfoUI.primary = it
            })
            add(EndSpeechUI(this@InterfaceRoot.gameState, {}).also {
                EndSpeechUI.primary = it
            })
            add(WaitUI(this@InterfaceRoot.gameState, {}).also {
                WaitUI.primary = it
            })

            //TODO: Place UI here


            add(ProgressBackgroundUI(this@InterfaceRoot.gameState, this@InterfaceRoot.skin))

            //We draw the following UIs above any other UIs, as they have to appear on top of everything else.
            add(PlaceSelectionUI(this@InterfaceRoot.gameState))
            add(CharacterSelectUI(this@InterfaceRoot.gameState))

            container {
                align(Align.topLeft)
                addActor(QuickSave(this@InterfaceRoot.gameState))
            }

            //We draw the following UIs above any other UIs.
            add(DialogueUI(this@InterfaceRoot.gameState))


        }


    }

    companion object {
        //Singleton
        lateinit var instance: InterfaceRoot
    }

}