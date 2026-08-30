@file:OptIn(ExperimentalComposeUiApi::class)

package me.apomazkin.ui.panel

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onInterceptKeyBeforeSoftKeyboard
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import me.apomazkin.theme.AppTheme
import kotlin.math.roundToInt
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.input.PrimaryTextFieldWidget
import me.apomazkin.ui.preview.PreviewWidget

/** Суммарный сдвиг пальца вниз, после которого отпускание закрывает панель. */
private val DISMISS_DRAG_THRESHOLD = 56.dp

/**
 * IS496: НЕмодальная докнутая панель «поле ввода + отправка» — замена
 * шторки [androidx.compose.material3.ModalBottomSheet] (та живёт в
 * отдельном диалоговом ОКНЕ и глушит весь фон — Р1 брифа).
 *
 * Панель — обычный composable в иерархии экрана: вызывающий кладёт её
 * в свой Box с `Modifier.align(Alignment.BottomCenter)` через [modifier].
 * Фон экрана остаётся живым: скролл и тапы работают (Р2).
 *
 * Закрытие (Р6): системный back (кнопка/жест — [BackHandler], активен
 * пока панель в композиции) и свайв-вниз по панели (порог
 * [DISMISS_DRAG_THRESHOLD]). «Тап мимо» панель НЕ закрывает — скрима
 * больше нет by design.
 *
 * Подъём над клавиатурой — вручную по фактическому зазору до низа окна
 * (см. комментарий в теле): панель стоит над BottomBar табов, и готовые
 * inset-паддинги давали двойной учёт высоты бара.
 *
 * [errorText] — строка ошибки валидации ПОД полем (прежнее решение Э3:
 * снекбар под клавиатурой не виден).
 */
@Composable
fun InputDockedPanelWidget(
    value: String,
    isSendEnabled: Boolean,
    onValueChange: (String) -> Unit,
    onSendAction: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
) {
    BackHandler { onDismissRequest() }
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { DISMISS_DRAG_THRESHOLD.toPx() }
    val dragAccumulated = remember { mutableFloatStateOf(0f) }
    // Подъём над клавиатурой ВРУЧНУЮ: `imePadding()` не знает, что панель
    // стоит НАД BottomBar табов (вкладка кончается выше низа окна) — и
    // поднимал панель на всю высоту IME, оставляя зазор в высоту бара
    // (ручной прогон IS496 M1). Меряем фактический зазор от низа панели
    // до низа ОКНА и поднимаем только на разницу.
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val gapBelowPanelPx = remember { mutableIntStateOf(0) }
    val liftPx = (imeBottomPx - gapBelowPanelPx.intValue).coerceAtLeast(0)
    Box(
        modifier = modifier.onGloballyPositioned { coords ->
            // bounds низа НЕподнятого якоря стабильны (Box прижат к низу
            // вкладки; растёт только паддинг ребёнка) — цикла нет.
            val rootHeight = coords.findRootCoordinates().size.height
            gapBelowPanelPx.intValue =
                (rootHeight - coords.boundsInRoot().bottom.roundToInt())
                    .coerceAtLeast(0)
        },
    ) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = with(density) { liftPx.toDp() })
            // Back при открытой клавиатуре по умолчанию ест IME (первый
            // back — только клавиатура, второй — панель; правка юзера,
            // прогон M2). Перехват ДО IME: back сразу закрывает панель,
            // клавиатура уходит вместе с ней (onDispose поля).
            .onInterceptKeyBeforeSoftKeyboard { event ->
                if (event.key == Key.Back) {
                    if (event.type == KeyEventType.KeyUp) onDismissRequest()
                    true
                } else {
                    false
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { dragAccumulated.floatValue = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        dragAccumulated.floatValue += dragAmount
                    },
                    onDragEnd = {
                        if (dragAccumulated.floatValue > dismissThresholdPx) {
                            onDismissRequest()
                        }
                    },
                )
            },
        color = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        shadowElevation = 8.dp,
    ) {
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            PrimaryTextFieldWidget(
                modifier = Modifier,
                isSendEnabled = isSendEnabled,
                value = value,
                onValueChange = onValueChange,
                onSendAction = onSendAction,
            )
            if (errorText != null) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    text = errorText,
                    style = LexemeStyle.BodyM,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
    }
}

@PreviewWidget
@Composable
private fun PreviewWithError() {
    AppTheme {
        InputDockedPanelWidget(
            value = "Дом",
            isSendEnabled = true,
            onValueChange = {},
            onSendAction = {},
            onDismissRequest = {},
            errorText = "Имя уже занято",
        )
    }
}
