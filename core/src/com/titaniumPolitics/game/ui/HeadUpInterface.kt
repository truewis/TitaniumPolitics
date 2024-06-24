package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.gameActions.GameAction
import com.titaniumPolitics.game.core.gameActions.NewAgenda
import com.titaniumPolitics.game.ui.map.MapUI
import com.titaniumPolitics.game.ui.map.PlaceSelectionUI
import ktx.scene2d.*

class HeadUpInterface(val gameState: GameState) : Table(Scene2DSkin.defaultSkin), KTable
{
    val mapUI = MapUI(gameState = this@HeadUpInterface.gameState)
    val calendarUI = CalendarUI()
    val politiciansInfoUI = PoliticiansInfoUI()

    init
    {
        instance = this
        addActor(CharacterInteractionWindowUI(gameState = this@HeadUpInterface.gameState))
        stack { cell ->
            cell.grow()

            container {
                align(Align.bottom)
                addActor(AvailableActionsUI(this@HeadUpInterface.gameState))
            }
            add(this@HeadUpInterface.mapUI)
            add(this@HeadUpInterface.calendarUI)
            add(this@HeadUpInterface.politiciansInfoUI)
            add(InformationViewUI())
            add(ResourceInfoUI())
            add(HumanResourceInfoUI())
            add(ApparatusInfoUI())
            add(ResourceTransferUI(this@HeadUpInterface.gameState) { _: GameAction -> })
            add(NewAgendaUI(this@HeadUpInterface.gameState) { _: GameAction -> })
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