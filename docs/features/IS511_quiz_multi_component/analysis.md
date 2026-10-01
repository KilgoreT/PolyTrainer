# IS511 | Анализ: несколько ядер в вопросе квиз-чата

Бриф: [brief.md](brief.md) (Д1–Д6), модель: [word-model/spec.md](../../handbook/specs/word-model/spec.md) §2.6.
Код — master 6fcb27e5.

## 1. Что есть (по слоям)

| Слой | Сейчас |
|---|---|
| Кандидаты пикера | `QuizChatUseCaseImpl.getAvailableTypes`: `lexemeApi.getComponentTypes(dict)` → фильтр `template == TEXT`. `core`/`enabled`/`removedAt` не смотрит (API-сущность их отдаёт). |
| Хранение выбора | prefs `quiz_picker_dict_<id>` = одна строка `builtin:translation` / `user:<name>` (`encodeRef`/`decodeRef` в use case); `ChatSub.QuizPicker` переиздаёт `QuizComponentTypesLoaded(types, restoredSelectedRef)` при записи. |
| Состояние/редьюсер | `ItemsState.QuizComponent(availableTypes, selectedRef)`, `isPickerEnabled = size > 1`, `resolveSelection` (restored ∈ available ? restored : first), `Msg.SelectQuizComponent(ref)` → `SaveQuizPickerSelection(ref)`. |
| Меню | `QuizComponentMenuItem` → `LexemeSubmenuMenuItem(title)` + radio `ComponentChoiceItem` (`LexemeRadioMenuItem`); скрыт при пустом списке, при одном — показан задизейбленным. Галка меню: `MenuItem.withCheckbox(isChecked, title, onCheckedChange)` без `enabled` (`iconDropDowned`). |
| Квиз | `QuizGameImpl.fetchData`: `getQuizConfig` (null → пусто), `effectiveRefs = selectedRef?.let { listOf(it) } ?: quizConfig.componentRefs`; `getRandomWriteQuizList(limit, maxGrade, dict)` → `mapNotNull { toQuizItem(effectiveRefs) }` — первый ref по порядку, у кого есть значение; нет — слово выбрасывается ПОСЛЕ отбора порции. |
| Вопрос | `fullQuestion` = заголовок (`chat_quiz_ask_translation_header` для built-in, `chat_quiz_ask_definition_header` для любого пользовательского) + `\n` + значение жирным. |
| Порция | `WordDao.getWriteQuizIds` (грейд, словарь, группа), `getEarliest`, `getFrequentMistakes` — без знания о компонентах. `component_values(lexeme_id, component_type_id, option_id, removed_at)`. |
| Часть речи | builtin CHOICE (`BuiltInComponent.PART_OF_SPEECH`), значение `ChoiceValues(optionId)`, опции `ComponentOption(systemKey: noun/verb/…, label для пользовательских)`; `LexemeApi.getComponentOptions(typeId)`. В квизе не используется. |
| Старт сессии | `QuizLoaded` → пузырь «Начать» (+ статистика только с debug) → `NextQuestion`. Правила игры бот не говорит. |

## 2. Целевое поведение (из брифа)

1. Кандидаты = ядра словаря: `core && enabled && removedAt == null` и шаблон из списка, который квиз умеет показать (`QUIZ_RENDERABLE_TEMPLATES = {TEXT}`), по `position`.
2. Один кандидат — подменю не показывается, вопросы по нему.
3. Несколько — галки; выбор = множество; без сохранённого выбора — все; единственная включённая задизейблена; подпись «минимум одно».
4. Порция отбирается только из лексем, у которых есть живое значение хотя бы в одном включённом ядре (фильтр в SQL грейдов и добавок).
5. Для лексемы ядро показа — случайно из включённых, которые у неё заполнены.
6. Вопрос: «Имя ядра:» / `[сущ.]` **значение**; чип части речи — если заполнена.
7. После «Начать» — пузырь-правило «Я даю подсказку — ты пишешь слово.», затем первый вопрос; после «Продолжить квиз» — нет.

## 3. Как делать

### 3.1. Кандидаты (`QuizChatUseCaseImpl.getAvailableTypes` → `getQuizCoreTypes`)

Фильтр по модели: `core && enabled && removedAt == null && template in QUIZ_RENDERABLE_TEMPLATES`. Список шаблонов — константа модуля чата (`quiz/QuizRenderable.kt`), передаётся в use case как параметр или проверяется в `QuizGameImpl`; чище — в use case один фильтр и KDoc «что квиз умеет показать». Сортировка по `position`.

### 3.2. Хранение набора

