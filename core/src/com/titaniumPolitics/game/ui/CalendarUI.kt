package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.image
import ktx.scene2d.label
import ktx.scene2d.scene2d
import ktx.scene2d.stack

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
                    ReadOnly.placeProp(meeting.place),
                    "A new meeting has been scheduled at ${ReadOnly.placeProp(meeting.place)}.",
                    gameState.scheduledMeetings.entries.find { it.value == meeting }?.key
                )
        }

        gameState.timeChanged += { _, time ->
            // Check if there is an entry within the next hour
            entries.firstOrNull { it.time - time in 0..3600 / ReadOnly.DT && !it.hasAlerted }?.let {
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

        val yearLabel =
            Label(gameState.formatDate("year") + " " + ReadOnly.prop("CalendarTitle"), skin, "docTitle").also {
                it.setFontScale(0.5f)
                it.setAlignment(Align.center)
            }
        addActor(yearLabel)
        yearLabel.setPosition(
            width / 2,
            height + 50f /*Somehow it overlaps with date label otherwise*/,
            Align.center
        ) // Set position of the year label


        // 헤더: 시간/요일
        dayTable.add(Label("H\\D", skin, "docTitle").also {
            it.setFontScale(1f)
        }) // 왼쪽 상단 빈 칸
        for (i in 0 until DAYS) {

            val dayLabel = Label(gameState.formatDate("monthDate", 60 * 24 * i), skin, "docTitle")
            if (i == 0) {
                dayLabel.color = Color.GREEN
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
                    val st = scene2d.stack {
                        add(createCellBackground(hour == currentHour && dayOffset == 0))
                        add(cellTable)
                    }
                    entriesAtThisHour.forEach { entry ->
                        //if there is a quest, add a quest label
                        if (entry.associatedQuestName != null) {
                            cellTable.add(
                                TasksUI.QuestMarker(
                                    gameState.eventSystem.quests.firstOrNull { it.name == entry.associatedQuestName }
                                        ?: return@forEach /*The quest is finished. No need to display marker anymore.*/
                                )
                            ).size(50f)
                        }
                        val meetingLabel = scene2d.label("Meeting: ${entry.place}", "description")
                        meetingLabel.setFontScale(0.2f)
                        cellTable.add(meetingLabel).left()
                        cellTable.row()
                    }
                    dataTable.add(st).growX().left()
                } else {
                    dataTable.add(createCellBackground(hour == currentHour && dayOffset == 0)).fill()
                }
            }
            dataTable.row()
        }

        // 현재 시간 행으로 스크롤
        dataTable.invalidate()
        scrollPane.layout()
        val rowHeight = dataTable.getRowHeight(1)
        println("Calendar height:$rowHeight")
        scrollPane.scrollTo(0f, dataTable.height - rowHeight * (currentHour), 10f, rowHeight)
    }

    fun createCellBackground(highlight: Boolean) = scene2d.image("icon_simpleshape_4").apply {
        if (highlight)
            setColor(0f, 1f, 0f, 0.5f)
        else
            setColor(0.5f, 0.5f, 0.5f, 0.5f)
    }

    fun timeToNextScheduledMeeting(): Int? {
        val upcomingMeetings = entries.filter { it.time > gameState.time }
        return if (upcomingMeetings.isNotEmpty()) {
            upcomingMeetings.minByOrNull { it.time }!!.time - gameState.time
        } else {
            null
        }
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