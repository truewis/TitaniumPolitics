package com.titaniumPolitics.game.ui
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import kotlin.text.get

class CalendarUI : WindowUI("CalendarTitle") {
    private val dataTable = Table(skin)
    private val dayTable = Table(skin)
    private lateinit var scrollPane: ScrollPane

    init {
        isVisible = false
        instance = this
        scrollPane = ScrollPane(dataTable)
        scrollPane.setScrollingDisabled(true, false) // 수직 스크롤만 허용
        content.add(dayTable).growX().padBottom(10f).row()
        content.add(scrollPane).grow()
    }

    fun refresh(gameState: GameState) {
        val DAYS = 5
        val HOURS = 24
        dataTable.clear()
        dataTable.defaults().pad(2f)
        dayTable.clear()
        dayTable.defaults().pad(2f)


        // 현재 시간
        val currentHour = (gameState.hour)


        // 헤더: 시간/요일
        dayTable.add(Label("H\\D", skin, "default").also {
            it.setFontScale(6f)}) // 왼쪽 상단 빈 칸
        for (i in 0 until DAYS) {
            val style = if (i == 0) {"console"
            } else {
                "default"
            }
            val dayLabel = Label("D${i + gameState.day}", skin, style)
            dayLabel.setFontScale(6f)
            dayLabel.setAlignment(Align.center)
            dayTable.add(dayLabel).center().padBottom(8f).width(300f)
        }
        dayTable.layout()

        // 헤더: adjust the column widths
        dataTable.add()// 왼쪽 상단 빈 칸
        for (i in 0 until DAYS) {
            dataTable.add().padRight(dayTable.cells[i + 1].prefWidth)
        }
        dataTable.row()
        for (hour in 0 until HOURS) {
            // 시간 라벨 (첫 번째 열)
            val hourLabel = Label(String.format("%02dH", hour), skin)
            hourLabel.setFontScale(4f)
            if (hour == currentHour) {
                hourLabel.setStyle(skin.get("console", Label.LabelStyle::class.java))
            }
            dataTable.add(hourLabel).right()

            // 각 날짜별 미팅 정보
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

        dayTable.debug()
        dataTable.debug()

        // 현재 시간 행으로 스크롤
        dataTable.invalidate()
        scrollPane.layout()
        val rowHeight = dataTable.cells[DAYS + 1].actor.height // 첫 시간 라벨의 높이
        scrollPane.scrollTo(0f, scrollPane.height - rowHeight * currentHour, 10f, rowHeight)
    }

    companion object {
        lateinit var instance: CalendarUI
    }
}