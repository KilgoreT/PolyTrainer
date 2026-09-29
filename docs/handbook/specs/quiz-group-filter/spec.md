# Спецификация: квиз по группе (фильтр выборки тренировки)

Канон фичи IS500 **по факту реализации** (2026-09-26, прогон ручников
на девайсе). Покрывает пикер группы в карточке chat-квиза таба
«Квизы», общий слой выборки/порога/персиста и групповой фильтр
квиз-выборок. Первая квизовая спека; пробел «спека QuizTab/QuizChat
целиком» из README остаётся открытым — инварианты pref'а текущего
словаря и контракт `QuizGameImpl` при null-словаре живут в
[dictionary-list](../dictionary-list/spec.md) и этой спекой НЕ
поглощаются.

---

## 1. Концепция

Пользователь ограничивает тренировку одной группой слов
([группы](../dictionary-groups/spec.md)): карточка квиза несёт пикер
(«Все» + группы словаря), выбранная группа сужает выборку сессии до
её слов. Таб «Квизы» спроектирован под НАБОР карточек-квизов (chat
сейчас, следующие типы подключаются к тем же трём кускам общего слоя);
в код конкретного квиза фильтр не зашивается.

**Общий слой — три куска:**

1. **домен** (`modules/domain/quiz`, pure-JVM zero-deps) — порог,
   типы пунктов, единая воронка валидации;
2. **персист** — pref `quiz_group_<тип>_dict_<id>` +
   `QuizGroupSelectionStore` (app);
3. **UI** — виджет пикера (`quiztab/widget/QuizGroupPickerWidget`,
   живёт в quiztab: будущие карточки — на этом же табе).

## 2. Термины

- **Тип квиза** — ось ключа персиста, константы `QuizTypes`
  (`CHAT = "chat"`). НЕ путать с quiz config mode (`"write"`,
  [component-constructor](../component-constructor/spec.md) §9.6):
  mode описывает, ЧТО спрашивать, тип квиза — КАКАЯ карточка таба.
- **«Все»** — виртуальный пункт (null groupId, не строка БД — инвариант
  групп §4.6): весь словарь, имя — ресурс `group_all_title`; никаких
  действий, кроме выбора фильтра.
- **Счётчик уровня 1** — число слов, имеющих хотя бы одну лексему.
- **Пригодный пункт** — счётчик ≥ порога `MIN_QUIZ_WORDS`.

## 3. Порог годности

- `MIN_QUIZ_WORDS = 3` (`domain/quiz`) — единственная точка правды,
  ОДИН на все типы квизов; слой принимает порог параметром с этим
  дефолтом — задел per-тип override без перестройки слоя.
- Порог действует и на «Все» (весь словарь < 3 тренируемых слов).
- Группы ниже порога видны в пикере **disabled-пунктами** с счётчиком
  (не скрываются — порог самообъясняющий).
- Нет ни одного пригодного пункта (включая «Все») → карточка квиза
  disabled и некликабельна, сабтайтл называет причину («Нужно минимум
  3 слова — сейчас N»); состояние — явный флаг state
  (`isChatCardEnabled`), не вычисление в composable; reducer UI не
  доверяет — `OpenChat` при disabled = no-op.
- **Порог — продуктовый гейт UI.** Контракт «`QuizGameImpl` при
  null-словаре/пустой выборке → пустая сессия без креша»
  (dictionary-list, «Системное ограничение: QuizChat») — системная
  страховка; порог его не заменяет и не отменяет.

## 4. Счётчик уровня 1

- Слово считается при наличии хотя бы одной лексемы; слова без лексем
  не считаются (счётчик группы может быть меньше счётчика той же
  группы на вкладке «Группы» — ожидаемо).
- Основание — инвариант data-слоя **«лексема ⇔ write_quiz»**: каждая
  лексема создаётся атомарно с квиз-строкой. Легальные пути создания —
  ТОЛЬКО compound-методы `WordDao.addLexemeWithQuiz` /
  `addLexemeWithComponents` (через них — все `LexemeApi.addLexemeWith*`);
  `LexemeApi.addLexeme(wordId)` и `WordDao.removeWriteQuiz` удалены
  IS500 как ломавшие инвариант (production-вызовов не имели).
