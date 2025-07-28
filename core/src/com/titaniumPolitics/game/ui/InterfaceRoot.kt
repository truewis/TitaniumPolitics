package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.map.PlaceSelectionUI
import com.titaniumPolitics.game.ui.widget.CharacterSelectUI
import ktx.scene2d.*

class InterfaceRoot(val gameState: GameState) : Table(Scene2DSkin.defaultSkin), KTable {
    val stack: Stack
    val actions = AvailableActionsUI(this@InterfaceRoot.gameState)

    init {
        instance = this
        addActor(CharacterInteractionWindowUI(gameState = this@InterfaceRoot.gameState))

        addActor(actions)
        actions.setPosition(1920f / 2 - actions.width / 2, -350f, Align.bottomLeft)
        stack = stack { cell ->
            cell.size(1920f, 1080f)

            //We draw the following UIs above any other UIs.
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
                val leftSeparator = table {
                    it.fill()
                    add(QuestUI(this@InterfaceRoot.gameState)).align(Align.bottomLeft).expandY().fill()
                    row()
                    add(AssistantUI(this@InterfaceRoot.gameState)).align(Align.bottomLeft)
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