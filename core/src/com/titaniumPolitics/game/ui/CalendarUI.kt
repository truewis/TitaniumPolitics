package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.label
import ktx.scene2d.scene2d

class CalendarUI(val gameState: GameState) : Table(Scene2DSkin.defaultSkin) {
    private val dataTable = Table(skin)
    private val dayTable = Table(skin)
    private lateinit var scrollPane: ScrollPane
    val entries = mutableListOf<CalendarEntry>()
    val newEntries = mutableListOf<CalendarEntry>()

    init {
        scrollPane = ScrollPane(dataTable)
        scrollPane.setScrollingDisabled(true, false) // 수직 스크롤만 허용
        add(dayTable).growX().padBottom(10f).row()
        add(scrollPane).grow()

        //Mark the calendar button When new meeting is scheduled within the next 5 days.
        //Also check AssistantUI for the button blinking condition.
        gameState.onAddScheduledMeeting += { meeting ->
            if (meeting.scheduledCharacters.contains(gameState.playerName))
                addEntry(
                    meeting.time,
                    ReadOnly.prop(meeting.type.toString()),
                    ReadOnly.prop(meeting.place),
                    "A new meeting has been scheduled at ${ReadOnly.prop(meeting.place)}.",
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

    fun addEntry(time: Int, title: String, place: String, docTitle: String, associatedMeeting: String? = null) {
        val entry = CalendarEntry(time, title, place, docTitle, associatedMeeting)
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
        dayTable.add(Label("H\\D", skin, "docTitle").also {
            it.setFontScale(1f)
        }) // 왼쪽 상단 빈 칸
        for (i in 0 until DAYS) {

            val dayLabel = Label("D${i + gameState.day}", skin, "docTitle")
            if (i == 0) {
                dayLabel.color == Color.GREEN
            } else {
                //Change color here if needed
            }
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
            val hourLabel = scene2d.label(String.format("%02d00", hour), "docTitle")
            hourLabel.setFontScale(0.6f)
            if (hour == currentHour) {
                hourLabel.color = Color.GREEN
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
                                TasksUI.QuestMarker(
                                    gameState.eventSystem.quests.first { it.name == entry.associatedQuestName }
                                )
                            ).size(50f)
                        }
                        val meetingLabel = scene2d.label("Meeting: ${entry.place}", "description")
                        meetingLabel.setFontScale(0.2f)
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
        val docTitle: String,
        var associatedMeeting: String? = null,
        var associatedQuestName: String? = null,
        var hasAlerted: Boolean = false
    )

}