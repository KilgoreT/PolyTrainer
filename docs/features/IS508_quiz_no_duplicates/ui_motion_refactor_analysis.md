# IS508 / чат-фикс 10 | Анализ: разбор анимаций ленты по функциям

Бриф: [ui_motion_refactor_brief.md](ui_motion_refactor_brief.md).
Код — ветка на 5d415d00. Первая версия анализа (свой движок соседей +
единые часы) отвергнута после ревью тремя агентами; ниже — итоговая.

## 1. Что есть

| Механизм | Где | Время | Дистанция |
|---|---|---|---|
| Въезд нового элемента | `slideInWithColumn` | `Animatable` на ключ | `tracker.distances[key]` + `slideExtra` |
| Движение соседей | `animateItem(placementSpec)` | `DockedPlacementSpec` | initial→target внутри LazyList |
| Морф кнопка→пузырь | `actionMorph` + `MorphState` | `Animatable` на ключ | — |
| Призраки чипов | inline в элементе (2 варианта) | t морфа / `flight.progress` | `skipChipWidthPx` (с живого чипа) |
| Спуск аватара | `SystemMessageWidget` | `Animatable` на order | `avatarDescentExtraPx()` + высота ряда |
| Полёт ответа | `FlightState`/`FlightLayer` | кадровый цикл | source/target |
| Паузы бота | `DatasourceEffectHandler` | константы 400–650 | — |

Всё это работает и проверено M10–M14. Механизмы времени и LazyList
остаются как есть (Д1). Меняется организация, точки согласования и
покрытие тестами.

## 2. Целевая структура

### 2.1. Вход элемента — `Entrance` + `entranceFor` (widget/motion)

```kotlin
internal sealed interface ChatItem {
    data class Message(val message: ChatMessage, val isInChain: Boolean) : ChatItem
    data object Actions : ChatItem     // ряд чипов
    data object Start : ChatItem       // кнопка «Начать»
}

internal sealed interface Entrance {
    data object None : Entrance
    data class SlideFromBelow(val avatarDescends: Boolean) : Entrance
    data class MorphFromButton(val origin: UserMessageOrigin) : Entrance
    data object Flight : Entrance
}

internal fun entranceFor(item: ChatItem, isNew: Boolean, flightOrder: Int?): Entrance
```

Правила (как сегодня, только в одном месте): не новый → `None`;
системное → `SlideFromBelow(isInChain)`; `INPUT` при активном полёте
(`flightOrder == order`) → `Flight`; `INPUT` без полёта →
`SlideFromBelow(false)`; из кнопки → `MorphFromButton(origin)`; чипы
новые → `SlideFromBelow(false)`; «Начать» → `None`. Защёлка —
`remember(itemKey) { entranceFor(...) }` (LazyList переиспользует
композицию).

### 2.2. «Новый» — по `order` (Д4)

`InsertShiftTracker.isNewKey` сейчас: `hasLaidOut && key !in knownKeys`,
где `knownKeys` — ключи, побывавшие в раскладке с момента создания
трекера. После поворота трекер новый, история выше вьюпорта — «новая»
при скролле: `actionMorph(enabled = isNew)` проигрывает морф старым
пузырям, `avatarDescends` — спуск. Замена: трекер хранит
`maxLaidOutOrder`; сообщение новое, если `hasLaidOut && order >
maxLaidOutOrder`; ключи кнопок — новые, если новое последнее
сообщение (`isLastMessageNew`, как сейчас). `knownKeys` уходит.
Дистанции по-прежнему раздаются один раз по ключу.

### 2.3. Рендер по `Entrance` — отдельные composable

`ChatMessageWidget` — только лента и диспетчер `when (entrance)`:

- `SlideFromBelow` → `bubbleModifier.slideInWithColumn(...)`;
  системное — `SystemMessageWidget(avatarDescends, avatarDescentExtraPx)`
  (как сейчас), юзерское — `UserMessageWidget`.
- `MorphFromButton(origin)` → `MorphingUserMessage(message, origin,
  morphState, modifier)`: внутри `actionMorph` (`Animatable` под
  ключом), `UserMessageWidget(colorMorph, geometryMorph, fromStart,
  shiftFromLeftPx)` и призрак соседнего чипа; ширина «Пропустить» —
  с самого призрака (`onSizeChanged` в `remember(key)`), не с живого
  чипа через ленту (`onSkipChipMeasured`/`IntArray` в ленте уходят).
- `Flight` → `FlyingUserMessage(message, flight, modifier)`: скрытый
  пузырь (`surfaceModifier`: цель + alpha 0), призрак ряда чипов через
  `ActionChipsRow(enabled = false, alpha = ...)`.
- `None` → как есть без модификаторов входа.

`ActionChipsRow(modifier, enabled, onShowAnswer, onSkip)` — один ряд и
для `UserActionsWidget`, и для призрака полёта. `ChatButtonWidget`
читает углы/отступы из `ChatMotion.BUBBLE_*` (сейчас 20/8 и 16/8 руками
— три копии одной геометрии с пузырём и копией полёта).

