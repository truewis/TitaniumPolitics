package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.widget.WindowUI

class CalendarUI(val gameState: GameState) : WindowUI("CalendarTitle") {
    private val dataTable = Table(skin)
    private val dayTable = Table(skin)
    private lateinit var scrollPane: ScrollPane
    val entries = mutableListOf<CalendarEntry>()
    val newEntries = mutableListOf<CalendarEntry>()

    init {
        scrollPane = ScrollPane(dataTable)
        scrollPane.setScrollingDisabled(true, false) // 수직 스크롤만 허용
        content.add(dayTable).growX().padBottom(10f).row()
        content.add(scrollPane).grow()

        //Mark the calendar button When new meeting is scheduled within the next 5 days.
        //Also check AssistantUI for the button blinking condition.
        gameState.onAddScheduledMeeting += { meeting ->
            if (meeting.scheduledCharacters.contains(gameState.playerName))
                addEntry(
                    meeting.time,
                    ReadOnly.prop(meeting.type.toString()),
                    meeting.place,
                    "A new meeting has been scheduled at ${meeting.time}H in ${meeting.place}.",
                    gameState.scheduledMeetings.entries.find { it.value == meeting }?.key
                )
        }

        gameState.timeChanged += { _, time ->
            // Check if there is an entry within the next hour
            entries.firstOrNull { it.time - time in 0..3600 / ReadOnly.dt && !it.hasAlerted }?.let {
                it.hasAlerted = true // Mark as alerted
                AlertUI.instance.addAlert("alarm") {
                    isVisible = true
                }
            }

        }
    }

    fun addEntry(time: Int, title: String, place: String, description: String, associatedMeeting: String? = null) {
        val entry = CalendarEntry(time, title, place, description, associatedMeeting)
        newEntries.add(entry)
    }

    fun refreshEntries() {
        entries.addAll(newEntries)
        newEntries.clear()
        gameState.eventSystem.quests.forEach { quest ->
            entries.firstOrNull { it.associatedMeeting?.equals(quest.tgtMeeting) ?: false }?.associatedQuestName =
                quest.name
        }
    }

    fun refresh() {
        val DAYS = 5
        val HOURS = 24

        refreshEntries()
        dataTable.clear()
        dataTable.defaults().pad(2f)
        dayTable.clear()
        dayTable.defaults().pad(2f)


        // 현재 시간
        val currentHour = (gameState.hour)


        // 헤더: 시간/요일
        dayTable.add(Label("H\\D", skin, "default").also {
            it.setFontScale(6f)
        }) // 왼쪽 상단 빈 칸
        for (i in 0 until DAYS) {
            val style = if (i == 0) {
                "console"
            } else {
                "default"
            }
            val dayLabel = Label("D${i + gameState.day}", skin, style)
            dayLabel.setFontScale(6f)
            dayLabel.setAlignment(Align.center)
            dayTable.add(dayLabel).center().padBottom(8f).width(350f)
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
                val entriesAtThisHour =
                    entries.filter { ReadOnly.toHours(it.time) == hour && ReadOnly.toDays(it.time) == gameState.day + dayOffset }
                if (entriesAtThisHour.isNotEmpty()) {
                    val cellTable = Table()
                    entriesAtThisHour.forEach { entry ->
                        //if there is a quest, add a quest label
                        if (entry.associatedQuestName != null) {
                            cellTable.add(
                                QuestUI.QuestMarker(
                                    gameState.eventSystem.quests.first { it.name == entry.associatedQuestName }
                                )
                            ).size(50f)
                        }
                        val meetingLabel = Label("Meeting: ${entry.place}", skin)
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

        // 현재 시간 행으로 스크롤
        dataTable.invalidate()
        scrollPane.layout()
        val rowHeight = dataTable.cells[DAYS + 1].actor.height // 첫 시간 라벨의 높이
        scrollPane.scrollTo(0f, dataTable.height - rowHeight * (currentHour - 2), 10f, rowHeight)
    }

    data class CalendarEntry(
        val time: Int,
        val title: String,
        val place: String,
        val description: String,
        var associatedMeeting: String? = null,
        var associatedQuestName: String? = null,
        var hasAlerted: Boolean = false
    )

}