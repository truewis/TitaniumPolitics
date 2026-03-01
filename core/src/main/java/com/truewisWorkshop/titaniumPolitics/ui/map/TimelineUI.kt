package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.InformationType
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.widget.CharacterSelectButton
import com.titaniumPolitics.game.ui.widget.WindowUI
import ktx.scene2d.image
import ktx.scene2d.scene2d
import ktx.scene2d.table

class TimelineUI(val gameState: GameState) : WindowUI("TimelineTitle") {
    val map = TimelineMapUI(gameState)
    var selectedCharacterName = ""
    val characterSelectButton = CharacterSelectButton(availableCharacters = null, callback = { charName ->
        selectedCharacterName = charName
        map.currentPlaceMarkerWindow.selectedCharacter = charName
        map.currentPlaceMarkerWindow.mode = "Timeline"
        map.refreshTimeline(charName)
    })

    init {
        instance = this
        isVisible = false
        content.add(scene2d.table {
            add(this@TimelineUI.characterSelectButton).size(150f, 150f).pad(10f)
        }).fillX()
        content.row()
        content.add(map).grow()
    }

    fun refresh(characterName: String) {
        selectedCharacterName = characterName
        characterSelectButton.setLabel(characterName)
        characterSelectButton.charPortrait.tgtCharacter = characterName
        map.refresh()
        map.currentPlaceMarkerWindow.selectedCharacter = characterName
        map.currentPlaceMarkerWindow.mode = "Timeline"
        map.refreshTimeline(characterName)
    }

    companion object {
        lateinit var instance: TimelineUI
    }
}

class TimelineMapUI(gameState: GameState) : MapUI(gameState) {
    private val sightingDots = mutableListOf<Actor>()

    fun refreshTimeline(characterName: String) {
        sightingDots.forEach { dataTable.removeActor(it) }
        sightingDots.clear()

        if (characterName.isEmpty()) return

        val sightings = gameState.informations.values
            .filter {
                it.type == InformationType.ACTION &&
                    it.tgtCharacter == characterName &&
                    it.knownTo.contains(gameState.playerName) &&
                    gameState.places.containsKey(it.tgtPlace)
            }
            .sortedByDescending { it.tgtTime }

        if (sightings.isEmpty()) return

        val mostRecentTime = sightings.first().tgtTime
        val oldestTime = sightings.last().tgtTime
        val timeRange = (mostRecentTime - oldestTime).toFloat().coerceAtLeast(1f)

        // Group by place, show one dot per place using most recent sighting opacity
        val sightingsByPlace = sightings.groupBy { it.tgtPlace }
        sightingsByPlace.forEach { (placeName, placeSightings) ->
            val mostRecent = placeSightings.maxBy { it.tgtTime }
            val place = gameState.places[placeName] ?: return@forEach
            val coords = convertToScreenCoords(
                place.coordinates.x.toFloat(),
                place.coordinates.z.toFloat()
            )
            val age = (mostRecentTime - mostRecent.tgtTime).toFloat() / timeRange
            val alpha = 1f - age * 0.8f

            val dot = scene2d.image("BadgeRound") {
                color = Color(1f, 0f, 0f, alpha)
                touchable = Touchable.enabled
                addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        currentPlaceMarkerWindow.refresh(placeName)
                    }
                })
            }
            dot.setSize(DOT_SIZE, DOT_SIZE)
            dot.setPosition(coords.first, coords.second, Align.center)
            dataTable.addActor(dot)
            sightingDots.add(dot)
        }
    }

    companion object {
        const val DOT_SIZE = 24f
    }
}
