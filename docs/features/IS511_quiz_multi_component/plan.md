# IS511 | План: несколько ядер в вопросе квиз-чата — до → после → зачем

Бриф: [brief.md](brief.md), анализ: [analysis.md](analysis.md), спеки:
[quiz-chat](../../handbook/specs/quiz-chat/spec.md),
[word-model](../../handbook/specs/word-model/spec.md) §2.6,
[quiz-group-filter](../../handbook/specs/quiz-group-filter/spec.md) §7.1.

Порядок: П1 → П2 → П3 → П4 → П5 → П6 → П7 → сборка, unit по модулям,
`:modules:screen:quiz:chat:compileDebugAndroidTestKotlin`, lint,
`installDebug`, device-тесты (DAO-фильтр в core-db-impl, движение ленты в
чате), ручник → один коммит (доки — в нём же). Правила: gradle через
`./scripts/cc-build.sh`; sed/find/cat/head/tail/awk/Python запрещены; на
девайс только `installDebug`; KDoc без «IS511»/дат.

Ревью плана (три ревьюера, 2026-10-01): 30 findings → 7 пунктов триажа,
решения вшиты в пункты ниже и собраны в разделе «Триаж ревью» в конце.

---

## П1. UI-кит: галка с `enabled`, подменю с подписью

**Файлы:** `modules/widget/iconDropDowned/.../MenuItem.kt`,
`modules/core/ui/.../dropdown/LexemeSubmenuMenuItem.kt`.

**До:** `MenuItem.withCheckbox(isChecked, title, onCheckedChange)`,
`MenuItemWithCheckbox(isChecked, title, onCheckedChange)`, внутренний
`MenuItem(isChecked, title, onCheckedChange)` с `Checkbox(enabled = true)`;
`LexemeSubmenuMenuItem(title, enabled, content)` — только заголовок.

**После:**

```kotlin
fun withCheckbox(isChecked: Boolean, title: StringSource, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit)
data class MenuItemWithCheckbox(val isChecked: Boolean, override val title: StringSource, val enabled: Boolean = true, val onCheckedChange: (Boolean) -> Unit)
// внутренний MenuItem(isChecked, title, enabled, onCheckedChange):
Checkbox(checked = isChecked, onCheckedChange = onCheckedChange, enabled = enabled, …)
DropdownMenuItem(…, enabled = enabled, onClick = { onCheckedChange(!isChecked) })
```

```kotlin
fun LexemeSubmenuMenuItem(title: String, subtitle: String? = null, enabled: Boolean = true, content: …) {
    DropdownMenuItem(text = {
        Column {
            Text(text = title)
            subtitle?.let { Text(text = it, style = LexemeStyle.BodyS, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }, …)
}
```

Подпись — `LexemeStyle.BodyS` (13sp), не `typography.bodySmall`: тема
переназначает M3-слоты, `bodySmall` здесь 15sp и был бы крупнее
заголовка пункта. Заголовок не трогаем (его разнобой с соседними галками
— вне скоупа).

**Зачем.** Задизейбленная последняя галка (Д2) и подпись «минимум одно».
Существующие вызовы (тумблеры чата) не меняются — параметры с дефолтом.

**Новый виджет `modules/core/ui/.../LexemeBadgeChip.kt`** — короткая метка
(часть речи в вопросе, Д6), общая для приложения:

```kotlin
@Composable
fun LexemeBadgeChip(text: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Text(text = text, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSecondaryContainer), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
    }
}
```

Кегль — из темы (`labelSmall` = слот 11sp), не сырым числом. Цвета —
явной парой (`secondaryContainer` / `onSecondaryContainer`), без
M3-дефолтов. Чип сам режет длинный текст многоточием в одну строку;
размеры ему задаёт место, где он стоит (П6).

---

## П2. Ресурсы

**Файлы:** `core/core-resources/src/main/res/values/strings.xml`,
`values-ru-rRU/strings.xml`.

**После:** добавить `chat_quiz_msg_system_rule`,
`chat_menu_quiz_component_hint`, `chat_quiz_pos_{noun,verb,adjective,adverb,preposition,phrase}`
(тексты — спека quiz-chat §5 «Новые»); удалить
`chat_quiz_ask_translation_header`, `chat_quiz_ask_definition_header`;
исправить `chat_quiz_summary_skipped` ru: «➖ Пропущено: %s» (было `::`).

Имя встроенного ядра в вопросе — тот же ключ, что подпись галки в меню:
`chat_menu_item_component_translation` («Перевод»). Отдельный
`chat_quiz_core_translation` НЕ заводить: два ключа на одно слово
разъедутся при правке, а инвариант спеки §6.5 требует совпадения.

