# IS508 / чат-фикс 6 | План правок: код до → код после → зачем

> **Ревизия по итогам прогонов (2026-09-28).** План ниже (fade / рост
> под константой) прогнан как M10a/M10b и забракован: на коротком чате
> новый пузырь встаёт на место сразу, соседи едут из старых позиций —
> наложение («раздувается»). Итоговая реализация — по анализу §5а:
>
> 1. Въезд снизу: новый элемент стартует ниже на **прирост высоты
>    контента ленты** (`InsertShiftTracker`, считается в `layout`-
>    модификаторе `LazyColumn` после измерения, пока все элементы видны)
>    и едет вверх той же пружиной, что placement соседей
>    (`StiffnessMediumLow`). Захват дистанции — при первом рисовании.
>    Fade/рост — нет. Только сообщения Lexeme и чипы; пузырь юзера
>    появляется на месте чипов.
> 2. Чипы — отдельный элемент (как в фиксе 2): элемент ленты не должен
>    менять высоту — в реверсе placement якорит за нижний край, смена
>    высоты даёт скачок верха (так «прыгала» цепочка над вопросом;
>    чипы внутри вопроса и их складывание — M10e/M10f — забракованы).
> 3. Пауза бота 400–650 мс (`botPauseMs`) — пузыри по одному.
>
> Прогоны: M10c «стало лучше» → M10e/M10f «прыгает» → M10g/M10h ✅.
>
> **Ревизия 2 (2026-09-29), итог в анализе §5б.** Поверх пунктов 1–3:
> `requestScrollToItem(0)` у конца ленты вместо `animateScrollToItem`
> (одна кривая на коротком и длинном чате); дистанция въезда — по ключу
> нового элемента, «новый» = ключа не было в раскладке; всё per-item
> состояние — под ключом (LazyList переиспользует композицию); кнопки
> «Начать»/чипы — системные элементы ленты, превращаются в пузырь юзера
> на месте (`UserMessageOrigin`, фазы `ChatMotion.window`); аватар
> съезжает по цепочке; `ChatButtonWidget` без минимальной зоны касания
> (кликабельный M3 Surface раздувал раскладку до 48dp — «качание
> вниз»); кривая — tween `EaseInOutCubic`, не пружина. Слоу-мо
> (1,5 с / пауза 2 с) остаётся в релизе по решению юзера.

Формат: триада «до / после / зачем» плюс короткое объяснение
конструкций. Решения — [ui_anim_brief.md](ui_anim_brief.md): Д1
`animateItem`; Р1 без индикатора «печатает»; Р2 — сделать ОБА варианта
появления (только fade / fade + рост из низа) и сравнить на девайсе;
переключение — константой в коде, две установки (решение юзера
2026-09-28); Р3 скорости — fade 150 мс, сдвиг соседей — дефолтный
spring, рост — 200 мс из scale 0.92 и +8dp снизу (подстроить по M10).

Порядок: П1 (константа = вариант А) → сборка + unit модуля → девайс,
M10a → константа = вариант Б → сборка → девайс, M10b → выбор юзера →
П3 (убрать константу и проигравший вариант) → сборка + unit + lint →
П2 (статус M10) → П4 (Backlog) → один коммит. A/B-сборки не
коммитятся.

---

## П1. `ChatMessageWidget` — `animateItem` и рост пузыря под константой

**Файл:** `modules/screen/quiz/chat/.../widget/ChatMessageWidget.kt`

**До** (корни элементов):

```kotlin
        if (showUserActions) {
            item(key = USER_ACTIONS_KEY) {
                UserActionsWidget(sendMessage = sendMessage)
            }
        }
        itemsIndexed(…) { reversedIndex: Int, item: ChatMessage ->
            …
            val topSpace = if (state.isPreviousHasSameType(index)) 0.dp else 8.dp

            if (item.isSystemMessage) {
                …
                SystemMessageWidget(
                    modifier = Modifier.padding(top = topSpace),
                    …
                )
            } else {
                UserMessageWidget(
                    modifier = Modifier.padding(top = topSpace),
                    message = item.message,
                )
            }
        }
```

