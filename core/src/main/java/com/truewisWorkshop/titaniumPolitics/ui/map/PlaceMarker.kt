package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.ui.Quest
import com.titaniumPolitics.game.ui.TasksUI
import ktx.scene2d.Scene2DSkin.defaultSkin
import ktx.scene2d.label
import ktx.scene2d.scene2d
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.min
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * A node on the map.
 *
 * Buildings: collapsed, a small count badge sits on the node; expanded, the buildings appear as a ring of icons
 * around the node. The ring is a plain [Group] with explicit geometry, so it never depends on a layout pass.
 *
 * Quests: shown as small markers in one row beside the node (or beside the building they belong to when the ring is
 * open). At most [MAX_QUESTS_SHOWN] slots are used per row; the rest collapse into a "+N" label.
 * Quests are never hidden behind buildings and are always touchable.
 */
open class PlaceMarker(val gameState: GameState, val owner: MapUI, val place: String) : Button() {
    val onClick = arrayListOf<(String) -> Unit>()
    val buildingsAvailableToPlayerList = gameState.places.filter {
        it.value.isBuildingIn == place && it.value.isAuthorized(gameState.playerName)
    }
    val buildingPlaceMarkers = mutableListOf<HomePlaceMarker>()

    // Buildings sit inside the node icon: the ring never grows, the map zooms in instead.
    private val ringRadius = SIZE * 0.3f
    private val backdropRadius = SIZE * 0.45f
    private val chipSize: Float = run {
        val n = buildingsAvailableToPlayerList.size
        val chord = if (n > 1) 2 * ringRadius * sin(PI.toFloat() / n) else SIZE
        min(SIZE * 0.3f, chord * 0.9f)
    }

    /** Diameter of the building backdrop in map units; MapUI zooms so that this fills part of the view. */
    val ringDiameter: Float get() = SIZE
    val isCorridor = place.contains("corridor")
    private val nodeSize = if (isCorridor) CORRIDOR_SIZE else SIZE
    private val buildingRing = Group().apply {
        setSize(SIZE, SIZE)
        setOrigin(Align.center)
    }

    private val buildingCountBadge = Group().apply {
        touchable = Touchable.disabled // The badge must never block clicks on the node.
        setSize(BADGE_SIZE, BADGE_SIZE)
    }

    val questMarkers = mutableListOf<TasksUI.QuestMarker>()
    private val overflowLabels = hashMapOf<Any, Label>()