**Зачем.** Вопрос «Имя ядра: значение», правило игры, чип части речи.

---

## П3. Данные: фильтр порции по ядрам

**Файлы:** `core/core-db-api/.../CoreDbApi.kt` (`QuizApi`),
`core/core-db-impl/.../room/WordDao.kt`, `core/core-db-impl/.../CoreDbApiImpl.kt`.

**До:** `getWriteQuizIds(grade, dictionaryId, groupId)`,
`getEarliestWriteQuizList(limit, dictionaryId, groupId)`,
`getFrequentMistakesWriteQuizList(limit, dictionaryId, groupId)`; SQL без
знания о компонентах.

**После:** у всех трёх — параметр `coreTypeIds: List<Long>`; в каждом
SQL после группового условия:

```sql
AND EXISTS (
    SELECT 1 FROM component_values cv
    WHERE cv.lexeme_id = write_quiz.lexeme_id
      AND cv.removed_at IS NULL
      AND cv.component_type_id IN (:coreTypeIds))
```

KDoc `QuizApi`: «`coreTypeIds` — включённые ядра словаря; непустой список
гарантирует вызывающий (use case), лексема без живого значения в них в
выборку не попадает (quiz-group-filter §7.1)».

**Зачем.** Д3: фильтр до окна, порция добирается из подходящих.

**Тест (device, обязательный):** каркас есть —
`core/core-db-impl/src/androidTest/.../room/QuizGroupFilterDaoTest.kt`
(14 тестов) зовёт все три запроса без `coreTypeIds`, а его фикстуры
(`addQuizWord`/`addQuizWordCustom`, `addLexemeWithQuiz`) сеют лексемы без
`component_values` — после EXISTS девять тестов выборки вернут пусто.
Переписать как факт: в `setUp` сеять `ComponentTypeDb` TRANSLATION
(`core = true`; образец `typeDb(...)` в `Phase3ConstructorDataTest`),
фикстуры вставлять живое значение через `WordDao.addLexemeWithComponents`
(прецедент `Is486DataLayerTest`), хелперы `quizIds()`/`getEarliest()`/
`getFrequentMistakes()` передают `coreTypeIds = listOf(translationTypeId)`.
Плюс три новых случая: значение в типе из списка попадает; без значения —
нет; `removed_at IS NOT NULL` не считается; тип не в списке — нет.

---

## П4. Use case приложения

**Файлы:** `modules/screen/quiz/chat/.../deps/QuizChatUseCase.kt`,
`app/.../di/module/quizchat/QuizChatUseCaseImpl.kt`,
`app/src/test/.../QuizChatUseCaseImplTest.kt`.

**До:** `getAvailableTypes` (фильтр TEXT), `getQuizPickerSelection():
ComponentTypeRef?`, `setQuizPickerSelection(ref)`, `getRandomWriteQuizList(limit,
maxGrade, dictionaryId)`, `getQuizConfig`.

**После (интерфейс):**

```kotlin
/** Ядра словаря, которыми квиз умеет спросить: core, включённые, не удалённые, шаблон из поддерживаемых; по position. */
suspend fun getQuizCoreTypes(dictionaryId: Long): List<ComponentType>
/** Сохранённый набор ядер; пусто — не сохранено (вызывающий берёт все кандидаты). */
suspend fun getQuizPickerSelection(dictionaryId: Long): Set<ComponentTypeRef>
suspend fun setQuizPickerSelection(dictionaryId: Long, refs: Set<ComponentTypeRef>)
/** Порция только из лексем с живым значением хотя бы в одном из [coreTypeIds]; пустой список → пусто без запросов. */
suspend fun getRandomWriteQuizList(limit: Int, maxGrade: Int, dictionaryId: Long, coreTypeIds: List<Long>): List<WriteQuiz>
/** Опции встроенной «Части речи» словаря (для чипа в вопросе); нет типа — пусто. */
suspend fun getPartOfSpeechOptions(dictionaryId: Long): List<ComponentOption>
```

`getQuizConfig` остаётся (не используется квизом; Backlog на удаление).

