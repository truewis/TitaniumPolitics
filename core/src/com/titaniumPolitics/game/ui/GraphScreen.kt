import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.titaniumPolitics.game.ui.widget.WindowUI
import space.earlygrey.shapedrawer.*
import kotlin.math.abs


class GraphScreen(private val data: HashMap<Int, Double>) : WindowUI("Graph") {


    private val font = BitmapFont()
    private lateinit var drawer: ShapeDrawer
    private var pixelTexture: TextureRegion

    private val padding = 50f
    private var hoveredPoint: Pair<Float, Float>? = null
    private var hoveredLabel: String? = null

    init {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.drawPixel(0, 0)
        val texture = Texture(pixmap) //remember to dispose of later
        //pixmap.dispose()
        pixelTexture = TextureRegion(texture, 0, 0, 1, 1)
        setSize(800f, 600f)

        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
                hoveredPoint = null
                hoveredLabel = null

                val (minX, maxX, minY, maxY) = getBounds()
                val sorted = data.toSortedMap()

                val width = width - 2 * padding
                val height = height - 2 * padding

                val localY = screenY.toFloat()

                sorted.forEach { (x, y) ->
                    val normX = (x - minX) / (maxX - minX).toFloat()
                    val normY = (y - minY) / (maxY - minY).toFloat()
                    val px = x.toFloat().map(minX.toFloat(), maxX.toFloat(), padding, width + padding)
                    val py = y.toFloat().map(minY.toFloat(), maxY.toFloat(), padding, height + padding)

                    val dist = 10f
                    if (abs(screenX - px) < dist && abs(Gdx.graphics.height - screenY - py) < dist) {
                        hoveredPoint = px to py
                        hoveredLabel = "($x, ${"%.2f".format(y)})"
                    }
                }

                return true
            }
        }
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        if (batch == null) return
        if (!::drawer.isInitialized) {
            drawer = ShapeDrawer(batch, pixelTexture)
        }

        val sorted = data.toSortedMap()
        val (minX, maxX, minY, maxY) = getBounds()

        val width = width - 2 * padding
        val height = height - 2 * padding

        // Axes
        //drawer.color = Color.BLACK
        drawer.line(padding, padding, padding, height + padding) // Y-axis
        drawer.line(padding, padding, width + padding, padding) // X-axis

        // Graph lines and points
        //drawer.color = Color.BLUE
        var prevX: Float? = null
        var prevY: Float? = null

        sorted.forEach { (x, y) ->
            val px = x.toFloat().map(minX.toFloat(), maxX.toFloat(), padding, width + padding)
            val py = y.toFloat().map(minY.toFloat(), maxY.toFloat(), padding, height + padding)

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


    private fun getBounds(): Quadruple<Int, Int, Double, Double> {
        val xs = data.keys
        val ys = data.values
        return Quadruple(xs.minOrNull() ?: 0, xs.maxOrNull() ?: 1, ys.minOrNull() ?: 0.0, ys.maxOrNull() ?: 1.0)
    }

    private fun Float.map(fromMin: Float, fromMax: Float, toMin: Float, toMax: Float): Float {
        return ((this - fromMin) / (fromMax - fromMin)) * (toMax - toMin) + toMin
    }

    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

}
