package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent
import com.badlogic.gdx.utils.Array
import com.titaniumPolitics.game.core.AgendaType
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.MeetingAgenda
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.widget.WindowUI
import ktx.scene2d.label
import ktx.scene2d.scene2d
import ktx.scene2d.selectBox
import ktx.scene2d.selectBoxOf
import ktx.scene2d.table


class GameOverUI(val gameState: GameState) : WindowUI("GameOversUITitle") {
    val reasonLabel = scene2d.label(ReadOnly.prop("GameOverUI-ReasonUnknown"), "description") {
        setFontScale(0.5f)
    }
    private val dataTable = scene2d.table {
        add(this@GameOverUI.reasonLabel)
        row()
        add(QuickSave(this@GameOverUI.gameState))
        row()
        add(QuickLoad())
        row()
        table {
            it.fill()
            label(ReadOnly.prop("GameOverUI-MusicPrefix"), "docTitle") {
                it.fill()
                setFontScale(0.5f)
            }

            //Select party to perform the request.
            selectBox<String> {
                items = Array(
                    listOf(
                        "TheAlters1.mp3",
                        "Capsule_old_lighthouse_loop.mp3",
                        "mainMenu.mp3",
                        "NierAutomataTheSoundOfTheEnd.mp3"
                    ).toTypedArray()
                )
                addListener(object : ChangeListener() {
                    override fun changed(event: ChangeEvent?, actor: Actor?) {
                        SoundEngine.playMusic(selected)
                    }
                })
            }.inCell.size(300f, 100f)

        }
    }

    init {
        instance = this
        isVisible = false
        val informationPane = ScrollPane(dataTable)
        informationPane.setScrollingDisabled(true, false)
        content.add(informationPane).grow()


    }

    fun displayGameOver(reasonKey: String) {
        reasonLabel.setText(ReadOnly.prop(reasonKey))
        this.isVisible = true
    }

    companion object {
        lateinit var instance: GameOverUI
    }

}