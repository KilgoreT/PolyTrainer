package me.apomazkin.quiz.chat.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import me.apomazkin.theme.LexemeStyle

/**
 * Полёт набранного ответа из поля ввода в пузырь ленты: UI-состояние
 * экрана от нажатия «отправить» до конца анимации. Лента по [order]
 * прячет свой пузырь и рисует на месте исчезнувших чипов их призрак;
 * слой [FlightLayer] ведёт копию текста от [sourceAnchor] к [target].
 *
 * [order] и [target] — snapshot-state: по первому перекомпонуется
 * элемент ленты, по второму стартует анимация. Остальное — обычные поля:
 * читаются в фазе рисования слоя вместе с [progress].
 */
class FlightState {
    /** Сообщение в полёте; null — полёта нет. */
    var order: Int? by mutableStateOf(null)

    /** Левый верх `Surface` пузыря в координатах корня; null — пузырь ещё не размещён. */
    var target: Offset? by mutableStateOf(null)

    /**
     * Доля полёта на линейном времени 0..1; фазы — через [ChatMotion.window].
     * Обнуляется синхронно в [launch], а не в корутине: иначе первый кадр
     * нового полёта читал бы 1.0 от прошлого — призрак чипов мелькал.
     */
    var progress: Float by mutableFloatStateOf(0f)

    var text: String = ""

    /** Левый край и базовая линия первой строки текста в поле (координаты корня). */
    var sourceAnchor: Offset = Offset.Zero

    /** Координаты поля ввода — снимаются при каждом размещении, нужны в момент отправки. */
    var fieldCoordinates: LayoutCoordinates? = null

    /** Левый верх слоя в координатах корня: якоря переводятся в локальные координаты слоя. */
    var layerOrigin: Offset = Offset.Zero

    /** Копия текста в стиле пузыря; измеряется один раз на полёт (по [copyOrder]). */
    var copyLayout: TextLayoutResult? = null
    var copyOrder: Int? = null
}

/**
 * Снимает исходную точку текста в поле и объявляет полёт сообщения
 * [order]. Вызывать до отправки `Msg.UserAttempt`: после апдейта поле
 * уже пустое. Без координат поля полёта нет — пузырь появится как обычно.
 */
fun FlightState.launch(
    order: Int,
    text: String,
    measurer: TextMeasurer,
    density: Density,
) {
    val field = fieldCoordinates ?: return
    if (text.isBlank()) return
    val padPx = with(density) { FIELD_CONTENT_PADDING.roundToPx() }
    val contentWidth = (field.size.width - 2 * padPx).coerceAtLeast(1)
    // Текст в поле: стиль поля, ширина содержимого поля; блок текста
    // стоит по центру высоты поля (при одной строке — отступ 16dp).
    val fieldLayout = measurer.measure(
        text = text,
        style = LexemeStyle.BodyL,
        constraints = Constraints(maxWidth = contentWidth),
    )
    val origin = field.positionInRoot()
    val textTop = origin.y + (field.size.height - fieldLayout.size.height) / 2f
    sourceAnchor = Offset(origin.x + padPx, textTop + fieldLayout.firstBaseline)
    this.text = text
    copyLayout = null
    target = null
    progress = 0f
    this.order = order
}

/**
 * Слой поверх ленты и поля: рисует копию отправленного текста и ведёт её
 * из поля к пузырю. Буквы стартуют в стиле поля (масштаб 17/15 вокруг
 * базовой линии), фон пузыря и цвет текста проявляются синхронно с
 * [ChatMotion.FLIGHT_RECOLOR_START]; на t = 1 копия геометрически равна
 * пузырю — подмена на реальный не видна. Пока цель неизвестна (первый
 * кадр после апдейта), копия стоит в источнике: при t = 0 это её место.
 * Рисование читает состояние в фазе draw — кадры не перекомпонуют слой.
 */
