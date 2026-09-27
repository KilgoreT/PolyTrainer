# IS500 | Квиз по группе — план реализации

Бриф: [brief.md](brief.md). Решения Д1–Д6 согласованы, план раскладывает
их по слоям и этапам. Сверка со спеками — раздел «Согласованность со
спеками» внизу.

---

## Разведка кода (факты, на которых стоит план)

- **Плашка входа** — `modules/screen/quiztab`: экран почти пустой
  (`QuizTabState` = только снекбар), одна карточка `QuizItemWidget` →
  `Msg.OpenChat(quizType = "chat")`. Подписок и datasource-эффектов у
  таба нет — появятся в этой фиче.
- **Выборка квиза** — `QuizChatUseCaseImpl.getRandomWriteQuizList(limit,
  maxGrade, dictionaryId)`: грейд-корзины из
  `WordDao.getWriteQuizIds(grade, langId)` + добавки
  `getEarliest` / `getFrequentMistakes`. Все три запроса — по
  `write_quiz` (у неё есть `lexeme_id`, слова нет — путь к группе:
  `write_quiz → lexemes.word_id → word_groups`).
- **Membership** — таблица `word_groups` (PK `(word_id, group_id)`);
  при удалении группы membership чистится hard-delete (спека групп §6),
  то есть квиз-запрос никогда не увидит слова мёртвой группы.
- **Прецедент персиста** — quiz picker (IS481):
  `quizPickerPrefKey(dictionaryId)` в `modules/datasource/prefs`, raw
  string API `PrefsProvider`, encode/decode в `QuizChatUseCaseImpl`,
  подписка чата на pref — `ChatSubHandler` (`getStringFlowByRawKey`).
