package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.widget.TimeAmountUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin

class TasksUI(var gameState: GameState) : Table(defaultSkin) {
    private val docList = VerticalGroup()

    init {
        instance = this
        isVisible = false
        val docScr = ScrollPane(docList)
        docList.grow()
        add(scene2d.label("Tasks", "description") {
            setAlignment(Align.left)
            setFontScale(0.5f)
            color = Color.WHITE
        }).left()
        row()
        add(docScr).grow()

        gameState.updateUI += { it ->
            refreshDocList(it.eventSystem.quests.toList())
        }
    }

    fun refreshDocList(quests: List<Quest>) {
        docList.clear()

        quests.forEach { quest ->
            docList.addActor(scene2d.table {
                //Number label with icon
                add(QuestMarker(quest)).size(50f)
                table {
                    it.size(500f, 50f)
                    it.fill()
                    it.left()
                    label(quest.name, "description") {
                        it.left()
                        it.fill()
                        setAlignment(Align.left)
                        setFontScale(0.25f)
                    }
                    row()
                    label(quest.description, "description") {
                        it.left()
                        it.fill()
                        setAlignment(Align.left)
                        setFontScale(0.3f)
                    }
                    addAction(
                        //Blinking effect for all quests, as an eyecatcher.
                        Actions.forever(
                            Actions.sequence(
                                Actions.alpha(0.5f, 0.5f),
                                Actions.alpha(1f, 0.5f)
                            )
                        )
                    )

                }
                //Display due time if it exists
                if (quest.dueTime != null) {
                    add(TimeAmountUI(quest.dueTime - gameState.time))
                }

                addListener(object : ClickListener() {
                    override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                        quest.onClick?.invoke()
                    }
                })

            })
        }
        isVisible = quests.isNotEmpty()
    }

    class QuestMarker(quest: Quest) : Table(defaultSkin), KTable {
        init {
            stack {
                it.size(50f, 50f)
                image("icon_app_133") {
                    setColor(0.6f, 0.3f, 0.3f, 0.5f) // Semi-transparent red
                }
                label((quest.index).toString(), "docTitle") {
                    setColor(Color.WHITE)
                    setAlignment(Align.center)
                    setFontScale(0.5f)
                }
                addListener(object : ClickListener() {
                    override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                        quest.onClick?.invoke()
                    }
                })
            }
        }

    }


    companion object {
        lateinit var instance: TasksUI
    }


}

/**
 * A quest assigned to the player.
 * This class is solely for UI representation of quests and should not contain any game state variable.
 * This class is not serializable, as quests are recreated on game load from the event system injectParent function.
 */
data class Quest(
    val name: String,
    val description: String,
    val tgtPlace: String? = null,
    val tgtCharacter: String? = null,
    val tgtMeeting: String? = null,
    val dueTime: Int? = null,
    val onClick: (() -> Unit)? = null
) {
    lateinit var parent: GameState

    //Do not check completion here, use eventObject completion instead.
    val index: Int
        get() = parent.eventSystem.quests.indexOf(this) + 1

}