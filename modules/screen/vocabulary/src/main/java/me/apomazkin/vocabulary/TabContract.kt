package me.apomazkin.vocabulary

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * IS493 Э1: вкладки таба словаря. Host владеет [TabSpec]-контрактом; наполняет
 * его app (CompositionRoot) — host и вкладки друг о друге не знают
 * (см. stage1_design_tree D1.3).
 *
 * КОНТРАКТ СТАБИЛЬНОСТИ: TabSpec собирается мостом ОДИН раз (`remember(handle)`)
 * и никогда не пересоздаётся. Вся изменчивость — через лямбды-читалки
 * ([FabSpec.visible], [TabSpec.isTopBarOverridden]), которые host вызывает в
 * своих локальных recomposition-скоупах. Пересоздание спеков в кадр показа
 * диалога/шторки вкладки рвёт их show-анимацию (баг Э1: невидимый
 * ModalBottomSheet) — поэтому значения-Boolean в полях запрещены.
 */
enum class VocabularyTab { WORDS, GROUPS }

/**
 * FAB активной вкладки; null в [TabSpec.fab] — FAB отсутствует.
 * [visible] — читалка State (host оборачивает в AnimatedVisibility scaleIn/Out).
 */
@Stable
data class FabSpec(
    @DrawableRes val iconRes: Int,
    val visible: () -> Boolean,
    val onClick: () -> Unit,
)

/**
 * IS493 Э2 (D9.4): текущий словарь для вкладки — параметр ВЫЗОВА content,
 * НЕ поле TabSpec (спеки стабильны, смена словаря их не пересобирает).
 *
 * Два явных поля вместо одного nullable (D9.1): [isResolved] false —
 * prefs-flow ещё не эмитил (вкладке показывать loading); true + [id] null —
 * честное «словарей нет».
 */
@Immutable
data class DictionarySlot(
    val id: Long?,
    val isResolved: Boolean,
)

/**
 * Контракт вкладки (стабильный, см. контракт стабильности выше).
 *
 * [isTopBarOverridden] true → host рендерит [topBarOverride] вместо AppBar и
 * переводит статусбар в action-цвет (D1.7); у groups — дефолт (никогда).
 *
 * [content] получает [DictionarySlot] (Э2, D9.4) — host резолвит словарь
 * один раз и раздаёт вкладкам (А9/Р8); words параметр игнорирует (резолвит
 * сам, В3).
 */
@Stable
data class TabSpec(
    @StringRes val titleRes: Int,
    val isTopBarOverridden: () -> Boolean = { false },
    val topBarOverride: @Composable () -> Unit = {},
    val fab: FabSpec? = null,
    val content: @Composable (SnackbarHostState, DictionarySlot) -> Unit,
)
