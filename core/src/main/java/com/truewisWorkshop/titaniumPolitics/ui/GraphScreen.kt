package com.truewisWorkshop.titaniumPolitics.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.titaniumPolitics.game.core.GameState
import com.titaniumPolitics.game.ui.widget.SimpleTextTooltipUI
import ktx.scene2d.Scene2DSkin
import ktx.scene2d.container
import ktx.scene2d.label
import ktx.scene2d.scene2d
import space.earlygrey.shapedrawer.ShapeDrawer


class GraphScreen(private var data: Map<Int, Float>, yDataType: DataType) : Table() {


    private val cache = BitmapFont() //TODO: use a skin font instead
    private lateinit var drawer: ShapeDrawer
    private var pixelTexture: TextureRegion

    private val axesPadding = 50f

    private val xAxisLabels = mutableListOf<Label>()
    private val yAxisLabels = mutableListOf<Label>()

    private val xLines = hashMapOf<String, LineAttributes>()
    private val yLines = hashMapOf<String, LineAttributes>()

    data class LineAttributes(val y: Float, val color: Color, val thickness: Float)

    var xAxisTitle: Actor? = null
    var xAxisTitleText: String? = null
    var yAxisTitle: Actor? = null
    var yAxisTitleText: String? = null
    var xDataType = DataType.TIME
        set(value) {
            field = value
            generateAxisLabels()
        }
    var yDataType = yDataType
        set(value) {
            field = value
            generateAxisLabels()
        }


    val minX
        get() = when (xDataType) {
            DataType.DURABILITY -> 0f
            DataType.TIME -> getBounds().first.toFloat()
            DataType.PERCENT -> 0f
            DataType.COUNT -> 0f
            DataType.MUTUALITY -> 0f
            DataType.PRICE -> 0f
        }
    val maxX
        get() = when (xDataType) {
            DataType.DURABILITY -> 100f
            DataType.TIME -> getBounds().second.toFloat()
            DataType.PERCENT -> 100f
            DataType.COUNT -> getBounds().second.toFloat()
            DataType.MUTUALITY -> 100f
            DataType.PRICE -> getBounds().second.toFloat()
        }
    val minY
        get() = when (yDataType) {
            DataType.DURABILITY -> 0f
            DataType.TIME -> getBounds().second.toFloat()
            DataType.PERCENT -> 0f
            DataType.COUNT -> 0f
            DataType.MUTUALITY -> 0f
            DataType.PRICE -> 0f
        }
    val maxY
        get() = when (yDataType) {
            DataType.DURABILITY -> 100f
            DataType.TIME -> getBounds().fourth
            DataType.PERCENT -> 100f
            DataType.COUNT -> getBounds().fourth
            DataType.MUTUALITY -> 100f
            DataType.PRICE -> getBounds().fourth
        }

    val effWidth get() = width - 2 * axesPadding
    val effHeight get() = height - 2 * axesPadding

    init {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.drawPixel(0, 0)
        val texture = Texture(pixmap) //remember to dispose of later
        //pixmap.dispose()
        pixelTexture = TextureRegion(texture, 0, 0, 1, 1)
        generateAxisLabels()
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)
        if (batch == null) return
        if (!::drawer.isInitialized) {
            drawer = ShapeDrawer(batch, pixelTexture)
        }

        val sorted = data.toSortedMap()


        // Axes
        //drawer.color = Color.BLACK
        drawer.line(axesPadding, axesPadding, axesPadding, effHeight + axesPadding) // Y-axis
        drawer.line(axesPadding, axesPadding, effWidth + axesPadding, axesPadding) // X-axis

        // Horizontal lines
        yLines.forEach { (key, attr) ->
            val y = attr.y.map(minY, maxY, axesPadding, effHeight + axesPadding)
            drawer.setColor(attr.color)
            drawer.line(axesPadding, y, effWidth + axesPadding, y, attr.thickness)
            cache.color = attr.color
            cache.draw(batch, key, effWidth - axesPadding, y + 15)
        }
        // Vertical lines
        xLines.forEach { (key, attr) ->
            val x = attr.y.map(
                minX, maxX, axesPadding,
                effWidth + axesPadding
            )
            drawer.setColor(attr.color)
            drawer.line(
                x, axesPadding, x, effHeight + axesPadding, attr.thickness
            )
            cache.color = attr.color
            cache.draw(batch, key, x + 15, effHeight - axesPadding)
        }
        drawer.setColor(Color.WHITE)
        cache.color = Color.WHITE

        // Graph lines and points
        //drawer.color = Color.BLUE
        var prevX: Float? = null
        var prevY: Float? = null

        sorted.forEach { (x, y) ->
            val px = x.toFloat().map(minX.toFloat(), maxX.toFloat(), axesPadding, effWidth + axesPadding)
            val py = y.toFloat().map(minY.toFloat(), maxY.toFloat(), axesPadding, effHeight + axesPadding)

            drawer.filledCircle(px, py, 4f)
            if (prevX != null && prevY != null) {
                drawer.line(prevX!!, prevY!!, px, py, 2f)
            }
            prevX = px
            prevY = py
        }