**После:**

```kotlin
        if (showUserActions) {
            item(key = USER_ACTIONS_KEY) {
                // fadeOut выключен: гаснущий чип остаётся кликабельным, второй
                // Msg.Skip прошёл бы в quizGame.skip() повторно. Соседи при
                // исчезновении всё равно едут плавно (их placement).
                UserActionsWidget(
                    modifier = Modifier.animateItem(
                        fadeInSpec = tween(FADE_MS),
                        fadeOutSpec = null,
                    ),
                    sendMessage = sendMessage,
                )
            }
        }
        itemsIndexed(…) { reversedIndex: Int, item: ChatMessage ->
            …
            val topSpace = if (state.isPreviousHasSameType(index)) 0.dp else 8.dp
            // animateItem — первым в цепочке (анимируется узел элемента);
            // рост — после padding, чтобы масштабировался пузырь, а не отступ.
            val bubbleModifier = Modifier
                .animateItem(fadeInSpec = tween(FADE_MS), fadeOutSpec = tween(FADE_MS))
                .padding(top = topSpace)
                .bubbleGrowth()

            if (item.isSystemMessage) {
                …
                SystemMessageWidget(
                    modifier = bubbleModifier,
                    …
                )
            } else {
                UserMessageWidget(
                    modifier = bubbleModifier,
                    message = item.message,
                )
            }
        }
```

Ниже `USER_ACTIONS_KEY` — константы и модификатор роста:

```kotlin
/** Fade появления/исчезновения элементов ленты. */
private const val FADE_MS = 150

/**
 * A/B чат-фикса 6 (временно, убрать после выбора): true — вариант Б,
 * пузырь при первом показе растёт из низа; false — вариант А, только fade.
 */
private const val BUBBLE_GROWTH_ENABLED = false
private const val GROWTH_MS = 200
private const val GROWTH_SCALE_FROM = 0.92f
private val GROWTH_OFFSET = 8.dp

/**
 * Рост пузыря из низа при первом показе (вариант Б). Прогресс — Animatable,
 * читается в graphicsLayer: анимация перерисовывает, не перекомпонует.
 * Проигрывается один раз на композицию элемента (после поворота — снова).
 */
@Composable
private fun Modifier.bubbleGrowth(): Modifier {
    if (!BUBBLE_GROWTH_ENABLED) return this
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(targetValue = 1f, animationSpec = tween(GROWTH_MS))
    }
    val offsetPx = with(LocalDensity.current) { GROWTH_OFFSET.toPx() }
    return graphicsLayer {
        val p = progress.value
        val scale = GROWTH_SCALE_FROM + (1f - GROWTH_SCALE_FROM) * p
        scaleX = scale
        scaleY = scale
        translationY = offsetPx * (1f - p)
        transformOrigin = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1f)
    }
}
```

Импорты: `androidx.compose.animation.core.Animatable`,
`androidx.compose.animation.core.tween`,
`androidx.compose.runtime.remember`,
`androidx.compose.ui.graphics.TransformOrigin`,
`androidx.compose.ui.graphics.graphicsLayer`,
`androidx.compose.ui.platform.LocalDensity`. `animation-core`
приходит транзитивно через foundation (`api`), отдельной зависимости
не нужно — проверить сборкой.

**Зачем.** Д1: fade + плавный сдвиг соседей + fade-out закрывают все
четыре рывка из анализа §2. Рост пузыря — вариант Б для сравнения;
константа `false`/`true` — две сборки.

**Конструкции.**
- `animateItem(fadeInSpec, placementSpec = дефолт, fadeOutSpec)` —
  `LazyItemScope`, требует ключ (есть). Первым в цепочке корня.
