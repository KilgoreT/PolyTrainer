# IS533 | План: значения из словаря в карточке слова

Бриф — `brief.md`, спека — `docs/handbook/specs/word-card-lexemes/spec.md`
(WCL), общие правила — AWP. Ветка `IS533_word_card_lexemes` от
`IS523_words_from_text`. Коммит один, после зелёных тестов и lint;
прогон на девайсе — по отдельному слову юзера.

## Э1. Спека и план — ВЫПОЛНЕН 2026-10-13

- **Было:** решения Д1–Д4 в брифе.
- **Будет:** спека WCL-01…14 (52 строки, проверка в основном unit), этот
  план.
- **Мотивация:** спека до кода; адреса строк — имена тестов.

## Э2. Общий модуль лексем-кандидатов — ВЫПОЛНЕН 2026-10-13

- **Было:** логика и виджеты окна снимка живут в
  `modules/screen/wordstab` (`logic/photo/*`, `deps/PhotoLookup.kt`,
  `deps/PhotoAdd.kt`, `ui/widget/photo/*`); атомы — расширения
  `WordsTabState`; виджеты шлют `wordstab.Msg`; экранные модули друг от
  друга не зависят, прецедент общего Compose-модуля —
  `modules/widget/component_widgets`.
- **Будет:** модуль `modules/widget/candidates` (namespace
  `me.apomazkin.candidates`, зависимости: theme, ui, core-resources,
  domain:lexeme, mate, logger). Переезжают как есть: данные ответа и
  формы (`PhotoLookup.kt`, `PhotoAdd.kt`), состояние окна
  (`PhotoWindowState.kt`), правила (`PhotoCards.kt`,
  `PhotoMappingRules.kt`, `PhotoTexts.kt`) и их тесты. Переезжают с
  правкой: `PhotoAddMappers.kt` — сборка пачки без `PhotoWordsState`
  (`wordsBatchOf(dictionaryId, cards, mapping, form, commonGroupId)`);
  тела атомов `PhotoStateAtoms.kt` — чистые переходы
  `PhotoWordsState → PhotoWordsState` в `PhotoWindowTransitions.kt`, в
  wordstab остаются атомы-обёртки с логом и эффектами; виджеты строки
  лексемы, блока «Куда пишем», листов части речи, соответствий и правки
  текста, нижней панели и подписей — с интерфейсом действий
  `CandidateActions` вместо `Msg`; листы групп и «куда класть», шапка
  карточки с «Куда»/«Группа», блок «не добавить» и окно целиком остаются
  в wordstab. `PhotoCard` получает `formNote` леммы. Имена с «Photo»
  остаются: переименование в «Candidate…» — отдельный шаг, не здесь.
  Проверка: все 212 тестов wordstab и 204 app зелёные без изменений
  смысла; тесты правил — в новом модуле.
- **Мотивация:** карточка слова не может зависеть от wordstab; общий
  код — один; атомы по конвенции остаются член-расширениями состояния
  экрана.

## Э3. Логика диалога в карточке слова (TDD) — ВЫПОЛНЕН 2026-10-13

- **Было:** `modules/screen/wordcard`: TEA с `WordCardReducer`,
  `DatasourceEffect` (RecoverableEffect), `WordCardUseCase` без сети;
  лексемы перечитываются `Msg.WordLoaded` после `getTermById`; app —
  `WordCardUseCaseImpl` без `WordsApi`.