- Глубокая проверка тренируемости (есть ли TEXT-значение под текущие
  component refs) в счётчик сознательно НЕ входит: она сделала бы
  счётчик функцией выбора компонента чата и продублировала резолв
  refs в SQL. Остаточный кейс «группа прошла порог, а тренируемых
  значений нет» → пустая сессия (существующий graceful skip). При
  будущей тренировке по любому/нескольким компонентам набор
  компонентов становится параметром той же выборки — аддитивно.

## 5. Домен (`modules/domain/quiz`)

- `QuizGroup(id, name, wordCount, isEligible)`,
  `QuizGroupOptions(isAllEligible, allWordCount, groups)`
  (+ `hasEligibleOption`), `QuizGroupState(options, selectedGroupId)`,
  вход — `QuizGroupCount(id, name, wordCount)`.
- **Единая воронка** `resolveQuizGroupState(groupCounts,
  dictionaryWordCount, persistedGroupId, threshold = MIN_QUIZ_WORDS)` —
  И selection-store (снапшот для квиза), И подписка плашки строят
  состояние ТОЛЬКО ею: пометка eligible + резолв персиста.
- Резолв: `persistedGroupId` вне пригодного множества (удалённая,
  усохшая, чужая группа) → `null` («Все»), молча — без снекбаров (Д3).
- Сортировка групп — locale-aware Collator вызывающей стороны
  (подписка плашки; групповая спека §5), домен порядок не навязывает.

## 6. Персист

