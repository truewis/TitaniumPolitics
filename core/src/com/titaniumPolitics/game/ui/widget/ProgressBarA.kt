/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.badlogic.gdx.scenes.scene2d.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.Disableable
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.Null
import com.badlogic.gdx.utils.Pools
import kotlin.math.ceil
import kotlin.math.floor

/** A progress bar is a widget that visually displays the progress of some activity or a value within given range. The progress
 * bar has a range (min, max) and a stepping between each value it represents. The percentage of completeness typically starts out
 * as an empty progress bar and gradually becomes filled in as the task or variable value progresses.
 *
 *
 * [ChangeEvent] is fired when the progress bar knob is moved. Cancelling the event will move the knob to where it was
 * previously.
 *
 *
 * For a horizontal progress bar, its preferred height is determined by the larger of the knob and background, and the preferred
 * width is 140, a relatively arbitrary size. These parameters are reversed for a vertical progress bar.
 * @author mzechner
 * @author Nathan Sweet
 */
open class ProgressBarA(
    min: Float,
    max: Float,
    stepSize: Float,
    vertical: Boolean,
    style: ProgressBar.ProgressBarStyle
) : Widget(),
    Disableable {
    private var style: ProgressBar.ProgressBarStyle? = null
    var minValue: Float
    var maxValue: Float
    var stepSize: Float
    var value: Float
        private set
    private var animateFromValue = 0f

    /** Returns progress bar visual position within the range (as it was last calculated in [.draw]).  */
    protected var knobPosition: Float = 0f

    /** True if the progress bar is vertical, false if it is horizontal.  */
    val isVertical: Boolean
    var animateDuration = 0f
    private var animateTime = 0f
    private var animateInterpolation: Interpolation = Interpolation.linear
    private var visualInterpolation: Interpolation = Interpolation.linear
    private var disabled: Boolean = false
    private var round = true
    private var programmaticChangeEvents = true

    constructor(min: Float, max: Float, stepSize: Float, vertical: Boolean, skin: Skin) : this(
        min,
        max,
        stepSize,
        vertical,
        skin.get<ProgressBar.ProgressBarStyle?>(
            "default-" + (if (vertical) "vertical" else "horizontal"),
            ProgressBar.ProgressBarStyle::class.java
        )
    )

    constructor(min: Float, max: Float, stepSize: Float, vertical: Boolean, skin: Skin, styleName: String?) : this(
        min,
        max,
        stepSize,
        vertical,
        skin.get<ProgressBar.ProgressBarStyle?>(styleName, ProgressBar.ProgressBarStyle::class.java)
    )

    /** Creates a new progress bar. If horizontal, its width is determined by the prefWidth parameter, and its height is determined
     * by the maximum of the height of either the progress bar [NinePatch] or progress bar handle [TextureRegion]. The
     * min and max values determine the range the values of this progress bar can take on, the stepSize parameter specifies the
     * distance between individual values.
     *
     *
     * E.g. min could be 4, max could be 10 and stepSize could be 0.2, giving you a total of 30 values, 4.0 4.2, 4.4 and so on.
     * @param min the minimum value
     * @param max the maximum value
     * @param stepSize the step size between values
     * @param style the [ProgressBarStyle]
     */
    init {
        require(!(min > max)) { "max must be > min. min,max: " + min + ", " + max }
        require(!(stepSize <= 0)) { "stepSize must be > 0: " + stepSize }
        setStyle(style)
        this.minValue = min
        this.maxValue = max
        this.stepSize = stepSize
        this.isVertical = vertical
        this.value = min
        setSize(prefWidth, prefHeight)
    }

    fun setStyle(style: ProgressBar.ProgressBarStyle) {
        requireNotNull(style) { "style cannot be null." }
        this.style = style
        invalidateHierarchy()
    }

    override fun act(delta: Float) {
        super.act(delta)
        if (animateTime > 0) {
            animateTime -= delta
            val stage = getStage()
            if (stage != null && stage.actionsRequestRendering) Gdx.graphics.requestRendering()
        }
    }

    override fun draw(batch: Batch, parentAlpha: Float) {
        val style: ProgressBar.ProgressBarStyle = this.style!!
        this.disabled
        val knob = style.knob
        val currentKnob = this.knobDrawable
        val bg = this.backgroundDrawable
        val knobBefore = this.knobBeforeDrawable
        val knobAfter = this.knobAfterDrawable

        val color = getColor()
        val x = getX()
        val y = getY()
        var width = getWidth()
        var height = getHeight()
        val knobHeight = if (knob == null) 0f else knob.minHeight
        val knobWidth = if (knob == null) 0f else knob.minWidth
        val percent = this.visualPercent

        batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)

        if (this.isVertical) {
            var bgTopHeight = 0f
            var bgBottomHeight = 0f
            if (bg != null) {
                drawRound(batch, bg, x, y, width, height)
                bgTopHeight = bg.topHeight
                bgBottomHeight = bg.bottomHeight
                height -= bgTopHeight + bgBottomHeight
            }

            val total = height - knobHeight
            val beforeHeight = MathUtils.clamp(total * percent, 0f, total)
            this.knobPosition = bgBottomHeight + beforeHeight

            val knobHeightHalf = knobHeight * 0.5f
            if (knobBefore != null) {
                drawRound(
                    batch, knobBefore,  //
                    x,  //
                    y + bgBottomHeight,  //
                    width, beforeHeight + knobHeightHalf
                )
            }
            if (knobAfter != null) {
                drawRound(
                    batch, knobAfter,  //
                    x,  //
                    y + this.knobPosition + knobHeightHalf,  //
                    width,
                    total - (if (round) ceil((beforeHeight - knobHeightHalf).toDouble()).toFloat() else beforeHeight - knobHeightHalf)
                )
            }
            if (currentKnob != null) {
                val w = currentKnob.minWidth
                val h = currentKnob.minHeight
                drawRound(
                    batch, currentKnob,  //
                    x + (width - w) * 0.5f,  //
                    y + this.knobPosition + (knobHeight - h) * 0.5f,  //
                    w, h
                )
            }
        } else {
            var bgLeftWidth = 0f
            var bgRightWidth = 0f
            if (bg != null) {
                drawRound(
                    batch,
                    bg,
                    x,
                    y,
                    width,
                    height
                )
                bgLeftWidth = bg.leftWidth
                bgRightWidth = bg.rightWidth
                width -= bgLeftWidth + bgRightWidth
            }

            val total = width - knobWidth
            val beforeWidth = MathUtils.clamp(total * percent, 0f, total)
            this.knobPosition = bgLeftWidth + beforeWidth

            val knobWidthHalf = knobWidth * 0.5f
            if (knobBefore != null) {
                drawRound(
                    batch, knobBefore,  //
                    x + bgLeftWidth,  //
                    y,  //
                    beforeWidth + knobWidthHalf, height
                )
            }
            if (knobAfter != null) {
                drawRound(
                    batch, knobAfter,  //
                    x + this.knobPosition + knobWidthHalf,  //
                    y,  //
                    total - (if (round) ceil((beforeWidth - knobWidthHalf).toDouble()).toFloat() else beforeWidth - knobWidthHalf),
                    height
                )
            }
            if (currentKnob != null) {
                val w = currentKnob.minWidth
                val h = currentKnob.minHeight
                drawRound(
                    batch, currentKnob,  //
                    x + this.knobPosition + (knobWidth - w) * 0.5f,  //
                    y + (height - h) * 0.5f,  //
                    w, h
                )
            }
        }
    }

    private fun drawRound(batch: Batch?, drawable: Drawable, x: Float, y: Float, w: Float, h: Float) {
        var x = x
        var y = y
        var w = w
        var h = h
        if (round) {
            x = floor(x.toDouble()).toFloat()
            y = floor(y.toDouble()).toFloat()
            w = ceil(w.toDouble()).toFloat()
            h = ceil(h.toDouble()).toFloat()
        }
        drawable.draw(batch, x, y, w, h)
    }

    val visualValue: Float
        /** If [animating][.setAnimateDuration] the progress bar value, this returns the value current displayed.  */
        get() {
            if (animateTime > 0) return animateInterpolation.apply(
                animateFromValue,
                value,
                1 - animateTime / animateDuration
            )
            return value
        }

    /** Sets the visual value equal to the actual value. This can be used to set the value without animating.  */
    fun updateVisualValue() {
        animateTime = 0f
    }

    val percent: Float
        get() {
            if (this.minValue == this.maxValue) return 0f
            return (value - this.minValue) / (this.maxValue - this.minValue)
        }

    val visualPercent: Float
        get() {
            if (this.minValue == this.maxValue) return 0f
            return visualInterpolation.apply((this.visualValue - this.minValue) / (this.maxValue - this.minValue))
        }

    @get:Null
    protected open val backgroundDrawable: Drawable?
        get() {
            if (disabled && style!!.disabledBackground != null) return style!!.disabledBackground
            return style!!.background
        }

    @get:Null
    protected open val knobDrawable: Drawable?
        get() {
            if (disabled && style!!.disabledKnob != null) return style!!.disabledKnob
            return style!!.knob
        }

    protected open val knobBeforeDrawable: Drawable?
        get() {
            if (disabled && style!!.disabledKnobBefore != null) return style!!.disabledKnobBefore
            return style!!.knobBefore
        }

    protected open val knobAfterDrawable: Drawable?
        get() {
            if (disabled && style!!.disabledKnobAfter != null) return style!!.disabledKnobAfter
            return style!!.knobAfter
        }

    /** Sets the progress bar position, rounded to the nearest step size and clamped to the minimum and maximum values.
     * [.clamp] can be overridden to allow values outside of the progress bar's min/max range.
     * @return false if the value was not changed because the progress bar already had the value or it was canceled by a
     * listener.
     */
    fun setValue(value: Float): Boolean {
        var value = value
        value = clamp(round(value))
        val oldValue = this.value
        if (value == oldValue) return false
        val oldVisualValue = this.visualValue
        this.value = value

        if (programmaticChangeEvents) {
            val changeEvent = Pools.obtain<ChangeListener.ChangeEvent?>(ChangeListener.ChangeEvent::class.java)
            val cancelled = fire(changeEvent)
            Pools.free(changeEvent)
            if (cancelled) {
                this.value = oldValue
                return false
            }
        }

        if (animateDuration > 0) {
            animateFromValue = oldVisualValue
            animateTime = animateDuration
        }
        return true
    }

    /** Rouinds the value using the progress bar's step size. This can be overridden to customize or disable rounding.  */
    protected fun round(value: Float): Float {
        return Math.round(value / stepSize) * stepSize
    }

    /** Clamps the value to the progress bar's min/max range. This can be overridden to allow a range different from the progress
     * bar knob's range.  */
    protected fun clamp(value: Float): Float {
        return MathUtils.clamp(value, this.minValue, this.maxValue)
    }

    /** Sets the range of this progress bar. The progress bar's current value is clamped to the range.  */
    fun setRange(min: Float, max: Float) {
        require(!(min > max)) { "min must be <= max: " + min + " <= " + max }
        this.minValue = min
        this.maxValue = max
        if (value < min) setValue(min)
        else if (value > max)  //
            setValue(max)
    }


    val isAnimating: Boolean
        get() = animateTime > 0

    /** If false, [.setValue] will not fire [ChangeEvent]. The event will only be fired when the user changes
     * the slider.  */
    fun setProgrammaticChangeEvents(programmaticChangeEvents: Boolean) {
        this.programmaticChangeEvents = programmaticChangeEvents
    }

    override fun setDisabled(isDisabled: Boolean) {
        disabled = isDisabled
    }

    override fun isDisabled(): Boolean {
        return disabled
    }
}