- **Будет:**
  - deps: `WordCardUseCase` + `lookupWord(wordId): CandidateLookup`
    (один токен — текст слова, языки словаря; исход: леммы, не найдено,
    отказ, недоступен), `loadCandidatesForm(wordId): PhotoFormOutcome`
    (форма словаря, лексемы только этого слова, запомненные
    соответствия), `addCandidates(batch): AddWordsBatchOutcome`,
    `saveCandidatesMapping(dictionaryId, mapping)`, `hasServerAccess()`;
  - app: `CandidatesGateway` (общая часть `WordsTabUseCaseImpl`: запрос и
    маппинг ответа, форма словаря, существующие леммы, запись, память
    соответствий) — используют оба use-case; `WordCardUseCaseImpl`
    получает `WordsApi`, `WordsBatchApi`, `PhotoMappingStore` через
    gateway; Dagger — без новых модулей;
  - state: `WordCardState.candidates: CandidatesDialogState?`
    (`PhotoWordsState` диалога: form, mapping, cards, summary, sheet,
    isDirty, isAdding, addFailure, requestId, phase LOADING|FORM|ERROR,
    errorRes) и `canAddFromDictionary` (флаг ключа);
  - Msg: `OpenCandidates`, `CandidatesLookedUp(outcome, requestId)`,
    `CandidatesFormLoaded(outcome, requestId)`, `CandidatesAdded(count,
    requestId)`, `CandidatesRejected(failure, requestId)`,
    `ServerAccessLoaded(hasKey)` и обёртка `Candidates(action)` для
    действий диалога (toggle, delete, edit, pos, mapping, sheets, add,
    retry, close, stay);
  - effects: `LookupCandidates(wordId, requestId)`,
    `LoadCandidatesForm(wordId, requestId)`, `AddCandidates(batch,
    requestId)` (после успеха handler перечитывает слово и шлёт
    `WordLoaded`, как восстановление лексемы), `SaveCandidatesMapping`,
    `LoadServerAccess`;
  - атомы `CandidatesAtoms : ReducerLogging` — обёртки над переходами
    Э2 на `WordCardState`; сборка разделов: карточки по леммам ответа,
    прямая лемма первой, пометка «есть» по ключам текстов лексем слова
    для всех карточек, цель записи — слово по id, без групп; без
    вопроса перед записью;
  - тесты (сначала красные): `CandidatesReducerTest` (WCL-01…06, 09…14
    по адресам), `CandidatesAtomsTest`, `WordCardUseCaseImplTest` +
    (WCL-03, 06.2, 10.4), `CandidatesGatewayTest` на вынос из
    `WordsTabUseCaseImplTest`.
- **Мотивация:** то же поведение, что в окне, в другом экране через его
  собственные эффекты; сеть в карточке — через use-case, как в вкладке.

## Э4. Диалог и кнопка (UI) — ВЫПОЛНЕН 2026-10-13, проверен юзером на девайсе

- **Переделки по проверке юзера (2026-10-13, прогон 7 коротких кейсов —
  `manual_tests.md`):** (1) большая кнопка под лексемами заменена второй
  FAB над «+» (`SmallFabWidget`, иконка `ic_download` — временная, своей
  иконки словаря нет), как кнопка-камера на вкладке «Слова»; (2) флаг
  «были изменения» ставится только при реальной правке: «сохранить» с
  прежним текстом, «сбросить» у неизменённого и та же часть речи вопрос
  при закрытии не дают — в диалоге и в окне снимка (AWP-19.9, WCL-12.5);
  (3) Д5 — у раздела леммы-формы выбор «переименовать слово в «correr»»,
  по умолчанию снят; при записи слово получает текст леммы той же
  транзакцией (`BatchWord.renameTo`, WCL-15; unit-тесты редьюсера,
  инструментальный тест пачки `Is533WordsBatchRenameTest`, ручник WCL-15);
  (4) Д7 — рядом с «переименовать» по строке «перенести в «correr» и
  удалить «corrían»» на каждое слово словаря с текстом леммы (несколько —
  со значениями в скобках): форма диалога грузит слова словаря по текстам
  лемм-форм (`CandidatesFormLoader.formForWord(lemmaTexts)`), выбор —
  `LemmaWordChoice` (Rename / Transfer) в общем модуле, пачка переноса
  `wordsBatchForTransfer` → `BatchWord.absorbWordId` → в базе `moveLexemes`
  + удаление источника той же транзакцией, после успеха карточка уходит в
  список (`Msg.CandidatesTransferred` → как `NavigateBack`); пометки «есть
  в «correr»» и строка «уже есть N таких же значений» (`withLemmaExistsMarks`,
  `sameValuesOf`); тесты всех слоёв, ручник переноса.

