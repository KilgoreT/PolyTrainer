# IS536 | HLD AWP — добавление слов со снимка, черновик

План экрана по спеке AWP: индикатор с подписью этапа, окно с
переводом и списками слов, карточка под словом, добавление по одному
слову. Этап 1 (IS537) — до окна со списками включительно, пометка
«в словаре» по словарю телефона, сделан. Этап 2 — карточка под
словом: значения по тапу, правка, слово «в словаре» с «Проверить по
тексту», «Добавить» с уходом слова из списка, вопрос при закрытии;
раздел «Этап 2: карточка под словом» ниже. Путь до ответа сервера —
`hld-tfp.md`, сервер — `hld-lft.md` и `hld-lfl.md`, диалог карточки
слова — `hld-wcl.md`.

## 1. Рамка

- **Цель** — после снимка пользователь через секунды видит перевод
  текста и слова текста в словарной форме тремя списками, плюс «не
  разобрано»; слова словаря помечены (Д68–Д75, Д84). По тапу на слово
  — его значения по тексту, разложенные по компонентам словаря, и
  добавление в словарь по одному слову (Д70, Д94).
- **Вход и выход** — вход: ответ `lemmas/from-tokens` — текст,
  перевод, четыре списка — и форма словаря снимка из Room; на этапе 2
  ещё ответ `lexemes/from-lemma` по слову. Выход: окно на месте
  индикатора; этап 2 — запись слова в словарь одной транзакцией.
- **Что меняется на устройстве, этап 1** — стейт экрана: вместо слов
  с лексемами — ответ со списками; фаза «анализирую текст»; окно —
  перевод свёрнутым блоком и списки вместо карточек; пометка «в
  словаре» по леммам словаря; закрытие без вопроса. У вкладки «Слова»
  снимается снекбар со счётом после окна.
- **Что меняется на устройстве, этап 2** — у слова списка карточка:
  загрузка, лексемы-кандидаты по сопоставлению с пометкой «в тексте»,
  «Замечания» и «Сопоставление», «куда класть» с датой (Д120), своя
  группа, правки как в первой версии; у слова «в словаре» —
  сохранённые лексемы и «Проверить по тексту», совпавшие по ядру
  «уже в словаре» (Д119); «Добавить» пишет слово и убирает его из
  списка (Д118), «Удалить» убирает без записи; при закрытии вопрос с
  числом недобавленных. Запрос из карточки слова уходит леммой на тот
  же `lexemes/from-lemma`.
- **Что не меняется** — форма словаря снимка (компоненты, группы,
  леммы по текстам слов), общая группа наверху окна и её лист,
  модальность окна (AWP-14), перезапуск (AWP-27, AWP-56), фаза
  «Собираю окно…», переходы над карточками и пакет записи из
  `widget/candidates`.
- **Что отложено** — замена сохранённой лексемы присланной (AWP-58,
  AWP-59): у несовпавшей лексемы-кандидата только «добавить» (Д119).
- **Документы** — [бриф](brief.md), [решения](decisions.md), спека
  AWP, спеки TFP, LFT и LFL, `contract.md` у LFT и LFL, контрольный
  набор `control_set.md` в бэкенде.

## 2. Приложение: схема и код

### Модули и экран

- **`screen/scancandidates` — меняется.** Этап 1: стейт — ответ LFT,
  текст, перевод, четыре списка вместо слов с лексемами; фаза
  `ANALYZING` вместо `LOOKING`; редьюсер — ветки ответа, формы,
  перевода и закрытия. Этап 2: у слова списка карточка `WordCardUi`
  с фазой — сохранённые, загрузка, лексемы-кандидаты, «не найдено»,
  ошибка; ветки тапа, ответа по слову, правок, записи и закрытия с
  вопросом; UI — карточка под строкой слова из общих виджетов
  лексем, кнопки слова, вопрос при закрытии.
- **`screen/wordstab` — меняется, этап 1.** Снята ветка
  `ScanCandidatesAdded` и снекбар «Добавлено N значений в M слов»:
  после окна вкладка показывает леммы словаря сама (AWP-52).
- **`widget/candidates` — меняется, этап 2.** Переходы над
  карточками, пометки, пакет записи — как были, их зовут атомы
  экрана; у лексемы-кандидата пометка «в тексте», у леммы словаря
  дата добавления и сохранённые лексемы; карточка одной леммы
  собирается из слова ответа, значения в ней — ядра, параметры,
  примеры; строки листа «куда класть» — чистой функцией; виджет
  лексем рисует и сохранённые лексемы без правок, текст пометки
  «уже в словаре» — параметром, у диалога карточки слова остаётся
  «есть». Виджет карточки, разделы «Замечания» и «Сопоставление»,
  подпись источника — публичные; крестик карточки и раскрытие
  «Сопоставления» — колбэками экрана. Панель окна первой версии, её
  вопрос перед записью и блок «не добавить» не используются.
- **`domain/lexeme` — меняется, этап 2.** Исход «лексемы леммы»
  вместо «слова по токенам», контекст леммы, у леммы словаря дата и
  сохранённые лексемы, у лексемы-кандидата «в тексте».
- **`app` — меняется, этап 2.** Юзкейс экрана: запрос значений по
  лемме с контекстом словаря снимка и запись одного слова; загрузчик
  формы читает дату и лексемы лемм словаря; юзкейс карточки слова
  зовёт ту же операцию без контекста.
- **`core-network` — меняется, этап 2.** Операция `lexemesFromLemma`
  вместо `lookup`: лемма с контекстом в запросе, одно слово в
  ответе, у лексемы `inContext`; старые зеркала запроса по токенам
  сняты.
- **`core-resources` — меняется.** Этап 1: строка отказа нейронки и
  заголовки списков. Этап 2: «Проверить по тексту», «уже в словаре»
  (свой ключ, «есть» у диалога остаётся), «в тексте», «В словаре»,
  «Не найдено», «Значений нет», «Не добавлено слов: N», дата и «Все» в
  строке «куда класть»; «Повторить» — прежний ключ.

Экран `ScanCandidatesScreen`, что видно. Индикатор с подписью фазы и
«Отменить»: «Распознаю текст…», «Анализирую текст…», «Собираю окно…».
Окно на весь экран: наверху общая группа, под ней перевод свёрнутым
блоком — тап раскрывает; ниже списки по порядку — смысловые, имена
собственные, служебные, «не разобрано», пустой список не рисуется;
слова в порядке текста, у слова словаря пометка «в словаре». Этап 2:
тап по слову раскрывает карточку под ним — у нового слова признак
загрузки, затем лексемы-кандидаты с пометкой «в тексте» у первой,
«Замечания», «Сопоставление», «куда класть», своя группа, подпись
источника, кнопки «Добавить» и «Удалить»; у слова «в словаре» —
сохранённые лексемы, «Проверить по тексту» и «Удалить», после
проверки — присланные с пометкой «уже в словаре» у совпавших и
«Добавить». Под словом с ошибкой — причина, «Удалить» и, когда можно,
«Повторить». «Добавить» записывает слово, строка уходит из списка;
«Удалить» убирает строку без записи. Крестик или «назад» закрывают
окно сразу, а если есть слова с пришедшей или идущей карточкой без
добавления — вопрос «Закрыть без добавления?» с их числом,
«Остаться» и «Закрыть»; пока идёт запись, крестик неактивен, а
«назад» не действует. Ошибки до окна на месте индикатора с
«Закрыть»: снимок не прочитался, слов не найдено, отказ по ключу,
лимиту, размеру или языку, окно не собралось; сеть, сервер и
нейронка — ещё «Повторить».

Редьюсер `ScanCandidatesReducer`, действия коротко.

- **От эффектов, этап 1.** `TextRecognized` — токены; `TextEmpty`;
  `RecognitionFailed`; `LemmasFound` — текст, перевод, списки;
  `LemmasEmpty`; `LemmasDenied`; `LemmasUnavailable`; `FormLoaded`;
  `FormFailed`.
- **От экрана, этап 1.** `RetryLemmas`; `ToggleTranslation`; общая
  группа: `OpenSheet`, `CloseSheet`, `SetCommonGroup`;
  `RequestClose`.
- **От эффектов, этап 2.** Ответ по слову: `LexemesFound`,
  `LexemesNotFound`, `LexemesDenied`, `LexemesUnavailable`. Запись:
  `WordAdded`, `AddGroupGone`, `AddWordGone`, `AddComponentGone`,
  `AddFailed`.
- **От экрана, этап 2.** Слово: `LoadLexemes`, `OpenSavedCard`,
  `ToggleCard`, `DeleteWord`, `AddWord`, `RetryAdd`. Карточка:
  `DeleteLexeme`, `DeleteValue`, `SubmitText`, `ResetText`,
  `SetOption`, `UpdateDraft`, `SetCardTarget`, `SetCardGroup`,
  `ToggleComponentMatches`. Закрытие: `ConfirmClose`, `Stay`.
- **Снято насовсем.** Ответ словами: `WordsFound`, `WordsEmpty`,
  `LookupDenied`, `LookupUnavailable`, `RetryLookup`; общая кнопка и
  вопрос перед записью: `RequestAdd`, `ConfirmAdd`, `CancelAdd`,
  `AddFinished`; «не добавить» и удаление карточки: `ToggleNotAdded`,
  `DeleteCard`.

### Условные знаки

![Условные знаки](/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS536_llm_values/hld-legend.a02d0bb8.png)

### Слои по C4

Кто кого зовёт, сверху вниз: экран, логика, порт, его реализация и
навигация, шлюзы, бэкенд снаружи. В коробке имя, тип в скобках, одна
фраза ответственности. Внизу справа сноска: что видит пользователь
по спеке AWP. Путь фичи по окнам — `hld-tfp.md`. Источник —
`hld-awp-layers-c4.dot`.

![Слои по C4](/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS536_llm_values/hld-awp-layers-c4.2ff7f17e.png)

Что меняет этап по слоям:

- **Экран** — окно показывает перевод и списки, не карточки;
  пометка «в словаре» на слове (AWP-39, AWP-40, AWP-41).
- **Логика** — ответ ложится в стейт четырьмя списками; после формы
  один атом ставит пометки; цепочки карточек сняты. Закрытие — без
  вопроса (AWP-60).
- **Порт и реализация** — `lemmasFromTokens` вместо `lookupWords`;
  форма — как была, её леммы по текстам слов списков.
- **Шлюз** — операция `lemmasFromTokens`, `hld-tfp.md`.

### Экран снимка

Экран `screen/scancandidates` от ответа сервера до окна. Ветки
распознавания и запроса — `hld-tfp.md`.

#### Стейт

Дельта к коду первой версии: новые поля, снятые; без изменений не
показаны.