**Реализация:**
- `getQuizCoreTypes`: `getComponentTypes(dict).map { toDomain }.filter { it.core && it.enabled && it.removedAt == null && it.template in QUIZ_RENDERABLE_TEMPLATES }.sortedBy { position }`; `QUIZ_RENDERABLE_TEMPLATES = setOf(ComponentTemplate.TEXT)` — константа в `deps/QuizChatUseCase.kt` рядом с интерфейсом (знание квиза).
- Кодек: `encodeRefs(refs) = refs.joinToString(REF_SEPARATOR) { encodeRef(it) }`, `REF_SEPARATOR = "\u001F"`; `decodeRefs(raw) = raw.split(REF_SEPARATOR).mapNotNull { decodeRef(it) }.toSet()`; пустая строка / null → пусто. Старое одиночное значение без разделителя → набор из одного.
- `getRandomWriteQuizList(…, coreTypeIds)`: `if (coreTypeIds.isEmpty()) return emptyList()`; прокинуть в `getWriteQuizIds`, `getEarliestWriteQuizList`, `getFrequentMistakesWriteQuizList`.
- `getPartOfSpeechOptions`: тип с `systemKey == PART_OF_SPEECH` среди `getComponentTypes(dict)` → `lexemeApi.getComponentOptions(typeId)` → domain; нет типа → пусто.
- `getAvailableTypes` удалить (единственный потребитель — пикер).

**Тесты:** анализ §4 (фильтры кандидатов, кодек round-trip / legacy / битые, `coreTypeIds` в три вызова, пустой список без запросов, опции части речи). Существующие тесты `getQuizPickerSelection decodes …` переписать на набор. Четыре теста `getAvailableTypes …` (`QuizChatUseCaseImplTest.kt:395-446`) переписать на `getQuizCoreTypes`; фикстура `ctApi(…)` там без `core` (API-дефолт `false`) — добавить `core: Boolean = true` в фикстуру и отдельный случай `core = false` → отфильтрован.

**Зачем.** Д1/Д1а/Д3/Д6 на стороне данных; кодек без миграции.

---

## П5. Логика чата: состояние, сообщения, редьюсер, эффекты

**Файлы:** `logic/State.kt`, `logic/Message.kt`, `logic/ChatReducer.kt`,
`logic/DatasourceEffectHandler.kt`, `logic/ChatSubHandler.kt`; тесты
`ChatReducerTest`, `DatasourceEffectHandlerTest`, `ChatSubHandlerTest`.

**До:** `QuizComponent(availableTypes, selectedRef: ComponentTypeRef?)`,
`isPickerEnabled = size > 1`, `Msg.SelectQuizComponent(ref)`,
`Msg.QuizComponentTypesLoaded(types, restoredSelectedRef: ComponentTypeRef?)`,
`DatasourceEffect.SaveQuizPickerSelection(ref)`, `resolveSelection` → один ref.
`DatasourceEffect.LoadQuiz` — `data object`, хендлер всегда отдаёт
`Msg.QuizLoaded`; `Msg.QuizReLoaded` объявлен, но никем не шлётся (с IS399)
— после «Продолжить квиз» пузырь юзера пишет «Начать» (живой баг).
`QuizLoaded` → «Начать» (+ stat одним апдейтом) → `NextQuestion`.
`DeliverSystemMessages(messages)` / `SystemMessageDelivered(message, rest)`
— капельная выдача без «что после».

**После (пикер):**

```kotlin
data class QuizComponent(val availableTypes: List<ComponentType> = emptyList(), val selectedRefs: Set<ComponentTypeRef> = emptySet())
/** Подменю есть только при выборе: два и более ядра. */
val ItemsState.QuizComponent.isPickerVisible: Boolean get() = availableTypes.size > 1

data class ToggleQuizComponent(val ref: ComponentTypeRef, val checked: Boolean) : Msg
data class QuizComponentTypesLoaded(val types: List<ComponentType>, val restoredSelectedRefs: Set<ComponentTypeRef>) : Msg
data class SaveQuizPickerSelection(val refs: Set<ComponentTypeRef>) : DatasourceEffect
```

- `QuizComponentTypesLoaded` → `updateQuizComponent(types, resolveQuizCores(types, restored).map { it.toRef() }.toSet())` — одна чистая функция на инвариант «выбор ∩ кандидаты, пусто → все» (П6, `quiz/QuizCoreSelection.kt`); редьюсер и игра — только проводка, старый `resolveSelection` в редьюсере удалить.
- `ToggleQuizComponent` → `next = if (checked) selected + ref else selected - ref`; `next.isEmpty()` → `state to emptySet()`; иначе `state.updateQuizComponent(types, next) to setOf(SaveQuizPickerSelection(next))` (оптимистично; саб переиздаст то же).
- `SelectQuizComponent` удалить.

**После (раунды и правило игры):**

```kotlin
data class LoadQuiz(val reload: Boolean) : DatasourceEffect      // было data object
data class DeliverSystemMessages(val messages: List<MessageContent>, val then: DatasourceEffect? = null) : DatasourceEffect
data class SystemMessageDelivered(val message: MessageContent, val rest: List<MessageContent>, val then: DatasourceEffect? = null) : Msg
```

