package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.map.MapUI
import ktx.scene2d.*

class HeadUpInterface(val gameState: GameState) : Table(Scene2DSkin.defaultSkin), KTable
{

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
            add(MapUI(gameState = this@HeadUpInterface.gameState))
            add(InformationViewUI())

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

        }


    }

    companion object
    {
        //Singleton
        lateinit var instance: HeadUpInterface
    }

}