```diff
 data class ScanCandidatesState(
     val uri: String,
     val phase: ScanPhase = ScanPhase.RECOGNIZING,
     val tokens: List<LookupToken> = emptyList(),
     val dictionaryId: Long? = null,
-    // снято: слов с лексемами в ответе нет
-    val words: List<LookupWordUi> = emptyList(),
-    // снято: рекомендации — у ответа по слову, этап 2
-    val recommendations:
-        List<ComponentRecommendationUi> = emptyList(),
+    // новое: ответ LFT — текст, перевод, списки
+    val text: String = "",
+    val translation: String = "",
+    val isTranslationExpanded: Boolean = false,
+    val lists: ScanWordListsUi = ScanWordListsUi(),
     @StringRes val errorRes: Int? = null,
     val hasRetryButton: Boolean = false,
     val form: DictionaryFormUi? = null,
     val commonGroupId: Long? = null,
-    // снято: карточки окна — уходят под слово, этап 2
-    val cards: List<LemmaCardUiItem> = emptyList(),
-    val notAdded: List<NotAddedWordUiItem> = emptyList(),
-    val remarks: List<RemarkUiItem> = emptyList(),
-    val componentMatchLines:
-        List<ComponentMatchLineUiItem> = emptyList(),
-    val summary: AddSummaryUi = AddSummaryUi(),
-    val isDirty: Boolean = false,
-    val isAdding: Boolean = false,
-    val addFailure: AddFailureUi? = null,
-    val isConfirmingAdd: Boolean = false,
-    val isConfirmingClose: Boolean = false,
-    val isComponentMatchesExpanded: Boolean = false,
-    val isNotAddedExpanded: Boolean = false,
     val sheet: CandidatesSheetUi? = null,
 )
```

Новые типы, пакет `scancandidates/mate`:

```kotlin
// ScanPhase — что на экране. Меняется: ANALYZING вместо
// LOOKING, подпись «Анализирую текст…» (AWP-38.2).
enum class ScanPhase {
    RECOGNIZING,
    ANALYZING,
    FORM_LOADING,
    WINDOW,
    NO_WORDS,
    ERROR,
}

// ScanWordListsUi — четыре списка окна в порядке показа;
// пустой список на экране не рисуется (AWP-40.7). Новый.
data class ScanWordListsUi(
    // lexical — смысловые, в порядке текста.
    val lexical: List<ScanWordUiItem> = emptyList(),
    // proper — имена собственные.
    val proper: List<ScanWordUiItem> = emptyList(),
    // function — служебные.
    val function: List<ScanWordUiItem> = emptyList(),
    // unparsed — «не разобрано», как прислал сервер.
    val unparsed: List<ScanWordUiItem> = emptyList(),
)

// ScanWordUiItem — слово списка. Новый. На этапе 2 сюда
// ляжет карточка под словом: загрузка, лексемы, ошибка,
// «добавлено».
data class ScanWordUiItem(
    // text — словарная форма или токен из «не разобрано».
    val text: String,
    // isInDictionary — в словаре снимка есть лемма с тем
    // же текстом (AWP-39).
    val isInDictionary: Boolean = false,
)
```

#### Редьюсер

Ветки от ответа сервера до окна; `TextRecognized`, `TextEmpty`,
`RecognitionFailed`, `RetryLemmas` — `hld-tfp.md`.

```kotlin
// LemmasFound — хендлер ExtractLemmas: сервер прислал
// леммы текста:
//   dictionaryId — словарь снимка
//   text — исправленный текст снимка
//   translation — его перевод
//   lists — четыре списка словами
// Меняется: было WordsFound со словами и лексемами
is Msg.LemmasFound -> state.startFormLoading(message)
//   startFormLoading: ответ в стейт, фаза «собираю окно»,
//     эффект LoadForm с текстами всех слов списков

// LemmasEmpty — хендлер ExtractLemmas: в ответе ни
// словарной формы, ни «не разобрано». Без данных.
// Меняется: было WordsEmpty
Msg.LemmasEmpty -> state.showNoWords()
//   showNoWords: фаза «слов не найдено», только «Закрыть»

// LemmasDenied — хендлер ExtractLemmas: отказ по ключу,
// лимиту, размеру или языку:
//   errorRes — текст причины
// Меняется: было LookupDenied
is Msg.LemmasDenied -> state.showError(message.errorRes)
//   showError: фаза «ошибка», текст, без «Повторить»

// LemmasUnavailable — хендлер ExtractLemmas: сети нет,
// сервер не ответил или нейронка недоступна:
//   errorRes — «Сервер не ответил» или «Анализ текста
//     сейчас недоступен, попробуй позже»
// Меняется: было LookupUnavailable
is Msg.LemmasUnavailable ->
    state.showRetryableError(message.errorRes)
//   showRetryableError: фаза «ошибка», текст, «Повторить»

// FormLoaded — хендлер LoadForm: форма словаря снимка:
//   form — компоненты, опции, группы, леммы словаря с
//     текстами слов списков
// Меняется: цепочка короче — карточек нет
is Msg.FormLoaded -> state
    .begin<ScanCandidatesState, Effect>()
    .then { it.applyForm(message.form) }
    .then { it.markInDictionary() }
//   applyForm: форма в стейт, общая группа «Все», перевод
//     свёрнут, фаза «окно»
//   markInDictionary: пометка «в словаре» у слов, чей
//     текст есть среди лемм словаря (AWP-39)

// FormFailed — хендлер LoadForm, эффект упал: словарь
// удалён или чтение упало:
//   errorRes — «Словарь удалён» или «Не удалось собрать
//     окно»
// Есть, не меняется
is Msg.FormFailed -> state.showError(message.errorRes)
//   showError: фаза «ошибка», текст, без «Повторить»

// ToggleTranslation — UI: тап по блоку перевода. Без
// данных. Новое
Msg.ToggleTranslation -> state.toggleTranslation()
//   toggleTranslation: блок раскрыт или свёрнут (AWP-41)

// SetCommonGroup — UI: выбор в листе общей группы:
//   groupId — группа словаря, null — «Все»
// Есть, не меняется
is Msg.SetCommonGroup -> state
    .begin<ScanCandidatesState, Effect>()
    .then { it.setCommonGroup(message.groupId) }
    .then { it.closeSheet() }
//   setCommonGroup: общая группа в стейт (AWP-08)
//   closeSheet: лист закрыт

// RequestClose — UI: крестик, системная «назад» или
// «Отменить» на индикаторе. Без данных. Меняется: без
// вопроса — терять нечего (AWP-60); вопрос с числом
// недобавленных — этап 2 (AWP-51)
Msg.RequestClose -> state.close()
//   close: эффект навигации назад, стейт не меняется
```

Атомы, которые трогает этап; `showNoWords`, `showError`,
`showRetryableError`, `setCommonGroup`, `openSheet`, `closeSheet`,
`close` — как были.

```kotlin
// startFormLoading — Ш3 → Ш4: ответ в стейт, фаза
// «собираю окно»; эффект формы — словарь и тексты всех
// слов списков без повторов: по ним форма несёт леммы
// словаря для пометки «в словаре». Меняется: списки
// вместо слов с лексемами.
fun ScanCandidatesState.startFormLoading(
    found: Msg.LemmasFound,
): ScanCandidatesResult {
    val lemmaTexts = found.lists.allTexts().distinct()
    logStep(
        "startFormLoading",
        "dictionaryId" to found.dictionaryId,
        "words" to lemmaTexts.size,
    )
    val next = copy(
        phase = ScanPhase.FORM_LOADING,
        dictionaryId = found.dictionaryId,
        text = found.text,
        translation = found.translation,
        lists = found.lists,
    )
    return next to setOf(
        DatasourceEffect.LoadForm(
            found.dictionaryId,
            lemmaTexts,
        ),
    )
}

// applyForm — Ш4 → Ш5: форма в стейт, общая группа «Все»
// (AWP-08.7), перевод свёрнут (AWP-41.2), фаза «окно».
// Меняется: карточек и «не добавить» нет.
fun ScanCandidatesState.applyForm(
    form: DictionaryFormUi,
): ScanCandidatesResult {
    logStep(
        "applyForm",
        "groups" to form.groups.size,
        "existing" to form.existingLemmas.size,
    )
    return copy(
        phase = ScanPhase.WINDOW,
        form = form,
        commonGroupId = null,
        isTranslationExpanded = false,
    ).begin()
}

// markInDictionary — Ш4: пометка «в словаре» у слова,
// чей текст совпал с леммой словаря без учёта регистра,
// краёв и формы Unicode (AWP-39). No-op без формы. Новый.
fun ScanCandidatesState.markInDictionary():
    ScanCandidatesResult {
    val form = form
        ?: return noOp("markInDictionary: no form")
    val marked = lists.withDictionaryMarks(
        form.existingLemmas,
    )
    logStep(
        "markInDictionary",
        "inDictionary" to marked.countInDictionary(),
    )
    return copy(lists = marked).begin()
}

// toggleTranslation — блок перевода раскрыт или свёрнут
// (AWP-41). Эффектов нет. Новый.
fun ScanCandidatesState.toggleTranslation():
    ScanCandidatesResult {
    logStep(
        "toggleTranslation",
        "expanded" to !isTranslationExpanded,
    )
    return copy(
        isTranslationExpanded = !isTranslationExpanded,
    ).begin()
}
```

Хендлер: ветка `LoadForm` — как была, зовёт `useCase.loadForm` и
шлёт `FormLoaded` или `FormFailed`; ветка `ExtractLemmas` —
`hld-tfp.md`. Ветки `AddWords` на этапе 1 нет.

Сообщения, которые трогает этап:

```kotlin
// LemmasFound — исход Found: ответ сервера на токены.
// Новое, вместо WordsFound.
data class LemmasFound(
    // dictionaryId — словарь снимка.
    val dictionaryId: Long,
    // text — исправленный текст снимка.
    val text: String,
    // translation — перевод текста.
    val translation: String,
    // lists — четыре списка слов.
    val lists: ScanWordListsUi,
) : Msg

// LemmasEmpty — исход NoWords. Новое, вместо WordsEmpty.
data object LemmasEmpty : Msg

// LemmasDenied — исход Denied, кроме нейронки:
// «Повторить» нет. Новое, вместо LookupDenied.
data class LemmasDenied(
    // errorRes — текст причины.
    @param:StringRes val errorRes: Int,
) : Msg

// LemmasUnavailable — сети нет, сервер не ответил, отказ
// 503 или сбой эффекта: «Повторить» есть. Новое, вместо
// LookupUnavailable.
data class LemmasUnavailable(
    // errorRes — текст причины.
    @param:StringRes val errorRes: Int,
) : Msg

// RetryLemmas — «Повторить» на ошибке. Новое, вместо
// RetryLookup.
data object RetryLemmas : Msg

// ToggleTranslation — тап по блоку перевода. Новое.
data object ToggleTranslation : Msg
```

#### Чистые функции

Пакет `scancandidates/mate`, без состояния и эффектов:

