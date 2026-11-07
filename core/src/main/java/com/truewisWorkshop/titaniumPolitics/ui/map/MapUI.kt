package com.titaniumPolitics.game.ui.map

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Color.LIGHT_GRAY
import com.badlogic.gdx.graphics.Color.RED
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.AlphaAction
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.core.ReadOnly
import com.titaniumPolitics.game.debugTools.Logger
import com.titaniumPolitics.game.ui.CapsuleStage
import com.titaniumPolitics.game.ui.TasksUI
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ktx.scene2d.*
import ktx.scene2d.Scene2DSkin.defaultSkin
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.max
import kotlin.math.sin

open class MapUI(val gameState: GameState) : Table(defaultSkin) {
    val currentConnections = arrayListOf<Connection>()
    val currentMarkers = arrayListOf<PlaceMarker>()
    val currentPlaceMarkerWindow = PlaceMarkerWindowUI(gameState, this)
    val currentPlaceIndicator = scene2d.image("Arrow2Right") {
        color = RED
        isVisible = false
    }
    private lateinit var scrollPane: ScrollPane
    val dataTable = Table(skin)
    private val background by lazy { MapBackground(gameState, this) }
    private val zoomLayer = ZoomLayer(dataTable)
    private var targetZoom = 1f
    val currentZoom: Float get() = zoomLayer.zoom
    private var zoomCenterMarker: PlaceMarker? = null
    private var zoomAnchor: Vector2? = null
    val backButton = scene2d.table {
        isVisible = false
        touchable = com.badlogic.gdx.scenes.scene2d.Touchable.enabled
        image(TextureRegionDrawable(MapArt.texture(MapArt.BACK_ICON))) {
            it.size(MapUI.BACK_ICON_SIZE)
            it.padBottom(2f)
        }
        row()
        label(ReadOnly.prop("MapUI-Back"), "docTitle") {
            setFontScale(1f / 3f)
        }
    }
    //Set when a refresh wants to focus a marker before the scroll pane has been laid out.
    private var pendingFocus: PlaceMarker? = null
    //Transparent gauges: altimeter on the right edge of the view, scale meter at the lower left.
    private val gaugeColor = Color(1f, 1f, 1f, 0.6f)
    private val altBar = Image(defaultSkin.getDrawable("white-pixel")).apply { color = gaugeColor; touchable = Touchable.disabled }
    private val altArrow = Image(defaultSkin.getDrawable("Arrow2Right")).apply { color = gaugeColor; touchable = Touchable.disabled }
    private val altLabel = scene2d.label("", "docTitle") {
        setFontScale(0.3f)
        color = gaugeColor
        touchable = Touchable.disabled
    }
    private val altTitle = scene2d.label("ALT", "docTitle") {
        setFontScale(0.2f)
        color = gaugeColor
        touchable = Touchable.disabled
    }
    private val altTickTop = Image(defaultSkin.getDrawable("white-pixel")).apply { color = gaugeColor; touchable = Touchable.disabled }
    private val altTickBottom = Image(defaultSkin.getDrawable("white-pixel")).apply { color = gaugeColor; touchable = Touchable.disabled }
    private val scaleBar = Image(defaultSkin.getDrawable("white-pixel")).apply { color = gaugeColor; touchable = Touchable.disabled }
    private val scaleTickL = Image(defaultSkin.getDrawable("white-pixel")).apply { color = gaugeColor; touchable = Touchable.disabled }
    private val scaleTickR = Image(defaultSkin.getDrawable("white-pixel")).apply { color = gaugeColor; touchable = Touchable.disabled }
    private val scaleLabel = scene2d.label("", "docTitle") {
        setFontScale(0.3f)
        color = gaugeColor
        touchable = Touchable.disabled
    }
    val PADDING = 100f
    val WIDTH = 1080f * 5
    val HEIGHT = 1080f * 5
    var minX = 0
    var minY = 0
    var maxX = 0
    var maxY = 0