Тот же ключ `quiz_picker_dict_<id>`, значение — токены `builtin:<key>` / `user:<name>`, разделённые `\u001F` (unit separator: в именах не встречается, в отличие от запятой/перевода строки). Старое одиночное значение читается как набор из одного — миграция не нужна. Контракт use case:

```kotlin
suspend fun getQuizPickerSelection(dictionaryId: Long): Set<ComponentTypeRef>   // пусто = не сохранено
suspend fun setQuizPickerSelection(dictionaryId: Long, refs: Set<ComponentTypeRef>)
```

Неизвестные/битые токены отбрасываются (как сейчас у одиночного).

### 3.3. Состояние и редьюсер

```kotlin
data class QuizComponent(
    val availableTypes: List<ComponentType> = emptyList(),
    val selectedRefs: Set<ComponentTypeRef> = emptySet(),
)
val ItemsState.QuizComponent.isPickerVisible get() = availableTypes.size > 1
```

`resolveQuizCores(candidates, selected)` — одна чистая функция в `quiz/QuizCoreSelection.kt`: `selected ∩ candidates`; пусто → все `candidates`. Её же зовёт `fetchData` (§3.5) — инвариант спеки §6.2 реализован и тестируется в одном месте, редьюсер и игра только проводят. Инвариант после загрузки: `selectedRefs` непусто, если `availableTypes` непусто.

`Msg.SelectQuizComponent(ref)` → `Msg.ToggleQuizComponent(ref, checked)`. Редьюсер: `next = if (checked) selected + ref else selected - ref`; если `next` пусто — состояние не меняется, эффекта нет (защита от гонки с задизейбленной галкой); иначе `state.selectedRefs = next` (оптимистично) + `SaveQuizPickerSelection(next)`. Саб `QuizPicker` после записи переиздаёт то же — без скачка.

### 3.4. Меню

- `QuizComponentMenuItem`: `if (!state.isPickerVisible) return`; `LexemeSubmenuMenuItem(title, subtitle = «минимум одно»)` — новый необязательный параметр `subtitle` в UI-ките (мелкий текст под заголовком).
- `ComponentChoiceItem` → галка: `MenuItem.withCheckbox(isChecked, title, enabled, onCheckedChange)`; `enabled = !(isChecked && selected.size == 1)`. В `iconDropDowned`: `MenuItemWithCheckbox.enabled: Boolean = true`, прокинуть в `Checkbox(enabled)` и в `DropdownMenuItem(enabled)`. `LexemeRadioMenuItem` остаётся для других мест (если есть) либо удаляется, если пикер был единственным потребителем — проверить grep.
- Заголовок галки — как сейчас (`ComponentChoiceItem`: перевод из ресурса, пользовательское — имя).

### 3.5. Квиз: порция, выбор ядра, вопрос

**Порция.** `QuizChatUseCase.getRandomWriteQuizList(limit, maxGrade, dictionaryId, coreTypeIds: List<Long>)`; `QuizApi.getWriteQuizIds/getEarliestWriteQuizList/getFrequentMistakesWriteQuizList` получают `coreTypeIds`; SQL — общий фрагмент:

```sql
AND EXISTS (
    SELECT 1 FROM component_values cv
    WHERE cv.lexeme_id = write_quiz.lexeme_id
      AND cv.removed_at IS NULL
      AND cv.component_type_id IN (:coreTypeIds))
```

Room поддерживает `IN (:list)`. Пустой список кандидатов невозможен (ядро у словаря есть всегда); если `coreTypeIds` пуст — use case возвращает пусто, не ходя в БД.

**Эффективные ядра в `fetchData`.** `candidates = useCase.getQuizCoreTypes(dict)`; `selected = useCase.getQuizPickerSelection(dict)`; `effective = resolveQuizCores(candidates, selected)` (§3.3). `QuizConfig` в `fetchData` больше не читается (Backlog: удалить сущность); null-гейт «нет конфига → пусто» заменяется на «нет кандидатов → пусто» (словарь без включённого ядра невозможен по §7.8).

**Выбор ядра для лексемы.** `toQuizItem(coreRefs, random: Random, …)`: `present = coreRefs.filter { ref -> lexeme.components.any { it.matchesRef(ref) && it.data is TextValues } }`; пусто → null (страховка; SQL уже отфильтровал); иначе `present.random(random)`. `Random` — параметр первичного конструктора `QuizGameImpl`; Dagger Kotlin-дефолтов не видит, поэтому прод получает `Random.Default` через вторичный `@Inject`-конструктор, тесты зовут первичный со стабом (`nextInt(until)` → нужный индекс).