```kotlin
// toUi — леммы текста → списки окна: по слову на строку
// каждого списка, без пометок. Новая.
fun TextLemmas.toUi(): ScanWordListsUi

// withDictionaryMarks — AWP-39: слово помечено «в
// словаре», когда ключ его текста совпал с ключом текста
// леммы словаря. Новая.
fun ScanWordListsUi.withDictionaryMarks(
    existing: List<ExistingLemmaUi>,
): ScanWordListsUi

// allTexts — тексты всех слов четырёх списков в порядке
// показа. Новая.
fun ScanWordListsUi.allTexts(): List<String>

// countInDictionary — сколько слов с пометкой, для лога.
// Новая.
fun ScanWordListsUi.countInDictionary(): Int

// wordTextKey — ключ «то же слово»: края, NFC, нижний
// регистр; «ё» и «е» разные (AWP-39.4). Пакет
// domain/lexeme, есть.
fun wordTextKey(
    raw: String,
    locale: Locale = Locale.ROOT,
): String
```

### Этап 2: карточка под словом

> [!IMPORTANT]
> Этап 3 ниже переделывает карточку в плашки по спеке v3: «куда класть»
> и своя группа переезжают с карточки на лексему-кандидата, запись идёт
> по одной лексеме, плашки словаря перечитываются после записи. Что
> этап 3 не называет, остаётся как здесь.

Тап по слову списка раскрывает под ним карточку: у нового слова —
лексемы-кандидаты с сервера, у слова «в словаре» — сохранённые
лексемы и «Проверить по тексту». В карточке правки, «куда класть»,
своя группа, «Добавить» и «Удалить». После «Добавить» слово уходит
из списка (Д118). Слово адресуется текстом: строки с одним текстом в
двух списках делят одну карточку. Правки лексем адресуются леммой
карточки, как в общих виджетах: две карточки одной леммы правятся
вместе.

#### Стейт

Дельта к коду этапа 1: у слова списка появляется карточка, у экрана
— вопрос при закрытии и признак записи.

```diff
 data class ScanCandidatesState(
     …
     val lists: ScanWordListsUi = ScanWordListsUi(),
     …
     val sheet: CandidatesSheetUi? = null,
+    // новое: идёт запись хотя бы одного слова — крестик
+    // и «назад» не действуют (AWP-51.12)
+    val isAdding: Boolean = false,
+    // новое: вопрос «Закрыть без добавления?» открыт
+    // (AWP-51)
+    val isConfirmingClose: Boolean = false,
+    // новое: число недобавленных слов в вопросе
+    val pendingWords: Int = 0,
 )

 data class ScanWordUiItem(
     val text: String,
     val isInDictionary: Boolean = false,
+    // новое: карточка под словом; null — не открывали
+    val card: WordCardUi? = null,
+    // новое: карточка раскрыта (AWP-42)
+    val isExpanded: Boolean = false,
 )
```

Новые типы, пакет `scancandidates/mate`:

```kotlin
// WordCardUi — карточка под словом (AWP-42). Новый.
data class WordCardUi(
    // phase — что в карточке
    val phase: CardPhase,
    // lemma — лемма с лексемами-кандидатами, «куда
    // класть», своей группой и пометкой формы; у слова
    // «в словаре» до проверки — без лексем
    val lemma: LemmaCardUiItem,
    // saved — сохранённые лексемы леммы словаря из «куда
    // класть» (AWP-46); у нового слова пусто
    val saved: List<SavedLexemeUi> = emptyList(),
    // remarks — строки «Замечаний» ответа (AWP-30)
    val remarks: List<RemarkUiItem> = emptyList(),
    // componentMatchLines — строки «Сопоставления»
    // (AWP-31)
    val componentMatchLines:
        List<ComponentMatchLineUiItem> = emptyList(),
    // isComponentMatchesExpanded — раздел раскрыт
    val isComponentMatchesExpanded: Boolean = false,
    // canAdd — «Добавить» активна: есть лексема без «уже
    // в словаре» и ни одной без ядра (AWP-47.2, 47.3)
    val canAdd: Boolean = false,
    // errorRes — «Не найдено», «Значений нет» или
    // причина ошибки (В4)
    @StringRes val errorRes: Int? = null,
    // hasRetryButton — у ошибки есть «Повторить»
    val hasRetryButton: Boolean = false,
    // isAdding — идёт запись этого слова, кнопки
    // неактивны (AWP-02)
    val isAdding: Boolean = false,
    // addFailure — запись не удалась, причина (AWP-21)
    val addFailure: AddFailureUi? = null,
)

// CardPhase — что показано в карточке. Новый.
enum class CardPhase {
    // SAVED — сохранённые лексемы слова «в словаре» и
    // «Проверить по тексту» (AWP-46)
    SAVED,
    // LOADING — запрос значений идёт (AWP-57)
    LOADING,
    // CANDIDATES — лексемы-кандидаты с сервера (AWP-43)
    CANDIDATES,
    // NOT_FOUND — «Не найдено» или «Значений нет»
    // (AWP-55)
    NOT_FOUND,
    // ERROR — ошибка или отказ на слове (AWP-61)
    ERROR,
}
```

Типы общих виджетов, пакет `widget/candidates`, меняются:

```diff
 data class ExistingLemmaUi(
     val wordId: Long,
     val text: String,
     val lexemeCount: Int,
     val coreValues: List<String>,
     val lexemeTextKeys: Set<String>,
     val groupNames: List<String>,
+    // новое: день добавления леммы в словарь, миллисекунды
+    // (AWP-18.10)
+    val addedAt: Long,
+    // новое: сохранённые лексемы по компонентам формы —
+    // карточка слова «в словаре» (AWP-46)
+    val lexemes: List<SavedLexemeUi>,
 )

 data class CardLexemeUiItem(
     val key: String,
     …
     val exists: Boolean,
     val lacksCore: Boolean,
     val existsInLemma: Boolean = false,
+    // новое: употреблена в тексте снимка — пометка
+    // «в тексте» (AWP-43)
+    val isInContext: Boolean = false,
 )

 data class LookupLexemeUi(
     …
     val componentMatches: List<ComponentMatchUi>,
+    // новое: пометка «в тексте» от сервера (LFL-113)
+    val inContext: Boolean,
 )
```

```kotlin
// SavedLexemeUi — лексема словаря в карточке под словом:
// значения по компонентам формы, как у лексемы-кандидата,
// без правок и крестиков (AWP-46). Новый.
data class SavedLexemeUi(
    // id — лексема словаря
    val id: Long,
    // values — значения по компонентам формы в их порядке
    val values: List<CandidateValueUiItem>,
)
```

#### Редьюсер

Ветки тапа и ответа по слову. Адрес — текст слова списка.

```kotlin
// LoadLexemes — UI: тап по новому слову без карточки,
// «Проверить по тексту» у слова «в словаре» или
// «Повторить» на ошибке слова:
//   text — слово списка
// Новое
is Msg.LoadLexemes -> state.startLookup(message.text)
//   startLookup: карточка «загрузка» раскрыта, прежняя
//     ошибка снята, у слова «в словаре» лемма и
//     сохранённые на месте; эффект LookupLemma — лемма
//     слова с текстом и переводом снимка (AWP-43,
//     AWP-49, AWP-57)

// OpenSavedCard — UI: тап по слову «в словаре» без
// карточки:
//   text — слово списка
// Новое
is Msg.OpenSavedCard -> state.openSavedCard(message.text)
//   openSavedCard: карточка «сохранённые» раскрыта: лемма
//     словаря по умолчанию самая новая, её лексемы и
//     группы, запроса нет (AWP-46, AWP-18.3)

// ToggleCard — UI: повторный тап по слову с карточкой:
//   text — слово списка
// Новое
is Msg.ToggleCard -> state.toggleCard(message.text)
//   toggleCard: карточка свёрнута или раскрыта такой,
//     какой была (AWP-42)

// LexemesFound — хендлер LookupLemma, исход Found: сервер
// прислал лемму с лексемами-кандидатами:
//   text — слово списка
//   word — слово ответа с леммой и замечаниями
//   recommendations — советы завести компонент
//   existing — леммы словаря с текстом леммы ответа, когда
//     она не из списков («children» → «child»); пусто,
//     если лемма та же
// Новое
is Msg.LexemesFound -> state
    .begin<ScanCandidatesState, Effect>()
    .then { it.applyLexemes(message) }
    .then { it.refreshCardValues(message.text) }
    .then { it.refreshCardMarks(message.text) }
//   applyLexemes: карточка «лексемы-кандидаты»: лемма
//     ответа по сопоставлению, первая «в тексте»,
//     «Замечания» и «Сопоставление» (AWP-43, AWP-29,
//     AWP-30, AWP-31); слова уже нет — ничего (AWP-48.4)
//   refreshCardValues: активность значений по
//     зависимостям компонентов (AWP-17.6)
//   refreshCardMarks: «уже в словаре» по ядрам выбранной
//     леммы (AWP-50), «не попадёт в ядро» (AWP-11),
//     «Добавить» активна или нет (AWP-47)

// LexemesNotFound — хендлер LookupLemma, исходы NotFound
// и NoLexemes: словарной формы в базе нет или нейронка
// ничего не выбрала:
//   text — слово списка
//   textRes — «Не найдено» или «Значений нет»
// Новое
is Msg.LexemesNotFound ->
    state.showCardNotFound(message.text, message.textRes)
//   showCardNotFound: карточка «не найдено» с текстом,
//     без «Добавить», «Удалить» есть (AWP-55)

// LexemesDenied — хендлер LookupLemma, исход Denied, кроме
// нейронки: отказ по ключу, лимиту, размеру или языку:
//   text — слово списка
//   errorRes — текст причины
// Новое
is Msg.LexemesDenied ->
    state.showCardError(message.text, message.errorRes)
//   showCardError: карточка «ошибка» без «Повторить»
//     (AWP-61.3, 61.4, 61.6, 61.8)

// LexemesUnavailable — хендлер LookupLemma: сети нет,
// сервер не ответил, нейронка недоступна или по слову не
// справилась:
//   text — слово списка
//   errorRes — текст причины
// Новое
is Msg.LexemesUnavailable -> state
    .showCardRetryableError(message.text, message.errorRes)
//   showCardRetryableError: карточка «ошибка» с
//     «Повторить» (AWP-61.1, 61.2, 61.5)

// DeleteWord — UI: «Удалить» у слова:
//   text — слово списка
// Новое
is Msg.DeleteWord -> state.deleteWord(message.text)
//   deleteWord: строки слова прочь из списков; ответ,
//     который придёт позже, некуда класть (AWP-48)
```

Ветки правок. Адрес — лемма карточки, как у общих виджетов;
правятся все карточки этой леммы.

