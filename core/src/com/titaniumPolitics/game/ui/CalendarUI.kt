package com.titaniumPolitics.game.ui
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
class CalendarUI : WindowUI("CalendarTitle") {
    private val dataTable = Table(skin)
    private lateinit var informationPane: ScrollPane

    init {
        isVisible = false
        instance = this
        informationPane = ScrollPane(dataTable)
        informationPane.setScrollingDisabled(false, false)
        content.add(informationPane).grow()
    }

    fun refresh(gameState: GameState) {
        val DAYS = 5
        val HOURS = 24
        dataTable.clear()
        dataTable.defaults().pad(2f)

        // 헤더: 빈 칸 + 요일
        dataTable.add("")
        for (i in 0 until DAYS) {
            val dayLabel = Label("D${i + gameState.day}", skin).also { it.setFontScale(6f)

            }
            dataTable.add(dayLabel).center().padBottom(8f).padRight(150f).padLeft(150f)
        }
        dataTable.row()

        // 현재 시간
        val currentHour = (gameState.time % ReadOnly.const("lengthOfDay").toInt())
        var currentHourLabel: Label? = null

        for (hour in 0 until HOURS) {
            val isCurrent = hour == currentHour
            val hourLabel = Label(String.format("%02dH", hour), skin)
            hourLabel.setFontScale(4f)
            if (isCurrent) {
                hourLabel.setStyle(skin.get("consoleHighlight", Label.LabelStyle::class.java))
                currentHourLabel = hourLabel
            }
            dataTable.add(hourLabel)

            for (dayOffset in 0 until DAYS) {
                val day = gameState.day + dayOffset
                val meetingsAtThisHour = gameState.scheduledMeetings.filter { entry ->
                    val meeting = entry.value
                    val meetingDay = meeting.time / ReadOnly.const("lengthOfDay").toInt()
                    val meetingHour = meeting.time % ReadOnly.const("lengthOfDay").toInt()
                    meetingDay == day &&
                            meetingHour == hour &&
                            meeting.scheduledCharacters.contains(gameState.playerName)
                }

                if (meetingsAtThisHour.isNotEmpty()) {
                    val cellTable = Table()
                    meetingsAtThisHour.forEach { entry ->
                        val meeting = entry.value
                        val meetingLabel = Label("Meeting: ${meeting.place}", skin)
                        meetingLabel.setFontScale(2f)
                        cellTable.add(meetingLabel).left()
                        cellTable.row()
                    }
                    dataTable.add(cellTable).growX().left()
                } else {
                    dataTable.add("")
                }
            }
            dataTable.row()
        }

        // 스크롤: 현재 시간 라벨로 이동
        dataTable.invalidate()
        informationPane.layout()
        currentHourLabel?.let {
            val y = it.y
            val height = it.height
            informationPane.scrollTo(0f, y, 10f, height)
        }
    }

    companion object {
        lateinit var instance: CalendarUI
    }
}