- **Было:** карточка — `WordCardScreen` (Column, `AddLexemeWidget`
  FAB, `TopBarWidget`), снекбары через `UiEffect`.
- **Будет:** кнопка «Добавить значения из словаря» под списком лексем,
  видна при `canAddFromDictionary`; `CandidatesDialog` в модуле Э2:
  `Dialog` с прокруткой и ограниченной высотой — заголовок с текстом
  слова и крестик, строка «Куда пишем» + «изменить» (раскрытие — четыре
  строки), разделы лемм: у леммы-формы заголовок «форма слова «…»» и
  пометка формы серым, строки лексем как в окне, «ещё N», атрибуция,
  панель с кнопкой «Добавить N значений» и причиной; признак загрузки,
  текст ошибки с «Закрыть»; «Закрыть без добавления?» и «Не добавилось —
  ничего не записано» + «Повторить» — как в окне; снекбар «Добавлено N
  значений» (`UiEffect.ShowSnackbar`). Строки: `word_card_add_from_
  dictionary`, `candidates_form_of`, `candidates_not_found`,
  `candidates_added`, остальное — `photo_add_*`.
- **Мотивация:** облегчённый интерфейс без групп и «куда» (Д2–Д3), один
  набор виджетов для обоих входов.

## Э5. Проверка и коммит — ВЫПОЛНЕН 2026-10-13 (ждёт «да» на коммит)

- **Итог:** тесты зелёные по модулям: `candidates` (правила и тексты,
  переехали из wordstab), `wordstab` 212 без изменения смысла, `wordcard`
  323 (из них ~35 новых по адресам WCL в `CandidatesReducerTest`, 5 в
  `DatasourceEffectHandlerTest`), `app` (+7 в `WordCardUseCaseImplTest`);
  `:app:lintDebug` зелёный; покрытие WCL 52/0, AWP 157/0 (адреса
  переехавших тестов скрипт видит в `modules/widget`); статусы WCL →
  «реализовано». На девайсе не смотрели — по слову юзера. Отклонения
  от плана: вместо интерфейса `CandidateActions` — sealed
  `CandidateAction` и `onAction: (CandidateAction) -> Unit` у виджетов
  (один `when` на экран вместо 23 методов); виджеты переехали в
  `candidates/ui` целиком (с частями, нужными только окну), а не
  раздельно; вид для виджетов — `CandidatesView`; тела атомов wordstab
  не переписаны целиком — общими стали переходы над списком карточек
  (`CardTransitions.kt`), атомы остались обёртками.
- **Было:** нет.
- **Будет:** тесты модулей по одному (`candidates`, `wordstab`,
  `wordcard`, `app`), `:app:lintDebug`, покрытие WCL скриптом
  (`SPEC_FILE`, `SPEC_PREFIX=WCL`) — строки «ручной» закрыты адресами в
  `manual_tests.md` (документ без прогона), статусы WCL →
  «реализовано», превью коммита в `IS533_word_card_lexemes`.
- **Мотивация:** коммит один, после зелёных тестов и lint.

## Риски и неочевидное

- Перенос файлов между модулями трогает ~40 файлов wordstab/app; защита
  — существующие тесты без изменения смысла.
- `WordLoaded` после записи перестраивает список лексем карточки и
  теряет несохранённые черновики полей; диалог модален, черновиков в
  момент записи нет — как и при восстановлении лексемы.
- Пометка «есть» в диалоге — по лексемам слова для всех разделов, в том
  числе лемм-форм; в окне — по одноимённым леммам карточки. Два правила
  рядом: в спеках разнесены (AWP-07, WCL-06).