```kotlin
// DeleteLexeme — UI: крестик у лексемы-кандидата:
//   lemma — лемма карточки
//   key — ключ лексемы
// Есть в первой версии, меняется: адрес — карточки леммы
is Msg.DeleteLexeme -> state
    .begin<ScanCandidatesState, Effect>()
    .then { it.deleteLexeme(message.lemma, message.key) }
    .then { it.refreshLemmaMarks(message.lemma) }
//   deleteLexeme: лексема со всеми значениями прочь
//     (AWP-15.1)
//   refreshLemmaMarks: «уже в словаре», «не попадёт в
//     ядро» и «Добавить» заново у карточек леммы

// DeleteValue — UI: крестик у значения компонента:
//   ref — лемма, ключ лексемы, компонент, номер значения
// Есть, меняется: адрес
is Msg.DeleteValue -> state
    .begin<ScanCandidatesState, Effect>()
    .then { it.deleteValue(message.ref) }
    .then { it.refreshLemmaValues(message.ref.lemma) }
    .then { it.refreshLemmaMarks(message.ref.lemma) }
//   deleteValue: одно значение прочь; ушёл последний
//     текст ядра — уходит вся лексема (AWP-15.2, 15.7,
//     AWP-11.5)
//   refreshLemmaValues: активность значений у карточек
//     леммы (AWP-17.6)

// SubmitText — UI: «Сохранить» в листе правки:
//   ref — адрес значения
//   text — текст из листа
// Есть, меняется: без флага правок — вопрос при закрытии
// считает слова, не правки (AWP-51)
is Msg.SubmitText -> state
    .begin<ScanCandidatesState, Effect>()
    .then { it.applyTextEdit(message.ref, message.text) }
    .then { it.dropEmptyTexts(message.ref) }
    .then { it.refreshLemmaValues(message.ref.lemma) }
    .then { it.closeSheet() }
    .then { it.refreshLemmaMarks(message.ref.lemma) }
//   applyTextEdit: текст с обрезанными краями (AWP-16)
//   dropEmptyTexts: стёртые тексты прочь (AWP-16.5)
//   closeSheet: лист закрыт

// ResetText — UI: «сбросить» в листе правки:
//   ref — адрес значения
// Есть, меняется: без флага правок
is Msg.ResetText -> state
    .begin<ScanCandidatesState, Effect>()
    .then { it.resetText(message.ref) }
    .then { it.closeSheet() }
    .then { it.refreshLemmaMarks(message.ref.lemma) }
//   resetText: текст ответа обратно (AWP-16.6)

// SetOption — UI: опция в листе «Выбора»:
//   ref — лемма, ключ лексемы, компонент
//   option — опция, null — «без значения»
// Есть, меняется: без флага правок
is Msg.SetOption -> state
    .begin<ScanCandidatesState, Effect>()
    .then { it.setOption(message.ref, message.option) }
    .then { it.refreshLemmaValues(message.ref.lemma) }
    .then { it.closeSheet() }
//   setOption: опция у одной лексемы-кандидата (AWP-17)

// SetCardTarget — UI: выбор в листе «куда класть»:
//   lemma — лемма карточки
//   target — лемма словаря или «новое слово»
// Есть, меняется: сохранённые и «уже в словаре» заново
is Msg.SetCardTarget -> state
    .begin<ScanCandidatesState, Effect>()
    .then {
        it.setCardTarget(message.lemma, message.target)
    }
    .then { it.refreshLemmaSaved(message.lemma) }
    .then { it.refreshLemmaMarks(message.lemma) }
    .then { it.closeSheet() }
//   setCardTarget: «куда класть» (AWP-18)
//   refreshLemmaSaved: сохранённые лексемы выбранной
//     леммы в карточке (AWP-18.8, AWP-46)

// SetCardGroup — UI: выбор в листе своей группы:
//   lemma — лемма карточки
//   group — «как у всех», «Все» или группа
// Есть, не меняется
is Msg.SetCardGroup -> state
    .begin<ScanCandidatesState, Effect>()
    .then { it.setCardGroup(message.lemma, message.group) }
    .then { it.closeSheet() }
//   setCardGroup: своя группа леммы (AWP-08)

// ToggleComponentMatches — UI: заголовок «Сопоставления»
// в карточке:
//   text — слово списка
// Есть, меняется: раздел у карточки, не у окна
is Msg.ToggleComponentMatches ->
    state.toggleCardMatches(message.text)
//   toggleCardMatches: раздел раскрыт или свёрнут
//     (AWP-31.4)

// OpenSheet, CloseSheet, UpdateDraft — как в первой
// версии; у листа правки черновик — текст значения из
// карточек по адресу, «сбросить» — если он отличается
// от текста ответа (AWP-16.6)
```

Ветки записи и закрытия.

```kotlin
// AddWord — UI: «Добавить» у слова:
//   text — слово списка
// Новое
is Msg.AddWord -> state.startAddingWord(message.text)
//   startAddingWord: пакет из карточки слова — лемма
//     новая или выбранная, лексемы без «уже в словаре»,
//     группа своя или общая; «идёт запись» у слова и у
//     экрана; эффект AddWord (AWP-20, AWP-03)

// WordAdded — хендлер AddWord, исход Added: записано:
//   text — слово списка
// Новое
is Msg.WordAdded -> state.removeAddedWord(message.text)
//   removeAddedWord: строки слова прочь из списков, как
//     будто переехало в словарь; «идёт запись» снят, если
//     других записей нет (Д118, AWP-47)

// AddGroupGone — хендлер AddWord, отказ: группа удалена:
//   text — слово списка
//   failure — id группы
// Есть, меняется: сбой у слова, не у окна
is Msg.AddGroupGone -> state
    .begin<ScanCandidatesState, Effect>()
    .then { it.resetDeadGroup(message.failure.groupId) }
    .then {
        it.showAddFailure(message.text, message.failure)
    }
//   resetDeadGroup: общая → «Все», своя у карточек → «как
//     у всех», из формы прочь (AWP-21.3, 21.5)
//   showAddFailure: «не добавилось» и «Повторить» под
//     словом, кнопки снова активны (AWP-21)

// AddWordGone — хендлер AddWord, отказ: выбранная лемма
// словаря удалена:
//   text — слово списка
//   failure — id леммы
// Есть, меняется: сбой у слова
is Msg.AddWordGone -> state
    .begin<ScanCandidatesState, Effect>()
    .then { it.resetDeadTarget(message.failure.wordId) }
    .then { it.refreshAllCards() }
    .then {
        it.showAddFailure(message.text, message.failure)
    }
//   resetDeadTarget: «куда класть» → «новое слово» у
//     карточек с ней, из формы прочь (AWP-21.4)
//   refreshAllCards: сохранённые, активность и пометки
//     всех карточек заново

// AddComponentGone — хендлер AddWord, отказ: компонент
// выключен:
//   text — слово списка
//   failure — id компонента
// Есть, меняется: сбой у слова
is Msg.AddComponentGone -> state
    .begin<ScanCandidatesState, Effect>()
    .then { it.dropComponent(message.failure.typeId) }
    .then { it.refreshAllCards() }
    .then {
        it.showAddFailure(message.text, message.failure)
    }
//   dropComponent: компонент из формы и его значения из
//     всех карточек прочь (AWP-21.6)

// AddFailed — хендлер AddWord: нет ядра, словарь удалён
// или сбой транзакции:
//   text — слово списка
//   failure — причина
// Есть, меняется: сбой у слова
is Msg.AddFailed ->
    state.showAddFailure(message.text, message.failure)
//   showAddFailure: «не добавилось» и «Повторить» под
//     словом, в словаре ничего (AWP-21.1)

// RetryAdd — UI: «Повторить» под словом:
//   text — слово списка
// Есть, меняется: адрес
is Msg.RetryAdd -> state
    .begin<ScanCandidatesState, Effect>()
    .then { it.clearAddFailure(message.text) }
    .then { it.startAddingWord(message.text) }
//   clearAddFailure: «не добавилось» снято (AWP-26)

// RequestClose — UI: крестик, «назад» или «Отменить» на
// индикаторе. Без данных. Меняется: пока идёт запись —
// ничего (AWP-51.12); вопрос, если есть слова с
// пришедшей или идущей карточкой без добавления
// (AWP-51). Оба условия — по стейту, правила спеки,
// тесты редьюсера; других условий в редьюсере нет
Msg.RequestClose -> when {
    state.isAdding -> state.noOp("requestClose: adding")
    state.lists.countPending() > 0 ->
        state.askCloseConfirm()
    else -> state.close()
}
//   noOp: запись идёт — крестик на экране неактивен по
//     флагу, «назад» сюда доходит и гасится
//   askCloseConfirm: вопрос «Закрыть без добавления?» с
//     числом таких слов (AWP-51.1)
//   close: эффект навигации назад (AWP-60)

// ConfirmClose — UI: «Закрыть» в вопросе. Без данных.
// Есть в первой версии
Msg.ConfirmClose -> state.close()
//   close: эффект навигации назад, сверх добавленного в
//     словарь ничего (AWP-51.4)

// Stay — UI: «Остаться» в вопросе. Без данных. Есть в
// первой версии
Msg.Stay -> state.closeCloseConfirm()
//   closeCloseConfirm: вопрос убран (AWP-51.3)
```

Атомы слова; переходы над карточками (`deleteLexeme`,
`deleteValue`, `applyTextEdit`, `dropEmptyTexts`, `resetText`,
`setOption`, `setCardTarget`, `setCardGroup`, `resetDeadGroup`,
`resetDeadTarget`, `dropComponent`, листы, вопрос при закрытии) —
как в первой версии, только над карточками леммы в списках.

