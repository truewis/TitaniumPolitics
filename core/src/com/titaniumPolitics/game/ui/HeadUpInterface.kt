package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.map.MapUI
import com.titaniumPolitics.game.ui.map.PlaceSelectionUI
import ktx.scene2d.*

class HeadUpInterface(val gameState: GameState) : Table(Scene2DSkin.defaultSkin), KTable
{
    val mapUI = MapUI(gameState = this@HeadUpInterface.gameState)

    init
    {
        debug()
        instance = this
        stack { cell ->
            cell.grow()

            container {
                align(Align.bottom)
                addActor(AvailableActionsUI(this@HeadUpInterface.gameState))
            }
            add(this@HeadUpInterface.mapUI)

            add(InformationViewUI())
            add(ResourceInfoUI())
            add(ResourceTransferUI(this@HeadUpInterface.gameState))
            add(TradeUI(this@HeadUpInterface.gameState))

            //We draw the following UIs above any other UIs, as they have to appear on top of everything else.
            add(PlaceSelectionUI(this@HeadUpInterface.gameState))

            //We draw the following UIs above any other UIs.
            table {
                val leftSeparator = table {
                    it.fill()
                    add(AlertUI(this@HeadUpInterface.gameState)).align(Align.bottomLeft).expandY()
                    row()
                    add(AssistantUI(this@HeadUpInterface.gameState)).align(Align.bottomLeft)
                }

                val centerSeparator = table {
                    it.grow()

                }
                val rightSeparator = table {
                    it.fill()
                    add(CharStatusUI(this@HeadUpInterface.gameState)).align(Align.bottomRight).expandY()
                }
            }
            container {
                align(Align.topLeft)
                addActor(QuickSave(this@HeadUpInterface.gameState))
            }

            //We draw the following UIs above any other UIs.
            add(DialogueUI(this@HeadUpInterface.gameState))

        }


    }

    companion object
    {
        //Singleton
        lateinit var instance: HeadUpInterface
    }

}