- `Msg.Start` → `LoadQuiz(reload = false)`; `UserAction.CONTINUE` → `LoadQuiz(reload = true)`.
- Хендлер `LoadQuiz`: как сейчас (loadData, имя группы), затем `if (effect.reload) Msg.QuizReLoaded(stat) else Msg.QuizLoaded(stat)`. Хендлер `DeliverSystemMessages`: `SystemMessageDelivered(first, rest, effect.then)`.
- `QuizLoaded` → `startQuiz().userMessage(«Начать», START_BUTTON)` to `botThenQuestion(listOfNotNull(ruleMessage(), stat))`; `ruleMessage()` = `resourceManager.stringByResId(R.string.chat_quiz_msg_system_rule).toMessageContent()`.
- `QuizReLoaded` → `userMessage(«Продолжить квиз»)` to `botThenQuestion(listOfNotNull(stat))` — правила нет.
- `botThenQuestion(messages)` = пусто → `setOf(NextQuestion)`; иначе `setOf(DeliverSystemMessages(messages, then = NextQuestion))`. Правило (и debug-стат) въезжают по одному после паузы, в апдейте с морфом «Начать» вставок нет — контракт ленты IS508 (`MorphingUserMessage`, KDoc `DeliverSystemMessages`).
- `SystemMessageDelivered` → `systemMessage(message)` to `if (rest.isEmpty()) setOfNotNull(then) else setOf(DeliverSystemMessages(rest, then))`. Существующие вызовы (`SessionOver`, `Summary`) — без `then`, поведение прежнее.

Хендлер: `SaveQuizPickerSelection(refs)` → `useCase.setQuizPickerSelection(dictId, refs)`. Саб `QuizPicker`: `QuizComponentTypesLoaded(types = useCase.getQuizCoreTypes(dictId), restoredSelectedRefs = useCase.getQuizPickerSelection(dictId))`; `LoadQuizComponentTypes` в хендлере — так же.

**Тесты:**
- `ChatReducerTest`: `QuizComponentTypesLoaded` (restored ∩ available, пусто → все, `isPickerVisible` при 1 и 2; старый тест «один тип — задизейблен» переписать); `ToggleQuizComponent` (добавить, снять, снять последнюю — no-op без эффекта); `Start` → `LoadQuiz(false)`, `CONTINUE` → `LoadQuiz(true)`; `QuizLoaded(stat = null)` → «Начать» + `DeliverSystemMessages([rule], then = NextQuestion)`; `QuizLoaded(stat)` → `[rule, stat]`; `QuizReLoaded` → «Продолжить квиз» + `NextQuestion`, без правила; `SystemMessageDelivered(rest = [], then = NextQuestion)` → `NextQuestion`; `NextQuestion` с `MessageContent.question(...)` кладёт `MessageValue.Question`.
- `DatasourceEffectHandlerTest`: `LoadQuiz(reload = true)` → `QuizReLoaded`, `false` → `QuizLoaded`; `DeliverSystemMessages(then)` пробрасывает `then` в `SystemMessageDelivered`. `FakeUseCase` там реализует весь интерфейс — снять `getAvailableTypes`, добавить `getQuizCoreTypes`, `getPartOfSpeechOptions`, 4-арг `getRandomWriteQuizList`, Set-пикер; стаб `nextQuestion()` → `QuizQuestion`.
- `ChatSubHandlerTest`: `returns null`/`returnsMany(null, ref)` → `emptySet()`/наборы; `coVerify(getAvailableTypes)` → `getQuizCoreTypes`.

**Принятый риск.** Два быстрых тапа по галкам → два параллельных
`SaveQuizPickerSelection`, порядок записей не гарантирован, эхо подписки
может откатить оптимистичное состояние. Унаследовано от одиночного ref и
принято для групп (quiz-group-filter §6); не чиним.

**Зачем.** Д1а (все по умолчанию), Д2 (последняя не снимается), Д4
(правило один раз, после «Начать»); попутно чинится пузырь «Продолжить
квиз».

---

## П6. Квиз: эффективные ядра, случайный выбор, структурный вопрос с чипом

**Файлы:** `quiz/QuizGameImpl.kt`, `quiz/QuizGame.kt` (интерфейс),
`quiz/QuizItem.kt` (если `QuizItem` там), `quiz/QuizQuestion.kt` (новый),
`quiz/QuizCoreSelection.kt` (новый), `logic/State.kt`
(`ChatMessage.MessageValue`), `logic/Message.kt` (`MessageContent`),
`widget/message/SystemMessageWidget.kt`, `widget/message/QuestionBody.kt`
(новый); тесты `QuizCoreSelectionTest` (новый), `QuizGameImplFetchDataTest`,
`QuizGameImplTest`, `QuizGameImplEmptyListTest`.