    init {
        Logger.write(this::class.java.simpleName + " initialized.", Logger.LogLevel.INFO)
        scrollPane = ScrollPane(zoomLayer)
        scrollPane.setScrollingDisabled(false, false)
        add(scrollPane).grow()


        dataTable.addActor(background)
        currentPlaceMarkerWindow.isVisible = true
        add(currentPlaceMarkerWindow).growY().fill().width(400f)
        dataTable.add().grow()

        //////// Add the back button, shown only while zoomed into a place with buildings.
        addActor(backButton)
        backButton.pack()
        backButton.addListener(object : ClickListener() {
            override fun clicked(event: com.badlogic.gdx.scenes.scene2d.InputEvent?, x: Float, y: Float) {
                deselectAll()
            }
        })

        //////// Add gauges
        addActor(GridOverlay(this, { scrollPane }, { zoomLayer.zoom }))
        listOf(altBar, altArrow, altLabel, altTitle, altTickTop, altTickBottom, scaleBar, scaleTickL, scaleTickR, scaleLabel).forEach { addActor(it) }

        //////// Add current place indicator
        addActor(currentPlaceIndicator)
        currentPlaceIndicator.setSize(50f, 50f)
        currentPlaceIndicator.setOrigin(Align.center)
        currentPlaceIndicator.addAction(
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


    }


    open fun refresh() {
        //Calculate the bounds.
        minX = gameState.places.minOf { it.value.coordinates.x }.toInt()
        minY = gameState.places.minOf { it.value.coordinates.z }.toInt()
        maxX = gameState.places.maxOf { it.value.coordinates.x }.toInt()
        maxY = gameState.places.maxOf { it.value.coordinates.z }.toInt()

        //Background size is determined by the extent of the markers.
        dataTable.cells[0].size(
            WIDTH + PADDING * 2, //Add some padding
            HEIGHT + PADDING * 2 //Add some padding
        )
        dataTable.pack()
        //Start every refresh unzoomed; markers are recreated below.
        zoomLayer.zoom = 1f
        targetZoom = 1f
        zoomCenterMarker = null
        zoomAnchor = null
        backButton.isVisible = false
        currentMarkers.forEach { dataTable.removeActor(it) }
        currentMarkers.clear()
        //Draw markers for places
        gameState.places.forEach { (placeName, plObj) ->
            if (placeName in gameState.knownPlacesToPlayer && plObj.isAuthorized(gameState.playerName)) {
                if (plObj.isBuildingIn == null)
                    PlaceMarker(gameState, this, placeName).also {
                        currentMarkers.add(it)
                        // Places that are known but physically inaccessible appear half-transparent and red.
                        val isInaccessible =
                            gameState.player.place.shortestPathAndTimeTo(placeName, gameState.playerName) == null &&
                                gameState.player.place.name != placeName
                        if (isInaccessible) {
                            it.color = Color(1f, 0f, 0f, 0.5f)
                        }
                        it.onClick += {
                            currentMarkers.forEach {
                                if (it != currentMarkers.first { it.place == placeName } && it.isBuildingVisible) {
                                    it.isBuildingVisible = false
                                }
                            }
                        }
                    }
            }
        }

        background.rebuild(currentMarkers.map { it.place })
        currentConnections.forEach { dataTable.removeActor(it) }
        currentConnections.clear()
        //Draw connections between places
        val drawnPairs = hashSetOf<Pair<String, String>>()
        currentMarkers.forEach {
            val placeName = it.place
            val place = gameState.places[placeName]!!
            if (!placeName.contains("home")) {
                place.movableConnectedPlaces(gameState.playerName).forEach { connection ->
                    val key = if (placeName < connection) placeName to connection else connection to placeName
                    if (!connection.contains("home") && drawnPairs.add(key)) {
                        Connection(gameState, this, placeName, connection).also { conn ->
                            currentConnections.add(conn)
                            conn.toBack()
                        }
                    }
                }

            }
        }
        //Keep the background grid behind everything and draw selected places last.
        restack()
        //Add quest markers. Quests attach to their own node, else to the node of the building they are in.
        //A quest whose place is unknown or unavailable to the player is skipped rather than shown at a wrong place.
        gameState.eventSystem.activeQuests.forEach { quest ->
            val relatedPlace = quest.relatedPlace ?: return@forEach
            val target = currentMarkers.firstOrNull { it.place == relatedPlace }
                ?: gameState.places[relatedPlace]?.isBuildingIn?.let { host -> currentMarkers.firstOrNull { it.place == host } }
            if (target != null) target.addQuestMarker(quest)
            else Logger.write("No map marker for quest ${quest.index} at $relatedPlace.", Logger.LogLevel.INFO)
        }

        //Scroll to the player's place.
        val playerPlace = gameState.player.place
        val playerPlaceMarker = currentMarkers.firstOrNull { it.place == playerPlace.name }
        //If the player is in a building, scroll to the node hosting it and open its building ring, which zooms in.
        val focusMarker = playerPlaceMarker
            ?: currentMarkers.firstOrNull { it.place == playerPlace.isBuildingIn }?.also { it.isBuildingVisible = true }
        //The scroll pane has no size until the first layout, so centering is finished in act().
        pendingFocus = focusMarker
        if (focusMarker != null) centerOn(focusMarker)

        currentPlaceMarkerWindow.refresh(gameState.player.place.name)
        //Refresh the place marker window.

    }

    /** Draw order, back to front: grid, connections, corridor markers, place markers, the place with open buildings. */
    private fun restack() {
        background.toBack()
        currentConnections.forEach { it.toFront() }
        currentMarkers.sortedWith(compareBy({ !it.isCorridor }, { it.isBuildingVisible })).forEach { it.toFront() }
    }

    /** Called by a [PlaceMarker] when its building ring opens or closes. Zooms into the opened place, or back out. */
    fun onBuildingFocusChanged() {
        val focus = currentMarkers.firstOrNull { it.isBuildingVisible }
        if (focus != null) {
            //Zoom so that the place with its building ring fills about half of the view.
            val view = min(scrollPane.width, scrollPane.height)
            targetZoom = if (view > 0f) (view * ZOOM_VIEW_FRACTION / focus.ringDiameter).coerceIn(1f, MAX_ZOOM) else 3f
            zoomCenterMarker = focus
            zoomAnchor = null
        } else {
            targetZoom = 1f
        }
        backButton.isVisible = focus != null
        restack()
    }

    /** Closes all building rings, zooms back out, and shows the player's own place again. */
    fun deselectAll() {
        currentMarkers.forEach { if (it.isBuildingVisible) it.isBuildingVisible = false }
        currentPlaceMarkerWindow.refresh(gameState.player.place.name)
    }

    /** Scrolls so that the marker is at the center of the view, at the current zoom. */
    private fun centerOn(marker: PlaceMarker) =
        centerOnPoint(marker.x + marker.width / 2, marker.y + marker.height / 2)

    /** Scrolls so that the point (in unscaled map coordinates) is at the center of the view, at the current zoom. */
    private fun centerOnPoint(px: Float, py: Float) {
        scrollPane.validate()
        val zoom = zoomLayer.zoom
        val cx = px * zoom
        val cy = py * zoom
        scrollPane.scrollX = cx - scrollPane.scrollWidth / 2
        //Scroll Y counts down from the top of the content.
        scrollPane.scrollY = zoomLayer.height - cy - scrollPane.scrollHeight / 2
        scrollPane.updateVisualScroll()
    }

    fun convertToScreenCoords(x: Float, y: Float): Pair<Float, Float> {
        //Converts coordinates to screen coordinates. The Screen in centered at the player's location.
        var rel_x = x - gameState.player.place.coordinates.x
        var rel_y = y - gameState.player.place.coordinates.z
        if (gameState.player.place.name == "home_" + gameState.playerName) //Homes does not have coordinates, so we use the place the player is living by.
        {
            rel_x = x - gameState.places[gameState.player.livingBy]!!.coordinates.x
            rel_y = y - gameState.places[gameState.player.livingBy]!!.coordinates.z
        }
        return Pair(
            PADDING + (dataTable.width - 2 * PADDING) * (x - minX) / (maxX - minX), //We do absolute coordinates for now. We can replace x with rel_x.
            PADDING + (dataTable.height - 2 * PADDING) * (y - minY) / (maxY - minY)
        )
    }

    /** Coordinates are in units of 100 m. Altitude is z (0 at z = 0, negative below); the scale follows the zoom. */
    private fun updateGauges() {
        val spanX = maxX - minX
        val spanZ = maxY - minY
        val contentW = dataTable.width - 2 * PADDING
        val contentH = dataTable.height - 2 * PADDING
        val visible = spanX > 0 && spanZ > 0 && contentW > 0 && contentH > 0 && scrollPane.width > 0
        listOf(altBar, altArrow, altLabel, altTitle, altTickTop, altTickBottom, scaleBar, scaleTickL, scaleTickR, scaleLabel).forEach { it.isVisible = visible }
        if (!visible) return
        val zoom = zoomLayer.zoom

        //Altitude of the view center.
        val centerContentY = (scrollPane.actor.height - scrollPane.scrollY - scrollPane.scrollHeight / 2) / zoom
        val z = minY + (centerContentY - PADDING) / contentH * spanZ
        val altitude = Math.round(z * 100f)
        val sign = if (altitude < 0) "-" else ""
        altLabel.setText(sign + "%05d m".format(abs(altitude)))
        altLabel.pack()
        //The bar spans from the planet's center (-50000 m) at the bottom to the highest point on the map at the top.
        val topAlt = max(maxY * 100f, CORE_ALTITUDE + 1f)
        val fraction = ((altitude - CORE_ALTITUDE) / (topAlt - CORE_ALTITUDE)).coerceIn(0f, 1f)
        val barHeight = scrollPane.height * 0.6f
        val barBottom = scrollPane.y + (scrollPane.height - barHeight) / 2
        val barX = scrollPane.x + scrollPane.width - 24f
        altBar.setSize(2f, barHeight)
        altBar.setPosition(barX, barBottom, Align.bottomRight)
        altTickTop.setSize(10f, 2f)
        altTickTop.setPosition(barX + 1f, barBottom + barHeight, Align.topRight)
        altTickBottom.setSize(10f, 2f)
        altTickBottom.setPosition(barX + 1f, barBottom, Align.bottomRight)
        altTitle.pack()
        altTitle.setPosition(barX, barBottom + barHeight + 6f, Align.bottomRight)
        val arrowY = barBottom + barHeight * fraction
        altArrow.setSize(24f, 24f)
        altArrow.setPosition(barX - 4f, arrowY, Align.right)
        altLabel.setPosition(barX - 4f - 24f - 6f, arrowY, Align.right)

        //Scale: meters per on-screen pixel, along x.
        val metersPerPixel = 100f * spanX / (contentW * zoom)
        val raw = 120f * metersPerPixel
        val pow = Math.pow(10.0, Math.floor(Math.log10(raw.toDouble()))).toFloat()
        val nice = listOf(1f, 2f, 5f, 10f).first { it * pow >= raw * 0.6f } * pow
        val barPx = nice / metersPerPixel
        val left = scrollPane.x + 24f
        val base = scrollPane.y + 28f
        scaleBar.setSize(barPx, 2f)
        scaleBar.setPosition(left, base)
        scaleTickL.setSize(2f, 10f)
        scaleTickL.setPosition(left, base - 4f)
        scaleTickR.setSize(2f, 10f)
        scaleTickR.setPosition(left + barPx - 2f, base - 4f)
        scaleLabel.setText(if (nice >= 1000f) "%.4g km".format(nice / 1000f) else "%.0f m".format(nice))
        scaleLabel.pack()
        scaleLabel.setPosition(left, base + 10f)
    }

    override fun act(delta: Float) {
        super.act(delta)
        pendingFocus?.let { marker ->
            if (scrollPane.width > 0f && scrollPane.height > 0f) {
                pendingFocus = null
                if (marker.isBuildingVisible) onBuildingFocusChanged() //Recompute the zoom with the real view size.
                centerOn(marker)
            }
        }
        //Animate the zoom, keeping the selected place (or, after an automatic reset, the view center) fixed.
        if (zoomLayer.zoom != targetZoom) {
            val diff = targetZoom - zoomLayer.zoom
            zoomLayer.zoom = if (abs(diff) < 0.005f) targetZoom else zoomLayer.zoom + diff * min(1f, delta * 10f)
            val anchor = zoomAnchor
            if (anchor != null) centerOnPoint(anchor.x, anchor.y)
            else zoomCenterMarker?.let { centerOn(it) }
        } else if (pendingFocus == null) {
            //Zoomed into a place that the player has scrolled out of view: zoom back out, keeping the current view.
            val focus = zoomCenterMarker
            if (focus != null && focus.isBuildingVisible && targetZoom > 1f) {
                val zoom = zoomLayer.zoom
                val cx = (focus.x + focus.width / 2) * zoom
                val cy = (focus.y + focus.height / 2) * zoom
                val viewTop = scrollPane.actor.height - scrollPane.scrollY
                val outside = cx < scrollPane.scrollX || cx > scrollPane.scrollX + scrollPane.scrollWidth ||
                        cy > viewTop || cy < viewTop - scrollPane.scrollHeight
                if (outside) {
                    zoomAnchor = Vector2(
                        (scrollPane.scrollX + scrollPane.scrollWidth / 2) / zoom,
                        (viewTop - scrollPane.scrollHeight / 2) / zoom
                    )
                    deselectAll()
                }
            }
        }
        backButton.setPosition(
            scrollPane.x + scrollPane.width / 2,
            scrollPane.y + backButton.height + 20f,
            Align.center
        )
        updateGauges()
        // Read scrollPane coordinates, check if player current position is out of view, if so, create a floating indicator pointing to the player position.
        currentMarkers.firstOrNull { it.place == gameState.player.place.name }?.let { playerMarker ->
            val zoom = zoomLayer.zoom
            //Marker bounds in scroll content coordinates.
            val markerX = playerMarker.x * zoom
            val markerY = playerMarker.y * zoom
            val markerW = playerMarker.width * zoom
            val markerH = playerMarker.height * zoom
            val viewX = scrollPane.scrollX
            val viewY = scrollPane.actor.height - scrollPane.scrollY
            val viewWidth = scrollPane.scrollWidth
            val viewHeight = scrollPane.scrollHeight
            if (markerX < viewX || markerX > viewX + viewWidth ||
                markerY > viewY || markerY < viewY - viewHeight
            ) {
                //Out of view
                currentPlaceIndicator.isVisible = true
                //Calculate angle to point to the player position.
                val centerX = viewX + viewWidth / 2
                val centerY = viewY - viewHeight / 2
                val angle = atan2(
                    (markerY + markerH / 2) - centerY,
                    (markerX + markerW / 2) - centerX
                ).toDouble()
                //Place the indicator at the edge of the view.
                //Pick r so that edgeX and edgeY are at the edge of the view.
                val r = if (abs(cos(angle)) * (viewHeight - 100) < abs(sin(angle)) * (viewWidth - 100)) {
                    (viewHeight - 100) / 2 / abs(sin(angle)).toFloat()
                } else {
                    (viewWidth - 100) / 2 / abs(cos(angle)).toFloat()
                }
                val edgeX = scrollPane.x + scrollPane.width / 2 + r * cos(angle).toFloat()
                val edgeY = scrollPane.y + scrollPane.height / 2 + r * sin(angle).toFloat()
                currentPlaceIndicator.setPosition(edgeX, edgeY, Align.center)
                currentPlaceIndicator.rotation = Math.toDegrees(angle).toFloat()
            } else {
                currentPlaceIndicator.isVisible = false
            }
        }


    }

    //DO not make this class singleton, as it is used in multiple places such as PlaceSelectionUI.

    companion object {
        const val ZOOM_VIEW_FRACTION = 0.25f // The selected place fills this fraction of the view, so nearby places stay visible.
        const val MAX_ZOOM = 24f
        const val BACK_ICON_SIZE = 48f
        const val CORE_ALTITUDE = -50000f // Altitude (m) of the planet's center, the bottom of the altimeter.
    }
}

/**
 * Transparent grid over the map view with a line every [stepKm] km (coordinates are in units of 100 m).
 * Lines are drawn in screen space so they stay one pixel wide at any zoom; the step widens when lines get too dense.
 * Labels at the top (x) and left (z) edges of the view give the coordinate of each line.
 */
class GridOverlay(
    private val map: MapUI,
    private val pane: () -> ScrollPane,
    private val zoom: () -> Float
) : com.badlogic.gdx.scenes.scene2d.Group() {
    private val pixel = defaultSkin.getDrawable("white-pixel")
    private val lineColor = Color(1f, 1f, 1f, 0.06f)
    private val labelColor = Color(1f, 1f, 1f, 0.6f)
    private val labels = ArrayList<com.badlogic.gdx.scenes.scene2d.ui.Label>()
    private var vLines = FloatArray(0)
    private var hLines = FloatArray(0)
    private var vCount = 0
    private var hCount = 0

    init {
        touchable = Touchable.disabled
    }

    private fun label(index: Int): com.badlogic.gdx.scenes.scene2d.ui.Label {
        while (labels.size <= index) {
            labels.add(scene2d.label("", "docTitle") {
                setFontScale(0.2f)
                color = labelColor
            }.also { addActor(it) })
        }
        return labels[index]
    }

    private fun update() {
        val sp = pane()
        val spanX = map.maxX - map.minX
        val spanZ = map.maxY - map.minY
        val contentW = map.dataTable.width - 2 * map.PADDING
        val contentH = map.dataTable.height - 2 * map.PADDING
        var used = 0
        vCount = 0
        hCount = 0
        if (spanX <= 0 || spanZ <= 0 || contentW <= 0 || contentH <= 0 || sp.scrollWidth <= 0f) {
            labels.forEach { it.isVisible = false }
            return
        }
        val z = zoom()
        val pxPerUnitX = contentW * z / spanX
        val pxPerUnitZ = contentH * z / spanZ
        //Unit = 100 m, so 10 units = 1 km. Widen the step until lines are at least 60px apart.
        var stepKm = 1
        while (stepKm * UNITS_PER_KM * min(pxPerUnitX, pxPerUnitZ) < 60f) {
            stepKm = when (stepKm.toString().first()) { '1' -> stepKm * 2; '2' -> stepKm / 2 * 5; else -> stepKm * 2 }
        }
        val step = stepKm * UNITS_PER_KM

        val viewLeft = sp.scrollX
        val viewBottom = sp.actor.height - sp.scrollY - sp.scrollHeight
        val left = sp.x
        val bottom = sp.y
        val right = left + sp.scrollWidth
        val top = bottom + sp.scrollHeight
        val labelPad = 4f

        //Vertical lines (constant x).
        val firstX = kotlin.math.ceil(((viewLeft / z - map.PADDING) / contentW * spanX + map.minX) / step).toInt()
        var k = firstX
        while (true) {
            val unit = k * step
            val sx = left + (map.PADDING + contentW * (unit - map.minX) / spanX) * z - viewLeft
            if (sx > right) break
            if (sx >= left) {
                if (vCount >= vLines.size) vLines = vLines.copyOf(max(8, vLines.size * 2))
                vLines[vCount++] = sx
                val l = label(used++)
                l.setText("x " + k * stepKm * 10)
                l.pack()
                l.isVisible = true
                l.setPosition(sx + labelPad, bottom + labelPad)
            }
            k++
        }
        //Horizontal lines (constant z).
        val firstZ = kotlin.math.ceil(((viewBottom / z - map.PADDING) / contentH * spanZ + map.minY) / step).toInt()
        k = firstZ
        while (true) {
            val unit = k * step
            val sy = bottom + (map.PADDING + contentH * (unit - map.minY) / spanZ) * z - viewBottom
            if (sy > top) break
            if (sy >= bottom) {
                if (hCount >= hLines.size) hLines = hLines.copyOf(max(8, hLines.size * 2))
                hLines[hCount++] = sy
                val l = label(used++)
                l.setText("z " + k * stepKm * 10)
                l.pack()
                l.isVisible = true
                l.setPosition(right - l.width - labelPad, sy + labelPad)
            }
            k++
        }
        for (i in used until labels.size) labels[i].isVisible = false
    }

    override fun draw(batch: com.badlogic.gdx.graphics.g2d.Batch, parentAlpha: Float) {
        update()
        val sp = pane()
        batch.setColor(lineColor.r, lineColor.g, lineColor.b, lineColor.a * parentAlpha)
        for (i in 0 until vCount) pixel.draw(batch, vLines[i], sp.y, 1f, sp.scrollHeight)
        for (i in 0 until hCount) pixel.draw(batch, sp.x, hLines[i], sp.scrollWidth, 1f)
        super.draw(batch, parentAlpha)
    }

    companion object {
        const val UNITS_PER_KM = 10
    }
}

/**
 * Scroll content that shows [content] scaled by [zoom]. Reports its scaled size to the ScrollPane, so scrolling and
 * scroll bars follow the zoom, and converts the ScrollPane's culling area back into unscaled content coordinates.
 */
class ZoomLayer(private val content: Table) : WidgetGroup() {
    var zoom = 1f
        set(value) {
            field = value
            content.setScale(value)
            invalidateHierarchy()
        }

    init {
        content.isTransform = true
        content.setOrigin(0f, 0f)
        addActor(content)
    }

    override fun getPrefWidth() = content.prefWidth * zoom
    override fun getPrefHeight() = content.prefHeight * zoom

    override fun layout() {
        content.setBounds(0f, 0f, content.prefWidth, content.prefHeight)
    }

    override fun setCullingArea(cullingArea: Rectangle?) {
        super.setCullingArea(null)
        content.setCullingArea(cullingArea?.let { Rectangle(it.x / zoom, it.y / zoom, it.width / zoom, it.height / zoom) })
    }
}