- **Прецедент «текущий словарь»** — `GroupsTabUseCaseImpl.flowCurrentDictId()`:
  `getLongFlow(CURRENT_DICTIONARY_ID_LONG)` + fallback на первый словарь
  (инварианты pref'а — спека dictionary-list).
- **Имя «Все»** — ресурс `group_all_title` в `core/core-resources`
  (инвариант «„Все" не в БД, имя — ресурс UI»). Переиспользуем.
- **domain-модули** (`domain/group`, `domain/lexeme`) — pure-JVM,
  zero-deps (даже без coroutines): чистые функции + типы границы.

---

## Архитектура: три куска общего слоя (Д6) + два потребителя

```
┌ modules/domain/quiz (НОВЫЙ, pure-JVM) ────────────────────────┐
│ MIN_QUIZ_WORDS = 3; QuizTypes.CHAT; QuizGroup(id, name,       │
│ wordCount, isEligible); воронка resolveQuizGroupState(...)    │
└───────────────────────────────────────────────────────────────┘
                             ▲   (core-db от домена НЕ зависит:
                             │    счётчики — Api-entity + маппинг в app)
┌ core-db (данные) ─┐  ┌ app: QuizGroupSelectionStore ──────────┐
│ счётчики групп,   │  │ prefs-ключ (тип×словарь) + валидация   │
│ groupId в квиз-   │  │ при чтении (Д1, Д3); один impl на всех │
│ запросах          │  │ потребителей                           │
└───────────────────┘  └────────────────────────────────────────┘
        ▲                    ▲                ▲
┌ quiztab (карточка chat) ───┐   ┌ quiz/chat (QuizGameImpl) ────┐
│ пикер в плашке (Д4),       │   │ фильтр выборки по groupId    │
│ disabled-флаг (Д2)         │   │                              │
└────────────────────────────┘   └──────────────────────────────┘
```

---

## Э1. Данные (core-db-api / core-db-impl)

**Фильтр группы в трёх квиз-запросах** `WordDao` (`getWriteQuizIds`,
`getEarliest`, `getFrequentMistakes`) — nullable-параметр `groupId`
одним SQL, без дублей запросов; фильтр — СТРОГО внутри WHERE тех же
`@Query` (Kotlin-постфильтр поверх LIMIT дал бы «те из глобального
топа, кто в группе» вместо «топ группы» — закрепить DAO-тест-пином):

```sql
AND (:groupId IS NULL OR EXISTS (
    SELECT 1 FROM lexemes l
    JOIN word_groups wg ON wg.word_id = l.word_id
    JOIN dictionary_groups dg ON dg.id = wg.group_id
        AND dg.removed_at IS NULL
    WHERE l.id = write_quiz.lexeme_id AND wg.group_id = :groupId))
```

Liveness-JOIN формально избыточен (у мёртвой группы membership'ов нет —
hard-delete в той же транзакции, спека групп §6), но канон §4.4 требует
«все read'ы word_groups JOIN'ят живые группы» — одна строка SQL
исполняет букву и страхует будущий restore групп (решение по ревью
2026-09-26). Гонка «группу удалили между валидацией и запросом» даёт
пустую выборку → существующее пустое состояние чата (TOCTOU принят).
Новых индексов не нужно: EXISTS кроется PK-lookup'ами lexemes(id) и
word_groups(word_id, group_id).

**Гигиена инварианта «лексема ⇔ write_quiz»** (фундамент счётчика П5) —
в этом же этапе: удалить мёртвые `LexemeApi.addLexeme(wordId)` (создаёт
лексему БЕЗ квиз-строки; прод-вызовов ноль, история — сирота IS299
после переезда карточки на `addLexemeWithQuiz`/`WithComponents`) и
`WordDao.removeWriteQuiz(lexemeId)` (сирота IS180) + их androidTest-
хвосты; инвариант зафиксировать KDoc'ом у легальных путей создания.

**Счётчики для пикера** — `CoreDbApi.QuizApi` (не GroupApi: семантика
квизовая — по ревью), живые Flow, возврат — Api-entity с маппингом в
app (core-db-api НЕ зависит от domain/quiz; прецедент D7.3
membershipSlice). Семантика счётчика — уровень 1 (П5): считаются
слова, имеющие **хотя бы одну лексему** (инвариант: каждая лексема при
создании получает строку `write_quiz`):

- `flowQuizGroupCounts(dictionaryId)` — живые группы словаря со
  счётчиком; ловушка INNER JOIN снята двойным LEFT JOIN (группа с
  0 слов возвращается со счётчиком 0 — порог живёт ТОЛЬКО в домене,
  SQL вторым скрытым порогом не становится):

  ```sql
  SELECT dg.id AS groupId, dg.name AS name,
         COUNT(DISTINCT l.word_id) AS wordCount
  FROM dictionary_groups dg
  LEFT JOIN word_groups wg ON wg.group_id = dg.id
  LEFT JOIN lexemes l ON l.word_id = wg.word_id
  WHERE dg.dictionary_id = :dictionaryId AND dg.removed_at IS NULL
  GROUP BY dg.id
  ```

  наблюдает `dictionary_groups` + `word_groups` + `lexemes` (words
  наблюдать не нужно — каскады инвалидируют через word_groups/lexemes);
- `flowDictionaryQuizWordCount(dictionaryId): Flow<Int>` — для порога
  «Все»: `COUNT(*) FROM words w WHERE w.dictionary_id = :id AND EXISTS
  (SELECT 1 FROM lexemes l WHERE l.word_id = w.id)` (существующий
  `flowWordCount(langId: Int)` не подходит: Int-id и считает слова без
  лексем).

`QuizApi`: `groupId: Long? = null` в `getWriteQuizIds` /
`getEarliestWriteQuizList` / `getFrequentMistakesWriteQuizList` —
дефолт сохраняет всех текущих вызывающих (mockk-стабы существующих
тестов не ломаются: стаб запишет `eq(null)`).

Тесты (честно: DAO-тесты проекта — androidTest, гоняются на девайсе,
`testDebugUnitTest` их не прогонит): фильтр (слово в группе / вне /
в двух группах, группа удалена → пусто), тест-пин earliest/mistakes
(глобально самая ранняя запись вне группы + записи группы →
возвращаются записи группы), счётчики (слово без лексем НЕ считается;
слово с 2 лексемами — один раз; пустая группа → 0; чужой словарь не
попадает).

## Э2. Домен (`modules/domain/quiz` — новый pure-JVM модуль)

По образцу `domain/group` (kotlin-jvm, zero-deps, junit):

- `const val MIN_QUIZ_WORDS = 3` — единственная точка правды порога
  (Д2, У1);
- `object QuizTypes { const val CHAT = "chat" }` (П2);
- `data class QuizGroup(val id: Long, val name: String,
  val wordCount: Int, val isEligible: Boolean)` — группы ниже порога
  НЕ выбрасываются, а помечаются (ревью-UX: в пикере они видны
  disabled-пунктами — порог самообъясняющий);
- `data class QuizGroupOptions(val isAllEligible: Boolean,
  val allWordCount: Int, val groups: List<QuizGroup>)`;
- **единая воронка** `fun resolveQuizGroupState(groupCounts,
  dictionaryWordCount, persistedGroupId, threshold = MIN_QUIZ_WORDS):
  QuizGroupState(options, selectedGroupId)` — И store чата, И подписка
  плашки зовут ТОЛЬКО её (иначе два пути валидации разъезжаются —
  ревью-архитектура); внутри: пометка eligible (порог действует и на
  «Все»), резолв персиста — молчаливый фолбэк на «Все» (Д3): id вне
  eligible-множества → null; задел per-тип — параметр `threshold` с
  дефолтом (Д2).

Сортировка групп — locale-aware Collator на вызывающей стороне (как
`wordGroups` в спеке групп §5); домен порядок не навязывает.

Тесты чистых функций (порог, границы, фолбэк).

## Э3. Персист — selection-store (Д1, Д3)

- `modules/datasource/prefs`: `quizGroupPrefKey(quizType: String,
  dictionaryId: Long) = "quiz_group_${quizType}_dict_$dictionaryId"` —
  рядом с `quizPickerPrefKey`, тот же single-source-of-truth стиль.
  Значение — `groupId` строкой через raw string API; отсутствие/мусор →
  «Все».
- `app/.../di/module/quizgroup/QuizGroupSelectionStore` — один класс на
  всех потребителей (инжектится в `QuizTabUseCaseImpl` и
  `QuizChatUseCaseImpl`):
  - `suspend fun getValidatedSelection(quizType, dictionaryId): Long?` —
    pref → снапшот счётчиков → воронка `resolveQuizGroupState` (Э2);
    карточки и чат получают уже валидированный выбор (Д3). KDoc-контракт:
    `dictionaryId` — ТОЛЬКО параметром, self-read pref'а словаря внутри
    store запрещён (иначе гонка смены словаря рвёт консистентность пары
    «словарь-группа» на момент выборки — ревью-SQL);
  - `suspend fun setSelection(quizType, dictionaryId, groupId: Long?)` —
    null («Все») пишет отсутствие значения;
  - `fun flowSelection(quizType, dictionaryId): Flow<Long?>` — сырой
    pref-поток для подписки плашки (валидацию делает combine подписки
    той же воронкой — счётчики уже в руках, без двойного чтения БД).
- **Общий `CurrentDictionaryProvider`** (app-слой) — решение ревью:
  резолв «текущий словарь + fallback на первый» скопирован уже в
  четырёх UseCaseImpl (аппбар, слова, группы, статистика), quiztab стал
  бы пятым. Вместо копии — один провайдер (`flowCurrentDictId()` /
  `getCurrentDictId()`), quiztab берёт его сразу, четыре существующих
  UseCaseImpl мигрируют на него в рамках фичи.
- Идентификатор типа квиза — константа общего слоя (`QuizTypes.CHAT` в
  `domain/quiz`), одна и та же в плашке и в `QuizChatUseCaseImpl` (П2);
  chat-модуль зависимости на `domain/quiz` не получает — резолв выбора
  целиком в app (см. Э4, «Чат»).

Смена словаря отдельного кода не требует: ключ содержит `dictionaryId`,
каждый словарь помнит свои выборы (У2), перечитку делает подписка Э4.

## Э4. UI: виджет пикера + карточка chat в quiztab

**Общий виджет** (Д4) — живёт ВНУТРИ quiztab
(`quiztab/widget/QuizGroupPickerWidget.kt`, П3: будущие карточки
появятся на этом же табе — дотянутся без выноса; вынос в
`modules/widget` — если пикер понадобится вне таба):
`QuizGroupPickerWidget(selectedName, options, enabled, onPick)` —
строка «Группа: Все ▾», по тапу DropdownMenu: «Все» — закреплённым
первым пунктом + группы словаря, у каждого пункта счётчик слов
(«Все · 47», «Быт · 12» — П1); группы ниже порога — в списке
disabled-пунктами (серым, некликабельно: «Быт · 2») — порог
самообъясняющий, «куда делась моя группа» не возникает (ревью-UX,
уточнение Д2); выбранный пункт отмечен; maxHeight дропдауна 400dp
(прецедент пикера групп карточки). Цвета — явные парами из прецедента
(дом-правило), шрифт из темы. Тап по пикеру НЕ открывает чат: зона
клика пикера — вся нижняя строка карточки на полную ширину, минимум
48dp высоты (промах не улетает в чат); длинное имя — ellipsis, шеврон
▾ вне ellipsis-текста.

**quiztab** — таб дорастает до полноценного mate-контура:

- `QuizTabState` +: `dictionaryId: Long?`, `groupOptions:
  List<GroupOptionUi>` (с флагом eligible и счётчиком),
  `isAllEligible: Boolean`, `allWordCount: Int`, `selectedGroupId:
  Long?`, `isChatCardEnabled: Boolean` — ЯВНЫЙ флаг (Д2, дом-правило),
  true ⇔ есть хоть один пригодный пункт. Имя выбранной группы в state
  НЕ дублируется (ревью: производное от id + options + ресурс
  `group_all_title` для null — вычисляется при маппинге в виджет).
  **Дефолт state: `isChatCardEnabled = true`, options пусты** (пикер до
  первой эмиссии — «Все» без меню): сохраняет сегодняшнее поведение,
  не мигает disabled; окно-гонка безопасна — чат сам читает
  валидированный выбор (ревью-реализация);
- `Msg`: `GroupOptionsLoaded(quizType, dictionaryId, options,
  selectedGroupId)`, `PickGroup(quizType, groupId: Long?)`,
  `GroupOptionsFailed(quizType)`; `quizType` в Msg/Effect уже сейчас —
  вторая карточка не перепишет контракт reducer'а (ревью-архитектура);
  state пока single-card (YAGNI);
- подписка `QuizTabSub.GroupOptions` (новый `QuizTabSubHandler`):
  `flowCurrentDictId()` (CurrentDictionaryProvider, Э3) →
  `flatMapLatest` → `combine(flowQuizGroupCounts,
  flowDictionaryQuizWordCount, flowSelection(CHAT, dictId))` → воронка
  `resolveQuizGroupState` (Э2) → `GroupOptionsLoaded`; воронка на
  КАЖДОМ эмите combine — транзиент «новые счётчики + мёртвый
  selectedGroupId» резолвится в «Все» без мигания невалидного пункта
  (пин reducer-тестом). Обязательно по конвенции GroupsSubHandler:
  `.catch → Msg.GroupOptionsFailed` (иначе упавший combine молча
  замораживает пикер навсегда; реакция — state не трогаем, дефолт
  рабочий, стектрейс в лог) + `distinctUntilChanged` на источниках
  (мутация групп дёргает и counts, и membership). Живая подписка сама
  отрабатывает смену словаря, создание/удаление/усыхание групп и
  возврат на таб;
- эффект `QuizTabDatasourceEffect.PersistGroupSelection(quizType,
  dictionaryId, groupId?)` — `RecoverableEffect<Msg>`, `onFail →
  Msg.Empty` (П6: ошибка записи pref некритична — state уже обновлён
  оптимистично, подписка pref-потока выровняет расхождение; стектрейс —
  ErrorLoggingObserver). Гонка быстрых перевыборов A→B (два persist
  параллельно, порядок записи не гарантирован) — принята как компромисс
  (вероятность мизерная, запись pref быстрая, эхо подписки выровняет);
  фиксируется в спеке, state-флагами не лечим (ревью-реализация);
- `QuizTabAssembly`: + subHandler, + datasource handler, +
  `ErrorLoggingObserver(logger, LogTags.QUIZ)` (тег `###QUIZ###` в
  модуле уже есть — не плодить новый) — как в остальных четырёх
  Assembly; вызывающий один — `QuizTabViewModel` (харнес quiztab не
  собирает, PilotRegistry не пострадает); правка ViewModel (инжект
  useCase, прокид в Assembly) — в перечне;
- `QuizTabUseCase` (сейчас пустой) наполняется:
  `flowCurrentDictId`, `flowQuizGroupCounts`,
  `flowDictionaryQuizWordCount`, `flowGroupSelection`,
  `setGroupSelection`; impl в
  `app/.../di/module/quiztab/QuizTabUseCaseImpl` делегирует в QuizApi
  (счётчики, маппинг Api-entity → QuizGroup) + store +
  CurrentDictionaryProvider;
- инфраструктурный хвост (ревью-реализация, чтобы не всплывал по одному
  на компиляции): `settings.gradle.kts` — `include(":modules:domain:quiz")`
  + build.gradle.kts модуля (образец domain/group); quiztab/build.gradle.kts —
  зависимость `:modules:domain:quiz` и ОТСУТСТВУЮЩИЕ тестовые
  зависимости (junit, `:modules:core:mate` — по образцу groupstab);
  app → domain:quiz; строки ru/en в core-resources (лейбл «Группа»,
  формат пункта «%s · %d», сабтайтл disabled, contentDescription);
  правка Preview-функций QuizTabScreen/QuizItemWidget + новые preview
  пикера (enabled/disabled);
- карточка: `QuizItemWidget` получает слот под пикер и
  `enabled: Boolean`; при false карточка целиком приглушена и
  некликабельна, пикер ВИДЕН и заморожен на «Все» (П4) — юзер видит
  привычную структуру, высота карточки не прыгает через порог; сабтайтл
  карточки при disabled заменяется строкой «Нужно минимум 3 слова —
  сейчас N» (ревью-UX: дизейбл самообъясняющий; порог из
  `MIN_QUIZ_WORDS`, N = `allWordCount` из state, ресурс с
  плейсхолдерами, ru/en);
- reducer-тесты: дефолт state (enabled=true, «Все»), загрузка опций,
  выбор (persist-эффект с верным quizType/dictionaryId),
  `PickGroup(null)` («Все»), фолбэк на «Все» при мёртвом/усохшем id в
  эмите options (пин транзиента), disabled-флаг, `GroupOptionsFailed` —
  no-op, смена словаря; sub-тест: `subscriptions()` от state (образец
  SubsTest groupstab).

**Чат** — сигнатуры `QuizChatUseCase` НЕ меняются (решение
ревью-архитектуры): groupId резолвится ВНУТРИ `QuizChatUseCaseImpl.
getRandomWriteQuizList` — store инжектится в impl, чтение валидированного
выбора рядом с существующими внутренними чтениями prefs
(`isEarliestOn`/`isFrequentMistakes` — прямой прецедент), groupId
уходит в три вызова `QuizApi`. Так буквально исполняется Д6 «в код
конкретного квиза фильтр не зашивается», chat-модуль не получает
зависимости на domain/quiz, а рукописный `FakeUseCase` и mockk-моки
существующих тестов чата не ломаются. Дополнительно (ревью-UX, У4):
**сабтайтл аппбара чата** с именем группы («Квиз чат · Быт»; «Все» —
без сабтайтла или ресурс «Все») — новый метод
`QuizChatUseCase.getSelectedQuizGroupName(dictionaryId): String?`
(impl: валидированный id → живая группа → имя; null = «Все»), поле в
state чата + AppBarWidget; юзер всю сессию видит, ПО ЧЕМУ тренируется —
закрывает и молчаливый сброс Д3. Механика вопросов не меняется (бриф,
«Не входит»). Тестовый хвост чата: `QuizChatUseCaseImplTest` — стабы
quizApi дополнить `groupId = null`/значением, кейс «валидированный
выбор прокинут во все три вызова»; `QuizGameImplFetchDataTest` — кейс
сабтайтла.

## Э5. Прогон, спека, хвосты

- Последовательные unit-тесты: `:modules:domain:quiz` →
  `:modules:screen:quiztab` → `:modules:screen:quiz:chat` → app-тесты
  (store, QuizChatUseCaseImpl) → build + lint (`./scripts/cc-build.sh`);
  DAO-тесты Э1 — androidTest, гоняются на девайсе перед ручным прогоном.
- Ручной прогон на девайсе — по документу
  `docs/features/IS500_quiz_group_filter/manual_test.md` (пишется в
  этом этапе по факту реализации): словарь логов + кейсы M1–M10 из
  раздела «Ручники» выше, статусы прогона цепочкой эмодзи.
- **Спека** (задание юзера): НОВАЯ
  `docs/handbook/specs/quiz-group-filter/spec.md` (по факту реализации,
  первая квизовая спека) + строка в README specs; в
  `dictionary-groups/spec.md` §13 — пометка-ссылка «тренировка по
  группе — реализована IS500» (аддитивно, канон не меняется).
  Обязательные фиксации в спеке (свод ревью): контракт SQL-фильтра с
  liveness-JOIN и кросс-ссылкой на §4.4/§6 групп; инвариант «лексема ⇔
  write_quiz» с перечнем легальных путей создания; порог — продуктовый
  гейт UI, контракт «QuizGameImpl при null/пустой выборке» —
  системная страховка, порог его не заменяет; семантика счётчика
  уровня 1 + принятый остаточный обман; формат ключа персиста и
  выбор «валидация-при-чтении вместо активной чистки prefs» (контраст
  с прецедентом component-constructor); фолбэк ЗАКРЕПЛЯЕТСЯ — плашка
  стирает невалидный pref reducer-эффектом по `selectionInvalidated`,
  воскрешений нет (решение прогона 2026-09-26, отменило «выбор
  дремлет» из ревью); гонка быстрых перевыборов —
  принята; TOCTOU; «Все» — виртуальна, только выбор фильтра, никаких
  действий (кросс-ссылка §4.6); `QuizTypes.CHAT` — отдельная ось от
  quiz config mode `"write"`; сортировка групп — Collator вызывающей
  стороны (§5); границы: инварианты pref'а словаря остаются в
  dictionary-list (пробел «спека QuizTab» открыт, П7).
- Бриф + план — коммит по отмашке (опечатка брифа поправлена:
  таблица membership — `word_groups`, не `word_group_words`).

---

## Логи фичи (словарь маркеров — дизайн)

Теги существующие: `###QUIZ###` (quiztab), `###CHAT###` (чат). Новые
события (полный словарь — в документе ручников Э5):

- **store (app), валидация Д3 — ключевой лог фичи:**
  `quizGroupStore: read type=chat dict=<id> raw=<val|none>
  resolved=<id|all> fallback=<none|garbage|dead|below_threshold>` —
  каждый фолбэк называет причину; + `quizGroupStore: write type=chat
  dict=<id> group=<id|all>`;
- **подписка (QuizTabSubHandler), event-лог на каждый эмит combine:**
  `groupOptions: dict=<id> groups=<n> eligible=<m> allCount=<c>
  selected=<id|all>`; catch: `groupOptions: failed | <exception>`;
- **reducer quiztab** — конвенция ReducerLogging (`Reduce ---message---`,
  атомы): `applyGroupOptions | dict=.. eligible=..`,
  `resolveSelection | selected=.. fallback=..`,
  `setCardEnabled | enabled=..`, `pickGroup | group=..`, `no-op | reason`;
- **persist:** сам эффект в event-логе handler'а; сбой записи —
  существующий `onEffectRecovered` ErrorLoggingObserver (стектрейс);
- **чат:** в `fetchData` к существующим логам добавляется
  `fetchData: groupFilter=<id|all>` (сужение видно по существующим
  count-по-грейдам логам рядом); сабтайтл: `subtitle: group=<name|all>`.

Весь флоу фичи читается одним grep по двум тегам: выбор → запись →
эмит подписки → резолв → выборка чата.

## Ручники (перечень кейсов — документ в Э5)

Документ `manual_test.md` в папке фичи (формат: словарь логов + кейсы
M1..MN со статусами прогона):

- **M1.** Дефолт «Все» — квиз по всему словарю, как до фичи
  (`groupFilter=all`).
- **M2.** Выбор группы — вопросы ТОЛЬКО из её слов
  (`groupFilter=<id>`, счётчики грейдов сузились).
- **M3.** Персист: выбрал группу → убил процесс → выбор жив; смена
  словаря → у него свой выбор («Все»); возврат — выбор жив (тип×словарь).
- **M4.** Порог: группа с 2 словами — disabled-пункт «· 2»; добавил
  3-е слово → пункт ожил без перезахода (живая подписка).
- **M5.** Удаление выбранной группы на табе «Группы» → плашка молча
  «Все» (`fallback=dead`), сабтайтл следующей сессии чата — без группы.
- **M6.** Усыхание ниже порога → фолбэк «Все» (`fallback=below_threshold`);
  добор слов обратно → выбор ОСТАЁТСЯ «Все» (фолбэк закреплён стиранием
  pref — решение прогона 2026-09-26).
- **M7.** Словарь < 3 слов → карточка disabled, сабтайтл «Нужно минимум
  3 слова — сейчас N»; добор → оживает.
- **M8.** Сабтайтл аппбара чата: «Квиз чат · Быт»; при «Все» — базовый
  заголовок.
- **M9.** Счётчик уровня 1: группа из 3 слов БЕЗ лексем → «· 0»,
  disabled (слова без лексем не считаются).
- **M10.** Анти-мигания: удалить выбранную группу → вернуться на таб —
  нет кадра старого имени; смена словаря в аппбаре — нет кадра чужой
  группы. Анти-проверки: ни одного вопроса из чужой группы в M2; ни
  снекбаров, ни крешей при фолбэках.

## Согласованность со спеками (canonical — не меняем, дополняем)

- **dictionary-groups**: читаем только живые группы — все read'ы
  word_groups с liveness-JOIN, буква §4.4 (по ревью; membership
  hard-delete §6 делает JOIN формально избыточным, но канон дороже);
  «Все» остаётся виртуальной, имя — ресурс `group_all_title` (§4.6);
  «тренировка по группе» из «вне скоупа v1» (§13) закрывается этой
  фичей — аддитивная пометка. Схема БД не меняется, миграции НЕТ.
- **dictionary-list (инварианты pref'а)**: подписка таба повторяет
  паттерн fallback'а groupstab; контракт «QuizGameImpl при null-словаре
  → пустой список» не трогается; новый фильтр не добавляет исключений
  (валидация → «Все», гонки → пустая выборка → пустое состояние).
- **component-constructor §9.6**: квиз-конфиги и component picker не
  затрагиваются; фильтр группы ортогонален `componentRefs` (группа
  сужает МНОЖЕСТВО слов, picker выбирает ЧТО спрашивать); белый список
  TEXT и graceful skip остаются как есть.
- **lexeme-domain**: доменные типы не меняются; `WriteQuiz` не трогаем.

## Порог и счётчик — принятая семантика (Д2, П5 — уровень 1)

Счётчик пригодности = число СЛОВ, имеющих хотя бы одну лексему
(distinct по membership; для «Все» — такие слова всего словаря).
Инвариант связывает счётчик с квизом: каждая лексема при создании
получает строку `write_quiz`. Глубокая проверка «есть ли у лексемы
тренируемый TEXT-компонент под текущие refs» в счётчик НЕ входит:
она сделала бы счётчик функцией от выбора компонента в чате,
продублировала бы резолв refs в SQL (второй источник правды рядом с
`matchesRef`) и упёрлась в JSON-envelope значений — цена высока, а
остаточный обман (лексема без единого тренируемого значения) — экзотика,
которую чат молча переживает graceful skip'ом (пустой чат — существующее
поведение).

**Задел на будущую гибкость** (тренировка по любому компоненту / по
нескольким): набор компонентов станет параметром той же домен-выборки
(«компонент?» уже в сигнатуре Д6), параметризованный глубокий счётчик
добавится аддитивно — уровень 1 инвариантен к выбору компонентов и
переделки не потребует.

## Решения по открытым вопросам (2026-09-26)

- **П1. Счётчики в UI пикера** — показывать у каждого пункта
  («Все · 47», «Быт · 12»).
- **П2. Ключ персиста** — тип квиза `"chat"` (как в навигации
  `OpenChat(quizType = "chat")`), константа `QuizTypes.CHAT` в
  `domain/quiz`; quiz config mode `"write"` — отдельная ось, не
  смешивается.
- **П3. Виджет пикера** — внутри quiztab (будущие карточки — на этом
  же табе); вынос в `modules/widget` — когда появится потребитель вне
  таба.
- **П4. Disabled-карточка** — приглушена целиком, пикер виден и
  заморожен на «Все».
- **П5. Счётчик** — уровень 1 (слова с хотя бы одной лексемой), см.
  раздел выше.
- **П6. Ошибка записи pref'а выбора** — молча: `onFail → Msg.Empty`,
  стектрейс в лог (ErrorLoggingObserver); state уже обновлён, худший
  исход — выбор не запомнился до следующего входа.
- **П7. Спека** — узкая `specs/quiz-group-filter/spec.md` (по факту
  реализации); объявленный в README пробел «спека QuizTab» остаётся
  открытым.

## Ревью плана (2026-09-26, 5 агентов) — свод решений

Позиции: архитектура, спеки/бизнес-логика, данные/SQL, UX, реализация/
тесты. Решения уже вкатаны в этапы выше; свод — для трассируемости.

**Принято (техническое):** шов чата — groupId резолвится внутри
`QuizChatUseCaseImpl`, сигнатуры UseCase не меняются; liveness-JOIN в
EXISTS (буква канона §4.4 — конфликт агентов решён в пользу канона);
корректный SQL счётчика (двойной LEFT JOIN, группа с 0 слов → 0);
счётчики в QuizApi, возврат Api-entity; фильтр earliest/mistakes строго
в WHERE + тест-пин; единая доменная воронка валидации на оба пути;
`catch → GroupOptionsFailed` + distinctUntilChanged в подписке;
`quizType` в Msg/Effect уже сейчас; дефолт state — enabled;
`selectedGroupName` из state убран (производное); инфраструктурный
перечень (settings.gradle, test-deps quiztab, strings, previews,
LogTags.QUIZ); DAO-тесты честно androidTest; store: dictionaryId
только параметром.

**Принято (UX, решения юзера):** сабтайтл disabled-карточки «Нужно
минимум 3 слова — сейчас N»; группы ниже порога — disabled-пунктами с
счётчиком (уточнение Д2); сабтайтл аппбара чата с именем группы.
Полировка: тап-зона пикера 48dp полной ширины, maxHeight дропдауна
400dp, «Все» первым, анти-мигания — в ручной прогон.

**В скоуп затянуто из ревью:** удаление мёртвых `addLexeme(wordId)` /
`removeWriteQuiz` (Э1, решение юзера «удалить»); общий
`CurrentDictionaryProvider` с миграцией четырёх UseCaseImpl (Э3,
решение юзера «решаем сейчас»).

**Отклонено:** «пустой чат-тупик» как правка IS500 — дизейбл-гейт Д2
ровно для этого и задуман, остаточная экзотика принята П5; заголовок
«слов к тренировке» внутри дропдауна — шум, семантика счётчика
фиксируется спекой; state-флаг против гонки перевыборов — экзотика,
раздувает state.

**В Backlog:** узел quiztab в PilotRegistry сценарного харнеса
(решение юзера: харнес ещё не щупали).
