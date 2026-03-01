package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.map.MapUI
import com.titaniumPolitics.game.ui.widget.CabinetWindowContainerUI
import ktx.scene2d.Scene2DSkin.defaultSkin

class AssistantUI(gameState: GameState) : Table(defaultSkin) {

    val cabinetWindowUIs = mutableListOf<CabinetWindowContainerUI>()

    val buttonWidth = 180f
    val buttonHeight = 540f
    val buttonYGap = 70f
    val buttonXGap = -7f
    val mapUI = MapUI(gameState)
    val mapButton = addCabinetButton(
        title = "MAP",
        content = mapUI,
        openAction = { mapUI.refresh() }
    )
    val infoUI = InformationViewUI(gameState)
    val informationButton = addCabinetButton(
        title = "INFORMATION",
        content = infoUI,
        openAction = { infoUI.refresh("creationTime") }
    )

    val calendarUI = CalendarUI(gameState)
    val calendarButton = addCabinetButton(
        title = "CALENDAR",
        content = calendarUI,
        openAction = { calendarUI.refresh() }
    )

    val politiciansUI = PoliticiansInfoUI(gameState)
    val politiciansInfoButton = addCabinetButton(
        title = "POLITICIANS",
        content = politiciansUI,
        openAction = { politiciansUI.refresh() }
    )

    var numCurrentCabinetButtons = 0
    fun addCabinetButton(title: String, content: Table, openAction: () -> Unit): CabinetWindowContainerUI {
        val newButton = CabinetWindowContainerUI(
            title = title,
            content = content,
            xOffset = (numCurrentCabinetButtons) * buttonXGap,
            yOffset = (numCurrentCabinetButtons) * buttonYGap,
            openAction = openAction
        )
        addActor(newButton)
        cabinetWindowUIs.add(newButton)
        newButton.setSize(buttonWidth, buttonHeight + 10f)
        newButton.setPosition((numCurrentCabinetButtons) * buttonXGap, (numCurrentCabinetButtons) * buttonYGap)
        numCurrentCabinetButtons++
        return newButton
    }

    init {
        instance = this
        padLeft(-20f)

        // Add PPS cabinet after all other cabinets are set up
        val ppsIndex = cabinetWindowUIs.size
        val ppsWindowUI = PPSWindowUI(gameState)
        val ppsHandleUI = PPSHandleUI(
            ppsWindow = ppsWindowUI,
            xOffset = -160f,//ppsIndex * buttonXGap, //Controlled by PPSHandleUI itself, so it can slide in and out smoothly
            yOffset = ppsIndex * buttonYGap
        )
        addActor(ppsHandleUI)
        cabinetWindowUIs.add(ppsHandleUI)
        ppsHandleUI.setSize(buttonWidth, buttonHeight + 10f)
        ppsHandleUI.setPosition(ppsHandleUI.xOffset, ppsIndex * buttonYGap)

        // Refresh PPS status markers whenever the game state updates
        gameState.updateUI += {
            Gdx.app.postRunnable {
                ppsWindowUI.refresh()
                println("PPS Refreshed.")
            }
        }

        // Close the map window when the move button is clicked
        mapUI.currentPlaceMarkerWindow.onClose += {
            mapButton.changeOpenState(false)
        }


        //Mark the calendar button When new meeting is scheduled within the next 5 days.
        //Also check CalendarUI
        gameState.onAddScheduledMeeting += { meeting ->
            Gdx.app.postRunnable {
                if (meeting.time - gameState.day * 86400 / ReadOnly.DT <= 5 * 86400 / ReadOnly.DT && meeting.scheduledCharacters.contains(
                        gameState.playerName
                    )
                ) {//5 days from today's start
                    calendarButton.changeMarkedState(true)
                }
            }
        }

        //Mark the information button when new information is received.
        gameState.onAddInfo += { info ->
            Gdx.app.postRunnable {
                if (info.knownTo.contains(gameState.playerName) && info.author != gameState.playerName) {
                    //I know information I wrote, no need to remind me.
                    informationButton.changeMarkedState(true)
                }
            }
        }
        calendarButton.isVisible = false
        politiciansInfoButton.isVisible = false
        informationButton.isVisible = false
    }

    companion object {
        lateinit var instance: AssistantUI
    }


}