- `tween(150)` для alpha — короче дефолтного spring (~350 мс),
  критерий «не замечает». Placement — дефолтный spring MediumLow без
  видимого overshoot по позиции (visibilityThreshold).
- Чипы: `fadeOutSpec = null` — защита от тапа по гаснущему чипу
  (анализ §5); `fadeIn` оставлен.
- `bubbleGrowth()`: `remember { Animatable(0f) }` — на композицию
  элемента; `LaunchedEffect(Unit)` — один запуск; `graphicsLayer {}` с
  лямбдой читает `progress.value` в фазе рисования — рекомпозиции
  нет. `transformOrigin` низ-центр — «растёт из низа».
  `if (!ENABLED) return this` до `remember` — в варианте А ни
  состояния, ни эффекта не создаётся; константа компилируется в
  ветку, `remember` под условием константы допустим (порядок
  вызовов стабилен).
- Порядок `padding → bubbleGrowth`: масштабируется пузырь без
  верхнего отступа; при `animateItem` первым — узел элемента
  анимируется целиком.

---

## П2. Ручник M10 (документ `manual_test.md`)

Оглавление: `[M10. Анимация ленты: A/B появления](#m10)`. Раздел
«## Чат-фикс 6: анимация ленты», кейс M10 в формате M1–M9; прогон —
две строки (A и Б) и выбор.

**Что доказывает.** Нет скачков раскладки: новые пузыри и чипы
проявляются, соседи едут; чипы исчезают без оседания. Сравнение А
(fade) и Б (fade + рост).

**Вход.** Debug-сборка, константа `BUBBLE_GROWTH_ENABLED = false`
(A), затем `true` (Б); «Show debug» OFF (короткие пузыри); группа
≥ 5 слов.

**Шаги.**

1. «Начать»: пузырь «Начали», вопрос, чипы — проявляются, соседи едут
   плавно (короткий чат).
2. «Пропустить»: чипы исчезли, пузырь «Пропустить» и вопрос
   проявились, лента сдвинулась один раз — без «осела → подскочила».
3. Ответь: свой пузырь, оценка, вопрос — проявляются.
4. Открой клавиатуру жестом: лента едет за клавиатурой без «желе».
5. Поверни экран: лента в конце, повторного «появления» всего списка
   нет (А) / есть рост у всех пузырей (Б — допустимо, отметить).

**Ожидание глазами.** Анимацию не замечаешь, скачков нет. Для Б —
пузыри слегка «вырастают» снизу.

**Анти-проверки.**

* Нет скачка соседей при появлении элемента.
* Двойной тап по «Пропустить» — одно действие (чипы исчезают сразу).
* Нет «желе» при клавиатуре.
* Нет `FATAL`.

**Прогон:** А — …; Б — …; выбор юзера — ….

---

## П3. После выбора — убрать константу и проигравший вариант

**Если А:** удалить `BUBBLE_GROWTH_ENABLED`, `GROWTH_*`,
`bubbleGrowth()` и его импорты; `bubbleModifier` без `.bubbleGrowth()`.
**Если Б:** удалить `BUBBLE_GROWTH_ENABLED` и проверку в
`bubbleGrowth()`; KDoc — без «A/B»; при жалобе на повтор после
поворота — отдельно.

**Зачем.** В коммит не идёт мёртвый вариант и временный флаг.

---

## П4. Backlog

Удалить пункт «Анимация сообщений чата как в Telegram» из «Срочное»
→ «Квиз-чат» (закрыт). Если выбран А и «рост» захочется позже —
одна строка в тот же раздел: «рост пузыря из низа — вариант Б
чат-фикса 6, код в истории ветки».

---

## Тесты

Unit-тестов нет: редьюсер и состояние не меняются, правка — модификаторы
в одном composable. Регресс: `:modules:screen:quiz:chat:testDebugUnitTest`,
`assembleDebug`, `:app:lintDebug` — через `./scripts/cc-build.sh`.