- Ключ: `quizGroupPrefKey(quizType, dictionaryId)` =
  `quiz_group_<тип>_dict_<id>` (`modules/datasource/prefs`, рядом с
  прецедентом quiz picker'а). Значение — `groupId` строкой; отсутствие
  ключа = «Все»; `setSelection(null)` стирает ключ.
- Изоляция по обеим осям: каждый словарь и каждый тип квиза помнят
  свой выбор; смена словаря выбор НЕ трогает.
- **Валидация при чтении** (`QuizGroupSelectionStore`): pref → снапшот
  счётчиков → воронка §5; мусор в pref'е (`fallback=garbage`), мёртвая
  (`dead`) и усохшая (`below_threshold`) группа → «Все». Store читает
  БЕЗ побочек; `dictionaryId` — только параметром (self-read pref'а
  словаря запрещён: пара «словарь-группа» фиксируется вызывающим на
  старте сессии).
- **Фолбэк ЗАКРЕПЛЯЕТСЯ** (решение прогона 2026-09-26): подписка
  плашки, увидев невалидный персист (`selectionInvalidated`), стирает
  pref reducer-эффектом — «Все» остаётся выбором и после исцеления
  группы, воскрешений нет. Store-фолбэк остаётся страховкой гонки.
- Стратегия — «валидация-при-чтении», НЕ активная чистка prefs при
  удалении группы (осознанный контраст с prefs-cleanup прецедентом
  component-constructor §9.3: проще и покрывает все причины
  невалидности разом). Утечка ключей при удалении словаря — принята
  (паритет с quiz picker).
- Гонка быстрых перевыборов (два persist параллельно) — принята:
  запись pref быстрая, эхо pref-подписки выравнивает state.

## 7. Групповой фильтр выборки

- `CoreDbApi.QuizApi`: `groupId: Long? = null` в `getWriteQuizIds` /
  `getEarliestWriteQuizList` / `getFrequentMistakesWriteQuizList`;
  null — весь словарь (поведение до фичи).
- SQL — в WHERE самих запросов, ДО `ORDER BY`/`LIMIT` (топы earliest/
  mistakes считаются ИЗ группы, Kotlin-постфильтр запрещён):

  ```sql
  AND (:groupId IS NULL OR EXISTS (
      SELECT 1 FROM lexemes l
      JOIN word_groups wg ON wg.word_id = l.word_id
      JOIN dictionary_groups dg ON dg.id = wg.group_id
          AND dg.removed_at IS NULL
      WHERE l.id = write_quiz.lexeme_id AND wg.group_id = :groupId))
  ```

  Liveness-JOIN — буква инварианта групп §4.4 («все read'ы word_groups
  JOIN'ят живые группы»); семантически он избыточен (membership
  мёртвой группы hard-delete'ится в её транзакции, §6 групп) — JOIN
  страхует будущий restore групп. Новых индексов не нужно (EXISTS
  кроется PK lexemes(id) и PK word_groups(word_id, group_id)).
- Резолв группового фильтра — ВНУТРИ `QuizChatUseCaseImpl.
  getRandomWriteQuizList` (store инжектится в impl; сигнатуры
  `QuizChatUseCase` не расширялись — Д6, прецедент внутренних чтений
  prefs `isEarliestOn`). Квиз-механика (`QuizGameImpl`) о фильтре не
  знает.
- TOCTOU принят: группа умерла между валидацией и запросом → пустая
  выборка → существующее пустое состояние чата.
- Component picker (IS481) ортогонален: группа сужает МНОЖЕСТВО слов,
  picker выбирает ЧТО спрашивать; quiz configs не затронуты.

### 7.1. Инвариант порции (IS508, 2026-09-28)

Порция сессии — `getRandomWriteQuizList` — собирается в контейнер,
ключом которого служит лексема; правила действуют на всех шагах
сборки (грейд-корзины, добор до `limit`, добавки «Самые давние» /
«Частые ошибки»):

- **Единица уникальности — лексема** (`write_quiz.lexeme_id`, с v14 —
  unique-индекс; одна лексема — одна квиз-строка). Один вопрос в
  порции не повторяется. `WriteQuiz.type` (`GRADES`/`EARLIEST`/
  `ERRORS`) на уникальность не влияет и читается только debug-выводом;
  запись-коллизия остаётся `GRADES`.
- **Разные лексемы одного слова допустимы** (разные вопросы, один
  ответ — by design), но **предпочитаются разные слова**: на каждом
  шаге сначала берутся кандидаты, чьё слово ещё не в порции, вторая
  лексема того же слова — только когда других кандидатов нет.
- **Счётчики не меняются:** корзина отдаёт свой `expectedCount`, добор
  — до `limit`, добавка — до 2; правила определяют лишь, какие записи
  брать.
- **Добавки фильтруют уже взятые лексемы ДО отбора двух**, «ошибки» —
  и от уже добавленных «давних». Окно кандидатов добавки — `limit`
  строк по критерию; если все они уже в порции, добавка даёт меньше
  двух или ноль — норма (в маленьком словаре порция и так покрывает
  почти всё). «Частые ошибки» — только строки с `error_count > 0`.
- Уточнение к запрету «Kotlin-постфильтр» выше: запрет касается
  ГРУППОВОГО предиката (топ считается из группы). Дедуп и предпочтение
  слов — инвариант сборки, выполняется после SQL-топа и его не сужает
  сверх описанного окна.

Инвариант временно живёт здесь как у ближайшего хоста, описывающего
этот метод; при появлении спеки QuizChat переезжает в неё с
кросс-ссылкой обратно. История решений — `docs/features/IS508_quiz_no_duplicates/`.

## 8. Счётчики данных (живые Flow, `QuizApi`)

- `flowQuizGroupCounts(dictionaryId)` — живые группы словаря со
  счётчиком уровня 1: двойной LEFT JOIN (`dictionary_groups ←
  word_groups ← lexemes`) + `COUNT(DISTINCT l.word_id)`; группа без
  пригодных слов возвращается со счётчиком 0 — порог применяет ТОЛЬКО
  домен. Наблюдает группы, membership, лексемы.
- `flowDictionaryQuizWordCount(dictionaryId)` — слова словаря с хотя
  бы одной лексемой (для «Все»).
- Возврат — Api-entity (`QuizGroupCountApiEntity`), маппинг в доменный
  `QuizGroupCount` — в app-слое (core-db-api от domain/quiz не зависит).

## 9. Плашка (quiztab)

- **State**: `dictionaryId`, `groupOptions: List<QuizGroup>`,
  `isAllEligible`, `allWordCount`, `selectedGroupId` (null = «Все»),
  `isChatCardEnabled`. Имя выбранной группы НЕ дублируется —
  производное (id + options + ресурс). **Дефолт — рабочее состояние**:
  карточка enabled, пикер «Все» без пунктов (до первой эмиссии не
  мигает disabled; тап в окно гонки безопасен — квиз сам читает
  валидированный выбор).
- **Подписка** `QuizTabSub.GroupOptions` (безусловная, одна):
  текущий словарь (`CurrentDictionaryProvider`) → `flatMapLatest` →
  `combine(счётчики групп, счётчик «Все», pref выбора)` → воронка §5
  на КАЖДОМ эмите (транзиент «новые счётчики + мёртвый выбор»
  резолвится без кадра невалидного пункта) → `GroupOptionsLoaded`.
  `catch → GroupOptionsFailed` (state не трогается, дефолт рабочий);
  `distinctUntilChanged` на источниках. null-словарь → пустые опции,
  карточка disabled.
- **Msg/Effect несут `quizType`** — контракт готов ко второй карточке
  (state пока single-card). `PickGroup`: guard'ы — чужой тип, нет
  словаря, same selection → no-op; иначе оптимистичный state + эффект
  `PersistGroupSelection` (`RecoverableEffect`, `onFail → Msg.Empty` —
  провал записи некритичен, стектрейс в ErrorLoggingObserver).
- **Пикер** (UI): контрол «<имя> ▾» прижат к правому краю; зона клика —
  вся строка карточки (min 48dp), тап по пикеру НЕ открывает чат;
  меню — от правого края, ширина по самому длинному пункту, но ≤90%
  карточки, maxHeight 400dp, «Все» первым, счётчики у всех пунктов,
  выбранный — жирным, непригодные — серые некликабельные; фон меню —
  явный `surface` (не M3-дефолт).

## 10. Чат

- Сабтайтл аппбара — охват тренировки, ВСЕГДА: имя группы либо «Все»
  (решение прогона 2026-09-26). Загружается init-эффектом
  `LoadQuizGroupName` на входе в экран (до «Начать») и обновляется на
  старте сессии; `QuizChatUseCase.getSelectedQuizGroupName(dictionaryId)`
  — имя валидированного выбора, null = «Все».
- Механика вопросов/оценок не изменена.

## 11. Логи

Теги: `###QUIZ###` (плашка + store), `###CHAT###` (чат). Ключевой
маркер — `quizGroupStore: read … resolved=<id|all>
fallback=<none|garbage|dead|below_threshold>` (причина фолбэка всегда
названа); подписка — `groupOptions: dict=… groups=… eligible=…
allCount=… selected=…`; выборка — `getRandomWriteQuizList:
groupFilter=<id|all>`; сабтайтл — `subtitle: group=<name|all>`.
Словарь маркеров и ручники —
`docs/features/IS500_quiz_group_filter/manual_test.md`.

IS508 — состав порции: `getRandomWriteQuizList: portion grades=<n>
earliest=+<a>/<cand> errors=+<b>/<cand> total=<t> words=<w>` —
`+a/cand` — сколько добавка дала из скольких кандидатов (перекрытие с
корзиной видно как `+0`; выключенная опция — `off`), `words == total`,
пока пул слов не исчерпан.
Ручники — `docs/features/IS508_quiz_no_duplicates/manual_test.md`.

## 12. Вне скоупа v1

- Мультивыбор групп; фильтр по набору компонентов (задел — параметры
  воронки/выборки); пункт «Без группы»; per-тип порог (задел —
  параметр `threshold`); вторая карточка квиза (контракт готов).

## 13. Ссылки

- Код: `modules/domain/quiz`, `modules/datasource/prefs`
  (`QuizGroupPrefKey`), `app/.../di/module/quizgroup/QuizGroupSelectionStore`,
  `app/.../di/module/dictionary/CurrentDictionaryProvider`,
  `modules/screen/quiztab` (Subs/SubHandler/Reducer/StateAtoms/widget),
  `core/core-db-impl` (`WordDao` квиз-секция),
  `app/.../di/module/quizchat/QuizChatUseCaseImpl`.
- Смежные спеки: [dictionary-groups](../dictionary-groups/spec.md)
  (§4.4 liveness, §4.6 «Все», §6 каскады),
  [dictionary-list](../dictionary-list/spec.md) (инварианты pref'а
  словаря, контракт QuizChat),
  [component-constructor](../component-constructor/spec.md) (§9.6
  квизы, quiz picker).
- История решений: `docs/features/IS500_quiz_group_filter/` (brief,
  plan со сводом ревью пяти агентов, manual_test).