**Одна функция выбора ядер** (инвариант спеки §6.2 в одном месте, П5 и
`fetchData` только проводят):

```kotlin
// quiz/QuizCoreSelection.kt
/** Ядра, которыми спрашивает раунд: выбор ∩ кандидаты; пустое пересечение → все кандидаты. */
fun resolveQuizCores(candidates: List<ComponentType>, selected: Set<ComponentTypeRef>): List<ComponentType> =
    candidates.filter { it.toRef() in selected }.ifEmpty { candidates }
```

`QuizCoreSelectionTest`: пересечение; пусто → все; пустые кандидаты → пусто.

**До:** `fetchData` читает `getQuizConfig`, `getQuizPickerSelection()` →
`effectiveRefs`, `getRandomWriteQuizList(dictionaryId, limit, maxGrade)`,
`toQuizItem(componentRefs, resourceManager, isDebugOn)` — первый ref по
порядку, заголовок из двух ресурсов.

**После:**

```kotlin
class QuizGameImpl(useCase, resourceManager, prefsProvider, logger, private val random: Random) {
    /** Dagger не видит Kotlin-дефолтов: прод получает Random.Default отсюда, тесты зовут первичный с seed/стабом. */
    @Inject constructor(useCase, resourceManager, prefsProvider, logger) : this(useCase, resourceManager, prefsProvider, logger, Random.Default)
}

private suspend fun fetchData(): List<QuizItem> {
    val dictionaryId = … ?: return emptyList()
    val candidates = quizChatUseCase.getQuizCoreTypes(dictionaryId)
    if (candidates.isEmpty()) { logger.w(…); return emptyList() }
    val selected = quizChatUseCase.getQuizPickerSelection(dictionaryId)
    val effective = resolveQuizCores(candidates, selected)
    val posOptions = quizChatUseCase.getPartOfSpeechOptions(dictionaryId).associateBy { it.id }
    return quizChatUseCase.getRandomWriteQuizList(limit = maxStepInSession, maxGrade = maxGrade, dictionaryId = dictionaryId, coreTypeIds = effective.map { it.id.id })
        .also { … debug stat … }
        .mapNotNull { it.toQuizItem(coreRefs = effective.map { it.toRef() }, posOptions = posOptions, random = random, resourceManager = resourceManager, isDebugOn = …) }
}
```

`toQuizItem`:
- `present = coreRefs.mapNotNull { ref -> lexeme.components.firstOrNull { it.matchesRef(ref) && (it.data as? TextValues)?.value?.value != null }?.let { ref to it } }`; пусто → null; `(ref, source) = present.random(random)`.
- `coreTitle` = built-in `TRANSLATION` → `R.string.chat_menu_item_component_translation` (тот же ключ, что подпись галки — спека §6.5); `UserDefined` → `ref.name`.
- `badge` = `lexeme.components.firstOrNull { it.type.systemKey == PART_OF_SPEECH }?.data as? ChoiceValues)?.optionId?.let { posOptions[it] }?.let { opt -> opt.systemKey?.let { posAbbreviation(it) } ?: opt.label }`; `posAbbreviation(key)` — `when (PartOfSpeechOption.key)` → ресурс `chat_quiz_pos_*`; неизвестный ключ → null.
- `QuizItem` (не `QuizInfo` — та запись апсерта `WriteQuizUpsertEntity`, презентации там не место) получает `coreTitle: String`, `badge: String?`, `value: String` (значение ядра как есть) и `debugHeader: AnnotatedString?` (шапка с грейдами, как сейчас, только в debug) — рядом с `answer`/`question`. `fullQuestion` удалить; `question` (значение) для оценки и отчёта — без изменений.

**Структурный вопрос (данные).** Вопрос едет по слоям объектом, а не
размеченной строкой — state чистый JVM, композаблов в нём нет:

```kotlin
// quiz/QuizQuestion.kt
data class QuizQuestion(val header: String, val badge: String?, val value: String, val debugHeader: AnnotatedString?)
// quiz/QuizGame.kt
fun nextQuestion(): QuizQuestion            // было AnnotatedString; non-null остаётся, гард — hasNextQuestion()

// logic/State.kt — ChatMessage.MessageValue: sealed, сегодня Plain + Rich (ВСЕ пузыри бота идут Rich(AnnotatedString))
data class Question(val question: QuizQuestion) : MessageValue   // одна запись, без дубля четырёх полей (logic уже зависит от quiz)
// asText() для Question: (debugHeader ?: "") + "$header:\n$value" — отчёт, логи, тесты; asString() — мёртвый (нет вызовов), удалить

// logic/Message.kt
data class MessageContent(val value: MessageValue, val buttons: List<ChatButton> = listOf())
// companion: все четыре create(String | AnnotatedString × buttons) ОСТАЮТСЯ (userTextEnter, sendSummary, тесты, androidTest ChatMessageMotionTest) + question(q: QuizQuestion, buttons = listOf())
```

