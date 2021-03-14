package me.apomazkin.view_progress_quiz

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import me.apomazkin.core_util.toPx
import kotlin.properties.Delegates

private const val DEFAULT_DOT_RADIUS_DP = 4
private const val DEFAULT_CURRENT_DOT_STROKE_DP = 1
private const val DEFAULT_CURRENT_DOT_RADIUS_DP = 4
private const val DEFAULT_SPACE_BETWEEN_DP = 16
private const val DEFAULT_COUNT = 10


////TODO kilg 14.03.2021 20:56 pack all calculated data in object before draw

/**
 * ProgressQuizView
 *
 * Terminology:
 * Dot - circle that indicates the single quiz
 * CurrentDot - stroke circle that indicate current quiz
 *
 * @property initialDotPosition - position of the first Dot
 * @property nextDotPositionOffset - offset for a position of next Dot
 */
class ProgressQuizView(
    context: Context,
    attributeSet: AttributeSet
) : View(context, attributeSet) {

    var currentQuiz: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    private var widthValue by Delegates.notNull<Int>()
    private var heightValue by Delegates.notNull<Int>()

    //Dot fields
    private val dotRadius: Float
    private val dotColor: Int

    // CurrentDot fields
    private val currentDotRadius: Float
    private val currentDotStroke: Float
    private val currentDotColor: Int

    // Common fields
    private val spaceBetween: Float
    private val count: Int

    // Paints
    private val dotPaint: Paint
    private val currentDotPaint: Paint

    private val initialDotPosition: Float
        get() = dotRadius + spaceBetween
    private val nextDotPositionOffset: Float
        get() = getDotDiameter() + spaceBetween

    private val primaryThemeColor: Int
        get() = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            context.resources.getColor(R.color.colorPrimary, context.theme)
        } else {
            context.resources.getColor(R.color.colorPrimary)
        }
    private val secondaryThemeColor: Int
        get() = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            context.resources.getColor(R.color.colorSecondary, context.theme)
        } else {
            context.resources.getColor(R.color.colorSecondary)
        }

    init {
        val typeArray = context
            .obtainStyledAttributes(attributeSet, R.styleable.ProgressQuizView)

        // dot fields
        dotRadius = typeArray.getDimension(
            R.styleable.ProgressQuizView_dot_radius,
            DEFAULT_DOT_RADIUS_DP.toPx(context)
        )
        dotColor = typeArray.getColor(
            R.styleable.ProgressQuizView_dot_color,
            primaryThemeColor
        )

        // currentIndicator fields
        currentDotRadius = dotRadius + typeArray.getDimension(
            R.styleable.ProgressQuizView_current_dot_radius,
            DEFAULT_CURRENT_DOT_RADIUS_DP.toPx(context)
        )
        currentDotStroke = typeArray.getDimension(
            R.styleable.ProgressQuizView_current_dot_stroke,
            DEFAULT_CURRENT_DOT_STROKE_DP.toPx(context)
        )
        currentDotColor = typeArray.getColor(
            R.styleable.ProgressQuizView_current_dot_color,
            secondaryThemeColor
        )

        spaceBetween = typeArray.getDimension(
            R.styleable.ProgressQuizView_dot_space_between,
            DEFAULT_SPACE_BETWEEN_DP.toPx(context)
        )
        count = typeArray.getInteger(R.styleable.ProgressQuizView_dot_count, DEFAULT_COUNT)


        dotPaint = Paint().apply {
            color = dotColor
        }
        currentDotPaint = Paint().apply {
            strokeWidth = currentDotStroke
            style = Paint.Style.STROKE
            color = currentDotColor
        }
        typeArray.recycle()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth =
            suggestedMinimumWidth + paddingStart + paddingEnd + getRequiredViewWidth()
        val desiredHeight =
            suggestedMinimumHeight + paddingTop + paddingBottom + getCurrentDotDiameter()

        widthValue = measureDimension(desiredWidth, widthMeasureSpec)
        heightValue = measureDimension(desiredHeight, heightMeasureSpec)
        setMeasuredDimension(widthValue, heightValue)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        var previousPosition = 0F
        for (currentNumber in 0 until count) {
            val currentPosition = initialDotPosition + nextDotPositionOffset * currentNumber
            canvas?.drawCircle(
                currentPosition,
                heightValue / 2F,
                dotRadius,
                dotPaint
            )
            if (previousPosition > 0) {
                canvas?.drawLine(
                    previousPosition,
                    heightValue / 2F,
                    currentPosition,
                    heightValue / 2F,
                    dotPaint
                )
            }
            if (currentNumber == currentQuiz) {
                canvas?.drawCircle(
                    currentPosition,
                    heightValue / 2F,
                    currentDotRadius,
                    currentDotPaint
                )
            }
            previousPosition = currentPosition
        }
    }

    private fun measureDimension(desiredSize: Int, measureSpec: Int): Int {
        val result: Int
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        result = when (specMode) {
            MeasureSpec.EXACTLY -> {
                specSize
            }
            MeasureSpec.AT_MOST -> {
                desiredSize.coerceAtMost(specSize)
            }
            MeasureSpec.UNSPECIFIED -> {
                desiredSize
            }
            else -> throw IllegalArgumentException("Unknown MeasureSpec in QuizView")
        }
        return result
    }

    private fun getDotDiameter() = dotRadius.toInt() * 2
    private fun getCurrentDotDiameter() = currentDotRadius.toInt() * 2

    /**
     * @return Int - required width of full Dot chain.
     */
    private fun getRequiredViewWidth(): Int {
        return spaceBetween.toInt() + nextDotPositionOffset.toInt() * count
    }

}