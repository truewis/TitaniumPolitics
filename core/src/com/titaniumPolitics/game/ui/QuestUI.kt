package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.EventSystem
import com.titaniumPolitics.game.core.GameEngine
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.core.gameActions.Move
import com.titaniumPolitics.game.ui.widget.TimeAmountUI
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.scene2d
import ktx.scene2d.*

class QuestUI(var gameState: GameState) : Table(defaultSkin) {
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
                    label(quest.name, "description") {
                        it.left()
                        it.fill()
                        setAlignment(Align.left)
                        setFontScale(0.3f)
                    }
                    row()
                    label(quest.description, "description") {
                        it.left()
                        it.fill()
                        setAlignment(Align.left)
                        setFontScale(0.4f)
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

            })
        }
        isVisible = quests.isNotEmpty()
    }

    class QuestMarker(quest: Quest) : Table(defaultSkin), KTable {
        init {
            stack {
                it.size(50f, 50f)
                image("icon_simpleshape_45") {
                    setColor(Color.GREEN)
                }
                label((quest.index).toString()) {
                    setColor(Color.WHITE)
                    setAlignment(Align.center)
                    setFontScale(2f)
                }
            }
        }

    }


    companion object {
        lateinit var instance: QuestUI
    }


}

@Serializable
data class Quest(
    val name: String,
    val description: String,
    val tgtPlace: String? = null,
    val tgtCharacter: String? = null,
    val tgtMeeting: String? = null,
    val dueTime: Int? = null,
) {
    lateinit var parent: GameState
    var isCompleted: Boolean = false
    val index: Int
        get() = parent.eventSystem.quests.indexOf(this) + 1

}