`DatasourceEffectHandler.NextQuestion` → `Msg.NextQuestion(MessageContent.question(q))`; `ChatState.addSystemMessage` пробрасывает `message.value` как есть (сейчас заворачивает `text` в `Rich`).

**Бабл со слотом (виджет).** `SystemMessageWidget` остаётся рамкой (аватар,
фон, въезд); тело по `when (value)` — исчерпывающе:

```kotlin
when (val value = message.message) {
    is MessageValue.Plain, is MessageValue.Rich -> Text(text = value.asText(), style = LexemeStyle.BodyM.copy(color = colorScheme.secondary))   // как сейчас
    is MessageValue.Question -> QuestionBody(
        question = value.question,
        leading = value.question.badge?.let { { LexemeBadgeChip(text = it) } },
    )
}
```

```kotlin
// widget/message/QuestionBody.kt — верхняя строка отдельно, ниже: слот слева, значение справа
@Composable
internal fun QuestionBody(question: QuizQuestion, leading: (@Composable () -> Unit)?) {
    val secondary = MaterialTheme.colorScheme.secondary
    val lineHeight = with(LocalDensity.current) { LexemeStyle.BodyM.lineHeight.toDp() }
    Column {
        question.debugHeader?.let { Text(text = it) }                       // стили внутри AnnotatedString, как сейчас (BodySBold, Gray)
        Text(text = "${question.header}:", style = LexemeStyle.BodyM.copy(color = secondary))
        Row(horizontalArrangement = Arrangement.spacedBy(LEADING_GAP)) {
            leading?.let { slot ->
                Box(modifier = Modifier.alignByBaseline().heightIn(max = lineHeight).widthIn(max = LEADING_MAX_WIDTH)) { slot() }
            }
            Text(text = question.value, style = LexemeStyle.BodyMBold.copy(color = secondary), modifier = Modifier.alignByBaseline().weight(1f, fill = false))
        }
    }
}
private val LEADING_GAP = 6.dp
private val LEADING_MAX_WIDTH = 120.dp   // ≈ треть ширины пузыря, приблизительно; длинная пользовательская опция режется чипом
```

Стили и цвета — из прецедента бабла (`SystemMessageWidget`: весь текст
`BodyM` цветом `secondary`, значение `BodyMBold`), иначе заголовок упадёт
в `LocalTextStyle` (17sp `onSurface`) — регресс кегля и цвета. Потолок
слота по высоте — не в dp, а от `lineHeight` стиля значения через
density: масштабируется с системным шрифтом вместе со строкой.

Тело вопроса не знает, что в слоте: только ограничивает его размер и
ставит по базовой линии первой строки значения (`FirstBaseline`
пробрасывается через `Box → Surface → padding → Text`; `alignByBaseline` и
`weight(1f, fill = false)` на одном `Text` совместимы). Перенос значения
идёт в его колонке — висячий отступ без разметки. Чип (`LexemeBadgeChip`,
П1) подставляет диспетчер в `SystemMessageWidget`.

**Тесты:**
- `QuizGameImplTest`: три F4-теста «первый ref по порядку» (`:150-193`) противоречат Д3 — удалить, не обновлять. `toQuizItem` — чистая функция, тестировать здесь со стабом `Random` (`object : Random() { override fun nextInt(until: Int) = index }` → детерминированное ядро): `present` исключает ядра без значения; по двум ядрам при индексе 0/1 — оба; `coreTitle` built-in/user; badge: systemKey → сокращение, пользовательская опция → label, нет значения / удалённая опция → null.
- `QuizGameImplEmptyListTest`: строгий mockk — застабить `getQuizCoreTypes → listOf(type)`, `getQuizPickerSelection → emptySet()`, `getPartOfSpeechOptions → emptyList()`, 4-арг `getRandomWriteQuizList`; снять стабы `getQuizConfig`.
- `QuizGameImplFetchDataTest` — только проводка: `coreTypeIds` = ядра из `resolveQuizCores` переданы; `QuizConfig` не читается; пусто кандидатов → пусто без запроса порции.
- `ChatReducerTest`: `NextQuestion` с `MessageContent.question` кладёт `MessageValue.Question`; `asText()` = «header:\nvalue».

**Зачем.** Д3 (случайное ядро), Д4 (имя ядра), Д6 (чип как отдельный
композабл в слоте, а не разметка в строке).

