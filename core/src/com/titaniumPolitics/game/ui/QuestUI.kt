package com.titaniumPolitics.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.quests.QuestObject
import com.titaniumPolitics.game.ui.ClockUI.Companion.formatTime
import ktx.scene2d.*

class QuestUI(var gameState: GameState) : Table(Scene2DSkin.defaultSkin)
{
    var titleLabel: Label
    private val docList = VerticalGroup()

    init
    {
        titleLabel = Label("Quest:", skin, "trnsprtConsole")
        titleLabel.setFontScale(2f)
        add(titleLabel).growX()
        row()
        val docScr = ScrollPane(docList)
        docList.grow()

        add(docScr).grow()
        gameState.updateUI += { _ -> Gdx.app.postRunnable { refreshList(); } }
    }


    fun refreshList()
    {
        docList.clear()
        gameState.questSystem.dataBase.forEach { tobj ->
            if (tobj.state != QuestObject.QuestState.ACTIVE) return@forEach
            val t = scene2d.table {
                if (tobj.state == QuestObject.QuestState.COMPLETED) image(
                    (this@QuestUI.stage as CapsuleStage).assetManager.get(
                        "data/dev/capsuleDevBoxCheck.png",
                        Texture::class.java
                    )
                ) {
                    color = Color.GREEN
                    it.size(36f)
                } else image(
                    (this@QuestUI.stage as CapsuleStage).assetManager.get(
                        "data/dev/capsuleDevBox.png",
                        Texture::class.java
                    )
                ) {
                    color = Color.GREEN
                    it.size(36f)
                }
                label(tobj.name, "trnsprtConsole") {
                    it.growX()
                    setFontScale(2f)
                }
                if (tobj.due != null && tobj.state == QuestObject.QuestState.ACTIVE)
                {
                    label(formatTime(tobj.due), "trnsprtConsole") {
                        if (tobj.due < gameState.time) color = Color.RED
                        setFontScale(2f)
                        it.width(150f)
                    }
                    val l = Label(formatTime(tobj.due), skin, "trnsprtConsole")
                    if (tobj.due < gameState.time) l.color = Color.RED
                    l.setFontScale(2f)
                }
            }
            docList.addActor(t)
        }
        isVisible = !docList.children.isEmpty

    }


}