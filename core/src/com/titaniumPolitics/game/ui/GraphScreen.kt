import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import ktx.scene2d.label
import ktx.scene2d.scene2d
import space.earlygrey.shapedrawer.ShapeDrawer
import kotlin.math.abs


class GraphScreen(private var data: Map<Int, Float>) : Table() {


    private val font = BitmapFont()
    private lateinit var drawer: ShapeDrawer
    private var pixelTexture: TextureRegion

    private val axesPadding = 50f
    private var hoveredPoint: Pair<Float, Float>? = null
    private var hoveredLabel: String? = null

    private val xAxisLabels = mutableListOf<com.badlogic.gdx.scenes.scene2d.ui.Label>()
    private val yAxisLabels = mutableListOf<com.badlogic.gdx.scenes.scene2d.ui.Label>()

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
        val (minX, maxX, minY, maxY) = getBounds()

        val width = width - 2 * axesPadding
        val height = height - 2 * axesPadding

        // Axes
        //drawer.color = Color.BLACK
        drawer.line(axesPadding, axesPadding, axesPadding, height + axesPadding) // Y-axis
        drawer.line(axesPadding, axesPadding, width + axesPadding, axesPadding) // X-axis

        // Graph lines and points
        //drawer.color = Color.BLUE
        var prevX: Float? = null
        var prevY: Float? = null

        sorted.forEach { (x, y) ->
            val px = x.toFloat().map(minX.toFloat(), maxX.toFloat(), axesPadding, width + axesPadding)
            val py = y.toFloat().map(minY.toFloat(), maxY.toFloat(), axesPadding, height + axesPadding)

            drawer.filledCircle(px, py, 4f)
            if (prevX != null && prevY != null) {
                drawer.line(prevX!!, prevY!!, px, py, 2f)
            }
            prevX = px
            prevY = py
        }

        // Tooltip
        hoveredPoint?.let {
            font.color = Color.BLACK
            font.draw(batch, hoveredLabel, it.first + 10, it.second + 10)
        }

        // Legend
        drawer.filledCircle(width - 100, height + 30, 5f)
        font.draw(batch, "Legend:", width - 90, height + 45)
        font.draw(batch, "Data Point", width - 90, height + 30)
    }


    private fun getBounds(): Quadruple<Int, Int, Float, Float> {
        val xs = data.keys
        val ys = data.values
        return Quadruple(xs.minOrNull() ?: 0, xs.maxOrNull() ?: 1, ys.minOrNull() ?: 0f, ys.maxOrNull() ?: 1f)
    }

    override fun layout() {
        super.layout()
        generateAxisLabels()
    }

    fun refresh(new: HashMap<Int, Float>) {
        data = new
        generateAxisLabels()
    }


    /**
     * Add Scene2D Labels for axes
     */
    private fun generateAxisLabels() {
        xAxisLabels.forEach { it.remove() }
        yAxisLabels.forEach { it.remove() }
        xAxisLabels.clear()
        yAxisLabels.clear()
        val minX = data.keys.minOrNull() ?: 0
        val maxX = data.keys.maxOrNull() ?: 1
        val minY = data.values.minOrNull() ?: 0.0f
        val maxY = data.values.maxOrNull() ?: 1.0f
        val xBins = 10
        val yBins = 10
        val xStep = (maxX - minX) / xBins.toFloat()
        val yStep = (maxY - minY) / yBins.toFloat()
        val xLabelDistanceToAxis = 10f
        val yLabelDistanceToAxis = 0f
        for (i in 0..<xBins) {
            val xValue = minX + i * xStep
            // Create and position labels accordingly
            scene2d.label("%.1f".format(xValue), "docTitle") {
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
        for (i in 0..<yBins) {
            val yValue = minY + i * yStep
            // Create and position labels accordingly
            scene2d.label("%.1f".format(yValue), "docTitle") {
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

    }

    private fun Float.map(fromMin: Float, fromMax: Float, toMin: Float, toMax: Float): Float {
        return ((this - fromMin) / (fromMax - fromMin)) * (toMax - toMin) + toMin
    }

    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

}
