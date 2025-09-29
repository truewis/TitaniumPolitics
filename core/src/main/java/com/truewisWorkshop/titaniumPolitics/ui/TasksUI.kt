package com.titaniumPolitics.game.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.EventListener
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.events.IQuestEventObject
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
        docList.grow().left()
        add(scene2d.label("Tasks", "description") {
            setAlignment(Align.left)
            setFontScale(0.5f)
            color = Color.WHITE
        }).left()
        row()
        add(docScr).grow()

        gameState.updateUI += { it ->
            refreshDocList(it.eventSystem.activeQuests.toList())
        }
    }

    fun refreshDocList(quests: List<Quest>) {
        docList.clear()

        quests.forEach { quest ->
            docList.addActor(scene2d.table {
                left()
                //Number label with icon
                add(QuestMarker(quest)).size(50f)
                table {
                    it.fill()
                    it.left()
                    label(quest.name, "description") {
                        it.left()
                        setAlignment(Align.left)
                        setFontScale(0.2f)
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
                quest.display?.run {
                    row()
                    val additionalTable = table { }
                    invoke(additionalTable)
                }
                quest.getTooltip()?.run {
                    addListener(this)
                }

            })
        }
        isVisible = quests.isNotEmpty()
    }

    class QuestMarker(val quest: Quest) : Table(defaultSkin), KTable {
        init {
            stack {
                it.size(50f, 50f)
                image("icon_app_133") {
                    setColor(0.6f, 0.3f, 0.3f, 0.5f) // Semi-transparent red
                }
                label((this@QuestMarker.quest.index).toString(), "docTitle") {
                    setColor(Color.WHITE)
                    setAlignment(Align.center)
                    setFontScale(0.5f)
                }
                addListener(object : ClickListener() {
                    override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                        this@QuestMarker.quest.onClick?.invoke()
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
 * This class is serializable, but active quests are recreated on game load from the event system injectParent function.
 * Only finished quests are saved in the event log.
 * @param name The name key of the quest.
 * @param description The description key of the quest.
 */
@Serializable
data class Quest(
    val name: String,
    val description: String,
    val tgtPlace: String? = null,
    val tgtCharacters: List<String> = listOf(),
    val tgtMeeting: String? = null,
    val dueTime: Int? = null,
    @Transient
    val onClick: (() -> Unit)? = null,
    @Transient
    val display: ((Table) -> Unit)? = null,
    /**
     * Optional tooltip to display when hovering over the quest entry.
     * This is evaluated lazily when the tooltip is requested, which happens in the UI thread while the quest itself is created in the game logic thread.
     */
    @Transient
    val getTooltip: () -> EventListener? = { null }
) {
    @Transient
    lateinit var parent: GameState

    @Transient
    lateinit var event: IQuestEventObject

    //Do not check completion here, use eventObject completion instead.
    val index: Int
        get() = parent.eventSystem.activeQuests.indexOf(this) + 1

    val relatedPlace: String?
        get() = tgtPlace ?: tgtMeeting?.let { parent.scheduledMeetings[it]?.place }

    var completionTime: Int? = null

}