    init {
        //Set style of the button to the default skin. Corridors are small round dots, so they do not compete with places.
        if (isCorridor) {
            val dot = defaultSkin.getDrawable("BadgeRound")
            style = ButtonStyle().apply {
                up = dot
                down = dot
            }
            color = Color(0.75f, 0.85f, 0.95f, 1f)
        } else {
            style = defaultSkin.get("diamond", ButtonStyle::class.java)
        }
        val start: Pair<Float, Float> = owner.convertToScreenCoords(
            gameState.places[place]!!.coordinates.x.toFloat(),
            gameState.places[place]!!.coordinates.z.toFloat()
        )
        //Set marker to different color if player is here.
        if (gameState.player.place.name == place) {
            color = Color.RED
            addAction(
                Actions.forever(
                    Actions.sequence(
                        Actions.delay(0.5f),
                        AlphaAction().apply {
                            duration = 0.2f
                            alpha = 0f
                        },
                        AlphaAction().apply {
                            duration = 0.2f
                            alpha = 1f
                        }
                    )))
            val infoText = scene2d.label(ReadOnly.prop("PlaceMarker-YouAreHere"), "docTitle") {
                setFontScale(0.2f)
                setAlignment(Align.center)
                color = Color.RED
                touchable = Touchable.disabled // Make the label not interactable as it will cover other markers
            }
            addActor(infoText)
            infoText.setPosition(
                nodeSize / 2,
                -10f, // Position it below the marker
                Align.center
            )
        }
        owner.dataTable.addActor(this)

        this.setSize(nodeSize, nodeSize)
        this.setPosition(start.first - width / 2, start.second - height / 2)
        this.addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            override fun changed(
                event: ChangeEvent?,
                actor: Actor?
            ) {
                if (isBuildingVisible)// If buildings are already visible, do nothing.
                    return
                onClick.forEach { it(place) }
                //Open Place UI
                owner.currentPlaceMarkerWindow.refresh(place)
                if (buildingsAvailableToPlayerList.isNotEmpty())
                    isBuildingVisible = true
            }
        })

        if (buildingsAvailableToPlayerList.isNotEmpty()) {
            buildBuildingRing()
            buildBuildingBadge()
        }
    }

    private fun buildBuildingRing() {
        val backdrop = Image(defaultSkin.getDrawable("BadgeRound"))
        backdrop.setColor(1f, 1f, 1f, 0.3f) // Semi-transparent white
        backdrop.setSize(backdropRadius * 2, backdropRadius * 2)
        backdrop.setPosition(SIZE / 2, SIZE / 2, Align.center)
        backdrop.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                onClick.forEach { it(place) }
                owner.currentPlaceMarkerWindow.refresh(place)
            }
        })
        buildingRing.addActor(backdrop)

        val total = buildingsAvailableToPlayerList.size
        buildingsAvailableToPlayerList.keys.forEachIndexed { index, buildingName ->
            val chip = HomePlaceMarker(gameState, owner, buildingName)
            buildingRing.addActor(chip)
            buildingPlaceMarkers.add(chip)
            //Evenly distributed on the ring, starting from the top and going clockwise.
            val angle = PI.toFloat() / 2 - index * 2 * PI.toFloat() / total
            chip.setSize(chipSize, chipSize)
            val radius = ringRadius //Always on the periphery, never at the center.
            chip.setPosition(
                SIZE / 2 + radius * cos(angle),
                SIZE / 2 + radius * sin(angle),
                Align.center
            )
        }
        addActor(buildingRing)
        buildingRing.setPosition(width / 2, height / 2, Align.center)
        buildingRing.isVisible = false
    }

    private fun buildBuildingBadge() {
        val bg = Image(defaultSkin.getDrawable("BadgeRound"))
        bg.setSize(BADGE_SIZE, BADGE_SIZE)
        bg.color = Color(0.2f, 0.2f, 0.3f, 0.9f)
        val count = scene2d.label(buildingsAvailableToPlayerList.size.toString(), "docTitle") {
            setFontScale(0.2f)
            setAlignment(Align.center)
            setSize(BADGE_SIZE, BADGE_SIZE)
        }
        buildingCountBadge.addActor(bg)
        buildingCountBadge.addActor(count)
        addActor(buildingCountBadge)
        //Bottom-right corner of the node.
        buildingCountBadge.setPosition(width - BADGE_SIZE / 3, -BADGE_SIZE / 2)
    }

    /** Adds a marker for the quest unless one is already present. */
    fun addQuestMarker(quest: Quest) {
        if (questMarkers.none { it.quest.index == quest.index }) {
            val marker = TasksUI.QuestMarker(quest, QUEST_SIZE)
            marker.setSize(QUEST_SIZE, QUEST_SIZE)
            questMarkers.add(marker)
            addActor(marker)
        }
    }

    override fun draw(batch: com.badlogic.gdx.graphics.g2d.Batch, parentAlpha: Float) {
        //Gold silhouette behind the node while it is the place shown in the info window.
        if (owner.currentPlaceMarkerWindow.placeDisplayed == place) {
            val d = style.up
            if (d != null) {
                val grow = width * 0.5f
                val c = HomePlaceMarker.SELECTION_COLOR
                batch.setColor(c.r, c.g, c.b, parentAlpha * color.a)
                d.draw(batch, x - grow / 2, y - grow / 2, width + grow, height + grow)
            }
        }
        super.draw(batch, parentAlpha)
        if (isBuildingVisible && leaders.isNotEmpty()) {
            val c = TasksUI.QuestMarker.COLOR
            batch.setColor(c.r, c.g, c.b, 0.9f * parentAlpha * color.a) //color.a carries the blink of the player's marker.
            leaders.forEach { p -> drawSegment(batch, p[0], p[1], p[2], p[3]) }
        }
    }

    override fun act(delta: Float) {
        super.act(delta)
        layoutQuestMarkers()
    }

    /**
     * Quests whose building has an icon on the open ring go beside that icon; every other quest goes in the row beside
     * the node (outside the ring when it is open). This also covers quests of buildings the player cannot see.
     */
    private fun layoutQuestMarkers() {
        val groups = questMarkers.groupBy { marker ->
            if (isBuildingVisible) buildingPlaceMarkers.firstOrNull { it.place == marker.quest.relatedPlace } else null
        }
        overflowLabels.values.forEach { it.isVisible = false }
        //When the map is zoomed into this node, quest markers keep their on-screen size.
        val s = if (isBuildingVisible) 1f / max(owner.currentZoom, 1f) else 1f
        val q = QUEST_SIZE * s
        val gap = 2f * s
        leaders.clear()
        val cx = width / 2
        val cy = height / 2

        groups.forEach { (chip, markers) ->
            //Quests of a building go outside the node's perimeter, in the direction of its icon, joined to it by a leader line.
            val chipX = if (chip != null) buildingRing.x + chip.x + chip.width / 2 else 0f
            val chipY = if (chip != null) buildingRing.y + chip.y + chip.height / 2 else 0f
            val outX = chipX - cx
            val outY = chipY - cy
            val slots = if (chip == null) MAX_QUESTS_SHOWN else MAX_QUESTS_PER_CHIP
            // With overflow, the last slot is taken by the "+N" label.
            val markerSlots = if (markers.size > slots) slots - 1 else markers.size
            //Leaders run at 45 degrees only. Pick the diagonals closest to pointing away from the node center first.
            val diagonals = listOf(1f to 1f, -1f to 1f, -1f to -1f, 1f to -1f)
                .sortedByDescending { (ux, uy) -> ux * outX + uy * outY }
            val leaderLength = 2f * (SIZE / 2 + q * 0.9f - ringRadius)

            //Center of slot i: row beside the node for unattached quests, else at the end of a diagonal leader.
            fun slotCenter(i: Int): Pair<Float, Float> =
                if (chip == null) {
                    val ox = if (isBuildingVisible) cx + backdropRadius + 4f * s else width + 4f
                    Pair(ox + q / 2 + i * (q + gap), cy)
                } else {
                    val (ux, uy) = diagonals[i % 4]
                    val d = leaderLength / SQRT2
                    Pair(chipX + ux * d, chipY + uy * d)
                }

            markers.forEachIndexed { i, marker ->
                marker.isVisible = i < markerSlots
                marker.touchable = Touchable.enabled
                marker.isTransform = true
                marker.setOrigin(0f, 0f)
                marker.setScale(s) //The marker keeps its layout size; scaling shrinks it to its on-screen size.
                val (mx, my) = slotCenter(i)
                marker.setPosition(mx - q / 2, my - q / 2)
                if (chip != null && i < markerSlots) {
                    //Trim the line to the edges of the building icon and the quest icon.
                    val (ux, uy) = diagonals[i % 4]
                    val k = 1f / SQRT2
                    val t0 = chip.width / 2
                    val t1 = leaderLength - q / 2
                    leaders.add(floatArrayOf(chipX + ux * k * t0, chipY + uy * k * t0, chipX + ux * k * t1, chipY + uy * k * t1))
                }
            }
            if (markers.size > slots) {
                val key: Any = chip ?: this
                val label = overflowLabels.getOrPut(key) {
                    scene2d.label("", "docTitle") {
                        setAlignment(Align.center)
                        touchable = Touchable.disabled
                    }.also { addActor(it) }
                }
                label.setFontScale(0.25f * s)
                label.setText("+" + (markers.size - markerSlots))
                label.setAlignment(Align.left)
                label.setSize(q, q)
                //Right side of the last visible quest marker.
                val (lx, ly) = slotCenter(max(markerSlots - 1, 0))
                label.setPosition(lx + q / 2 + gap, ly - q / 2)
                label.isVisible = true
            }
        }
        leaderThickness = 1.5f * s
    }

    private val leaders = mutableListOf<FloatArray>()
    private var leaderThickness = 1f
    private val leaderPixel = defaultSkin.getRegion("white-pixel")

    private fun drawSegment(batch: com.badlogic.gdx.graphics.g2d.Batch, x1: Float, y1: Float, x2: Float, y2: Float) {
        val len = hypot(x2 - x1, y2 - y1)
        if (len < 0.001f) return
        val angle = Math.toDegrees(atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())).toFloat()
        batch.draw(
            leaderPixel, x + x1, y + y1 - leaderThickness / 2, 0f, leaderThickness / 2,
            len, leaderThickness, 1f, 1f, angle
        )
    }

    var isBuildingVisible = false
        set(value) {
            //Failsafe: a node without accessible buildings has no ring to open.
            if (value && buildingsAvailableToPlayerList.isEmpty()) return
            field = value
            isDisabled = value
            buildingCountBadge.isVisible = !value
            buildingRing.clearActions()
            if (value) {
                buildingRing.isVisible = true
                buildingRing.color.a = 0f
                buildingRing.setScale(0.1f)
                buildingRing.addAction(
                    Actions.parallel(
                        Actions.fadeIn(0.3f),
                        Actions.scaleTo(1f, 1f, 0.3f)
                    )
                )
            } else {
                buildingRing.addAction(
                    Actions.sequence(
                        Actions.parallel(
                            Actions.fadeOut(0.3f),
                            Actions.scaleTo(0.1f, 0.1f, 0.3f)
                        ),
                        Actions.run { buildingRing.isVisible = false }
                    )
                )
            }
            owner.onBuildingFocusChanged()
        }


    companion object {
        const val SIZE = 30f // Size of the marker
        const val CORRIDOR_SIZE = 14f // Size of a corridor marker
        const val CHIP_SIZE = 24f // Size of a building icon on the ring
        const val BADGE_SIZE = 16f // Size of the building count badge
        const val QUEST_SIZE = 48f // Size of a quest marker on the map
        private const val SQRT2 = 1.41421356f
        const val MAX_QUESTS_SHOWN = 4 // Quest slots in the row beside a node, including the "+N" slot
        const val MAX_QUESTS_PER_CHIP = 2 // Quest slots beside a building icon, including the "+N" slot
    }


}
