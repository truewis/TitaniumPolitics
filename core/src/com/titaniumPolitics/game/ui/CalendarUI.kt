package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener

import com.badlogic.gdx.utils.Align

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly

import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin


class CalendarUI : WindowUI("CalendarTitle")
{
    private val dataTable = Table()

    init
    {
        isVisible = false
        instance = this
        val informationPane = ScrollPane(dataTable)
        informationPane.setScrollingDisabled(false, false)
        content.add(informationPane).grow()


    }

    fun refresh(gameState: GameState)
    {
        val DAYS = 5
        dataTable.clear()
        dataTable.apply {
            defaults().width(Value.percentWidth(1f / DAYS, this)) // Set default column width
            for (i in 0 until DAYS)
            {
                add(scene2d.table {
                    top()
                    label("Day ${i + gameState.day}") {
                        setAlignment(Align.center)
                        setFontScale(2f)
                        it.growX()
                    }
                    gameState.scheduledMeetings.filter {
                        it.value.time / ReadOnly.const("lengthOfDay")
                            .toInt() == gameState.day + i && it.value.scheduledCharacters.contains(gameState.playerName)
                    }.forEach { meeting ->
                        row()
                        label("Meeting with ${meeting.value.scheduledCharacters} at ${meeting.value.place}") {
                            setAlignment(
                                Align.center
                            )
                        }
                    }
                    gameState.scheduledMeetings.filter {
                        it.value.time / ReadOnly.const("lengthOfDay")
                            .toInt() == gameState.day + i && it.value.scheduledCharacters.contains(gameState.playerName)
                    }.forEach { conference ->
                        row()
                        label("Conference at ${conference.value.place}") {
                            setAlignment(Align.center)
                        }
                    }
                }).grow().align(Align.top)

                //Call alert UI for conferences and meetings that are within an hour.
                gameState.scheduledMeetings.filter {
                    it.value.time in gameState.time..gameState.time + 2 && it.value.scheduledCharacters.contains(
                        gameState.playerName
                    )
                }.forEach { meeting ->
                    AlertUI.instance.addAlert("meeting")
                }
                gameState.scheduledMeetings.filter {
                    it.value.time in gameState.time..gameState.time + 2 && it.value.scheduledCharacters.contains(
                        gameState.playerName
                    )
                }.forEach { conference ->
                    AlertUI.instance.addAlert("meeting")
                }
            }
        }

    }

    companion object
    {
        //Singleton
        lateinit var instance: CalendarUI
    }


}