```kotlin
// openSavedCard — Ш6 Б: карточка слова «в словаре» без
// сервера: лемма словаря по умолчанию самая новая, её
// сохранённые лексемы, «куда класть» и своя группа «как у
// всех»; раскрыта. No-op без формы и без слова. Новый.
fun ScanCandidatesState.openSavedCard(
    text: String,
): ScanCandidatesResult {
    val form = form ?: return noOp("openSavedCard: no form")
    lists.wordOf(text)
        ?: return noOp("openSavedCard: $text")
    val lemma = emptyLemmaCard(text, form)
    logStep(
        "openSavedCard",
        "word" to text,
        "existing" to lemma.existingCount,
    )
    val card = WordCardUi(
        phase = CardPhase.SAVED,
        lemma = lemma,
        saved = form.savedLexemesOf(lemma.target),
    )
    return copy(
        lists = lists.updateWord(text) {
            it.copy(card = card, isExpanded = true)
        },
    ).begin()
}

// startLookup — Ш6 А, Ш8: карточка «загрузка» раскрыта,
// ошибка снята, лемма и сохранённые как были или пустая
// лемма у нового слова; эффект запроса — лемма слова с
// текстом и переводом снимка у словаря снимка (LFL-103,
// AWP-01). No-op без формы, без словаря и без слова.
// Новый.
fun ScanCandidatesState.startLookup(
    text: String,
): ScanCandidatesResult {
    val form = form ?: return noOp("startLookup: no form")
    val dictionaryId = dictionaryId
        ?: return noOp("startLookup: no dictionary")
    val word = lists.wordOf(text)
        ?: return noOp("startLookup: $text")
    val lemma = word.card?.lemma
        ?: emptyLemmaCard(text, form)
    logStep("startLookup", "word" to text)
    val card = WordCardUi(
        phase = CardPhase.LOADING,
        lemma = lemma,
        saved = word.card?.saved.orEmpty(),
    )
    val next = copy(
        lists = lists.updateWord(text) {
            it.copy(card = card, isExpanded = true)
        },
    )
    return next to setOf(
        DatasourceEffect.LookupLemma(
            word = text,
            lemma = text,
            dictionaryId = dictionaryId,
            text = this.text,
            translation = translation,
        ),
    )
}

// applyLexemes — Ш6 → карточка: леммы словаря с текстом
// леммы ответа, которых в форме не было, — в форму
// (AWP-43.7); лемма ответа с лексемами-кандидатами по
// сопоставлению, первая «в тексте»; «куда класть» и своя
// группа — как были у карточки; «Замечания» и
// «Сопоставление» по ответу. Слова уже нет — ничего
// (AWP-48.4). Новый.
fun ScanCandidatesState.applyLexemes(
    found: Msg.LexemesFound,
): ScanCandidatesResult {
    val form = form
        ?.withExistingLemmas(found.existing)
        ?: return noOp("applyLexemes: no form")
    val word = lists.wordOf(found.text)
        ?: return noOp("applyLexemes: ${found.text}")
    val prior = word.card?.lemma
        ?: emptyLemmaCard(found.text, form)
    val lemma = lemmaCardOf(found.word, prior, form)
    logStep(
        "applyLexemes",
        "word" to found.text,
        "lemma" to lemma.lemma,
        "lexemes" to lemma.lexemes.size,
    )
    val card = WordCardUi(
        phase = CardPhase.CANDIDATES,
        lemma = lemma,
        saved = form.savedLexemesOf(lemma.target),
        remarks = remarksOf(
            listOf(found.word),
            found.recommendations,
            form,
        ),
        componentMatchLines = componentMatchLinesOf(
            listOf(found.word),
            form,
        ),
    )
    return copy(
        form = form,
        lists = lists.updateWord(found.text) {
            it.copy(card = card)
        },
    ).begin()
}

// refreshCardMarks — пометки карточки слова: «уже в
// словаре» по ключам ядер леммы из «куда класть»
// (AWP-50), «не попадёт в ядро» (AWP-11), «Добавить»
// активна, когда есть лексема без «уже в словаре» и нет
// лексем без ядра (AWP-47.2, 47.3). No-op без формы и
// без карточки. Новый.
fun ScanCandidatesState.refreshCardMarks(
    text: String,
): ScanCandidatesResult

// startAddingWord — Ш9: пакет одного слова из карточки —
// лемма новая или выбранная, лексемы без «уже в словаре»,
// группа своя или общая; «идёт запись» у слова и экрана,
// прежний сбой снят; эффект записи. No-op без формы и без
// карточки с лексемами. Новый.
fun ScanCandidatesState.startAddingWord(
    text: String,
): ScanCandidatesResult {
    val form = form
        ?: return noOp("startAddingWord: no form")
    val card = lists.wordOf(text)?.card
        ?: return noOp("startAddingWord: $text")
    val batch = wordsBatchOf(
        listOf(card.lemma), form,
        commonGroupId,
    )
    logStep(
        "startAddingWord",
        "word" to text,
        "lexemes" to batch.words.sumOf { it.lexemes.size },
    )
    val next = copy(
        isAdding = true,
        lists = lists.updateWord(text) {
            it.copy(
                card = card.copy(
                    isAdding = true,
                    addFailure = null,
                ),
            )
        },
    )
    return next to setOf(
        DatasourceEffect.AddWord(text, batch),
    )
}

// removeAddedWord — Ш9 → Ш5: строки слова прочь из
// списков (Д118); «идёт запись» экрана снят, когда других
// записей нет. No-op без слова. Новый.
fun ScanCandidatesState.removeAddedWord(
    text: String,
): ScanCandidatesResult {
    lists.wordOf(text)
        ?: return noOp("removeAddedWord: $text")
    logStep("removeAddedWord", "word" to text)
    val rest = lists.removeWord(text)
    return copy(
        lists = rest,
        isAdding = rest.anyAdding(),
    ).begin()
}

// deleteWord — «Удалить» у слова: строки прочь из
// списков без записи (AWP-48). No-op без слова. Новый.
fun ScanCandidatesState.deleteWord(
    text: String,
): ScanCandidatesResult

// askCloseConfirm — вопрос при закрытии с числом слов,
// чьи лексемы-кандидаты пришли или идут и не добавлены
// (AWP-51). Новый, в первой версии без числа.
fun ScanCandidatesState.askCloseConfirm():
    ScanCandidatesResult
```

Хендлер: ветки `RecognizeText`, `ExtractLemmas`, `LoadForm` — как на
этапе 1; две новые.

```kotlin
// LookupLemma — значения леммы с контекстом снимка у
// словаря снимка; хендлер шлёт сообщение на каждый исход
is DatasourceEffect.LookupLemma -> withContext(io) {
    val outcome = useCase.lexemesFromLemma(
        dictionaryId = effect.dictionaryId,
        lemma = effect.lemma,
        context = LemmaContext(
            effect.text, effect.translation,
        ),
    )
    when (outcome) {
        is LexemesOutcome.Found -> Msg.LexemesFound(
            text = effect.word,
            word = outcome.word.toUi(),
            recommendations =
                outcome.recommendations.toUi(),
            existing = outcome.existing.toUi(),
        )
        LexemesOutcome.NotFound -> Msg.LexemesNotFound(
            effect.word, R.string.scan_not_found,
        )
        LexemesOutcome.NoLexemes -> Msg.LexemesNotFound(
            effect.word, R.string.scan_no_lexemes,
        )
        // отказ по нейронке — свой текст с «Повторить»,
        // остальные — текст по причине без повтора
        is LexemesOutcome.Denied ->
            deniedMsg(effect.word, outcome)
        LexemesOutcome.Unavailable ->
            Msg.LexemesUnavailable(
                effect.word,
                R.string.photo_words_error_server,
            )
        LexemesOutcome.WordFailed ->
            Msg.LexemesUnavailable(
                effect.word,
                R.string.scan_server_unavailable,
            )
    }
}

// AddWord — запись одного слова одной транзакцией
is DatasourceEffect.AddWord -> withContext(io) {
    when (val outcome = useCase.addWord(effect.batch)) {
        is AddWordsBatchOutcome.Added ->
            Msg.WordAdded(effect.word)
        is AddWordsBatchOutcome.Rejected.GroupMissing ->
            Msg.AddGroupGone(effect.word, outcome.toUi())
        is AddWordsBatchOutcome.Rejected.WordMissing ->
            Msg.AddWordGone(effect.word, outcome.toUi())
        is AddWordsBatchOutcome.Rejected.TypeUnavailable ->
            Msg.AddComponentGone(
                effect.word, outcome.toUi(),
            )
        is AddWordsBatchOutcome.Rejected.NoCoreValue,
        is AddWordsBatchOutcome.Rejected.DictionaryMissing,
        -> Msg.AddFailed(effect.word, outcome.toUi())
    }
}
```

Эффекты этапа:

```kotlin
// LookupLemma — запрос значений слова (AWP Ш6, Ш8). Сбой
// эффекта — «Сервер не ответил» с «Повторить». В лог —
// toString счётчиками: слово, лемма, словарь, длина
// текста, не сам текст. Новый.
data class LookupLemma(
    // word — слово списка, адрес карточки
    val word: String,
    // lemma — что ищет сервер: текст слова как есть
    val lemma: String,
    // dictionaryId — словарь снимка: языки и дерево
    val dictionaryId: Long,
    // text — исправленный текст снимка, контекст
    val text: String,
    // translation — перевод текста, контекст
    val translation: String,
) : DatasourceEffect, RecoverableEffect<Msg>

// AddWord — запись одного слова одной транзакцией (AWP
// Ш9). Сбой эффекта — «не добавилось» у слова. В лог —
// слово и число лексем. Новый, бывший AddWords первой
// версии.
data class AddWord(
    // word — слово списка, адрес карточки
    val word: String,
    // batch — пакет из одного слова
    val batch: WordsBatch,
) : DatasourceEffect, RecoverableEffect<Msg>
```

#### Чистые функции

Пакет `scancandidates/mate`, над списками:

```kotlin
// wordOf — слово списков по тексту; null — его нет.
// Новая.
fun ScanWordListsUi.wordOf(text: String): ScanWordUiItem?

// updateWord — строки с этим текстом во всех списках с
// изменением; остальные как были. Новая.
fun ScanWordListsUi.updateWord(
    text: String,
    change: (ScanWordUiItem) -> ScanWordUiItem,
): ScanWordListsUi

// removeWord — без строк с этим текстом (AWP-48, Д118).
// Новая.
fun ScanWordListsUi.removeWord(
    text: String,
): ScanWordListsUi

// updateLemmaCards — карточки с этой леммой у всех слов с
// изменением; null от перехода — шага нет. Новая.
fun ScanWordListsUi.updateLemmaCards(
    lemma: String,
    change: (LemmaCardUiItem) -> LemmaCardUiItem?,
): ScanWordListsUi?

// allLemmaCards — карточки лемм всех слов с карточкой, для
// листов правки и «куда класть». Новая.
fun ScanWordListsUi.allLemmaCards(): List<LemmaCardUiItem>

// countPending — слова, чьи лексемы-кандидаты идут или
// пришли и есть что добавить: карточка «загрузка» или
// «лексемы-кандидаты» с лексемой без «уже в словаре»
// (AWP-51.5–51.11). Новая.
fun ScanWordListsUi.countPending(): Int

// anyAdding — идёт запись хотя бы одного слова. Новая.
fun ScanWordListsUi.anyAdding(): Boolean
```

Пакет `widget/candidates`, рядом с функциями карточек:

```kotlin
// emptyLemmaCard — карточка леммы без лексем по форме:
// леммы словаря с тем же текстом, «куда класть» — самая
// новая или «новое слово», своя группа «как у всех»
// (AWP-18.3). Новая, из newCard первой версии.
fun emptyLemmaCard(
    lemma: String,
    form: DictionaryFormUi,
): LemmaCardUiItem

// lemmaCardOf — карточка одной леммы из слова ответа:
// первая лемма с лексемами, лексемы по сопоставлению с
// «в тексте» и «англ.», пометка формы; «куда класть» и
// своя группа — от прежней карточки, если текст леммы
// тот же, иначе по форме заново (AWP-43.7). Новая.
fun lemmaCardOf(
    word: LookupWordUi,
    prior: LemmaCardUiItem,
    form: DictionaryFormUi,
): LemmaCardUiItem

// savedLexemesOf — сохранённые лексемы леммы словаря из
// «куда класть»; «новое слово» — пусто (AWP-46). Новая.
fun DictionaryFormUi.savedLexemesOf(
    target: CardTargetUi,
): List<SavedLexemeUi>

// lexemeKeysOf — ключи ядер лексем леммы словаря из «куда
// класть» для «уже в словаре» (AWP-50); «новое слово» —
// пусто. Новая.
fun DictionaryFormUi.lexemeKeysOf(
    target: CardTargetUi,
): Set<String>

// withExistingLemmas — форма с леммами словаря, которых в
// ней не было: лемма ответа вне списков (AWP-43.7).
// Новая.
fun DictionaryFormUi.withExistingLemmas(
    added: List<ExistingLemmaUi>,
): DictionaryFormUi

// targetRowsOf — строки листа «куда класть» для леммы
// (AWP-18): по лемме словаря с тем же текстом — значения
// ядра, число лексем, группы или «Все», день добавления;
// лист только рисует. Новая.
fun DictionaryFormUi.targetRowsOf(
    lemma: String,
): List<TargetRowUi>

// TargetRowUi — строка леммы словаря в листе «куда
// класть». Новый.
data class TargetRowUi(
    // wordId — лемма словаря
    val wordId: Long,
    // coreValues — первые значения лексем в первом ядре
    val coreValues: List<String>,
    // lexemeCount — число лексем
    val lexemeCount: Int,
    // groupNames — группы леммы; пусто — «Все»
    val groupNames: List<String>,
    // addedAt — день добавления, миллисекунды
    val addedAt: Long,
)

// inCardOrder — значения лексемы в порядке карточки под
// словом (AWP-44): ядра, затем параметры, затем «текст с
// подписью»; внутри части — порядок формы, зависимые
// сразу за своим. Зовёт только lemmaCardOf, у диалога
// карточки слова порядок свой (WCL-20). Новая.
fun List<CandidateValueUiItem>.inCardOrder(
    form: DictionaryFormUi,
): List<CandidateValueUiItem>

// withExistsMarks, withCoreMarks, withActiveValues,
// wordsBatchOf, remarksOf, componentMatchLinesOf,
// deletingLexeme, deletingValue, withTextEdit,
// droppingEmptyTexts, withTextReset, withOption,
// withCardTarget, withCardGroup, withoutGroup,
// withoutTarget, withoutComponent — есть, не меняются.
// buildLemmaCards — снята: карточек окна по списку слов
// нет; summaryOf остаётся у диалога карточки слова.
```