### 2.4. Трекер сдвига — тестируемый

`InsertShiftTracker`: логику `trackInsertShift` (якорь, `shift`,
раздача дистанций, чистка ключей кнопок, `maxLaidOutOrder`) перенести в
метод `onLaidOut(items: List<LaidOutItem>)`, `LaidOutItem(key: Any,
offset: Int, isAction: Boolean, order: Int?)` — без Compose-типов;
поля — private, наружу `isNewKey`, `distances[key]`, `slideExtra`,
`armed`. Модификатор ленты — тонкая обёртка: проекция
`layoutInfo.visibleItemsInfo` → `onLaidOut`. `DockedPlacementSpec.docked`
— вынести в `internal fun dockedFraction(extra, d, e)` для юнита.

### 2.5. Тайминг — `ChatTiming` (logic)

```kotlin
internal object ChatTiming {
    const val MOTION_DURATION_MS = 350
    /** Кадры старта анимаций (LaunchedEffect → первый withFrameNanos), ~3 кадра. */
    const val MOTION_LATENCY_MS = 50L
    const val BOT_PAUSE_MIN_MS = MOTION_DURATION_MS + MOTION_LATENCY_MS + 50L   // 450
    const val BOT_PAUSE_MAX_MS = 650L
}
```

`ChatMotion.DURATION_MS` убрать, читать `ChatTiming.MOTION_DURATION_MS`.
Единственная точка, где `logic` знает о темпе UI — сказать в KDoc.
Тест инварианта: `BOT_PAUSE_MIN_MS ≥ MOTION_DURATION_MS +
MOTION_LATENCY_MS`; тесты хендлера: `currentTime ≥ BOT_PAUSE_MIN_MS` для
`DeliverSystemMessages`, `NextQuestion`, `CheckAnswer`, `GetAnswer`.

### 2.6. Что не трогаем

`slideInWithColumn` (со страховкой `SLIDE_IN_HIDE_UNTIL`), `animateItem`,
`DockedPlacementSpec` (только вынос формулы), `MorphState`, `FlightState`
(кадровый цикл, синхронный сброс `progress`), спуск аватара
(`Animatable` в `SystemMessageWidget`), якорение скролла.

## 3. Тесты

**JVM (`src/test`):**
- `EntranceTest` — таблица правил 2.1, включая чипы/«Начать» и «не
  новый → None».
- `InsertShiftTrackerTest` — первая раскладка: новых нет, дистанций
  нет; вставка при `armed`: `shift` = Δ якоря, дистанция у нового и у
  чипов, не у старых; ключи кнопок чистятся при исчезновении, сообщения
  — нет; `isNewKey` по `order`: история (order ≤ max) не новая;
  `slideExtra` = extra при d > 0, 0 при d ≤ 0.
- `DockedFractionTest` — при e = 0 сосед на месте; стоит, пока новый
  проходит добавку; после — равны; при e = 1 — 1; d = 0 → 1.
- `ChatTimingTest` — инвариант 2.5.
- `DatasourceEffectHandlerTest` — паузы ≥ `BOT_PAUSE_MIN_MS`.

**Device (`src/androidTest`, Д2):** модуль — `defaultConfig {
testInstrumentationRunner = AndroidJUnitRunner }` (его нет),
`androidTestImplementation(composeLibs.uiTestJunit4)`,
`debugImplementation(composeLibs.uiTestManifest)`. `ChatMessageMotionTest`:
`createComposeRule()`, `mainClock.autoAdvance = false`, состояние в
`mutableStateOf`, запись через `runOnIdle`, `testTag("msg-<order>")` на
корне элемента и `testTag("avatar-<order>")`, координаты через
`getUnclippedBoundsInRoot()` (clipped-bounds режутся лентой), конец —
`mainClock.advanceTimeUntil { ... }` по факту (позиция не меняется два
кадра подряд), середина — только неравенствами:
1. Въезд со стыковкой: кадр после вставки — новый ниже низа ленты,
   сосед на месте; середина — сосед в [y0 − D, y0], новый выше старта;
   конец — сосед ровно на D выше, новый на месте.
2. Цепочка: y аватара одинаков на кадрах 0 / середина / конец.
3. Морф без сдвига (чипы → пузырь `SKIP_CHIP`): сосед неподвижен весь
   переход.
4. Многострочный ответ заменяет чипы (Δ высоты > 0): сосед доезжает
   плавно (середина — между), не прыгает.
Известное слепое пятно: первый кадр соседей (`waitForIdle` сам
рассылает apply-уведомления) — только глазами.

## 4. Риски

1. Замена `knownKeys` на `order` меняет поведение только для истории
   после поворота (баг) — проверить M10 шаг с поворотом.
2. Первые Compose UI-тесты в проекте: `ui-test-manifest` в debug,
   `AppTheme` в тесте, тайминг через `advanceTimeUntil`, а не по числу
   кадров.
3. Вынос ветки морфа в отдельную функцию не должен изменить порядок
   `remember(key)` защёлок — перенос как есть, без смены ключей.