**Вопрос.** `QuizItem` (не `QuizInfo` — та запись апсерта) получает `coreTitle: String` (имя ядра: built-in TRANSLATION → тот же ресурс, что подпись галки, `chat_menu_item_component_translation` «Перевод»; пользовательское → `type.name`), `badge: String?` (часть речи), `value: String` и `debugHeader: AnnotatedString?`. Вопрос идёт по слоям **объектом**, не размеченной строкой: `QuizGame.nextQuestion(): QuizQuestion` (non-null, как сейчас; `header, badge, value, debugHeader`), в state — `ChatMessage.MessageValue.Question(val question: QuizQuestion)` рядом с существующими `Plain` и `Rich` (все пузыри бота сегодня `Rich(AnnotatedString)`); `asText()` для вопроса = «header:\nvalue» (отчёт/логи/тесты), `asString()` без вызовов — удалить. `MessageContent(value: MessageValue, buttons)`, четыре перегрузки `create(String | AnnotatedString × buttons)` остаются (на них прод-код и device-тест), добавляется `question(q)`. State — чистый JVM, композаблов в нём нет; бейдж в данных — просто текст («сущ.»).

В виджете бабл (`SystemMessageWidget`) остаётся рамкой и по `when (value)` рисует либо `Text` (`Plain`, `Rich` — как сейчас), либо `QuestionBody(question, leading: (@Composable () -> Unit)?)`: верхняя строка «Имя ядра:» отдельно, под ней `Row` — слева слот, справа значение жирным. Стили и цвета — из прецедента бабла: `LexemeStyle.BodyM`/`BodyMBold` цветом `secondary` (без явного стиля заголовок упал бы в `LocalTextStyle` 17sp `onSurface`). Тело вопроса не знает, что в слоте, только ограничивает его размер (высота — `lineHeight` стиля значения через density, масштабируется с системным шрифтом; ширина ≈ 120dp, приблизительно) и ставит по базовой линии первой строки значения (`alignByBaseline`; `FirstBaseline` пробрасывается через `Box → Surface → Text`). Значение переносится в своей колонке — висячий отступ сам собой. В слот диспетчер подставляет `LexemeBadgeChip(text)` — новый общий виджет в `modules/core/ui` (`Surface(secondaryContainer, RoundedCornerShape(6.dp)) { Text(typography.labelSmall, onSecondaryContainer, maxLines = 1, Ellipsis, padding 4/1) }`); нет бейджа — слота нет. Два legacy-заголовка удаляются из ресурсов (ru/en).

**Часть речи.** В `fetchData` один раз: `posType = candidatesAll.firstOrNull { it.systemKey == PART_OF_SPEECH }` (из полного списка типов, не только ядер — нужен отдельный `useCase.getComponentTypes(dict)` либо `getQuizCoreTypes` возвращает пару; проще: use case `getPartOfSpeechOptions(dict): List<ComponentOption>` — находит тип и грузит опции). В `toQuizItem`: значение `PART_OF_SPEECH` у лексемы → `ChoiceValues.optionId` → опция → `systemKey?.let { abbr(it) } ?: label`. Сокращения — ресурсы `chat_quiz_pos_noun` = «сущ.», `verb` = «гл.», `adjective` = «прил.», `adverb` = «нареч.», `preposition` = «предл.», `phrase` = «фраз.» (en: n., v., adj., adv., prep., phr.; словарные пометки). Нет значения — чипа нет.

**Правило игры.** Факт кода: `Msg.QuizReLoaded` объявлен, но никем не шлётся с IS399 — «Продолжить квиз» идёт тем же `DatasourceEffect.LoadQuiz` → `QuizLoaded` (поэтому после продолжения пузырь юзера пишет «Начать»). Чиним по дороге: `LoadQuiz(reload: Boolean)`, хендлер отдаёт `QuizReLoaded` при `reload`. Правило — отдельный пузырь бота после паузы, не в одном апдейте с морфом «Начать»: контракт ленты IS508 (`MorphingUserMessage`: вставок во время морфа нет; `DeliverSystemMessages`: пачка одним апдейтом по одному не въезжает). Механизм — расширить капельную выдачу «что сделать после последнего»: `DeliverSystemMessages(messages, then: DatasourceEffect?)` / `SystemMessageDelivered(message, rest, then)`; редьюсер `QuizLoaded` → `userMessage(«Начать»)` + `DeliverSystemMessages([rule (+ debug-стат)], then = NextQuestion)`; на пустом `rest` эмитится `then`. `QuizReLoaded` → `userMessage(«Продолжить квиз»)` + (стат так же, иначе сразу `NextQuestion`) — без правила.