### Этап 3: плашки под словом

Спека v3, Ш6–Ш9. Под словом два ряда плашек. Плашки словаря — по
слову словаря снимка с этим текстом: группы, дата, сохранённые
лексемы; читаются с телефона при раскрытии и перечитываются после
каждой записи (AWP-46, AWP-63). Плашки нейронки — по
лексеме-кандидату ответа: значения, пометки, своё «Куда» с умолчанием
«новое слово», своя группа при «новое слово», кнопка «Создать» или
«Перенести» и «Не нужно» (AWP-18, AWP-47, AWP-64). Запись — одна
лексема одной транзакцией (AWP-20); слово уходит из списка, когда
плашек нейронки не осталось (AWP-62).

Ключи. Адрес слова — его текст в списках, как в этапе 2. Лемма
карточки — тот же текст: плашки словаря, «Куда», «уже в словаре» и
перечитка идут по тексту слова списка, а не по лемме ответа; ответ
другой леммой («children» → «child») — ошибка этапа, окно её не
ловит (AWP-43.7 снята, бэклог). Адрес плашки нейронки — текст слова
и ключ лексемы-кандидата.

#### Стейт

Дельта к этапу 2: «куда класть», своя группа, запись и сбой записи
уходят с карточки на лексему-кандидата; сохранённые — по слову
словаря.

```diff
 // widget/candidates: CandidatesUi.kt
 data class LemmaCardUiItem(
     val lemma: String,
     val sourceWords: List<String>,
     val lexemes: List<CardLexemeUiItem>,
     val existingCount: Int,
-    val target: CardTargetUi,
-    val ownGroup: CardOwnGroupUi,
     val formNote: String? = null,
 )

 data class CardLexemeUiItem(
     …
     val isInContext: Boolean = false,
+    // новое: «Куда» плашки, по умолчанию «новое слово»
+    // (AWP-18.3)
+    val target: CardTargetUi = CardTargetUi.NewWord,
+    // новое: своя группа при «новое слово» (AWP-08)
+    val ownGroup: CardOwnGroupUi = CardOwnGroupUi.AsCommon,
+    // новое: идёт запись этой плашки (AWP-47.4)
+    val isAdding: Boolean = false,
+    // новое: запись не удалась, причина под плашкой
+    // (AWP-21)
+    val addFailure: AddFailureUi? = null,
 ) {
+    // кнопка записи активна: без «уже в словаре» и с
+    // ядром (AWP-47.3, AWP-50.12)
+    val canWrite: Boolean
+        get() = !exists && !lacksCore
 }

 sealed interface CandidatesSheetUi {
-    data class CardGroup(val lemma: String)
-    data class CardTarget(val lemma: String)
+    // лист своей группы и «Куда» — по плашке
+    data class CardGroup(
+        val lemma: String,
+        val key: String,
+    )
+    data class CardTarget(
+        val lemma: String,
+        val key: String,
+    )
     …
 }

 // widget/candidates: FormUi.kt
+// SavedWordUi — плашка словаря (AWP-46): слово словаря с
+// этим текстом, группы (пусто — «Все»), день добавления,
+// сохранённые лексемы в порядке карточки. Новый.
+data class SavedWordUi(
+    val wordId: Long,
+    val text: String,
+    val groupNames: List<String>,
+    val addedAt: Long,
+    val lexemes: List<SavedLexemeUi>,
+)

 // scancandidates/mate: State.kt
 data class WordCardUi(
     val phase: CardPhase,
     val lemma: LemmaCardUiItem,
-    val saved: List<SavedLexemeUi> = emptyList(),
+    // плашки словаря: по слову словаря с текстом слова
+    // списка, самое новое первым (AWP-46)
+    val saved: List<SavedWordUi> = emptyList(),
     val remarks: List<RemarkUiItem> = emptyList(),
     val componentMatchLines:
         List<ComponentMatchLineUiItem> = emptyList(),
     val isComponentMatchesExpanded: Boolean = false,
-    val canAdd: Boolean = false,
     val errorRes: Int? = null,
     val hasRetryButton: Boolean = false,
+    // идёт запись одной из плашек: кнопки и правки всех
+    // плашек слова неактивны (AWP-47.4)
     val isAdding: Boolean = false,
-    val addFailure: AddFailureUi? = null,
 )
```

`CardPhase` без изменений: `SAVED` — плашки словаря и «Проверить по
тексту», `LOADING`, `CANDIDATES` — плашки нейронки, `NOT_FOUND`,
`ERROR`. Плашки словаря видны в любой фазе, когда они есть.

#### Сообщения и эффекты

```diff
 sealed interface Msg {
     data class LexemesFound(
         val text: String,
         val word: LookupWordUi,
         val recommendations:
             List<ComponentRecommendationUi>,
-        val existing: List<ExistingLemmaUi>,
     )

-    data class WordAdded(val text: String)
+    // «Перенести» прошло: лексема легла в выбранное
+    // слово
+    data class LexemeAdded(
+        val text: String,
+        val key: String,
+    )
+    // «Создать» прошло: новое слово wordId с этой
+    // лексемой (AWP-63)
+    data class WordCreated(
+        val text: String,
+        val key: String,
+        val wordId: Long,
+    )
-    data class AddGroupGone(…text, failure)
+    // отказы записи адресуют плашку: так же
+    // AddWordGone, AddComponentGone, AddFailed
+    data class AddGroupGone(
+        val text: String,
+        val key: String,
+        val failure: AddFailureUi.GroupMissing,
+    )
+    // леммы словаря с текстом слова перечитаны после
+    // записи (AWP-63)
+    data class LemmasReloaded(
+        val text: String,
+        val existing: List<ExistingLemmaUi>,
+    )
+    // перечитка упала: плашки словаря как были, лог
+    data class LemmasReloadFailed(val text: String)

-    data class DeleteWord(val text: String)
+    // «Убрать из списка» у слова (AWP-48)
+    data class RemoveWord(val text: String)
-    data class AddWord(val text: String)
+    // «Создать» или «Перенести» у плашки (AWP-20);
+    // какую кнопку нажали, решает «Куда» плашки
+    data class AddLexeme(
+        val text: String,
+        val key: String,
+    )
-    data class RetryAdd(val text: String)
+    data class RetryAdd(
+        val text: String,
+        val key: String,
+    )
+    // «Не нужно» у плашки (AWP-64)
+    data class DismissLexeme(
+        val text: String,
+        val key: String,
+    )
-    data class SetCardTarget(…lemma, target)
-    data class SetCardGroup(…lemma, group)
+    // «Куда» и своя группа: адрес — плашка
+    data class SetCardTarget(
+        val lemma: String,
+        val key: String,
+        val target: CardTargetUi,
+    )
+    data class SetCardGroup(
+        val lemma: String,
+        val key: String,
+        val group: CardOwnGroupUi,
+    )
 }

 sealed interface DatasourceEffect : Effect {
-    data class AddWord(…word, batch)
+    // запись плашки в новое слово (AWP-20.9); сбой —
+    // «не добавилось» у плашки
+    data class CreateWord(
+        val word: String,
+        val key: String,
+        val batch: WordsBatch,
+    )
+    // запись плашки в выбранное слово словаря
+    // (AWP-20.10)
+    data class TransferLexeme(
+        val word: String,
+        val key: String,
+        val batch: WordsBatch,
+    )
+    // леммы словаря с текстом слова заново (AWP-63):
+    // тот же loadForm по одному тексту; сбой —
+    // LemmasReloadFailed
+    data class ReloadLemmas(
+        val word: String,
+        val dictionaryId: Long,
+    )
 }
```

Два эффекта записи вместо одного с флагом: хендлер шлёт на успех
`WordCreated` с id нового слова или `LexemeAdded`, не разбирая пакет;
юзкейс у обоих один — `addWord(batch)`. Перечитка — отдельный эффект
после записи: запись и чтение — разные исходы с разными сбоями, и
запись, прошедшая до упавшей перечитки, остаётся «записано».

#### Атомы

Новые и изменённые; остальные атомы этапа 2 как были. Лемма карточки
— текст слова, поэтому `lemmaStep` и `lemmaCardsStep` адресуют
карточку словом.