---

## П7. Меню

**Файлы:** `widget/appbar/menu/QuizComponentMenuItem.kt`,
`widget/appbar/menu/ComponentChoiceItem.kt`, `widget/appbar/ActionsWidget.kt`;
`modules/core/ui/.../dropdown/LexemeRadioMenuItem.kt` — удалить
(проверено: единственный потребитель — `ComponentChoiceItem`), поправить
KDoc-ссылку `[LexemeRadioMenuItem]` в `modules/core/ui/.../dropdown/LexemeRadioRow.kt`.

**До:** подменю с radio, показано при одном типе (disabled).

**После:**

```kotlin
internal fun QuizComponentMenuItem(state: ItemsState.QuizComponent, onToggle: (ref: ComponentTypeRef, checked: Boolean) -> Unit) {
    if (!state.isPickerVisible) return
    LexemeSubmenuMenuItem(title = stringResource(R.string.chat_menu_item_quiz_component), subtitle = stringResource(R.string.chat_menu_quiz_component_hint)) {
        state.availableTypes.forEach { type ->
            val ref = type.toRef()
            val isChecked = ref in state.selectedRefs
            ComponentChoiceItem(type = type, isChecked = isChecked, enabled = !(isChecked && state.selectedRefs.size == 1), onToggle = { onToggle(ref, it) })
        }
    }
}
// ComponentChoiceItem: MenuItem.withCheckbox(isChecked, title = StringSource.fromRaw(title, style = LexemeStyle.BodyL) /* перевод: fromRes(R.string.chat_menu_item_component_translation, style = LexemeStyle.BodyL) */, enabled, onCheckedChange = onToggle).Widget()
// ActionsWidget: onToggle = { ref, checked -> sendMessage(Msg.ToggleQuizComponent(ref, checked)) }
```

Стиль подписи — `LexemeStyle.BodyL`, как у соседних галок
(`EarliestReviewedMenuItem` и др.); без стиля `StringSource` упадёт в
`LocalTextStyle` пункта меню (13sp ExtraBold) — ряды одного меню разным
кеглем.

**Зачем.** Д1 (скрыто при одном), Д2 (галки, последняя задизейблена,
подпись).

---

## П8. Доки, Backlog, ручник

- `docs/features/IS511_quiz_multi_component/manual_test.md` — M1: один
  кандидат → меню нет; M2: два ядра → галки, обе по умолчанию,
  последняя задизейблена, подпись; M3: раунд с двумя ядрами — вопросы
  обеих сторон, слово один раз, имя ядра в заголовке; M4: «только
  определение» — только слова с определением, раунд добирается; M5: чип
  части речи (есть / нет значения / пользовательская опция) + шаг с
  крупным системным шрифтом (чип не выше строки, значение переносится
  висячим отступом); M6: правило один раз после «Начать» — въезжает
  отдельным пузырём после паузы; нет ни после «Продолжить квиз»
  напрямую, ни после «Посмотреть отчёт → Продолжить квиз»; пузырь юзера
  при этом «Продолжить квиз», не «Начать»; M7: выбор переживает
  перезапуск; M8: старое сохранённое одиночное значение читается
  (поставить 0.1.10, выбрать, обновиться); M9: переключил галку посреди
  раунда — текущий раунд не меняется, следующий — по новому выбору;
  M10: пустой раунд (галка только «Определение», определений нет ни у
  одного слова) — правило, затем «Сессия завершена! Всего слов: 0» и
  кнопки, без крэша (принято как есть, см. Backlog). Раздел «Регресс
  IS508»: кейсы M10–M14 ручника IS508 (морф «Начать», полёт ответа,
  стыковка, финал по одному) + device-тест `ChatMessageMotionTest`.
  Формат — `~/.claude/skills/manual-tests`.
- `docs/Backlog.md` → Tech Debt: удалить `QuizConfig`/`quiz_config` (не
  редактируется, квизом не читается); `ChatScreenState.disableUserInput()`
  ставит `isUserInputEnable = true` (TODO в коде, поле не блокируется пока
  бот думает); `Answer.toSummaryString()` — русский хардкод «(ваш ответ:
  …)»/«(пропущено)» в отчёте, en-локаль получает русский;
  `ChatSub.QuizPicker` переиздаёт типы (с походом в БД) на запись любого
  ключа DataStore — нет `distinctUntilChanged` по значению. Продолжение
  фич: настраиваемый набор атрибутов в вопросе (несколько чипов,
  CHOICE-атрибуты пользователя); пустой раунд под фильтр ядер — отдельное
  сообщение бота «нет слов для выбранных компонентов» вместо итога с
  нулями. ВекторныйПиздеж: миграция `ChatReducer` на
  `ReducerResult`/атомарные экстеншны (ревьюер B, IS511: редьюсер на
  Pair-стиле, план остаётся в нём; почему не сейчас — out-of-scope,
  отдельный бриф).
