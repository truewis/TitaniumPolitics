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
    val mapButton = CabinetWindowContainerUI(
        title = "MAP",
        content = mapUI,
        xOffset = buttonXGap,
        yOffset = 0f,
        openAction = { mapUI.refresh() }
    )
    val infoUI = InformationViewUI(gameState)
    val informationButton =
        CabinetWindowContainerUI(
            title = "INFORMATION",
            content = infoUI,
            xOffset = buttonXGap,
            yOffset = buttonYGap,
            openAction = { infoUI.refresh("creationTime") }
        )

    val calendarUI = CalendarUI(gameState)
    val calendarButton =
        CabinetWindowContainerUI(
            title = "CALENDAR",
            content = calendarUI,
            xOffset = 2 * buttonXGap,
            yOffset = 2 * buttonYGap,
            openAction = { calendarUI.refresh() }
        )

    val politiciansUI = PoliticiansInfoUI(gameState)
    val politiciansInfoButton = CabinetWindowContainerUI(
        title = "POLITICS",
        content = politiciansUI,
        xOffset = 3 * buttonXGap,
        yOffset = 3 * buttonYGap,
        openAction = { politiciansUI.refresh() }
    )

    init {
        instance = this
        padLeft(-20f)

        // Close the map window when the move button is clicked
        mapUI.currentPlaceMarkerWindow.onClose += {
            mapButton.changeOpenState(false)
        }
        addActor(mapButton)
        cabinetWindowUIs.add(mapButton)
        mapButton.setSize(buttonWidth, buttonHeight + 10f)
        mapButton.setPosition(0f, 0f)

        addActor(informationButton)
        cabinetWindowUIs.add(informationButton)
        informationButton.setSize(buttonWidth, buttonHeight + 10f)
        informationButton.setPosition(buttonXGap, buttonYGap)

        addActor(calendarButton)
        cabinetWindowUIs.add(calendarButton)
        calendarButton.setSize(buttonWidth, buttonHeight + 10f)
        calendarButton.setPosition(2 * buttonXGap, 2 * buttonYGap)

        addActor(politiciansInfoButton)
        cabinetWindowUIs.add(politiciansInfoButton)
        politiciansInfoButton.setSize(buttonWidth, buttonHeight + 10f)
        politiciansInfoButton.setPosition(3 * buttonXGap, 3 * buttonYGap)


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
    }

    companion object {
        lateinit var instance: AssistantUI
    }


}