```kotlin
// Все атомы — расширения ScanCandidatesState, возвращают
// ScanCandidatesResult; здесь только аргументы.

// openSavedCard — карточка слова «в словаре» (AWP-46):
// плашки словаря по тексту слова, лемма без лексем;
// раскрыта. Меняется: saved — по слову.
openSavedCard(text: String)

// applyLexemes — ответ в карточку (AWP-43, AWP-49):
// лексемы ответа в лемму с текстом слова, у каждой
// «Куда» «новое слово» и группа «как у всех»; плашки
// словаря по тексту. Меняется: existing ответа не
// нужен, лемма — текст слова.
applyLexemes(found: Msg.LexemesFound)

// refreshCardMarks — «уже в словаре» по ядрам всех слов
// словаря с текстом слова (AWP-50), «не попадёт в
// ядро». Меняется: ключи — lemmaWordKeys(text), canAdd
// снят.
refreshCardMarks(text: String)

// removeWord — «Убрать из списка» (AWP-48). Было
// deleteWord.
removeWord(text: String)

// dismissLexeme — «Не нужно»: лексема-кандидат прочь
// из леммы слова (AWP-64). No-op без неё. Новый.
dismissLexeme(text: String, key: String)

// removeEmptyWord — у слова карточка с ответом и ни
// одной лексемы-кандидата: строки прочь (AWP-62);
// иначе no-op. Новый.
removeEmptyWord(text: String)

// setLexemeTarget, setLexemeGroup — «Куда» и своя
// группа плашки (AWP-18, AWP-08). Были setCardTarget,
// setCardGroup.
setLexemeTarget(
    text: String, key: String, target: CardTargetUi,
)
setLexemeGroup(
    text: String, key: String, group: CardOwnGroupUi,
)

// refreshCardSaved — плашки словаря слова по форме
// заново (AWP-46, AWP-63). Было refreshLemmaSaved по
// «куда класть».
refreshCardSaved(text: String)

// startAddingLexeme — запись плашки (AWP-20, AWP-02):
// пакет одной лексемы в новое слово или в выбранное по
// «Куда», эффект CreateWord или TransferLexeme по типу
// «Куда»; «идёт запись» у плашки, карточки и экрана,
// прежний сбой плашки снят. No-op без лексемы и у
// плашки без canWrite. Было startAddingWord.
startAddingLexeme(text: String, key: String)

// finishAdding — записано: плашка прочь, «идёт запись»
// карточки снят, у экрана — когда других записей нет.
// Новый.
finishAdding(text: String, key: String)

// retargetNewWordLexemes — после «Создать»: у плашек
// слова с «Куда: новое слово» — созданное слово
// (AWP-63.1, 63.3); явный выбор не трогается. No-op
// без таких плашек. Новый.
retargetNewWordLexemes(text: String, wordId: Long)

// startLemmasReload — эффект перечитки лемм словаря с
// текстом слова (AWP-63). No-op без слова в списках:
// оно уже ушло. Новый.
startLemmasReload(text: String)

// applyReloadedLemmas — леммы словаря с текстом слова
// в форме заменены присланными, плашки словаря и счёт
// лемм заново (AWP-63). Пометки «в словаре» и «уже в
// словаре» — следующие атомы цепочки. Новый.
applyReloadedLemmas(
    text: String, existing: List<ExistingLemmaUi>,
)

// showAddFailure, clearAddFailure — «не добавилось» у
// плашки (AWP-21, AWP-26). Меняется: адрес — плашка.
showAddFailure(
    text: String, key: String, failure: AddFailureUi,
)
clearAddFailure(text: String, key: String)

// resetDeadGroup, resetDeadTarget — своя группа и
// «Куда» плашек (AWP-21.3–21.5). Меняется: переходы по
// лексемам.
```

#### Редьюсер

Цепочки новых и изменённых сообщений; условия по-прежнему только в
ветке закрытия.

```kotlin
LexemesFound -> applyLexemes → refreshCardValues
    → refreshCardMarks
LexemeAdded -> finishAdding → removeEmptyWord
    → startLemmasReload
WordCreated -> finishAdding → retargetNewWordLexemes
    → removeEmptyWord → startLemmasReload
LemmasReloaded -> applyReloadedLemmas → markInDictionary
    → refreshCardMarks
LemmasReloadFailed -> noOp
AddGroupGone -> resetDeadGroup → showAddFailure
AddWordGone -> resetDeadTarget → refreshAllCards
    → showAddFailure
AddComponentGone -> dropComponent → refreshAllCards
    → showAddFailure
AddFailed -> showAddFailure
RemoveWord -> removeWord
AddLexeme -> startAddingLexeme
RetryAdd -> clearAddFailure → startAddingLexeme
DismissLexeme -> dismissLexeme → refreshLemmaMarks
    → removeEmptyWord
DeleteValue -> deleteValue → refreshLemmaValues
    → refreshLemmaMarks → removeEmptyWord
SubmitText -> applyTextEdit → dropEmptyTexts
    → refreshLemmaValues → closeSheet
    → refreshLemmaMarks → removeEmptyWord
SetCardTarget -> setLexemeTarget → closeSheet
SetCardGroup -> setLexemeGroup → closeSheet
```

`removeEmptyWord` стоит в хвосте цепочек, где плашка может уйти:
«Не нужно», запись, стирание последнего ядра (AWP-11.5). «Куда» на
пометки не влияет (AWP-50.14), поэтому за `setLexemeTarget` пересчёта
нет.

#### Хендлер

```kotlin
// CreateWord → Added:
//   WordCreated(word, key, wordIds.first())
// TransferLexeme → Added: LexemeAdded(word, key)
// отказы обоих — AddGroupGone / AddWordGone /
//   AddComponentGone / AddFailed с ключом плашки
//   (AWP-21.3–21.6)
// ReloadLemmas → loadForm(dictionaryId, listOf(word)):
//   Loaded → LemmasReloaded(word, existingLemmas.toUi())
//   DictionaryMissing, Failed → LemmasReloadFailed(word)
```

Перечитка — тем же `loadForm` по одному тексту: порт не растёт,
форма заново читается целиком, но из ответа берутся только леммы.

#### Чистые функции

```kotlin
// widget/candidates: LemmaCards.kt

// emptyLemmaCard — лемма без лексем по тексту слова;
// «куда класть» и группы у леммы больше нет. Меняется.
fun emptyLemmaCard(
    lemma: String,
    form: DictionaryFormUi,
): LemmaCardUiItem

// lemmaCardOf — лексемы первой леммы ответа с лексемами
// в лемму prior: текст леммы — prior.lemma, то есть
// слово списка; пометка формы — из ответа. Меняется:
// ветка «лемма ответа другая» снята (AWP-43.7).
fun lemmaCardOf(
    word: LookupWordUi,
    prior: LemmaCardUiItem,
    form: DictionaryFormUi,
): LemmaCardUiItem

// savedWordsOf — плашки словаря: по слову словаря с
// текстом, самое новое первым, значения лексем в
// порядке карточки (AWP-46, AWP-44). Заменяет
// savedLexemesOf(target). Новая.
fun DictionaryFormUi.savedWordsOf(
    text: String,
): List<SavedWordUi>

// withLemmasOf — форма, где леммы словаря с этим
// текстом заменены присланными (AWP-63). Заменяет
// withExistingLemmas. Новая.
fun DictionaryFormUi.withLemmasOf(
    text: String,
    lemmas: List<ExistingLemmaUi>,
): DictionaryFormUi

// lemmaWordKeys — есть (WCL-15.4): ключи ядер всех слов
// с текстом; теперь и для «уже в словаре» окна
// (AWP-50). lexemeKeysOf(target) — снята.

// widget/candidates: CardTransitions.kt

// withLexemeTarget, withLexemeGroup — «Куда» и своя
// группа лексемы-кандидата; null — её нет. Заменяют
// withCardTarget / withCardGroup.
fun List<LemmaCardUiItem>.withLexemeTarget(
    lemma: String,
    key: String,
    target: CardTargetUi,
): List<LemmaCardUiItem>?

fun List<LemmaCardUiItem>.withLexemeGroup(
    lemma: String,
    key: String,
    group: CardOwnGroupUi,
): List<LemmaCardUiItem>?

// withNewWordRetargeted — у лексем леммы с «новое
// слово» — созданное слово (AWP-63.3); null — таких
// нет. Новая.
fun List<LemmaCardUiItem>.withNewWordRetargeted(
    lemma: String,
    wordId: Long,
): List<LemmaCardUiItem>?

// withoutGroup, withoutTarget — те же сбросы, но по
// лексемам (AWP-21.3–21.5). Меняются.

// widget/candidates: WordsBatches.kt

// lexemeBatchOf — пакет одной лексемы (AWP-20): слово
// пакета — новое с текстом леммы или выбранное по
// «Куда», группа — своя или общая только у нового
// (AWP-20.10), одна лексема по сопоставлению и
// правкам; null — лексемы нет. Заменяет wordsBatchOf.
fun lexemeBatchOf(
    card: LemmaCardUiItem,
    key: String,
    form: DictionaryFormUi,
    commonGroupId: Long?,
): WordsBatch?

// groupIdOr — группа по своей группе лексемы (AWP-08).
// Переезжает с карточки на лексему.
fun CardLexemeUiItem.groupIdOr(commonGroupId: Long?): Long?
```

#### UI

`widget/candidates/ui`: `LemmaCardWidget` с шапкой леммы и «Куда»
карточки снята; вместо неё `SavedWordPlate(saved, form)` — плашка
словаря: группы или «Все», дата коротким форматом локали, лексемы
строками «Имя: значение» (AWP-46); `LexemeRow` открыт наружу с
`onDelete: (() -> Unit)?` — крестик только у диалога карточки слова;
`LexemeChoices(card, lexeme, form, onAction)` — «Куда: …▾», когда у
слова есть леммы словаря (AWP-18.2), и «Группа: …▾» при «новое
слово» и группах в словаре (AWP-08.6). Листы `CardTargetSheet` и
`CardGroupSheet` берут выбор у лексемы по `(lemma, key)`; подсказка о
группе снята (AWP-18.5).

`scancandidates/ui`: `WordCardBlock` — плашки словаря в любой фазе,
когда есть; `SAVED` — «Проверить по тексту» и «Убрать из списка»;
`LOADING` — признак и «Убрать из списка» (AWP-57.4); `CANDIDATES` —
по плашке на лексему: `LexemeRow` без крестика, `LexemeChoices`, «не
добавилось» с «Повторить» или кнопка записи — «Создать» при «новое
слово», «Перенести» при выбранном слове, неактивна без `canWrite` и
при `isAdding` карточки, — и «Не нужно»; под плашками «Замечания»,
«Сопоставление», подпись источника; `NOT_FOUND`, `ERROR` — причина,
«Повторить» по флагу, «Убрать из списка». `WordAction`: `Tap`,
`Remove`, `CheckByText`, `RetryLookup`, `Write(text, key)`,
`RetryAdd(text, key)`, `Dismiss(text, key)`, `ToggleMatches`.

### Юзкейсы

Порт `ScanCandidatesUseCase`:

```kotlin
// loadForm — Ш4: форма словаря снимка — включённые
// компоненты с опциями и зависимостями, группы, леммы
// словаря с текстами слов списков, у леммы дата
// добавления и сохранённые лексемы; словарь удалён или
// чтение упало — исходом. Меняется: дата и лексемы.
suspend fun loadForm(
    dictionaryId: Long,
    lemmaTexts: List<String>,
): FormOutcome

// lexemesFromLemma — Ш6, Ш8: лемма слова с текстом и
// переводом снимка, языки и дерево компонентов словаря
// снимка на сервер (LFL-103); исход — слово с леммой,
// не найдено, значений нет, отказ с причиной, сервер не
// ответил, слово «ошибка». Лемма ответа не та, что
// просили, — с исходом леммы словаря с её текстом
// (AWP-43.7). Новый, вместо lookupWords первой версии.
suspend fun lexemesFromLemma(
    dictionaryId: Long,
    lemma: String,
    context: LemmaContext,
): LexemesOutcome

// addWord — Ш9: пакет одного слова одной транзакцией
// (AWP-20); исход — записано или отказ до первой вставки
// с причиной. Новый, бывший addWords первой версии.
suspend fun addWord(batch: WordsBatch): AddWordsBatchOutcome
```