**Пустой раунд.** Если под фильтр ядер (Д3) не попала ни одна лексема (например, галка только «Определение», определений нет), порция пустая: «Начать» → правило → `NextQuestion` → `hasNextQuestion() == false` → «Сессия завершена! Всего слов: 0» + кнопки. Принято как есть (решение триажа), кейс в ручник; отдельное сообщение бота — Backlog.

### 3.6. Отчёт и оценка

Без изменений: оценка/«Правильный ответ» показывают слово; отчёт — `question` (значение ядра) и ответ. Чип в отчёт не идёт.

## 4. Тесты

- **app `QuizChatUseCaseImplTest`:** `getQuizCoreTypes` фильтрует не-ядра, выключенные, удалённые, не-TEXT; сортировка; кодек набора: round-trip, старое одиночное значение → набор из одного, битые токены отбрасываются, пустая строка → пусто; `getRandomWriteQuizList` передаёт `coreTypeIds` в все три запроса; пустой список → пусто без запросов.
- **`ChatReducerTest`:** `QuizComponentTypesLoaded`: restored ∩ available, пусто → все, `isPickerVisible` при 1 и 2; `ToggleQuizComponent`: добавление, снятие, снятие последней — no-op без эффекта; `QuizLoaded` → правило перед вопросом, `QuizReLoaded` — без.
- **`QuizGameImplFetchDataTest`:** эффективные ядра = выбор ∩ кандидаты, пусто → кандидаты; `QuizConfig` не читается; seeded `Random` — распределение по двум ядрам (оба встречаются), лексема без значения во включённых → пропуск; `coreTitle` для built-in/user; badge из опции (systemKey → сокращение, label для пользовательской опции, null без значения).
- **`ChatSubHandlerTest` / `DatasourceEffectHandlerTest`:** новые сигнатуры (`Set`), `SaveQuizPickerSelection(set)`.
- **core-db-impl androidTest (если есть DAO-тесты квиза):** EXISTS-фильтр: лексема с живым значением в типе из списка попадает, без — нет, удалённое значение не считается. Иначе — проверка на девайсе через ручник (словарь с двумя ядрами, часть слов без определения, галка «только определение»).
- **Ручник:** меню скрыто при одном ядре; две галки по умолчанию; последняя задизейблена, подпись; раунд с обоими ядрами — вопросы обеих сторон, слово один раз; «только определение» — раунд из слов с определением; чип части речи; правило после «Начать», нет после «Продолжить».

## 5. Риски

1. **Формат prefs** — разделитель `\u001F`; старое значение читается без миграции. Пользовательское имя с таким символом невозможно (ввод с клавиатуры его не даёт).
2. **Структурный вопрос** — `MessageContent.text` → `MessageContent.value: MessageValue`: единственный системный читатель — `SystemMessageWidget`; `asText()` exhaustive; `addSystemMessage` заворачивает `text` в `Rich` — пробрасывать `value`; перегрузки `create(String)` оставить, иначе молча ломается androidTest (не компилируется ни unit, ни lint — отдельный шаг компиляции androidTest в регресс-круге). Ширина слота приблизительная, проверить на девайсе с крупным шрифтом.
3. **Room `IN (:list)`** в трёх запросах — Room генерирует параметры; пустой список даёт `IN ()` — use case гарантирует непустой.
4. **Тесты квиза** — `QuizGameImplFetchDataTest` завязан на `QuizConfig` и одиночный ref — переписывается на проводку; три теста «первый ref по порядку» в `QuizGameImplTest` противоречат Д3 — удаляются; `QuizGameImplEmptyListTest` на строгом mockk — застабить новые методы. DAO-тесты квиза (`QuizGroupFilterDaoTest`, 14 штук) сеют лексемы без значений — после EXISTS вернут пусто, фикстуры переписать.
5. **Удаление `LexemeRadioMenuItem`** — потребитель один (`ComponentChoiceItem`), удаляем; KDoc-ссылка в `LexemeRadioRow` — поправить.
6. **Часть речи** — у пользовательской CHOICE-опции label может быть длинным; слот ограничен по ширине, чип режет текст многоточием в одну строку, значение не страдает.
7. **`QuizReLoaded` оживает впервые с IS399** — ветка редьюсера с `continueUserMessage` не гонялась; тест + ручник.
8. **Гонка записи набора** при быстрых тапах по галкам — порядок параллельных `SaveQuizPickerSelection` не гарантирован, эхо подписки может откатить оптимистичное состояние. Унаследовано, принято (как для групп, quiz-group-filter §6).