        // Legend
        drawer.filledCircle(width - 100, height + 30, 5f)
        cache.draw(batch, "Legend:", width - 90, height + 45)
        cache.draw(batch, "Data Point", width - 90, height + 30)
    }


    private fun getBounds(): Quadruple<Int, Int, Float, Float> {
        val xs = data.keys
        val ys = data.values
        return Quadruple(xs.minOrNull() ?: 0, xs.maxOrNull() ?: 1, ys.minOrNull() ?: 0f, ys.maxOrNull() ?: 1f)
    }

    override fun layout() {
        super.layout()
        generateAxisLabels()
        vertices.forEach { it.value.forEach { v -> v.remove() } }
        vertices.clear()
        data.forEach {
            createVertex("default", it.key.toFloat(), it.value)
        }
    }

    fun refresh(new: Map<Int, Float>, yDataType: DataType = this.yDataType) {
        data = new
        this.yDataType = yDataType
        print(data)
        invalidate()
    }

    private fun generateLegend() {
        // Implement legend generation if needed
    }

    fun addHorizontalLine(yValue: Float, color: Color = Color.BLACK, thickness: Float = 2f, key: String) {
        yLines[key] = LineAttributes(yValue, color, thickness)
        invalidate()
    }

    fun removeHorizontalLine(key: String) {
        yLines.remove(key)
        invalidate()
    }

    fun addVerticalLine(xValue: Float, color: Color = Color.BLACK, thickness: Float = 2f, key: String) {
        xLines[key] = LineAttributes(xValue, color, thickness)
        invalidate()
    }

    fun removeVerticalLine(key: String) {
        xLines.remove(key)
        invalidate()
    }

    /**
     * Add Scene2D Labels for axes
     */
    private fun generateAxisLabels() {
        xAxisLabels.forEach { it.remove() }
        yAxisLabels.forEach { it.remove() }
        xAxisLabels.clear()
        yAxisLabels.clear()
        val xBins = 10
        val yBins = 10
        val xStep = (maxX - minX) / xBins.toFloat()
        val yStep = (maxY - minY) / yBins.toFloat()
        val xLabelDistanceToAxis = 10f
        val yLabelDistanceToAxis = 0f
        for (i in 1..<xBins) { //Do not draw on the axis itself, so start at 1.
            val xValue = minX + i * xStep
            // Create and position labels accordingly
            scene2d.label(formatValue(xValue, xDataType), "docTitle") {
                setFontScale(0.3f)
                this@GraphScreen.addActor(this)
                xAxisLabels.add(this)
                setPosition(
                    axesPadding + i * (this@GraphScreen.width - 2 * axesPadding) / xBins,
                    axesPadding - xLabelDistanceToAxis,
                    Align.center
                )
            }
        }
        xAxisTitle?.remove()
        xAxisTitle = scene2d.label(xAxisTitleText ?: xDataType.name, "docTitle") {
            setFontScale(0.4f)
            this@GraphScreen.addActor(this)
            setAlignment(Align.center)
            setPosition(
                axesPadding + (this@GraphScreen.width - 2 * axesPadding) / 2,
                axesPadding - xLabelDistanceToAxis - 20f,
                Align.center
            )
        }
        for (i in 1..<yBins) { //Do not draw on the axis itself, so start at 1.
            val yValue = minY + i * yStep
            // Create and position labels accordingly
            scene2d.label(formatValue(yValue, yDataType), "docTitle") {
                setFontScale(0.3f)
                this@GraphScreen.addActor(this)
                setAlignment(Align.right)
                yAxisLabels.add(this)
                setPosition(
                    axesPadding - yLabelDistanceToAxis,
                    axesPadding + i * (this@GraphScreen.height - 2 * axesPadding) / yBins,
                    Align.right
                )
            }
        }
        yAxisTitle?.remove()
        yAxisTitle = scene2d.container(scene2d.label(yAxisTitleText ?: yDataType.name, "docTitle") {
            setFontScale(0.4f)
            this@GraphScreen.addActor(this)
            setAlignment(Align.center)
        }) {
            this.isTransform = true
        }
        addActor(yAxisTitle)
        yAxisTitle!!.setPosition(
            axesPadding - yLabelDistanceToAxis - 40f,
            axesPadding + (this@GraphScreen.height - 2 * axesPadding) / 2,
            Align.center
        )
        yAxisTitle!!
        yAxisTitle!!.rotateBy(90f)

    }

    val vertices = mutableMapOf<String, ArrayList<Actor>>()

    fun createVertex(key: String, xValue: Float, yValue: Float): Actor {
        return object : Image(Scene2DSkin.defaultSkin.getDrawable("white-pixel")) {
            val xValue = xValue
            val yValue = yValue
            override fun layout() {
                super.layout()
                setPosition(
                    this.xValue.map(minX, maxX, axesPadding, effWidth + axesPadding),
                    this.yValue.map(minY, maxY, axesPadding, effHeight + axesPadding), Align.center
                )
            }

        }.apply {
            setSize(15f, 15f)
            color = Color.RED
            if (!vertices.containsKey(key)) {
                vertices[key] = arrayListOf()
            }
            vertices[key]!!.add(this)
            addActor(this)
            addListener(
                SimpleTextTooltipUI(
                    "(${formatValue(xValue, xDataType)}, ${
                        formatValue(
                            yValue,
                            yDataType
                        )
                    })"
                )
            )//TODO: does not work currently. Why?
            this.layout()
            this.debug()
        }
    }

    private fun Float.map(fromMin: Float, fromMax: Float, toMin: Float, toMax: Float): Float {
        return ((this - fromMin) / (fromMax - fromMin)) * (toMax - toMin) + toMin
    }

    fun formatValue(value: Float, type: DataType): String {
        return when (type) {
            DataType.TIME -> GameState.formatTime(value.toInt())
            DataType.DURABILITY -> "%.1f".format(value)
            DataType.PERCENT -> "%.1f%%".format(value)
            DataType.COUNT -> "%.0f".format(value)
            DataType.MUTUALITY -> "%.1f%%".format(value)
            DataType.PRICE -> "$%.0f".format(value)
        }
    }

    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
    enum class DataType { DURABILITY, TIME, PERCENT, COUNT, MUTUALITY, PRICE }
}