`recognize`, `releaseImageAccess`, `lemmasFromTokens` —
`hld-tfp.md`. Домен, пакет `domain/lexeme`:

```kotlin
// LemmaContext — контекст леммы из ответа
// lemmas/from-tokens (LFL Ш1 А). Новый.
data class LemmaContext(
    // text — исправленный текст снимка
    val text: String,
    // translation — его перевод
    val translation: String,
)

// LexemesOutcome — исход запроса значений леммы (LFL Ш8).
// Новый, вместо LookupOutcome со списком слов.
sealed interface LexemesOutcome {
    // Found — слово ответа с леммой и лексемами; existing —
    // леммы словаря снимка с текстом леммы ответа, когда
    // она не та, что просили («children» → «child»),
    // иначе пусто (AWP-43.7)
    data class Found(
        val word: LookupWord,
        val recommendations: List<ComponentRecommendation>,
        val existing: List<ExistingLemma>,
    ) : LexemesOutcome

    // NotFound — словарной формы в базе нет
    data object NotFound : LexemesOutcome

    // NoLexemes — найдено, нейронка ничего не выбрала
    data object NoLexemes : LexemesOutcome

    data class Denied(
        val reason: LookupDenyReason,
    ) : LexemesOutcome

    // Unavailable — сети нет или сервер не ответил
    data object Unavailable : LexemesOutcome

    // WordFailed — слово ответа «ошибка»
    data object WordFailed : LexemesOutcome
}

// ExistingLemma — лемма словаря с текстом слова списка.
// Меняется: дата добавления и сохранённые лексемы.
data class ExistingLemma(
    …
    val groupNames: List<String>,
    // addedAt — день добавления, миллисекунды
    val addedAt: Long,
    // lexemes — сохранённые лексемы со значениями
    val lexemes: List<SavedLexeme>,
)

// SavedLexeme — лексема словаря: id и значения
// компонентов по id типов словаря. Новый.
data class SavedLexeme(
    val id: Long,
    val components: List<BatchComponent>,
)

// LookupLexeme — лексема-кандидат. Меняется: «в тексте».
//   inContext — употреблена в тексте снимка (LFL-113)
```

Реализация в `app`:

```kotlin
// ScanCandidatesUseCaseImpl.lexemesFromLemma — словарь
// снимка по id (AWP-01), дерево компонентов словаря,
// запрос леммой с контекстом, исход слоя сети в домен;
// лемма ответа не та, что просили, — в исход леммы
// словаря с её текстом через CandidatesFormLoader
// (AWP-43.7). Новый.

// ScanCandidatesUseCaseImpl.addWord — запись пакета
// одной транзакцией через WordsBatchApi. Новый.

// CandidatesFormLoader — у леммы словаря дата добавления
// слова и лексемы с живыми значениями компонентов формы;
// леммы словаря по текстам — для юзкейса по лемме ответа.
// Меняется.

// WordCardUseCaseImpl.lookupWord — та же операция без
// контекста: лемма — текст слова карточки; исход в
// WordLookupOutcome. Меняется, `hld-wcl.md`.
```

### Шлюз

`core-network-api`, зеркала контракта:

```kotlin
// WordsApi.lexemesFromLemma — один запрос
// `lexemes/from-lemma` (LFL-103, LFL-104). Новый, вместо
// lookup.
suspend fun lexemesFromLemma(
    request: LexemesRequestApiEntity,
    runId: Long,
): LexemesResult

// LexemesRequestApiEntity — языки словаря, лемма,
// контекст или null, дерево компонентов. Новый, вместо
// LookupRequestApiEntity с токенами.
data class LexemesRequestApiEntity(
    val learningLanguage: String,
    val translationLanguage: String,
    val lemma: String,
    val context: LemmaContextApiEntity?,
    val components: List<ComponentNodeApiEntity>,
)

// LemmaContextApiEntity — текст снимка и перевод. Новый.
data class LemmaContextApiEntity(
    val text: String,
    val translation: String,
)

// LexemesResult — ровно один из трёх: успех с одним
// словом, отказ с причиной, недоступен. Новый, вместо
// LookupResult со списком слов.
//   Success — attribution, word, recommendations
//   Denied — reason, те же причины
//   Unavailable — cause

// CandidateLexemeApiEntity — меняется: поле inContext.
```

`core-network-impl`: `LexemeService` — `@POST("v1/lexemes/from-lemma")`
вместо `v1/words/lookup`; зеркала провода `LexemesRequestNet`,
`LexemesResponseNet` вместо `LookupRequestNet`, `LookupResponseNet`;
разбор кодов отказа — как был (`call`), 413 остаётся
`REQUEST_TOO_LARGE`. Операция `lemmasFromTokens` — `hld-tfp.md`;
форма — `CoreDbApi`, как была.

## 3. Контракт

Своих полей у экрана нет. Из ответа `lemmas/from-tokens` он читает
`text` и `translation` — в стейт и в контекст запроса по слову,
`lexical`, `proper`, `function`, `unparsed` — в списки как есть. Из
ответа `lexemes/from-lemma` — `word` целиком: статус, леммы с
лексемами и их `componentMatches`, `inContext`, `formNote`,
`remarks`; `recommendations` — в «Замечания». Дельты контрактов —
`hld-tfp.md` и `hld-lfl.md`.

## 4. Ключевые решения

- **Три списка и «не разобрано»** (Д75, Д84): порядок на экране —
  смысловые, имена собственные, служебные, «не разобрано»; обрывки
  видны, пользователь решает сам.
- **Перевод свёрнутым блоком** (Д71): над списками, тап раскрывает.
- **Пометка «в словаре» — по словарю телефона** (AWP-39): сервер про
  словарь пользователя не знает; сравнение по ключу текста.
- **Карточка под словом, адрес — текст слова** (Д70, Д94, Д116):
  значения по тапу, одна карточка на текст; правки — по лемме
  карточки, как в общих виджетах.
- **Тап шлёт разные сообщения** (AWP-42, AWP-43, AWP-46): по строке
  видно, есть ли карточка и слово ли это «в словаре», поэтому UI шлёт
  `ToggleCard`, `LoadLexemes` или `OpenSavedCard`; условий в
  редьюсере нет.
- **Добавленное слово уходит из списка** (Д118): пометки «добавлено»
  нет; добавление по одному слову, общей кнопки и вопроса перед
  записью нет.
- **Слово «в словаре» — сохранённые, затем проверка** (Д119, AWP-46,
  AWP-49, AWP-50): «уже в словаре» по ключам ядер леммы из «куда
  класть»; «заменить» отложено, у несовпавшей только «добавить».
- **«Куда класть» с датой** (Д120, AWP-18): значения, число лексем,
  группы или «Все», день добавления.
- **Условия редьюсера — только в ветке закрытия** (AWP-51): пока идёт
  запись, закрытие не действует, а крестик на экране неактивен по
  флагу; вопрос — когда есть слова с идущей или пришедшей карточкой,
  которым есть что добавить; флага правок нет.
- **Лемма ответа вне списков** (AWP-43.7): юзкейс отдаёт с исходом
  леммы словаря с её текстом, они ложатся в форму; окно словарь после
  записи не перечитывает — слово в двух списках не стоит (Д121).
- **Снекбара после окна нет** (AWP-52): вкладка показывает леммы
  словаря сама.

## 5. Сквозное

- **Отказы до окна** — тексты и «Повторить» выбирает хендлер по
  причине, `hld-tfp.md`; окно не собралось — «Не удалось собрать
  окно» или «Словарь удалён» без повтора (AWP-24); пусто — «Слов не
  найдено» (AWP-54).
- **Отказы на слове** — хендлер по исходу: «Не найдено» и «Значений
  нет» без «Повторить» и без «Добавить» (AWP-55); ключ, лимит,
  размер, язык — причина без «Повторить» (AWP-61.3–61.8); сеть,
  сервер, нейронка и слово «ошибка» — с «Повторить» (AWP-61.1, 61.2,
  61.5). Словарь снимка удалён между окном и тапом — юзкейс отдаёт
  «сервер не ответил»: такое бывает только при сборке без модальности.
- **Запись** — отказ до первой вставки типизирован, сбой транзакции
  — исключение эффекта; и то и другое — «не добавилось» у слова, в
  словаре ничего (AWP-21); «идёт запись» гасит кнопки слова, крестик
  и «назад» (AWP-02, AWP-51.12).
- **Отмена** — «Отменить» на индикаторе и крестик — одно сообщение
  `RequestClose`: экран закрывается, цикл с эффектами умирает
  (TFP-21); ответ по слову после закрытия некуда класть.
- **Поворот экрана** — стейт живёт в `ViewModel`: фаза, списки,
  карточки с правками и открытый лист на месте (AWP-14.3, AWP-43.6).
- **Перезапуск** — маршрут экрана не восстанавливается: после
  возврата вкладка без индикатора и без окна; добавленные слова уже в
  словаре (AWP-27, AWP-56).
- **Модальность** — окно на весь экран, вкладки и словарь недоступны
  (AWP-14); словарь снимка один на весь экран (AWP-01).
- **Лог** — `###SCAN###`: шаги атомов, число слов в списках, число
  пометок «в словаре», слово и число лексем у карточки, пакет
  записи счётчиками.

## 6. Выкатка

Этап 1 — вместе с `hld-tfp.md`: бэкенд v0.1.32 раньше сборки. Этап 2
— бэкенд с `lexemes/from-lemma` раньше сборки, `hld-lfl.md`; старая
операция `words/lookup` уходит вместе с ним, сборка первой версии не
в проде (Д46). Релиз мобилки — после всей IS536 (Д116).

## 7. Риски

- **Длинные списки** — плотная страница даёт сотни слов, окно
  прокручивается целиком без «ещё N» (AWP-40.8); сворачивание
  списков — не в этапе.
- **Форма по текстам слов** — леммы словаря читаются по текстам всех
  слов четырёх списков, теперь с лексемами; на плотной странице это
  сотни текстов в одном запросе к Room.
- **Две карточки одной леммы** — «children» в «не разобрано» и
  «child» в смысловых дают две карточки леммы «child»: правки
  ложатся в обе, «Добавить» у каждой пишет свою лемму; второй
  «Добавить» заводит вторую лемму «child» — выбор пользователя, как
  у любого дубля.
- **Форма не обновляется после записи** — леммы словаря в форме
  читаются при сборке окна; добавленное слово в «куда класть»
  соседней карточки той же леммы не видно до нового снимка.
- **Запрос по тапу на каждое слово** — лимит нейронки списывается
  на каждую карточку, кеша с контекстом у сервера нет
  (`hld-lfl.md`).

## 8. Как перерисовать схемы

Условные знаки — Mermaid, слои — Graphviz: Mermaid не ставит сноску
рядом с коробкой и тасует порядок блоков. Источник в `.dot` рядом,
картинка рендерится скриптом, в документ идёт по абсолютному пути с
хешем содержимого в имени: превью студии кеширует картинку по имени.
Перерисовать все схемы папки и обновить ссылки:

```
scripts/mmd-render.sh docs/features/IS536_llm_values
```
