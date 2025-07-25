package com.titaniumPolitics.game.ui


import com.badlogic.gdx.scenes.scene2d.ui.*

import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.Character

import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.widget.WindowUI
import ktx.scene2d.Scene2DSkin

import ktx.scene2d.Scene2DSkin.defaultSkin

//Human Resource Management is currently done without information. The report is instant.
class PoliticiansInfoUI(val gameState: GameState) : Table(defaultSkin) {
    private val dataTable = Table()

    init {
        val informationPane = ScrollPane(dataTable)
        informationPane.setScrollingDisabled(false, false)
        add(informationPane).grow()


    }

    fun refresh() {
        dataTable.clear()

        // Header row
        dataTable.add(Label("Name", defaultSkin, "trnsprtConsole").apply { setFontScale(2f) }).width(400f).left()
        dataTable.add(Label("Position", defaultSkin, "trnsprtConsole").apply { setFontScale(2f) }).width(400f).left()
        dataTable.add(
            Label(
                "Mutuality",
                defaultSkin,
                "trnsprtConsole"
            ).apply { setFontScale(2f); setAlignment(Align.center) }).width(600f).center()
        dataTable.row()

        // List all characters except the player
        val player = gameState.playerName
        val allCharacters = gameState.characters.filter { it.key != player && !it.key.contains("Anon") }
        for (character in allCharacters) {
            // Name
            dataTable.add(
                Label(
                    ReadOnly.charName(character.key),
                    defaultSkin,
                    "trnsprtConsole"
                ).apply { setFontScale(2f) }).width(400f).left()

            // Position (replace with your own logic)
            val position = getCharacterPosition(character.value) // Implement this method as needed
            dataTable.add(Label(position, defaultSkin, "trnsprtConsole").apply { setFontScale(2f) }).width(400f).left()

            // Mutuality Meter
            val meter = MutualityMeter(gameState, character.key, player)
            dataTable.add(meter).width(600f).pad(30f)
            dataTable.row()
        }
    }

    private fun getCharacterPosition(character: Character): String {
        // Replace this with your own logic to determine the character's position
        return when {
            character.trait.contains("ctrler") -> "The Controller"
            character.trait.contains("observer") -> "The Observer"
            character.trait.contains("mechanic") -> "The Mechanic"
            character.trait.any { it.contains("DivisionLeader") } -> character.trait.first { it.contains("DivisionLeader") }
                .replace("DivisionLeader", "") + " Division Leader"

            character.trait.contains("engineer") -> "Engineer"
            character.trait.contains("soldier") -> "Soldier"
            else -> ""
        }
    }


}