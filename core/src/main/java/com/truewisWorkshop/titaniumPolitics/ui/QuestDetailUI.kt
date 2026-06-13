package com.titaniumPolitics.game.ui

import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.widget.WindowUI
import ktx.scene2d.*

class QuestDetailUI : WindowUI("QuestInfoTitle") {
    private val dataTable = scene2d.table()

    init {
        isVisible = false
        instance = this
        val informationPane = ScrollPane(dataTable)
        informationPane.setScrollingDisabled(true, false)
        content.add(informationPane).grow()
    }

    fun refresh(quests: List<Quest>, focusedQuest: Quest? = null) {
        dataTable.clear()
        val focus = focusedQuest ?: quests.firstOrNull()
        quests.forEach { quest ->
            dataTable.add(scene2d.table {
                add(label("${quest.index}. ${quest.name}", "description") {
                    setFontScale(0.35f)
                }).growX().pad(20f)
                row()
                add(label(quest.description, "docTitle") {
                    wrap = true
                    setFontScale(0.45f)
                    if (quest == focus) {
                        setColor(1f, 1f, 0.8f, 1f)
                    }
                }).growX().padLeft(20f).padRight(20f).padBottom(20f).padTop(10f)
            }).growX()
            dataTable.row()
        }
        if (quests.isEmpty()) {
            dataTable.label(ReadOnly.prop("TasksUI-Tasks"), "description")
        }
    }

    companion object {
        lateinit var instance: QuestDetailUI
    }
}
