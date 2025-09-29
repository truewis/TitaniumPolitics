package com.titaniumPolitics.game.ui.meeting

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.titaniumPolitics.game.core.Meeting
import ktx.scene2d.Scene2DSkin

class VoteResultWindowUI(
    meeting: Meeting,
    title: String = "Results"
) : Window(title, Scene2DSkin.defaultSkin) {

    init {
        val results = meeting.voteResults // Map<String, Int>
        val table = Table(Scene2DSkin.defaultSkin)
        table.add(Label("Candidates", Scene2DSkin.defaultSkin)).pad(5f)
        table.add(Label("Votes", Scene2DSkin.defaultSkin)).pad(5f)
        table.row()
        for ((candidate, votes) in results) {
            table.add(Label(candidate, Scene2DSkin.defaultSkin)).pad(5f)
            table.add(Label(votes.toString(), Scene2DSkin.defaultSkin)).pad(5f)
            table.row()
        }
        add(table).pad(10f)
        pack()
        isMovable = true
    }
}