- Спеки обновлены до плана (quiz-chat, word-model, quiz-group-filter §7.1,
  component-constructor, README) — при исполнении сверить с кодом.

---

## Тесты (сводно)

JVM: `QuizChatUseCaseImplTest`, `ChatReducerTest`, `DatasourceEffectHandlerTest`,
`ChatSubHandlerTest`, `QuizCoreSelectionTest`, `QuizGameImplFetchDataTest`,
`QuizGameImplTest`, `QuizGameImplEmptyListTest`, `EntranceTest` (строит
`MessageContent.create(String)` — перегрузка остаётся). Device:
`QuizGroupFilterDaoTest` (переписан под ядра, П3) и `ChatMessageMotionTest`
(регресс IS508; его компиляция — отдельный шаг
`:modules:screen:quiz:chat:compileDebugAndroidTestKotlin`, иначе поломка
androidTest незаметна ни unit-тестам, ни lint). Регресс: unit всех
затронутых модулей по одному, `:app:lintDebug`, `installDebug`,
device-тесты, ручник M1–M10 + регресс IS508.

## Риски исполнения

1. Структурный вопрос: `MessageContent.text` → `value: MessageValue` —
   читатели: `SystemMessageWidget` (единственный системный), `asText()`
   (exhaustive `when`, компилятор заставит добавить `Question`),
   `ChatState.addSystemMessage` (заворачивает `text` в `Rich` — пробрасывать
   `value`), превью `ChatWidget`, тесты. Пользовательские виджеты
   (`MorphingUserMessage`, `FlyingUserMessage`, `UserMessageWidget`) зовут
   `asText()` — тип не меняется. Ширина слота 120dp приблизительная —
   проверить на девайсе с крупным системным шрифтом.
2. Room `IN (:coreTypeIds)` — use case гарантирует непустой список.
3. `QuizGame.nextQuestion()` меняет тип возврата — `DatasourceEffectHandler.NextQuestion`,
   стабы в `DatasourceEffectHandlerTest`/`QuizGameImplTest`.
4. Удаление `getAvailableTypes`/`SelectQuizComponent`/`LexemeRadioMenuItem`/двух
   строк — компилятор найдёт всех потребителей; инвентарь тестов — в П4–П6.
5. `LoadQuiz(reload)`: `QuizReLoaded` оживает впервые с IS399 — его ветка
   редьюсера (`continueUserMessage`) не гонялась; покрыть тестом и M6.
6. Гонка записи набора при быстрых тапах — принятый риск (П5).

## Триаж ревью (2026-10-01)

| # | Пункт | Решение |
|---|---|---|
| 1 | `QuizReLoaded` мёртв, правило одним апдейтом с морфом (C-1, C-2, A-2, B-9) | `LoadQuiz(reload)` → `QuizReLoaded`; правило через `DeliverSystemMessages([rule], then = NextQuestion)` (П5) |
| 2 | Пустой раунд под фильтр (C-3) | Оставить итог с нулями; кейс M10; отдельное сообщение бота — Backlog |
| 3 | Модель вопроса (B-1, B-2/A-5, B-3, B-4/A-6, B-8/C-4, B-11) | Ветка `Rich`; поля на `QuizItem`; `nextQuestion()` non-null; вторичный `@Inject`-конструктор с `Random.Default`; перегрузки `create` остаются + компиляция androidTest в круг; `Question(val question: QuizQuestion)` (П6) |
| 4 | Инвариант выбора в двух местах (B-5) | `resolveQuizCores` в `quiz/QuizCoreSelection.kt`, редьюсер и игра — проводка (П5, П6) |
| 5 | Стили и размеры (A-3, A-4, A-7, A-8, C-7) | Прецедент бабла (`BodyM`/`BodyMBold`, `secondary`), подпись `BodyS`, галка `BodyL`, чип `labelSmall`, потолок слота от `lineHeight` (П1, П6, П7) |
| 6 | Два ключа «Перевод» (C-5) | Один ключ `chat_menu_item_component_translation`, `chat_quiz_core_translation` не заводить (П2, П6) |
| 7 | Инвентарь (B-6/A-1, B-7, B-10, B-12, C-6, C-8/A-9, A-10) | DAO-тест переписать как факт; F4-тесты снять; фикстура `core = true`; fake/моки; ручник M9/M10 + регресс IS508; KDoc `LexemeRadioRow`; гонка — принятый риск |