@Composable
internal fun FlightLayer(
    modifier: Modifier = Modifier,
    flight: FlightState,
) {
    val measurer = rememberTextMeasurer()
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val fieldTextColor = MaterialTheme.colorScheme.onSurface
    val bubbleStyle = LexemeStyle.BodyM
    val fieldScale = LexemeStyle.BodyL.fontSize.value / LexemeStyle.BodyM.fontSize.value
    val durationMs = ChatMotion.DURATION_MS

    LaunchedEffect(flight.order) {
        val order = flight.order ?: return@LaunchedEffect
        // Цель приходит после раскладки нового пузыря. Не пришла за окно
        // (пузырь вне экрана — отправка из истории) — полёт без анимации.
        val positioned = withTimeoutOrNull(durationMs.toLong()) {
            snapshotFlow { flight.target }.first { it != null }
        }
        if (positioned != null) {
            // Линейное время по кадрам: кривые — у каждой фазы своя (window).
            val durationNanos = durationMs * 1_000_000L
            val start = withFrameNanos { it }
            while (flight.progress < 1f) {
                withFrameNanos { now ->
                    flight.progress = ((now - start).toFloat() / durationNanos).coerceIn(0f, 1f)
                }
            }
        }
        if (flight.order == order) flight.order = null
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { flight.layerOrigin = it.positionInRoot() }
            .drawBehind {
                val order = flight.order ?: return@drawBehind
                val padH = ChatMotion.BUBBLE_PADDING_H.toPx()
                val padV = ChatMotion.BUBBLE_PADDING_V.toPx()
                val listPad = ChatMotion.LIST_PADDING.toPx()
                val layout = flight.copyLayout?.takeIf { flight.copyOrder == order }
                    ?: measurer.measure(
                        text = flight.text,
                        style = bubbleStyle,
                        constraints = Constraints(
                            maxWidth = (size.width - 2 * listPad - 2 * padH).toInt().coerceAtLeast(1),
                        ),
                    ).also {
                        flight.copyLayout = it
                        flight.copyOrder = order
                    }

                val target = flight.target
                val t = if (target == null) 0f else flight.progress
                val e = ChatMotion.EASING.transform(t)
                val c = ChatMotion.window(t, ChatMotion.FLIGHT_RECOLOR_START, 1f)
                val s = lerp(fieldScale, 1f, e)

                // Якорь — левый край и базовая линия первой строки текста:
                // по нему совпадают буквы поля и копии, по нему же масштаб.
                val targetAnchor = target?.let {
                    Offset(it.x + padH, it.y + padV + layout.firstBaseline)
                }
                val anchor = (targetAnchor?.let { lerp(flight.sourceAnchor, it, e) } ?: flight.sourceAnchor) -
                    flight.layerOrigin
                val copySize = Size(
                    width = layout.size.width + 2 * padH,
                    height = layout.size.height + 2 * padV,
                )
                val copyOrigin = anchor - Offset(padH, padV + layout.firstBaseline)
                val corner = CornerRadius(ChatMotion.BUBBLE_CORNER.toPx())
                val tail = CornerRadius(ChatMotion.BUBBLE_TAIL_CORNER.toPx())
                val bubble = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = Rect(Offset.Zero, copySize),
                            topLeft = corner,
                            topRight = corner,
                            bottomRight = tail,
                            bottomLeft = corner,
                        ),
                    )
                }
                scale(scaleX = s, scaleY = s, pivot = anchor) {
                    translate(left = copyOrigin.x, top = copyOrigin.y) {
                        drawPath(path = bubble, color = primary.copy(alpha = c))
                        drawText(
                            textLayoutResult = layout,
                            color = lerp(fieldTextColor, onPrimary, c),
                            topLeft = Offset(padH, padV),
                        )
                    }
                }
            },
    )
}

/** Внутренний отступ содержимого M3 `OutlinedTextField` (дефолт `contentPadding`). */
private val FIELD_CONTENT_PADDING = 16.dp
