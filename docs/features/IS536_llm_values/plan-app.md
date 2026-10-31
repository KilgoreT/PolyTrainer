# IS536 | План приложения

LLD к `hld-tfp.md`, `hld-awp.md` и `hld-wcl.md`. Этап 1 фичи — IS537
«леммы из текста»: ветка `IS537_lemmas_from_tokens` от
`IS536_llm_values`, PR в неё; этап 2 — своя подфича с ишью и веткой от
`IS536_llm_values` после закрытия первого; релиз мобилки — после всей
IS536 (Д116). Этапы в порядке
выполнения, независимое первым; каждый этап собирается и проходит
тесты сам. Внутри этапа — триады: было — нынешний код из репо, будет
— код после этапа, мотивация — зачем он меняется. Строки нынешнего
кода перенесены под шестьдесят знаков, текст тот же. Код — по
команде. Этап выполнен — пометка «ВЫПОЛНЕН <дата>» с одной фразой, что
разошлось с планом. План первой версии — `plan-app-v1.md`.

Команды: сборка и тесты только `./scripts/cc-build.sh <задача>`, тесты
по одному модулю: `:core:core-network-impl:testDebugUnitTest`,
`:app:testDebugUnitTest`, `:modules:screen:scancandidates:testDebugUnitTest`,
`:modules:widget:candidates:testDebugUnitTest`,
`:modules:screen:wordcard:testDebugUnitTest`,
`:modules:screen:wordstab:testDebugUnitTest`; линт `:app:lintDebug`;
на устройство только `co.lexeme.app.dev`: `:app:installDebug`, затем
`adb shell am start -n co.lexeme.app.dev/me.apomazkin.polytrainer.MainActivity`.

Что входит в этап 1 (Э1–Э5): запрос `lemmas/from-tokens` вместо
`lookupWords` у экрана снимка, окно с переводом и четырьмя списками,
пометка «в словаре», закрытие без вопроса, снекбар после окна снят.
Этап 2 (Э6–Э10): операция `lexemes/from-lemma` в шлюзе и домене,
юзкейсы запроса по лемме и записи одного слова, карточка под словом с
правками, слово «в словаре» с «Проверить по тексту», «Добавить» с
уходом слова из списка (Д118), «куда класть» с датой (Д120), вопрос
при закрытии, диалог карточки слова леммой; «заменить» сохранённую
лексему отложено (Д119). Бэкенд этапа 2 — `plan-backend.md`, Э9–Э14,
в проде раньше сборки. Этап 3 (Э11–Э12): плашки под словом по спеке
v3 — плашки словаря по слову, плашки нейронки по лексеме со своим
«Куда», запись по одной лексеме, перечитка плашек после записи
(Д122–Д131); код этапа 2 закоммичен точкой отката до переделки.

## Э1. Домен и шлюз

ВЫПОЛНЕН 2026-10-27. Как в плане; сверх плана в `WordsApiImplTest` —
разбор тела 200 и тела не по схеме для новой операции, `ACC-05_1` для
неё же; `TFP-26_3` — отказы по ключу, лимиту, размеру и языку одним
тестом.

Тезисы: LFT-02 (сторона устройства), TFP-26 (кому «Повторить»),
ACC-19. Проверка: `core/core-network-impl/src/test/…/WordsApiImplTest.kt`
на поддельном сервере — `LFT-02_1`, `LFT-02_2`, `LFT-02_3`, `ACC-19_1`,
`TFP-26_1`, `TFP-26_2`, `TFP-26_3`, `LFL-21_1`, `LFL-22_1`, `LFL-23_1`
для новой операции; прежние тесты `lookup` зелёные без правок.

Допущение: 400 без кода `unsupported_language` и 503 без кода
`llm_unavailable` — «сервер не ответил», как у `lookup`.

### Домен

**Было.** `modules/domain/lexeme/Lookup.kt` — `LookupToken`,
`LookupOutcome`, `LookupDenyReason`; исхода лемм текста нет.

**Будет.** Файл `modules/domain/lexeme/Lemmas.kt` — `LemmasOutcome` и
`TextLemmas` из `hld-tfp.md`, «Юзкейсы».

**Мотивация.** Порт экрана говорит доменом, исход — типом на случай
(Д57).

### Зеркала и операция шлюза

**Было.** `core-network-api/CoreNetworkApi.kt`:

```kotlin
interface WordsApi {
    suspend fun lookup(
        request: LookupRequestApiEntity,
        runId: Long,
    ): LookupResult
}
```

**Будет.** Метод `lemmasFromTokens` рядом с `lookup`; файлы
`entity/LemmasRequestApiEntity.kt` и `entity/LemmasResult.kt` —
`hld-tfp.md`, «Шлюз»; `DenyReason` общий. В шапке `CoreNetworkApi` —
словарь имён дополнен: запрос лемм, исход лемм.

**Мотивация.** Зеркало контракта по операции, транспорт наружу не
выходит.

### Транспорт и мапперы

**Было.** `core-network-impl/LexemeService.kt`:

```kotlin
internal interface LexemeService {
    @POST("v1/words/lookup")
    suspend fun lookup(
        @Header(RUN_ID_HEADER) runId: String,
        @Body body: LookupRequestNet,
    ): Response<LookupResponseNet>
}
```

`mapper/LookupMappers.kt:37` — помощник токена закрыт для соседнего
файла:

```kotlin
private fun TokenApiEntity.toNet(): TokenNet = TokenNet(
    text = text,
    confidence = confidence,
    language = language,
)
```

**Будет.** Второй метод `lemmasFromTokens` с `@POST("v1/lemmas/from-tokens")`;
файл `net/LemmasNet.kt` — `LemmasRequestNet`, `LemmasResponseNet`;
`mapper/LemmasMappers.kt` — `LemmasRequestApiEntity.toNet()`,
`LemmasResponseNet.toApiEntity()`; `TokenApiEntity.toNet()` —
`internal`, общий с `lookup`. Всё — `hld-tfp.md`, «Шлюз».

**Мотивация.** Один в один с `openapi.yaml` 0.4.0, умолчания у списков
— разбор не падает на отсутствующем поле; помощник токена один на
две операции.

### Обмен с сервером

**Было.** `WordsApiImpl.kt`:

```kotlin
override suspend fun lookup(
    request: LookupRequestApiEntity,
    runId: Long,
): LookupResult {
    if (!options.hasAccessKey) return deniedWithoutKey()
    if (!options.isSecureEnough) {
        return unavailableOverInsecureUrl()
    }
    val exchange = exchange(request, runId)
    if (exchange.failed()) {
        return unavailableOf(exchange.failure)
    }
    if (exchange.rejected()) {
        return denialOf(exchange.response)
    }
    return successOf(exchange.response)
}

private class Exchange(
    val response: Response<LookupResponseNet>?,
    val failure: Throwable?,
)

private suspend fun exchange(
    request: LookupRequestApiEntity,
    runId: Long,
): Exchange
```

**Будет.** `Outcome<T>` и обобщённый `call { … }` из `hld-tfp.md`,
«Шлюз»; `lookup` переписывается через `call` с тем же поведением:

```kotlin
override suspend fun lookup(
    request: LookupRequestApiEntity,
    runId: Long,
): LookupResult {
    val outcome = call {
        service.lookup(runId.toString(), request.toNet())
    }
    return when (outcome) {
        is Outcome.Success -> outcome.body.toApiEntity()
        is Outcome.Denied ->
            LookupResult.Denied(outcome.reason)
        is Outcome.Unavailable ->
            LookupResult.Unavailable(outcome.cause)
    }
}
```

Помощники `denialOf`, `badRequestOf`, `serviceUnavailableOf`,
`errorCodeOf`, `unavailableOf` — обобщаются по типу ответа, тела те
же: тело 200 не по схеме падает внутри вызова сервиса и ловится как
сейчас. Тесты `lookup` в `WordsApiImplTest` зелёные без правок —
проверка, что поведение не ушло.

**Мотивация.** Коды, ключ, HTTPS и таймаут одинаковы у операций; два
экземпляра одной логики разъедутся.

## Э2. Юзкейс и мапперы

ВЫПОЛНЕН 2026-10-27. Как в плане; `TFP-01_1` в тесте юзкейса — номер
запуска общий у `lemmasFromTokens` и `lookupWords`.

Тезисы: LFT-02, TFP-28, TFP-01, TFP-19.2, TFP-19.4, AWP-01. Проверка:
`app/src/test/…/mapper/LemmasMappersTest.kt` — `LFT-02_1`, `LFT-02_2`,
`LFT-02_3`, `TFP-19_2`, `TFP-19_4` (три списка пусты, «не разобрано»
`sh` → `Found`), `AWP-01_1`; `ScanCandidatesUseCaseImplTest.kt` —
`TFP-28_1`, `TFP-01_1`, `LFT-02_1`; прежние тесты `recognize`,
`loadForm`, `lookupWords`, `addWords` как были — их снимает Э3.

### Порт экрана

**Было.** `scancandidates/deps/ScanCandidatesUseCase.kt`:

```kotlin
/**
 * Запрос слов на сервер (TFP Ш6): токены, языки и дерево
 * компонентов текущего словаря (LFL-78). Исход: слова с
 * рекомендациями, нет слов, отказ с причиной, сервер не
 * ответил.
 */
suspend fun lookupWords(
    tokens: List<LookupToken>,
): LookupOutcome
```

**Будет.** `lemmasFromTokens(tokens): LemmasOutcome` — `hld-tfp.md`,
«Юзкейсы» — рядом с прежними методами; `lookupWords` и `addWords`
остаются до Э3: их зовут хендлер экрана, `FakeUseCase` и пилотный
сценарий, без них модуль экрана не собрать. `FakeUseCase.kt` получает
`lemmasFromTokens` с исходом по сценарию теста.

**Мотивация.** Этап добавляет, не ломает: снятие старого — там, где
уходит последний вызов.

### Реализация

**Было.** `app/…/scancandidates/ScanCandidatesUseCaseImpl.kt`:

```kotlin
override suspend fun lookupWords(
    tokens: List<LookupToken>,
): LookupOutcome {
    val dictionary = currentDictionary()
    val components = componentTree.treeOf(dictionary.id)
    val request = lookupRequestOf(
        dictionary, tokens, components,
    )
    return wordsApi
        .lookup(request, lastRunId.incrementAndGet())
        .toDomain(dictionaryId = dictionary.id)
}
```

`app/mapper/LookupMappers.kt:60` — `private fun
LookupToken.toApiEntity()`.

**Будет.** `lemmasFromTokens` из `hld-tfp.md`, «Юзкейсы» — рядом с
`lookupWords`; поле `componentTree` и `ComponentTreeLoader` остаются:
их зовёт форма и карточка слова. Файл `app/mapper/LemmasMappers.kt` —
`lemmasRequestOf`, `LemmasResult.toDomain`, `isEmpty`,
`toTextLemmas`; `LookupToken.toApiEntity()` — `internal`, общий;
`lookupRequestOf` и `LookupResult.toDomain` остаются ради диалога
карточки слова. В `ScanCandidatesUseCaseImplTest` — тесты новой
операции, старые как были.

**Мотивация.** Граница «слой сети → домен» в одном месте (Д57);
пустой ответ — «слов нет» здесь, экран пустоту не считает
(TFP-19.2); помощник токена один на две операции.

## Э3. Экран снимка

ВЫПОЛНЕН 2026-10-27. Как в плане; сверх плана: чистые функции списков
в своём тесте `ScanWordListsTest.kt` (AWP-39 по строкам), `AWP-54_1` и
`TFP-11_1` в петле, `AWP-24_2` и `AWP-24_3` — петли с воротами на
форме; `ScanNavigationEffect` снят здесь же вместе с веткой `AppNavTable`
(Э4 начат в том же заходе); `componentTree` из реализации порта ушёл
вместе с `lookupWords`.

Тезисы: TFP-19, TFP-21, TFP-25, TFP-26, AWP-01, AWP-08.7, AWP-24,
AWP-38, AWP-39, AWP-40, AWP-41, AWP-53, AWP-54, AWP-60. Проверка в
`modules/screen/scancandidates/src/test/…/mate/`:
`ScanCandidatesAtomsTest.kt` — атомы по одному; `ScanCandidatesReducerTest.kt`
— `TFP-19_1`, `TFP-19_2`, `TFP-26_1`, `TFP-26_2`, `TFP-26_3`, `TFP-26_4`,
`AWP-01_1`, `AWP-08_7`, `AWP-24_1`, `AWP-24_4`, `AWP-24_5`, `AWP-38_1`,
`AWP-38_2`, `AWP-39_1`, `AWP-39_2`, `AWP-39_3`, `AWP-39_4`, `AWP-40_1`,
`AWP-40_2`, `AWP-40_6`, `AWP-41_1`, `AWP-41_2`, `AWP-54_1`, `AWP-54_2`,
`AWP-60_1`, `AWP-60_2`; `DatasourceEffectHandlerTest.kt` — `AWP-53_1`,
`AWP-53_2`, `AWP-53_3`, `AWP-53_4`, `AWP-53_5`, `AWP-53_8`;
`ScanCandidatesScenarioTest.kt` — `TFP-21_1`, `TFP-21_2`, `TFP-11_1`,
`TFP-11_3` и `AWP-53_7` (сбой после закрытия экрана), `AWP-24_2`
(форма упала после закрытия — стейт тот же, ошибки нет), `AWP-24_3`,
цепочка «снимок → списки» по абзацу учебника;
`ui/CandidateActionsTest.kt` — по новому списку действий. После
этапа: `:modules:screen:scancandidates`, `:modules:widget:candidates`,
`:modules:screen:wordcard` зелёные, `:app:lintDebug` чист.

Допущение: `ListKind` на экране не нужен — списки приходят четырьмя
полями, слово знает свой список по месту.

### Стейт

**Было.** `mate/State.kt` — `ScanCandidatesState` с полями слов,
карточек, записи и вопросов; `ScanPhase.LOOKING`; `toView()` собирает
`CandidatesView` из всех полей; `textChanged`, `textEdited`,
`optionChanged` — условия правок. У `CandidatesView`
(`widget/candidates/CandidateAction.kt:69–76`) семь полей без
умолчаний: `form`, `cards`, `summary`, `sheet`, `isAdding`,
`addFailure`, `isConfirmingClose`.

**Будет.** Дельта стейта и новые типы `ScanPhase`, `ScanWordListsUi`,
`ScanWordUiItem` — `hld-awp.md`, «Стейт». `textChanged`, `textEdited`,
`optionChanged` сняты. `toView()` остаётся ради заголовка общей
группы и её листа, поля карточек — пустые значения явно:

```kotlin
// toView — вид для общих виджетов окна: форма, общая
// группа и лист; карточек, записи и вопросов у экрана
// на этом этапе нет. Меняется: пустые значения вместо
// полей стейта.
fun ScanCandidatesState.toView(): CandidatesView =
    CandidatesView(
        form = form,
        cards = emptyList(),
        summary = AddSummaryUi(),
        sheet = sheet,
        isAdding = false,
        addFailure = null,
        isConfirmingClose = false,
        commonGroupId = commonGroupId,
    )
```

**Мотивация.** Окно показывает ответ LFT, не карточки; слот под
карточку — на этапе 2 (AWP-42); модуль виджетов не меняется.

### Сообщения и эффекты

**Было.** `mate/Message.kt`: `WordsFound`, `WordsEmpty`, `LookupDenied`,
`LookupUnavailable`, `RetryLookup`, сообщения карточек, записи и
вопросов; `mate/Effects.kt`:

```kotlin
data class LookupWords(
    val tokens: List<LookupToken>,
) : DatasourceEffect,
    RecoverableEffect<Msg> {
    override fun onFail(error: Throwable) =
        Msg.LookupUnavailable(
            errorRes = R.string.photo_words_error_server,
        )
}

data class AddWords(
    val batch: WordsBatch,
) : DatasourceEffect, RecoverableEffect<Msg>

sealed interface ScanNavigationEffect : NavigationEffect {
    data class CloseWithResult(
        val lexemeCount: Int,
        val wordCount: Int,
    ) : ScanNavigationEffect
}
```

**Будет.** Сообщения `LemmasFound`, `LemmasEmpty`, `LemmasDenied`,
`LemmasUnavailable`, `RetryLemmas`, `ToggleTranslation` — `hld-awp.md`,
«Редьюсер»; остаются `TextRecognized`, `TextEmpty`,
`RecognitionFailed`, `FormLoaded`, `FormFailed`, `RequestClose`,
`OpenSheet`, `CloseSheet`, `SetCommonGroup`, `NoOperation`. Сняты:
сообщения карточек, правок, записи, `ConfirmClose`, `Stay`,
`UpdateDraft`, `SetCardTarget`, `SetCardGroup`,
`ToggleComponentMatches`, `ToggleNotAdded`. Эффект `ExtractLemmas` —
`hld-tfp.md`; `AddWords` снят. `ScanNavigationEffect` на этом этапе
никто не шлёт, сам он снимается в Э4 вместе с веткой `AppNavTable`;
закрытие — `NavigationEffect.Back`.

**Мотивация.** Ответ сервера — леммы текста; записи и правок на экране
нет до этапа 2; эффект навигации уходит там, где уходит его
исполнитель.

### Атомы

**Было.** `mate/ScanCandidatesAtoms.kt`:

```kotlin
fun ScanCandidatesState.startLooking(
    tokens: List<LookupToken>,
): ScanCandidatesResult {
    logStep("startLooking", "tokens" to tokens.size)
    val next = copy(
        phase = ScanPhase.LOOKING,
        tokens = tokens,
        errorRes = null,
        hasRetryButton = false,
    )
    return next to setOf(
        DatasourceEffect.LookupWords(tokens),
    )
}

fun ScanCandidatesState.startFormLoading(
    found: Msg.WordsFound,
): ScanCandidatesResult {
    val lemmaTexts = found.words
        .flatMap { word -> word.lemmas.map { it.text } }
        .distinct()
    …
}

fun ScanCandidatesState.applyForm(
    form: DictionaryFormUi,
): ScanCandidatesResult {
    val build = buildLemmaCards(words, form)
    …
}

fun ScanCandidatesState.openSheet(
    sheet: CandidatesSheetUi,
): ScanCandidatesResult {
    val opened = if (sheet is CandidatesSheetUi.TextEdit) {
        val text = cards.textOf(sheet.ref)
            ?: return noOp(
                "openSheet: no text ${sheet.ref}",
            )
        sheet.copy(
            draft = text.text, canReset = text.isEdited,
        )
    } else {
        sheet
    }
    logStep("openSheet", "sheet" to sheet::class.simpleName)
    return copy(sheet = opened).begin()
}
```

**Будет.** `startAnalyzing` — `hld-tfp.md`; `startFormLoading`,
`applyForm`, `markInDictionary`, `toggleTranslation` — `hld-awp.md`,
«Редьюсер». `openSheet` без чтения карточек — на этапе 1 лист один,
общей группы; лист правки текста вернётся с карточкой:

```kotlin
// openSheet — открыть лист, один за раз. Меняется: без
// черновика правки текста — карточек на экране нет.
fun ScanCandidatesState.openSheet(
    sheet: CandidatesSheetUi,
): ScanCandidatesResult {
    logStep("openSheet", "sheet" to sheet::class.simpleName)
    return copy(sheet = sheet).begin()
}
```

Остаются `showNoWords`, `showError`, `showRetryableError`,
`setCommonGroup`, `closeSheet`, `close`. Сняты: `applyRemarks`,
`applyComponentMatchLines`, `refreshActiveValues`,
`refreshExistsMarks`, `refreshCoreMarks`, `refreshSummary`,
`deleteCard`, `deleteLexeme`, `deleteValue`, `applyTextEdit`,
`dropEmptyTexts`, `resetText`, `setOption`, `setCardTarget`,
`setCardGroup`, `toggleComponentMatchesExpanded`,
`toggleNotAddedExpanded`, `updateSheetDraft`, `markDirty`,
`askAddConfirm`, `closeAddConfirm`, `startAdding`, `showAddFailure`,
`clearAddFailure`, `resetDeadGroup`, `resetDeadTarget`,
`dropComponent`, `askCloseConfirm`, `closeCloseConfirm`,
`closeWithResult` и приватные помощники `cardsStep`, `attrs`,
`countLexemes`, `activeValueCount`, `withoutDeadGroup`,
`withoutDeadLemma`.

**Мотивация.** Атом — один технический шаг; шаги карточек вернутся под
словом, когда появится слово с карточкой.

### Редьюсер

**Было.** `mate/ScanCandidatesReducer.kt`:

```kotlin
is Msg.TextRecognized -> state.startLooking(message.tokens)
…
is Msg.WordsFound -> state.startFormLoading(message)
Msg.WordsEmpty -> state.showNoWords()
is Msg.LookupDenied -> state.showError(message.errorRes)
is Msg.LookupUnavailable ->
    state.showRetryableError(message.errorRes)
is Msg.FormLoaded -> state
    .begin<ScanCandidatesState, Effect>()
    .then { it.applyForm(message.form) }
    .then { it.applyRemarks() }
    .then { it.applyComponentMatchLines() }
    .then { it.refreshActiveValues() }
    .then { it.refreshExistsMarks() }
    .then { it.refreshCoreMarks() }
    .then { it.refreshSummary() }
…
Msg.RetryLookup -> state.startLooking(state.tokens)
Msg.RequestClose -> when {
    state.isDirty -> state.askCloseConfirm()
    else -> state.close()
}
```

**Будет.** Ветки из `hld-tfp.md` и `hld-awp.md`, «Редьюсер»; ветки
карточек, правок, записи и вопроса при закрытии сняты;
`reduceSubmitText`, `reduceResetText`, `reduceSetOption` сняты;
`logName()` — `TextRecognized`, `LemmasFound`, `FormLoaded` именем
класса.

**Мотивация.** Редьюсер ветвится только по стейту; условий на этапе 1
нет вовсе.

### Хендлер, порт, чистые функции

**Было.** `mate/DatasourceEffectHandler.kt` — ветки `RecognizeText`,
`LookupWords`, `LoadForm`, `AddWords`; порт с `lookupWords` и
`addWords`; `LookupOutcome.describe()` в `widget/candidates/Outcomes.kt`.

**Будет.** Ветка `ExtractLemmas` — `hld-tfp.md`; ветки `LookupWords` и
`AddWords` сняты; `RecognizeText` и `LoadForm` как были. Здесь же
уходят последние вызовы старых методов: из порта — `lookupWords` и
`addWords`, из реализации — `lookupWords`, `addWords` и поле
`wordsBatchApi`, из `ScanCandidatesUseCaseImplTest` — их тесты и
аргумент `wordsBatchApi`, тест «lookup without a current dictionary»
— на `lemmasFromTokens`; из `FakeUseCase` — старые методы. Файл
`mate/ScanWordLists.kt` — `TextLemmas.toUi()`, `withDictionaryMarks`,
`allTexts`, `countInDictionary` из `hld-awp.md`, «Чистые функции»;
`LemmasOutcome.describe()` — в хендлере.

**Мотивация.** Хендлер шлёт сообщение на каждый исход, редьюсер
исходов не видит; старое снимается с последним вызовом, не раньше.

### Фазы, подписи, «Назад»

**Было.** `ScanCandidatesScreen.kt`:

```kotlin
BackHandler {
    if (!state.isAdding) sendMessage(Msg.RequestClose)
}
…
ScanPhase.LOOKING -> ProgressPanel(
    captionRes = R.string.photo_words_looking,
    onCancel = { sendMessage(Msg.RequestClose) },
)
…
ScanPhase.WINDOW -> ScanCandidatesContent(
    view = state.toView(),
    onAction = { sendMessage(it.toMsg()) },
)
ScanPhase.NO_WORDS -> MessagePanel(
    …
    onRetry = { sendMessage(Msg.RetryLookup) },
    …
)
```

Превью экрана в `:231–233` собирают стейт с `cards` и `summary`.

**Будет.** `BackHandler { sendMessage(Msg.RequestClose) }` — закрытие
без вопроса (AWP-60); `ScanPhase.ANALYZING` с подписью
`R.string.scan_analyzing` «Анализирую текст…», `photo_words_looking`
остаётся диалогу карточки слова; `onRetry` у `NO_WORDS` и `ERROR` —
`sendMessage(Msg.RetryLemmas)`; `ScanPhase.WINDOW` →
`ScanWordListsContent(state, onAction, onToggleTranslation)` в
`scancandidates/ui`; превью — на новых фазах и стейте со списками.
`ProgressPanel` и `MessagePanel` как были.

**Мотивация.** Подпись этапа — по спеке (AWP-38.2); без `isAdding`
условие в `BackHandler` не нужно; окно снимка — своё, у диалога
карточки слова оно другое.

### Окно со списками и общие виджеты

**Было.** `widget/candidates/ui/ScanCandidatesContent.kt` — шапка с
«×», `CommonGroupHeader`, карточки `LemmaCardWidget`, разделы
«Замечания» и «Сопоставление», блок «не добавить», атрибуция, нижняя
панель с «Добавить», листы и вопросы; единственный публичный вход
модуля для окна снимка. Общие куски закрыты в своём модуле:

```kotlin
internal fun CommonGroupHeader(
internal fun CandidatesSheets(
internal fun DividedItem(
```

**Будет.** `CommonGroupHeader`, `CandidatesSheets`, `DividedItem` —
публичные: их зовёт окно снимка из своего модуля, диалог карточки
слова не меняется. Файл `scancandidates/ui/ScanWordListsContent.kt`:
та же верхняя строка с «×» и заголовком, `CommonGroupHeader` поверх
`state.toView()`, блок перевода — строка «Перевод» с «▾»/«▴», по
тапу текст (AWP-41); четыре секции `WordListSection(titleRes, items)`
— заголовок и слова столбиком, у слова с `isInDictionary` пометка
«в словаре» (AWP-39, AWP-40); пустая секция не рисуется (AWP-40.7);
список прокручивается целиком (AWP-40.8); `CandidatesSheets` поверх
ради листа общей группы. `ScanCandidatesContent.kt` снимается; его
части — `LemmaCardWidget`, `RemarksSection`,
`ComponentMatchesSection`, `Attribution` — остаются диалогу карточки
слова и этапу 2. Атрибуции в окне нет: подпись источника — под
карточкой (AWP-45), этап 2. Цвета явные, как у панелей фаз: фон
`formBackground`, текст `formTextTertiary`, второй план
`formTextSecondary`, пометка — `LexemeColor.primary`.

**Мотивация.** Окно показывает то, что в стейте, и ничего не толкует;
из чужого модуля `internal` не вызвать — без публичности окно не
соберётся; карточки под словом добавятся секциями этого же списка.

### Действия окна

**Было.** `scancandidates/ui/CandidateActions.kt` — `toMsg()` на все
действия `CandidateAction`: карточки, правки, запись, вопросы;
`CandidateActionsTest.kt` на них же.

**Будет.** `toMsg()` остаётся исчерпывающим `when`: `OpenSheet`,
`CloseSheet`, `SetCommonGroup`, `RequestClose` — в свои сообщения;
остальные действия общих виджетов — `Msg.NoOperation`, как сейчас
`ToggleLemmaWord`. Перевод и «Повторить» — свои колбэки экрана →
`ToggleTranslation`, `RetryLemmas`. `CandidateActionsTest` — по
новому списку.

**Мотивация.** Общие виджеты шлют один тип действий; у экрана снимка
на этапе 1 из них четыре.

### Строки

**Было.** `core-resources/src/main/res/values/strings.xml` —
английский, язык по умолчанию (`photo_words_looking` — «Looking up
words in the dictionary…»); `values-ru-rRU/strings.xml` — русский
(«Ищу слова в словаре…»). Папки `values-en` нет.

**Будет.** Восемь ключей в оба файла; `values` — английский:
`scan_analyzing` «Analyzing text…», `scan_analysis_unavailable` «Text
analysis is unavailable right now, try again later», `scan_translation`
«Translation», `scan_list_lexical` «Content words», `scan_list_proper`
«Proper names», `scan_list_function` «Function words»,
`scan_list_unparsed` «Unparsed», `scan_in_dictionary` «in dictionary»;
`values-ru-rRU` — русский: «Анализирую текст…», «Анализ текста сейчас
недоступен, попробуй позже» (AWP-53.1), «Перевод», «Смысловые», «Имена
собственные», «Служебные», «Не разобрано», «в словаре». Остальные
тексты отказов — как были (AWP-53.2–53.6, 53.8).

**Мотивация.** Тексты из спеки AWP; умолчание — английский, иначе любой
третий язык увидел бы русский; старые строки живут у диалога карточки
слова.

### Тесты модуля

**Было.** `AddReducerTest`, `CardsReducerTest`, `ValuesReducerTest`,
`GroupsReducerTest`, `CloseReducerTest`, `ConditionsTest` — ветки
карточек, правок, записи, вопросов; `Fixtures.kt` на 696 строк —
слова с лексемами; `ReducerHelpers.kt:54–112` — `answered()` на
`Msg.WordsFound`, `batchOf()` на записи, `card()`, `lexeme()`,
`shown()`, `texts()`, `isActive()`, `chosen()` на поле `cards`;
`DomainFixtures.kt` — `perro` и `batch`, доменного ответа LFT нет;
`ScanCandidatesAtomsTest.kt:32–45` — `window()` на `startLooking`,
`Msg.WordsFound` и карточных атомах; `FakeUseCase.kt` с
`lookupWords` и `addWords`.

**Будет.** Сняты `AddReducerTest`, `CardsReducerTest`,
`ValuesReducerTest`, `ConditionsTest`; `GroupsReducerTest` — только
общая группа (`AWP-08_7` и лист); `CloseReducerTest` — `AWP-60_1`,
`AWP-60_2` и тест закрытия из «слов нет» под именем `AWP-54_2`;
`Fixtures.kt` — ответ LFT по абзацу учебника и `es_dense_screenshot`
(четыре списка, «не разобрано» `sh`, `e`, `ier`, `destibrg`) и форма
с леммами словаря; `DomainFixtures.kt` — `TextLemmas` абзаца учебника
вместо `perro` и `batch`; `ReducerHelpers.kt` — `answered()` на
`Msg.LemmasFound`, `window()` — из ответа и формы, помощники по
`cards` и `batchOf()` сняты; `ScanCandidatesAtomsTest.window()` —
через `startAnalyzing`, `startFormLoading`, `applyForm`,
`markInDictionary`; `FakeUseCase.kt` — только `recognize`,
`releaseImageAccess`, `lemmasFromTokens`, `loadForm`. Переименования:
`ScanCandidatesScenarioTest.kt:366` (сбой распознавания после
закрытия) → `TFP-11_3 AWP-53_7`. Остальные тесты — по списку в шапке
этапа.

**Мотивация.** Тесты на снятые ветки — ложная правда; общие помощники
иначе не соберутся; фикстуры — из контрольного набора, чтобы адреса
строк спеки совпадали с данными.

## Э4. Вкладка «Слова» и навигация

ВЫПОЛНЕН 2026-10-27. Как в плане; сверх плана: параметр
`savedStateHandle` снят с `CompositionRoot.VocabularyHostDep` и с вызова
в `main` — он жил только ради счёта; зависимость `wordstab` от виджета
`candidates` снята — после `addedMessage` она пустая; тест уведомления в
`UiEffectHandlerTest` переписан на «Слово удалено»; в пилоте путь до окна
со списками идёт без адреса вместо `AWP-23_1`.

Тезисы: AWP-52. Проверка: `:modules:screen:wordstab:testDebugUnitTest`
и `:app:testDebugUnitTest` зелёные, `:app:lintDebug` чист.

### Снекбар со счётом и результат навигации

**Было.** `wordstab/logic/Message.kt` и `WordsTabReducer.kt`:

```kotlin
data class ScanCandidatesAdded(
    val lexemeCount: Int,
    val wordCount: Int,
) : Msg

is Msg.ScanCandidatesAdded -> state
    .begin<WordsTabState, Effect>()
    .withEffect(
        UiEffect.ShowNotification(
            message = addedMessage(
                message.lexemeCount, message.wordCount,
            ),
        ),
    )
```

`addedMessage` в `widget/candidates/CountPhrases.kt:14` зовёт только
эта ветка. В `app`: `AppScreen.ScanCandidatesAdded`,
`navigation/ScanCandidatesResult.kt`, ветка `returnWithScanResult` в
`AppNavigationExecutor`, `CompositionRootImpl` →
`words.onScanCandidatesAdded(…)`, `WordsTabScreen.kt:103–110` —
`onScanCandidatesAdded`, `AppNavTable`:

```kotlin
on<ScanNavigationEffect.CloseWithResult> {
    push(
        AppScreen.ScanCandidatesAdded(
            it.lexemeCount, it.wordCount,
        ),
    )
}
```

Тесты: `ScanDialogReducerTest`, `UiEffectHandlerTest:71`,
`CountPhrasesTest:25` — `AWP-23_1`; `app/…/navigation/AppNavigationExecutorTest.kt:30–57`
— три теста результата; `scenario/PilotRegistry.kt:42, 128–139` —
`ResultOnly` и `on<AppScreen.ScanCandidatesAdded>`;
`PilotScenarioTest.kt:287–294` — `photoWithPerro()` стабит
`lookupWords` и `loadForm(1L, listOf("perro"))`, `:353–371` —
`AWP-19_1` с `addWords` и `ScanCandidatesAdded`.

**Будет.** Сняты: сообщение и ветка вкладки, `addedMessage` с тестом
`AWP-23_1`, `onScanCandidatesAdded`, маршрут результата и его
сохранение, `returnWithScanResult`, приём в `CompositionRootImpl`,
ветка `AppNavTable`, `ScanNavigationEffect` в `Effects.kt`;
`AppNavigationExecutorTest` — три теста результата сняты;
`PilotRegistry` — без `ResultOnly` и ветки результата;
`PilotScenarioTest`: `photoWithPerro()` стабит `lemmasFromTokens` с
`LemmasOutcome.Found` и `loadForm` по текстам списков, `AWP-19_1` →
`AWP-60_1` без `addWords` и `ScanCandidatesAdded`, `TFP-22_1` на новом
стабе; `UiEffectHandlerTest:71` остаётся без адреса — путь
уведомления живёт, им показывается «Слово удалено». Закрытие экрана
снимка — `NavigationEffect.Back` по общей ветке `AppNavTable`.

**Мотивация.** После окна вкладка показывает леммы словаря сама
(AWP-52); добавление по одному слову не даёт одного счёта на окно;
результат навигации держат пять файлов `app` и пилот — снимать всё
разом.

## Э5. Покрытие, ручники, устройство

ВЫПОЛНЕН 2026-10-27 без устройства: девайс не был подключён, поэтому
инструментальные `tfp10_1`, `tfp10_3`, `tfp10_4` и ручники не гонялись
— прогон при следующем подключении. Сверх плана: `tfp10_4` рисует
12000×12000 в RGB_565 (пиксели в нативной памяти); кейс AWP-38.4 —
пауза контейнера сервера по ssh; TFP-06.3 и TFP-07.1 — отключение
приложения камеры через `pm disable-user`; в тесте групп сняты адреса
`AWP-08_1` и `AWP-08_3` — их строки про свою группу слова, это этап 2;
статусы TFP и LFT-02 в спеках бэкенда — диффом в репозитории бэкенда без
кода, запись в CHANGELOG обеих спек «Без тега (2026-10-27)».

Тезисы: все строки этапа. Проверка: `scripts/spec-coverage.sh` по трём
спекам без открытых строк у тезисов «реализовано»; ручники на
устройстве.

### Скрипт покрытия

**Было.** `scripts/spec-coverage.sh` — по умолчанию
`words-add-from-photo/spec-v1.md`; строки `:89–91` дают код 1 на адрес
из теста, которого нет в спеке; ручники — только из `SPEC_MANUALS`,
по умолчанию пусто. В тестах и ручниках живут адреса v1, которых нет
в AWP v2: AWP-04, 07, 09, 12, 13, 19, 22, 23, 25 — в
`widget/candidates/…/LemmaCardsTest.kt`, `CountPhrasesTest.kt`,
`domain/lexeme/…/WordTextKeyTest.kt`, `LexemeTextKeyTest.kt`,
`wordcard/…/CandidatesReducerTest.kt:277`,
`app/…/WordCardUseCaseImplTest.kt:592`, тестах `loadForm` в
`ScanCandidatesUseCaseImplTest.kt:568–674`,
`core-db-impl/src/androidTest/…/Is532WordsBatchTest.kt`.

**Будет.** Умолчание — `spec.md` (v2); переменная `SPEC_KNOWN` — та же,
что в копии скрипта бэкенда: спеки, чьи адреса считаются
существующими только для проверки «нет в спеке». Прогоны: AWP —
`SPEC_KNOWN=docs/handbook/specs/words-add-from-photo/spec-v1.md
SPEC_MANUALS=docs/features/IS536_llm_values/manual_tests.md
./scripts/spec-coverage.sh`; TFP —
`SPEC_FILE=backend/docs/handbook/specs/tokens-from-photo/spec.md
SPEC_PREFIX=TFP SPEC_SIDE=устройство SPEC_KNOWN=…/spec-v1.md
SPEC_MANUALS=…`; LFT — `SPEC_FILE=backend/…/lemmas-from-text/spec.md
SPEC_PREFIX=LFT SPEC_SIDE=устройство SPEC_MANUALS=…`. Адреса v1 в
чужих тестах не переименовываются: они про код диалога карточки
слова и этапа 2, их адреса переедут с теми фичами.

**Мотивация.** Массовое переименование адресов v1 — работа этапа 2 и
WCL; `SPEC_KNOWN` держит проверку опечаток и не роняет прогон на
чужих адресах.

### Адреса, у которых сменился смысл

**Было.** `wordstab/…/ScanDialogReducerTest.kt:66, 74` — `TFP-06_4`,
`TFP-06_5`; `manual_tests.md:499` — кейс `TFP-04.2` (сборка без
ключа); `manual_tests.md:157` — кейс «TFP-26.3 … «Повторить»», а в v2
TFP-26.3 — «отказ по ключу или по лимиту → повтора нет»;
`manual_tests.md:190, 238, 271` — адреса TFP-13.1, TFP-24.1, TFP-24.2,
AWP-04.10, AWP-19.2, AWP-19.5, AWP-23.1, AWP-25.1.

**Будет.** `TFP-06_4` → `AWP-37_3`, `TFP-06_5` → `AWP-37_2`; кейс
`TFP-04.2` → `TFP-27.2, AWP-35.2`; кейс «Повторить» при отказе
переписан под v2 — повтора нет. Кейсы первой версии целиком — в
замороженный `manual_tests-v1.md`; в `manual_tests.md` — кейсы этапа
1, из v1 взяты заново кейсы TFP с неизменными строками: TFP-08.3,
08.4, 10.3, 11.1, 19.1, 19.2, 21.3, 21.4, 22.4.

**Мотивация.** Адрес закрывает строку спеки: кейс со старым смыслом
закрыл бы строку, которая говорит обратное.

### Строки этапа 1 без проверки

**Было.** Ни тестом, ни кейсом не закрыты: TFP-27.1–2, TFP-06.3,
TFP-10.1, TFP-10.4, TFP-11.3, TFP-12.1–2; AWP-14.1–2, AWP-35.1–3,
AWP-36.1–2, AWP-37.1–3, AWP-38.4, AWP-40.7–8, AWP-53.6–7, AWP-54.2,
AWP-56.2.

**Будет.** Тесты: TFP-10.1, TFP-10.4 — инструментальные в
`library/textrecognition` (`tfp10_1`, `tfp10_4`); TFP-11.3 и AWP-53.7
— петля (Э3); AWP-37.1–3 — `ScanDialogReducerTest`; AWP-53.6 — адрес в
имени у теста «камера недоступна» в `UiEffectHandlerTest`; AWP-54.2 —
`CloseReducerTest` (Э3). Кейсы `manual_tests.md`: TFP-27.1, AWP-35.1 —
сборка с ключом, кнопка есть; TFP-27.2, AWP-35.2 — сборка без ключа;
AWP-35.3 — вкладка «Группы»; TFP-06.3 — эмулятор без камеры; TFP-12.1–2
— фикстура `en_guide_landscape` и скриншот; AWP-14.1–2 — вкладки и
словарь недоступны, «назад»; AWP-36.1–2 — панель ввода; AWP-38.4 —
сервер молчит 120 с; AWP-40.7–8 — пустой список не рисуется,
прокрутка `es_dense`; AWP-56.2 — новый снимок после возврата. В
обоих случаях адрес — в имени теста или в заголовке кейса.

**Мотивация.** Статус «реализовано» ставится только тезису без
открытых строк; у строк без проверки статус не поднять.

### Статусы тезисов

**Было.** AWP v2 — статусы «принято»; план называл этап 2 диапазоном
«AWP-55…61 без AWP-57», куда попали AWP-56 и AWP-60 этапа 1.

**Будет.** «Реализовано» по покрытию у тезисов этапа 1: TFP-01, 06,
07, 08, 09, 10, 11, 12, 15, 18, 19, 21, 22, 23, 25, 26, 27, 28;
AWP-01, 24, 35, 36, 37, 38, 39, 40, 41, 53, 54, 56, 60; LFT-02.
«Принято» до этапа 2: AWP-02, 03, 11, 15–18, 20, 21, 26, 29–32, 34,
42–51, 55, 57–59, 61 и AWP-08, 14, 27, 52 — у них есть строки про
удаление и добавление слов.

**Мотивация.** Покрытие считается по спеке v2; тезис со строками
этапа 2 не закрыть на этапе 1.

### Ручники и устройство

**Было.** `manual_tests.md` — кейсы первой версии по окну с
карточками.

**Будет.** Кейсы этапа 1 (к списку выше): снимок абзаца учебника →
перевод свёрнут, три списка по контрольному набору (AWP-40.1,
AWP-41.1); `es_dense` → «не разобрано» (AWP-40.2); слово словаря →
«в словаре» (AWP-39.1); пустой снимок → «Слов не найдено» (AWP-54.1);
сеть выключена → «Сервер не ответил», «Повторить» (AWP-53.5,
TFP-26.1); «Отменить» на индикаторе (TFP-21.2); поворот во время
ожидания (AWP-38.3); закрытие крестиком (AWP-60.1); режим полёта на
распознавании (TFP-10.2). Прогон — на `co.lexeme.app.dev` против
прода v0.1.32; перед PR — `:app:installDebug -PMINIFY_DEBUG=true` и
один сетевой кейс.

**Мотивация.** Ручники закрывают строки со статусом «ручной», сборка с
минификацией ловит выпиленный код шлюза.

## Э6. Домен и шлюз этапа 2

ВЫПОЛНЕН 2026-10-28. Как в плане; разошлось: у `LexemesOutcome.Found`
поле `existing` с умолчанием `emptyList()` — маппер слоя сети леммы
словаря не знает, их кладёт юзкейс; `SavedLexeme` — в
`DictionaryForm.kt` рядом с `ExistingLemma`; ресурсы — четыре ответа
прода v0.1.37 (`lexemes_response_v0_1_37_{tap_es,card_es,not_found,
error}.json`), ветка 2.4 (толкование с `no_translation`) — строкой в
тесте: на проде es → de даёт `error` — модель не кладёт строку в ядро,
сервер снимает значение (`pick_without_core`), дефект бэкенда LFL-87.3,
записан открытым; ветка `error` снята с него же; тесты домена —
`:modules:domain:lexeme:test`, модуль чисто JVM.

Этап только добавляет: новые типы и операция рядом со старыми, ничего
не снимается — старое уходит в Э7, когда уходит последний вызов.

Тезисы: LFL-103, LFL-104, LFL-113 (сторона устройства), LFL-50.2,
LFL-50.6, LFL-97.2 (исход по статусу слова), WCL-04.7, WCL-04.8,
WCL-11.1–11.3, WCL-11.6, WCL-11.7 (`wordLookupOf`). Проверка:
`core/core-network-impl/src/test/…/WordsApiImplTest.kt` — новые
`LFL-103_1` (тело запроса: лемма, контекст, языки, дерево),
`LFL-104_1` (без `context`) и разбор ответа 200 на ответах прода одним
словом — новые ресурсы `src/test/resources/lexemes_response_*.json` по
веткам 2.1–2.5 `contract.md`; тесты `lookup` остаются до Э7;
`app/src/test/…/mapper/LexemesMappersTest.kt` (новый) — `LFL-50_2`,
`LFL-50_6`, `LFL-97_2`, `LFL-113_1` (`inContext` доехал до домена);
`modules/domain/lexeme/src/test/…/LookupTest.kt` — `WCL-04_7`,
`WCL-04_8`, `WCL-11_1`, `WCL-11_2`, `WCL-11_3`, `WCL-11_6`,
`WCL-11_7` на новой `wordLookupOf(LexemesOutcome)`, старые четыре
теста живут до Э7. После этапа: собираются и зелёные
`:modules:domain:lexeme`, `:core:core-network-impl`,
`:modules:widget:candidates`, `:modules:screen:wordcard`,
`:modules:screen:scancandidates`, `:app` — ничего не снято.

Допущения: `LookupToken` и `lemmasFromTokens` не трогаются — токены
по-прежнему уходят в `lemmas/from-tokens`; у ответа `found` без
лексем ни у одной леммы свой исход «значений нет» — экрану нужен
другой текст (AWP-55.7); у `inContext` в домене, зеркалах и проводе
умолчание `false`, чтобы прежние конструкторы в тестах собирались до
Э7 и ответ без поля не падал.

### Домен

**Было.** `modules/domain/lexeme/…/Lookup.kt`:

```kotlin
sealed interface LookupOutcome {
    data class Words(
        val dictionaryId: Long,
        val words: List<LookupWord>,
        val recommendations: List<ComponentRecommendation>,
    ) : LookupOutcome

    data object NoWords : LookupOutcome

    data class Denied(
        val reason: LookupDenyReason,
    ) : LookupOutcome

    data object Unavailable : LookupOutcome
}

fun wordLookupOf(outcome: LookupOutcome): WordLookupOutcome
```

`LookupLexeme` без `inContext`; `DictionaryForm.kt` — `ExistingLemma`
с шестью полями: `wordId`, `text`, `lexemeCount`, `coreValues`,
`lexemeTextKeys`, `groupNames`.

**Будет.** `LexemesOutcome` с `Found(word, recommendations,
existing)`, `LemmaContext`, `SavedLexeme`, `LookupLexeme.inContext =
false` — `hld-awp.md`, «Юзкейсы». `LookupOutcome` и старая
`wordLookupOf` остаются до Э7; поля `addedAt` и `lexemes` у
`ExistingLemma` — в Э7 вместе с загрузчиком формы. Новая перегрузка
`wordLookupOf` читает новый исход:

```kotlin
// wordLookupOf — исход запроса → исход по слову карточки
// (WCL-04.7, WCL-11): «ошибка» — сервер по слову не
// справился; не найдено и значений нет — лексем нет.
// Меняется: вход — LexemesOutcome.
fun wordLookupOf(
    outcome: LexemesOutcome,
): WordLookupOutcome = when (outcome) {
    is LexemesOutcome.Found -> WordLookupOutcome.Found(
        word = outcome.word,
        recommendations = outcome.recommendations,
    )
    LexemesOutcome.NotFound,
    LexemesOutcome.NoLexemes,
    -> WordLookupOutcome.NotFound
    is LexemesOutcome.Denied ->
        WordLookupOutcome.Denied(outcome.reason)
    LexemesOutcome.Unavailable ->
        WordLookupOutcome.Unavailable
    LexemesOutcome.WordFailed ->
        WordLookupOutcome.WordFailed
}
```

**Мотивация.** Операция отдаёт одно слово (Д114); лемме словаря нужны
дата и лексемы для карточки «в словаре» (Д120, AWP-46).

### Зеркала и транспорт

**Было.** `core-network-api/…/CoreNetworkApi.kt`:

```kotlin
suspend fun lookup(
    request: LookupRequestApiEntity,
    runId: Long,
): LookupResult
```

`LookupRequestApiEntity(learningLanguage, translationLanguage,
tokens, components)`; `LookupResult.Success(attribution, words,
recommendations)`; `CandidateLexemeApiEntity` без `inContext`.
`core-network-impl`: `LexemeService.lookup` с
`@POST("v1/words/lookup")`; `net/LookupNet.kt` — `LookupRequestNet`,
`TokenNet`, `LookupResponseNet`, `WordResultNet`, `LemmaNet`,
`CandidateLexemeNet`, …; `mapper/LookupMappers.kt` — `toNet()` и
`toApiEntity()`; `WordsApiImpl.lookup`.

**Будет.** Зеркала — `hld-awp.md`, «Шлюз»; у
`CandidateLexemeApiEntity.inContext` умолчание `false`. Транспорт в
`net/LexemesNet.kt`, умолчания — как у соседей: отсутствующее поле
разбор не роняет:

```kotlin
// LexemesRequestNet — тело запроса по контракту: context
// null → поля нет (contract 1.1), components null → поля
// нет (LFL-78.7). Новый, рядом с LookupRequestNet до Э7.
@Serializable
internal data class LexemesRequestNet(
    val learningLanguage: String,
    val translationLanguage: String,
    val lemma: String,
    val context: LemmaContextNet? = null,
    val components: List<ComponentNet>? = null,
)

// LemmaContextNet — текст снимка и перевод; перевода
// нет — null (contract 1.2). Новый.
@Serializable
internal data class LemmaContextNet(
    val text: String,
    val translation: String?,
)

// LexemesResponseNet — одно слово вместо списка;
// рекомендаций нет — пусто. Новый, рядом с
// LookupResponseNet до Э7.
@Serializable
internal data class LexemesResponseNet(
    val attribution: String,
    val word: WordResultNet,
    val recommendations:
        List<ComponentRecommendationNet> = emptyList(),
)
```

`CandidateLexemeNet` — поле `inContext: Boolean = false`; `TokenNet`
остаётся у `LemmasRequestNet`. `LexemeService.lexemesFromLemma` с
`@POST("v1/lexemes/from-lemma")` рядом с `lookup`,
`WordsApiImpl.lexemesFromLemma` через тот же `call`. Мапперы провода —
поле в поле, как были. `lookup`, `LookupRequestNet`,
`LookupResponseNet` и их мапперы уходят в Э7.

**Мотивация.** Маршрут один (Д114), старый не держится (Д46); разбор
кодов отказа общий для всех операций; этап добавляет, не ломая
сборку.

### Мапперы в `app`

**Было.** `app/…/mapper/LookupMappers.kt` — `lookupRequestOf(
dictionary, tokens, components)`, `LookupResult.toDomain(dictionaryId)`:

```kotlin
fun LookupResult.toDomain(
    dictionaryId: Long,
): LookupOutcome = when (this) {
    is LookupResult.Success -> when {
        words.isEmpty() -> LookupOutcome.NoWords
        else -> LookupOutcome.Words(
            dictionaryId = dictionaryId,
            words = words.map { it.toDomain() },
            recommendations = recommendations
                .mapNotNull { it.toDomain() },
        )
    }
    is LookupResult.Denied ->
        LookupOutcome.Denied(reason.toDomain())
    is LookupResult.Unavailable -> LookupOutcome.Unavailable
}
```

**Будет.** Файл `LexemesMappers.kt`: `lexemesRequestOf(dictionary,
lemma, context, components)` — `hld-wcl.md`, «Юзкейсы»; исход:

```kotlin
// toDomain — исход слоя сети → исход значений леммы:
// not_found → не найдено; error → слово «ошибка»; found
// без лексем ни у одной леммы → значений нет (AWP-55.7);
// иначе найдено. Новая, вместо LookupResult.toDomain.
fun LexemesResult.toDomain(): LexemesOutcome = when (this) {
    is LexemesResult.Success -> word.toOutcome(
        recommendations.mapNotNull { it.toDomain() },
    )
    is LexemesResult.Denied ->
        LexemesOutcome.Denied(reason.toDomain())
    is LexemesResult.Unavailable ->
        LexemesOutcome.Unavailable
}

private fun WordApiEntity.toOutcome(
    recommendations: List<ComponentRecommendation>,
): LexemesOutcome = when {
    status == WordStatus.NOT_FOUND ->
        LexemesOutcome.NotFound
    status == WordStatus.ERROR -> LexemesOutcome.WordFailed
    lemmas.none { it.lexemes.isNotEmpty() } ->
        LexemesOutcome.NoLexemes
    else -> LexemesOutcome.Found(
        toDomain(), recommendations,
    )
}
```

`WordApiEntity.toDomain()` и остальные — как были, у лексемы плюс
`inContext`; `RecognizedWord.toDomain()`, `LookupToken.toApiEntity()`
остаются у лемм из токенов. `lookupRequestOf`, `LookupResult.toDomain`
и файл `LookupMappers.kt` живут до Э7.

**Мотивация.** Граница «сеть → домен» одна (Д57); статус слова
становится исходом здесь, хендлер экрана исходы только маппит.

## Э7. Юзкейсы в `app` и снятие старой операции

ВЫПОЛНЕН 2026-10-28. Как в плане; разошлось: файлы старой операции не
чистились, а переименованы по тому, что в них осталось —
`LookupRequestApiEntity.kt` → `RequestEntities.kt` (токен и дерево),
`LookupResult.kt` → `DenyReason.kt`, `net/LookupNet.kt` → `WordNet.kt`,
`mapper/LookupMappers.kt` провода → `WordMappers.kt`; в `app` мапперы
слова ответа переехали в `LexemesMappers.kt`, `RecognizedWord.toDomain`
и `LookupToken.toApiEntity` — в `LemmasMappers.kt`, тест
`LookupMappersTest` разошёлся по `LexemesMappersTest` и
`LemmasMappersTest`; `withExistingOf` сравнивает леммы по ключу текста
(`wordTextKey`), её тест — в `ScanCandidatesUseCaseImplTest` (AWP-43_7):
в тестах домена нет корутин; у загрузчика формы `FormShape` (первое
ядро, ядра, все компоненты) вместо `TextCores`, `lemmasOf` — с
группами, как у формы снимка; в `WordsApiImplTest` отказы по причине
идут под `WCL-11_2`/`WCL-11_6` — тезиса LFL-63 в спеке v2 нет, тесты
`LFL-20_*`, `LFL-49_1` и записи старых ответов сняты, в карточке слова
`WCL-03_*` → `LFL-104_1`, `LFL-104_2`, `WCL-18_2…18_4`; действие
`ToggleNotAdded` снято из общих виджетов здесь же (Э8 его больше не
знает).

Тезисы: AWP-01 (словарь снимка у запроса по слову), AWP-18.10,
AWP-43.7 (леммы словаря с текстом леммы ответа), AWP-46.1, AWP-46.4
(форма несёт лексемы лемм), LFL-103.1, LFL-103.2, LFL-104.1–104.3,
WCL-01.3, WCL-18.2–18.4; LFL-21, LFL-22, LFL-23, ACC-19.1, ACC-05.1
— прежние тесты шлюза на новой операции. Проверка:
`app/src/test/…/ScanCandidatesUseCaseImplTest.kt` — `LFL-103_1`,
`LFL-103_2`, `AWP-01_1` (запрос по словарю снимка, не текущему),
`AWP-43_7` (ответ «child» на запрос «children» → с исходом леммы
словаря «child»), `AWP-18_10` и `AWP-46_1`, `AWP-46_4` у тестов
`loadForm` (дата добавления, лексемы со значениями, слово без лексем);
`…/WordCardUseCaseImplTest.kt` — `LFL-104_1 WCL-18_3` (бывший
`WCL-03_1 WCL-03_2 WCL-03_3`), `WCL-18_4` (бывший `WCL-03_4`),
`WCL-18_2` («give up»: пробел внутри остаётся), `WCL-01_3`, у
`WCL-01_1 WCL-03_5 LFL-78_5` снят `WCL-03_5`;
`core-network-impl/…/WordsApiImplTest.kt` — `LFL-21_1`, `LFL-21_2`,
`LFL-22_1`–`LFL-22_3`, `LFL-23_1`–`LFL-23_5`, `ACC-19_1`, `ACC-05_1`
переписаны на `lexemesFromLemma`, `LFL-63_*` — на `LFL-23_*` по смыслу
отказа, `LFL-20_*`, `LFL-49_1` и ресурсы `lookup_response_*.json` сняты;
`LookupTest.kt` домена — четыре старых теста сняты;
`candidates/…/OutcomesTest.kt` — `LexemesOutcome.describe`. После
этапа: `:core:core-network-impl`, `:modules:domain:lexeme`,
`:modules:widget:candidates`, `:modules:screen:wordcard`,
`:modules:screen:scancandidates`, `:app` зелёные, старой операции в
коде нет (`grep -rn "words/lookup\|LookupOutcome\|lookupRequestOf"`
пуст).

Допущение: словарь снимка удалён между окном и тапом — исход «сервер
не ответил», без своего случая: при модальном окне это не случается.

### Порт экрана и реализация

**Было.** `scancandidates/deps/ScanCandidatesUseCase.kt` — четыре
метода: `recognize`, `releaseImageAccess`, `lemmasFromTokens`,
`loadForm`. `ScanCandidatesUseCaseImpl` без дерева компонентов и
`WordsBatchApi` — ушли в Э2. В родительской ветке `IS536_llm_values`
было:

```kotlin
override suspend fun addWords(
    batch: WordsBatch,
): AddWordsBatchOutcome = wordsBatchApi.addWordsBatch(batch)
```

**Будет.** Порт — `hld-awp.md`, «Юзкейсы». Реализация:

```kotlin
// Ш6, Ш8, LFL-103: словарь снимка по id (AWP-01) и его
// дерево компонентов, запрос леммой с контекстом, исход
// слоя сети в домен; лемма ответа не та, что просили, —
// с исходом леммы словаря с её текстом (AWP-43.7);
// словаря уже нет — «не ответил».
override suspend fun lexemesFromLemma(
    dictionaryId: Long,
    lemma: String,
    context: LemmaContext,
): LexemesOutcome {
    val dictionary = dictionaryApi
        .getDictionaryById(dictionaryId)
        ?: return LexemesOutcome.Unavailable
    val request = lexemesRequestOf(
        dictionary,
        lemma = lemma,
        context = context,
        components = componentTree.treeOf(dictionary.id),
    )
    val runId = lastRunId.incrementAndGet()
    val outcome = wordsApi
        .lexemesFromLemma(request, runId)
        .toDomain()
    return outcome.withExistingOf(lemma) { text ->
        formLoader.lemmasOf(dictionary, listOf(text))
    }
}

// withExistingOf — у найденного слова лемма ответа с
// другим текстом, чем просили (ключ текста, AWP-39) →
// исход с леммами словаря по этому тексту; иначе как
// есть. Новая, в домене рядом с LexemesOutcome.
suspend fun LexemesOutcome.withExistingOf(
    requested: String,
    load: suspend (String) -> List<ExistingLemma>,
): LexemesOutcome

// Ш9, AWP-20: пакет одного слова одной транзакцией;
// исключение пролетает к эффекту.
override suspend fun addWord(
    batch: WordsBatch,
): AddWordsBatchOutcome = wordsBatchApi.addWordsBatch(batch)
```

Зависимости `componentTree: ComponentTreeLoader` и `wordsBatchApi:
CoreDbApi.WordsBatchApi` возвращаются в конструктор и
`ScanCandidatesModule`.

**Мотивация.** Запрос идёт по словарю снимка, а не текущему (AWP-01);
запись — тем же `WordsBatchApi`, что у диалога карточки слова.

### Загрузчик формы

**Было.** `app/…/candidates/CandidatesFormLoader.kt`:

```kotlin
private fun TermApiEntity.toExistingLemma(
    cores: TextCores,
    groupNames: List<String>,
): ExistingLemma = ExistingLemma(
    wordId = word.id,
    text = word.value,
    lexemeCount = lexemes.size,
    coreValues = coreValues(cores.firstId),
    lexemeTextKeys = coreTextKeys(cores.ids),
    groupNames = groupNames,
)
```

`TermApiEntity` несёт `word.addDate` и `lexemes` с компонентами
(`type`, `data`, `removedAt`).

**Будет.** Два новых поля:

```kotlin
    addedAt = word.addDate.time,
    lexemes = savedLexemes(componentIds),
```

```kotlin
// savedLexemes — лексемы слова с живыми значениями
// компонентов формы, по id типов, для карточки слова
// «в словаре» (AWP-46); выключенные компоненты не идут.
// Новая.
private fun TermApiEntity.savedLexemes(
    componentIds: Set<Long>,
): List<SavedLexeme> = lexemes.map { lexeme ->
    SavedLexeme(
        id = lexeme.id,
        components = lexeme.components
            .filter { it.removedAt == null }
            .filter { it.type.id in componentIds }
            .map { BatchComponent(it.type.id, it.data) },
    )
}
```

`componentIds` — id компонентов формы, считаются один раз на вызов
`formOf`; у `formForWord` диалога — так же. Новый публичный
`lemmasOf(dictionary, texts): List<ExistingLemma>` — леммы словаря с
этими текстами, тот же `existingLemmasOf`, для юзкейса по лемме
ответа (AWP-43.7). Все конструкторы `ExistingLemma` — здесь же:
`candidates/…/MappersTest.kt:30, 192`,
`wordcard/…/DatasourceEffectHandlerTest.kt:439, 610`,
`scancandidates/…/DomainFixtures.kt:59`.

**Мотивация.** Д120, AWP-46: дата и сохранённые лексемы приходят с
формой, второго похода в базу по тапу нет, кроме леммы ответа вне
списков.

### Карточка слова леммой

**Было.** `app/…/wordCard/WordCardUseCaseImpl.kt`, строки 78–88:

```kotlin
val token = LookupToken(
    text = text,
    confidence = 1f,
    language = dictionary.learningLanguage,
)
val components = componentTree.treeOf(dictionary.id)
val request = lookupRequestOf(
    dictionary, listOf(token), components,
)
val outcome = wordsApi
    .lookup(request, runId)
    .toDomain(dictionaryId = dictionary.id)
return wordLookupOf(outcome)
```

**Будет.** `hld-wcl.md`, «Юзкейсы»: лемма — текст слова без краёв,
контекст null.

**Мотивация.** Д114, Д46: диалог живёт на той же операции.

### Снятие старой операции

**Было.** Последние вызовы старой операции — `WordCardUseCaseImpl`
(выше), `candidates/Outcomes.kt:28–34`:

```kotlin
fun LookupOutcome.describe(): String = when (this) {
    is LookupOutcome.Words ->
        "dict=$dictionaryId words=${words.size} " +
            "lemmas=${words.sumOf { it.lemmas.size }} " +
            "recommendations=${recommendations.size}"
    LookupOutcome.NoWords -> "no words"
    is LookupOutcome.Denied -> "denied=$reason"
    LookupOutcome.Unavailable -> "unavailable"
}
```

и порт экрана снимка в тестах — `scancandidates/src/test/…/FakeUseCase.kt:14–41`
реализует четыре метода.

**Будет.** Сняты вместе: `WordsApi.lookup`, `LookupRequestApiEntity`,
`TokenApiEntity` остаётся у лемм, `LookupResult`,
`LexemeService.lookup`, `LookupRequestNet`, `LookupResponseNet` и их
мапперы, `WordsApiImpl.lookup`; в домене `LookupOutcome` и старая
`wordLookupOf`; в `app` `lookupRequestOf`, `LookupResult.toDomain`,
файл `LookupMappers.kt` (`RecognizedWord.toDomain`,
`LookupToken.toApiEntity` переезжают в `LemmasMappers.kt`);
`LookupOutcome.describe` → `LexemesOutcome.describe()` в `Outcomes.kt`
(`found lemmas=… existing=…`, `not found`, `no lexemes`, `denied=…`,
`unavailable`, `word failed`). `FakeUseCase` экрана снимка получает
`lexemesFromLemma` и `addWord` с настраиваемыми исходами.

**Мотивация.** Снятие старого — там, где уходит последний вызов;
модули собираются после каждого этапа.

## Э8. Виджеты карточки `widget/candidates`

ВЫПОЛНЕН 2026-10-28. Как в плане; разошлось: `inContext` у
`LookupLexemeUi` и `isInContext` у `CardLexemeUiItem` — с умолчанием
`false`: диалог карточки слова лексем «в тексте» не знает;
`SavedLexemeUi` лежит в `FormUi.kt` рядом с `ExistingLemmaUi`;
`inCardOrder` объявлена на списке значений, обёртка на лексеме — рядом;
`SavedLexeme.toUi()` без формы: значения по компонентам в порядке первого
появления, порядок карточки ставит `savedLexemesOf` через `inCardOrder`;
пометка «уже в словаре» у `LemmaCardWidget` — параметром `existsMarkRes`,
у диалога остаётся `photo_add_exists_mark`; тест маппера —
`SavedLexemeMappersTest`, не `FormMappersTest`; `AWP-18_11` (подсказка о
группе) — у редьюсера экрана в Э9, не у чистых функций; строки Э9
(`scan_*`) добавлены здесь же; ожидания `CandidateValuesTest` и
`WordsBatchesTest` переставлены под порядок карточки — «Толкование»
перед «Примером».

Тезисы: AWP-18 (18.1–18.4, 18.7, 18.9–18.11 — чистые функции),
AWP-43.4, AWP-43.7 (карточка леммы из слова ответа), AWP-44 (порядок
значений в карточке), AWP-46.3, AWP-50 (50.1, 50.3–50.6, 50.13 —
сравнение по ядру), AWP-45 (подпись источника). Проверка:
`modules/widget/candidates/src/test/…/LemmaCardsTest.kt` —
`AWP-18_1`, `AWP-18_9`, `AWP-18_10`, `AWP-18_11` на `targetRowsOf`,
`AWP-18_2`, `AWP-18_3`, `AWP-18_4`, `AWP-18_7` на `emptyLemmaCard`,
`AWP-43_4`, `AWP-43_7`, `AWP-44_1`, `AWP-44_2`, `AWP-44_3` на
`lemmaCardOf` и `inCardOrder`, `AWP-46_3`, `AWP-50_1`, `AWP-50_3`,
`AWP-50_4`, `AWP-50_5`, `AWP-50_6`, `AWP-50_13` (адреса v1 `AWP-07_*`
у сравнения переименованы); `…/mapper/FormMappersTest.kt` — `AWP-46_1`
(значения сохранённой лексемы по компонентам формы). После этапа:
`:modules:widget:candidates` и `:modules:screen:wordcard` зелёные —
диалог карточки слова зовёт те же функции.

Допущение: дата в листе «куда класть» — короткий формат локали
(`DateFormat.getDateInstance(SHORT)`), как в спеке «12.10.2026» на
русской локали; отдельного тезиса о формате нет.

### UI-типы и мапперы

**Было.** `FormUi.kt`:

```kotlin
data class ExistingLemmaUi(
    val wordId: Long,
    val text: String,
    val lexemeCount: Int,
    val coreValues: List<String>,
    val lexemeTextKeys: Set<String>,
    val groupNames: List<String>,
)
```

`CandidatesUi.kt` — `CardLexemeUiItem` без `isInContext`;
`LookupUi.kt` — `LookupLexemeUi` без `inContext`;
`mapper/FormMappers.kt`, `mapper/LookupMappers.kt` — поле в поле.

**Будет.** Дельты типов и `SavedLexemeUi` — `hld-awp.md`, «Стейт»,
блок типов виджетов. Маппер сохранённой лексемы:

```kotlin
// toUi — значения сохранённой лексемы по компонентам
// формы в её порядке: TextValues и CaptionedTextValues →
// Text, ChoiceValues → Choice; компонент без значения
// пропускается (AWP-46). Новая.
fun SavedLexeme.toUi(form: DictionaryForm): SavedLexemeUi
```

Пометка `exists` у лексемы-кандидата теперь зовётся «уже в словаре»:
поле то же, строка `photo_add_exists_mark` меняет текст.

**Мотивация.** Карточка «в словаре» рисует сохранённые лексемы тем же
виджетом, что и кандидатов (AWP-42.3, AWP-46).

### Чистые функции карточки

**Было.** `LemmaCards.kt`:

```kotlin
fun buildLemmaCards(
    words: List<LookupWordUi>,
    form: DictionaryFormUi,
): CardsBuildUi

private fun newCard(
    lemma: LookupLemmaUi,
    word: LookupWordUi,
    form: DictionaryFormUi,
): LemmaCardUiItem

fun List<LemmaCardUiItem>.withExistsMarks(
    form: DictionaryFormUi,
): List<LemmaCardUiItem>

fun List<LemmaCardUiItem>.withExistsMarks(
    keys: Set<String>,
    form: DictionaryFormUi,
): List<LemmaCardUiItem>
```

`toCardLexeme` не знает про «в тексте»; `CardsBuildUi`,
`NotAddedWordUiItem`, `NotAddedReason` — для `buildLemmaCards`,
`CandidatesView.notAdded` и `isNotAddedExpanded`
(`CandidateAction.kt:80–81`), `notAddedReasonLabel`
(`CandidatesLabels.kt:173–177`); `newCard` зовут `buildLemmaCards` и
`buildCandidateCards` (`LemmaCards.kt:91`); `valuesOf`
(`CandidateValues.kt:34–38`) кладёт значения в порядке дерева формы;
строку леммы в листе «куда класть» собирает composable
(`CandidatesSheets.kt:217–239`).

**Будет.** Сигнатуры — `hld-awp.md`, «Чистые функции», пакет виджетов.
`newCard` остаётся приватным у `buildCandidateCards`, `emptyLemmaCard`
— его публичный двойник без ответа. Тела:

```kotlin
// emptyLemmaCard — карточка леммы без лексем: леммы
// словаря с тем же текстом, «куда класть» — самая новая
// или «новое слово» (AWP-18.3), своя группа «как у всех».
// Новая, из newCard.
fun emptyLemmaCard(
    lemma: String,
    form: DictionaryFormUi,
): LemmaCardUiItem {
    val existing = form.existingOf(lemma)
    return LemmaCardUiItem(
        lemma = lemma,
        sourceWords = listOf(lemma),
        lexemes = emptyList(),
        existingCount = existing.size,
        target = existing.firstOrNull()
            ?.let { CardTargetUi.Existing(it.wordId) }
            ?: CardTargetUi.NewWord,
        ownGroup = CardOwnGroupUi.AsCommon,
    )
}

// lemmaCardOf — карточка из слова ответа: первая лемма с
// лексемами (по тапу она одна, LFL-107), лексемы по
// сопоставлению с «в тексте» и «англ.», пометка формы;
// «куда класть» и группа — от прежней карточки, если
// лемма та же, иначе по форме заново (AWP-43.7). Новая.
fun lemmaCardOf(
    word: LookupWordUi,
    prior: LemmaCardUiItem,
    form: DictionaryFormUi,
): LemmaCardUiItem {
    val lemma = word.lemmas
        .first { it.lexemes.isNotEmpty() }
    val base = when (lemma.text) {
        prior.lemma -> prior
        else -> emptyLemmaCard(lemma.text, form)
    }
    return base.copy(
        sourceWords = listOf(word.text),
        lexemes = lemma.lexemes.map {
            it.toCardLexeme(lemma, word, form)
                .inCardOrder(form)
        },
        formNote = lemma.formNote,
    )
}

// inCardOrder — лексема со значениями в порядке карточки
// под словом (AWP-44): ядра, затем параметры, затем
// «текст с подписью»; внутри части — порядок формы,
// зависимые сразу за своим (AWP-44.3). Диалог карточки
// слова её не зовёт: там порядок формы (WCL-20). Новая.
fun CardLexemeUiItem.inCardOrder(
    form: DictionaryFormUi,
): CardLexemeUiItem

// targetRowsOf — строки листа «куда класть» (AWP-18):
// по лемме словаря с тем же текстом, самая новая первой
// — значения ядра, число лексем, группы (пусто — «Все»),
// день добавления. Лист только рисует. Новая.
fun DictionaryFormUi.targetRowsOf(
    lemma: String,
): List<TargetRowUi> = existingOf(lemma).map {
    TargetRowUi(
        wordId = it.wordId,
        coreValues = it.coreValues,
        lexemeCount = it.lexemeCount,
        groupNames = it.groupNames,
        addedAt = it.addedAt,
    )
}

// savedLexemesOf — сохранённые лексемы леммы словаря из
// «куда класть»; «новое слово» — пусто (AWP-46). Новая.
fun DictionaryFormUi.savedLexemesOf(
    target: CardTargetUi,
): List<SavedLexemeUi> = when (target) {
    is CardTargetUi.Existing -> existingLemmas
        .firstOrNull { it.wordId == target.wordId }
        ?.lexemes
        .orEmpty()
    CardTargetUi.NewWord -> emptyList()
}

// lexemeKeysOf — ключи ядер лексем леммы словаря из
// «куда класть» для «уже в словаре» (AWP-50); «новое
// слово» — пусто. Новая.
fun DictionaryFormUi.lexemeKeysOf(
    target: CardTargetUi,
): Set<String>
```

`toCardLexeme` переносит `isInContext = lexeme.inContext`;
`withExistingLemmas(added)` — форма с леммами, которых в ней не было
(`hld-awp.md`). `buildLemmaCards`, `CardsBuildUi`, `NotAddedWordUiItem`,
`NotAddedReason`, `withExistsMarks(form)` по всем леммам текста,
`CandidatesView.notAdded` и `isNotAddedExpanded`, `notAddedReasonLabel`
со строками `photo_add_not_found`, `photo_add_error`,
`photo_add_no_lexemes` (после `grep`, что их больше никто не зовёт) —
сняты; `withExistsMarks(keys, form)`, `withCoreMarks`,
`withActiveValues`, `summaryOf`, `buildCandidateCards` и `newCard`
остаются у диалога карточки слова.

**Мотивация.** Карточка под словом — одна лемма, «уже в словаре» —
по выбранной лемме словаря, не по всем с тем же текстом (AWP-18.8,
AWP-50); порядок значений в карточке и строка листа — чистые функции,
UI их не считает.

### Виджет карточки и листы

**Было.** `ui/LemmaCardWidget.kt:51` — `internal fun LemmaCardWidget`:
заголовок с крестиком `CardHead` (`:100`, шлёт
`CandidateAction.DeleteCard(card.lemma)`), строка «in the text: …» из
`sourceWords`, лексемы со значениями, крестики, пометки «англ.»,
«есть» (`Marks`, `:362–374`, `photo_add_exists_mark`), «не попадёт в
ядро», «изменено», строки «Куда:» и «Группа:»;
`ui/CandidatesSections.kt:32, 66` — `internal fun RemarksSection`,
`internal fun ComponentMatchesSection(view, onAction)`, заголовок
шлёт `CandidateAction.ToggleComponentMatches` (`:77`);
`ui/CandidatesPanel.kt:168, 195` — `internal fun CloseConfirmDialog`
без числа слов, `internal fun Attribution`;
`ui/CandidatesLabels.kt:181` — `internal fun failureLabel`;
`ui/CandidatesSheets.kt:209–239` — `private fun CardTargetSheet`
собирает строку леммы сам: `joinToString`, `lexemesCount`, `if
(existing.groupNames.isEmpty())`; строки
`photo_add_target_existing` «into «%1$s»: %2$s»,
`photo_add_target_groups` «groups: %1$s»,
`photo_add_target_no_groups` «no groups»,
`photo_add_target_group_hint`.

**Будет.** Видимость: `LemmaCardWidget`, `RemarksSection`,
`ComponentMatchesSection`, `Attribution`, `failureLabel` и новый
`SavedLexemeRows` — публичные, как `CommonGroupHeader` в Э3; у
`ComponentMatchesSection` вместо `onAction` — `onToggle: () -> Unit`
(диалог передаёт `ToggleComponentMatches`, окно — своё действие);
у `LemmaCardWidget` крестик карточки — параметр `onDeleteCard: (()
-> Unit)?`, `null` — крестика нет (у окна «Удалить» у слова), и
параметр текста пометки `exists` — у диалога по-прежнему
`photo_add_exists_mark` «есть», у окна `scan_exists_mark` «уже в
словаре». У лексемы с `isInContext` пометка «в тексте»
(`scan_in_context`). Новый composable в том же файле:

```kotlin
// SavedLexemeRows — сохранённые лексемы леммы словаря под
// заголовком «В словаре»: значения как у кандидата, без
// крестиков и правки (AWP-46). Новый.
@Composable
fun SavedLexemeRows(
    saved: List<SavedLexemeUi>,
    form: DictionaryFormUi,
)
```

`CardTargetSheet` рисует `form.targetRowsOf(lemma)`: значения через
«, », число через `lexemesCount`, группы или «Все», дата —
`DateFormat.getDateInstance(SHORT)` по `addedAt`. Строки, в `values`
(английский) и `values-ru-rRU`: `photo_add_target_existing` →
«%1$s · %2$s · %3$s · %4$s» (без перевода), `photo_add_target_groups`
снята, `photo_add_target_no_groups` → «All» / «Все», новые
`scan_in_context` «in the text» / «в тексте», `scan_exists_mark`
«already in dictionary» / «уже в словаре», `scan_saved_title` «In the
dictionary» / «В словаре»; `CloseConfirmDialog` не используется окном
— вопрос с числом рисует окно само (Э9).

**Мотивация.** AWP-43, AWP-46, AWP-50, AWP-18; диалог карточки слова
не меняется (WCL-06 «есть», `hld-wcl.md`).

### Тесты модуля

**Было.** `src/test/…/Fixtures.kt:659–666` — `Fx.cards()` собирает
карточки через `buildLemmaCards(…)` и `withExistsMarks(form)`; её
зовут `WordsBatchesTest`, `CandidateValuesTest`, `RemarksTest`,
`CardTransitionsTest`, `LemmaCardsTest`; прямые вызовы
`buildLemmaCards` — `CardTransitionsTest.kt:69`,
`LemmaCardsTest.kt:22, 399, 421`; адреса v1 `AWP-04_*`, `AWP-12_*`
(сборка по списку слов, «не добавить»), `AWP-07_*` («есть»);
`RemarksTest` — `AWP-30_1 unclear component of two words`,
`AWP-31_1`.

**Будет.** `Fx.cards()` — по слову: `emptyLemmaCard` →
`lemmaCardOf` → `withExistsMarks(keys, form)` → `withCoreMarks`;
тесты многословной сборки с адресами `AWP-04_*`, `AWP-12_*` сняты;
`AWP-07_*` → `AWP-50_*`; `RemarksTest`: `AWP-30_1` → `AWP-30_15`
(одно слово), `AWP-31_1` → `AWP-31_6`; `WordsBatchesTest` —
`AWP-20_2`, `AWP-20_3`, `AWP-20_4`, `AWP-20_7` как были, плюс
`AWP-32_13` («sister», три «Пример» с подписью); `CandidateValuesTest`
— `AWP-29_*`, `AWP-32_*` как были; `CardTransitionsTest` —
`AWP-15_*`, `AWP-16_*`, `AWP-17_*`, `AWP-11_*` как были на карточке
одного слова.

**Мотивация.** Фикстура — через те же функции, что и экран; адрес
закрывает строку v2.

## Э9. Экран снимка: карточка под словом

ВЫПОЛНЕН 2026-10-28. Как в плане и `hld-awp.md`; разошлось: у списков,
кроме `updateLemmaCards`, появилась `updateCards` — шаг над карточками
слов целиком, на ней `refreshLemmaSaved`, `refreshLemmaMarks`,
`refreshAllCards`, `resetDeadGroup`, `resetDeadTarget`, `dropComponent`
(помощник `lemmaCardsStep` рядом с `lemmaStep`); `refreshAllCards` заново
считает и «есть в словаре: N» у карточки — после удалённой леммы словаря
счёт иначе врал; `startAddingWord` — no-op и без лексем на запись (все
«уже в словаре» или удалены), кнопка в этом случае неактивна по `canAdd`;
`showCardNotFound`, `showCardError`, `showCardRetryableError` — no-op без
карточки: слово удалили, пока шёл запрос (AWP-48.4); `closeCloseConfirm`
обнуляет число слов вопроса; `openSavedCard`, `toggleCard` и прочие
атомы слова — no-op без слова в списках; UI: пока у слова `isAdding`,
действия его карточки (правки, листы «куда класть» и группы) глушатся
в `CandidatesBlock` — AWP-47.8 закрывается ручником, у редьюсера
условия нет; `CloseQuestion` живёт в `ScanWordListsContent.kt`;
`AWP-47_8` в `CardEditsReducerTest` не писался, `AWP-18_11` — в нём же
(подсказка о группе через `groupIdOr`); `AWP-43_6` и `AWP-14_3` в
сценариях — повторное чтение стейта цикла при загрузке и после правки,
запрос один; фикстуры ответов по лемме вернулись из первой версии с
`inContext` у первой лексемы; `ScanCandidatesAtomsTest` вместо тестов «по
одному» на каждый атом — по тесту на группу атомов с их no-op.

Тезисы: AWP-02, AWP-03, AWP-08.1, AWP-08.3–08.6, AWP-11, AWP-14.3,
AWP-15, AWP-16, AWP-17, AWP-18.5, AWP-18.6, AWP-18.8, AWP-20.2–20.5,
AWP-20.7, AWP-21.3–21.6, AWP-26, AWP-29–AWP-31, AWP-42, AWP-43,
AWP-44, AWP-45, AWP-46, AWP-47, AWP-48, AWP-49, AWP-50.12, AWP-51,
AWP-55, AWP-57, AWP-61, LFL-103 (эффект). Проверка в
`modules/screen/scancandidates/src/test/…/mate/`:
`ScanCandidatesAtomsTest.kt` — новые атомы по одному;
`WordCardReducerTest.kt` (новый) — `AWP-42_1`, `AWP-42_2`,
`AWP-42_4`, `AWP-42_5`, `AWP-42_6`, `AWP-43_1`, `AWP-43_4`,
`AWP-43_7`, `AWP-46_1`, `AWP-46_4`, `AWP-47_1`, `AWP-47_2`,
`AWP-47_3`, `AWP-47_6`, `AWP-47_7`, `AWP-48_1`, `AWP-48_3`,
`AWP-48_4`, `AWP-49_1`, `AWP-49_4`, `AWP-49_5`, `AWP-50_12`,
`AWP-55_1`, `AWP-55_7`, `AWP-57_1`, `AWP-57_2`, `AWP-57_3`,
`AWP-61_7`, `AWP-18_3`, `AWP-18_8`; `CardEditsReducerTest.kt`
(новый) — `AWP-15_1`, `AWP-15_2`, `AWP-15_3`, `AWP-15_7`, `AWP-15_8`,
`AWP-16_1`, `AWP-16_2`, `AWP-16_3`, `AWP-16_5`, `AWP-16_6`,
`AWP-16_7`, `AWP-17_1`, `AWP-17_2`, `AWP-17_3`, `AWP-17_5`,
`AWP-17_6`, `AWP-17_7`, `AWP-11_1`, `AWP-11_5`, `AWP-11_9`,
`AWP-31_3`, `AWP-31_4`, `AWP-08_4`, `AWP-08_5`, `AWP-08_6`,
`AWP-18_5`, `AWP-18_6`, `AWP-47_8`; `AddWordReducerTest.kt` (новый) —
`AWP-02_1`, `AWP-02_2`, `AWP-03_1`, `AWP-03_2`, `AWP-03_3`,
`AWP-20_4`, `AWP-20_5`, `AWP-21_3`, `AWP-21_4`, `AWP-21_5`,
`AWP-21_6`, `AWP-26_1`, `AWP-26_2`, `AWP-47_4`, `AWP-62_1`,
`AWP-62_2`, `AWP-62_3`; `CloseReducerTest.kt` — `AWP-51_1`,
`AWP-51_3`, `AWP-51_4`, `AWP-51_5`, `AWP-51_7`, `AWP-51_8`,
`AWP-51_9`, `AWP-51_10`, `AWP-51_11`, `AWP-51_12` (запись идёт —
закрытие не действует), `AWP-60_2`; `DatasourceEffectHandlerTest.kt` —
`LFL-103_1` (аргументы юзкейса из эффекта: лемма, контекст, словарь
снимка), `AWP-55_1`, `AWP-55_7`, `AWP-61_1`, `AWP-61_2`, `AWP-61_3`,
`AWP-61_4`, `AWP-61_5`, `AWP-61_6`, `AWP-61_8`, `AWP-21_3`,
`AWP-21_4`, `AWP-21_6` (исход записи → сообщение);
`ScanCandidatesScenarioTest.kt` — `AWP-43_6`, `AWP-14_3` (стейт
переживает поворот), `AWP-48_4` (поздний ответ удалённого слова),
цепочка «тап → карточка → «Добавить» → строки нет»;
`ScanWordListsTest.kt` — `wordOf`, `updateWord`, `removeWord`,
`countPending`; `ui/CandidateActionsTest.kt` и новый
`ui/WordActionsTest.kt` — по спискам действий. Чистые функции
`remarksOf`, `componentMatchLinesOf`, `valuesOf`, `wordsBatchOf` —
тесты виджетов, Э8 (AWP-29, AWP-30, AWP-31, AWP-32, AWP-20.2,
AWP-20.3, AWP-20.7). После этапа: `:modules:screen:scancandidates`,
`:modules:widget:candidates`, `:modules:screen:wordcard` зелёные,
`:app:lintDebug` чист.

Допущения: адрес слова — текст; правки карточки адресуются леммой и
ложатся во все карточки этой леммы; у тапа три сообщения от UI по
видимому состоянию строки; условия в редьюсере только в ветке
закрытия — запись идёт (AWP-51.12) и есть что терять (AWP-51).

### Стейт

**Было.** `mate/State.kt` — `ScanCandidatesState` с полями `uri`,
`phase`, `tokens`, `dictionaryId`, `text`, `translation`,
`isTranslationExpanded`, `lists`, `errorRes`, `hasRetryButton`,
`form`, `commonGroupId`, `sheet`; `ScanWordUiItem(text,
isInDictionary)`; `toView()` с пустыми карточками.

**Будет.** Дельта стейта, `WordCardUi`, `CardPhase` — `hld-awp.md`,
«Стейт». `toView()`:

```kotlin
// toView — вид для общих виджетов: форма, общая группа,
// лист, карточки лемм всех слов для листов правки и
// «куда класть», записи и вопросы — по стейту.
// Меняется: карточки и флаги из слов.
fun ScanCandidatesState.toView(): CandidatesView =
    CandidatesView(
        form = form,
        cards = lists.allLemmaCards(),
        summary = AddSummaryUi(),
        sheet = sheet,
        isAdding = isAdding,
        addFailure = null,
        isConfirmingClose = isConfirmingClose,
        commonGroupId = commonGroupId,
    )
```

**Мотивация.** Карточка живёт у слова, листы общих виджетов ищут
значение по адресу среди карточек всех слов.

### Сообщения и эффекты

**Было.** `mate/Message.kt` — 16 сообщений этапа 1; `mate/Effects.kt`
— `RecognizeText`, `ExtractLemmas`, `LoadForm`. В родительской ветке
`IS536_llm_values` — сообщения карточек, правок, записи и вопросов,
эффект `AddWords(batch)`.

**Будет.** Сообщения и эффекты — `hld-awp.md`, «Редьюсер» (над
ветками) и блок эффектов. Объявления новых сообщений:

```kotlin
// ===== От экрана: слово =====

/** Тап по новому слову без карточки, «Проверить по
 *  тексту» или «Повторить» у слова: запрос значений. */
data class LoadLexemes(val text: String) : Msg

/** Тап по слову «в словаре» без карточки. */
data class OpenSavedCard(val text: String) : Msg

/** Повторный тап по слову с карточкой (AWP-42). */
data class ToggleCard(val text: String) : Msg

data class DeleteWord(val text: String) : Msg

data class AddWord(val text: String) : Msg

data class RetryAdd(val text: String) : Msg

data class ToggleComponentMatches(val text: String) : Msg

// ===== От эффектов: значения слова =====

data class LexemesFound(
    val text: String,
    val word: LookupWordUi,
    val recommendations: List<ComponentRecommendationUi>,
    val existing: List<ExistingLemmaUi>,
) : Msg

data class LexemesNotFound(
    val text: String,
    @param:StringRes val textRes: Int,
) : Msg

data class LexemesDenied(
    val text: String,
    @param:StringRes val errorRes: Int,
) : Msg

data class LexemesUnavailable(
    val text: String,
    @param:StringRes val errorRes: Int,
) : Msg

// ===== От эффектов: запись =====

data class WordAdded(val text: String) : Msg

data class AddGroupGone(
    val text: String,
    val failure: AddFailureUi.GroupMissing,
) : Msg

data class AddWordGone(
    val text: String,
    val failure: AddFailureUi.WordMissing,
) : Msg

data class AddComponentGone(
    val text: String,
    val failure: AddFailureUi.TypeUnavailable,
) : Msg

data class AddFailed(
    val text: String,
    val failure: AddFailureUi,
) : Msg
```

Сообщения правок `DeleteLexeme`, `DeleteValue`, `SubmitText`,
`ResetText`, `SetOption`, `UpdateDraft`, `SetCardTarget`,
`SetCardGroup`, `ConfirmClose`, `Stay` — как в первой версии.
Эффекты `LookupLemma` и `AddWord` — `hld-awp.md`; `onFail` —
`LexemesUnavailable(word, R.string.photo_words_error_server)` и
`AddFailed(word, AddFailureUi.Failed)`; `toString` счётчиками, как у
`ExtractLemmas`:

```kotlin
override fun toString(): String =
    "LookupLemma(word=$word, lemma=$lemma, " +
        "dictionaryId=$dictionaryId, text=${text.length})"

override fun toString(): String =
    "AddWord(word=$word, " +
        "lexemes=${batch.words.sumOf { it.lexemes.size }})"
```

**Мотивация.** Сообщение на каждый исход, адрес слова в каждом:
хендлер исходы маппит, редьюсер их не разбирает.

### Атомы

**Было.** `mate/ScanCandidatesAtoms.kt` — атомы этапа 1; в родительской
ветке — атомы карточек окна (`deleteCard`, `deleteLexeme`,
`deleteValue`, `applyTextEdit`, `dropEmptyTexts`, `resetText`,
`setOption`, `setCardTarget`, `setCardGroup`, `openSheet` с черновиком,
`updateSheetDraft`, `markDirty`, `startAdding`, `showAddFailure`,
`clearAddFailure`, `resetDeadGroup`, `resetDeadTarget`,
`dropComponent`, `askCloseConfirm`, `closeCloseConfirm`) над
`state.cards` через помощник:

```kotlin
private fun ScanCandidatesState.cardsStep(
    step: String,
    vararg attrs: Pair<String, Any?>,
    change: (List<LemmaCardUiItem>) ->
        List<LemmaCardUiItem>?,
): ScanCandidatesResult {
    val next = change(cards)
        ?: return noOp("$step: nothing to change")
    logStep(step, *attrs)
    return copy(cards = next).begin()
}
```

**Будет.** Атомы слова `openSavedCard`, `startLookup` (словарь снимка
из стейта в эффект, no-op без него), `applyLexemes` (леммы словаря из
исхода — в форму), `refreshCardValues`, `refreshCardMarks`,
`showCardNotFound`, `showCardError`, `showCardRetryableError`,
`toggleCard`, `deleteWord`, `toggleCardMatches`, `startAddingWord`,
`removeAddedWord`, `showAddFailure(text, failure)`,
`clearAddFailure(text)`, `askCloseConfirm` с числом — `hld-awp.md`,
«Редьюсер», блок атомов.
Атомы правок — из родительской ветки, над карточками леммы:

```kotlin
// lemmaStep — шаг над карточками леммы у всех слов: null
// от перехода — no-op с именем шага. Меняется: карточки в
// списках, адрес — лемма.
private fun ScanCandidatesState.lemmaStep(
    step: String,
    lemma: String,
    vararg attrs: Pair<String, Any?>,
    change: (LemmaCardUiItem) -> LemmaCardUiItem?,
): ScanCandidatesResult {
    val next = lists.updateLemmaCards(lemma, change)
        ?: return noOp("$step: nothing to change")
    logStep(step, "lemma" to lemma, *attrs)
    return copy(lists = next).begin()
}
```

`refreshLemmaValues(lemma)`, `refreshLemmaSaved(lemma)`,
`refreshLemmaMarks(lemma)` — те же пересчёты, что у слова, по всем
карточкам леммы; `refreshAllCards()` — по всем словам с карточкой.
`markDirty`, `startAdding`, `askAddConfirm`, `closeAddConfirm`,
`toggleNotAddedExpanded`, `closeWithResult` не возвращаются.

**Мотивация.** Переходы над карточкой — чистые функции виджетов, как
в первой версии; атом только адресует карточку и пишет лог.

### Редьюсер

**Было.** `mate/ScanCandidatesReducer.kt` — 16 веток, из них
`Msg.RequestClose -> state.close()`; `logName()` прячет
`TextRecognized`, `LemmasFound`, `FormLoaded`.

**Будет.** Ветки — `hld-awp.md`, «Редьюсер»; `logName()` прячет ещё
`LexemesFound`. Условия — только в ветке `RequestClose`: первой
строкой `state.isAdding → noOp` (AWP-51.12), затем
`lists.countPending()`; `reduceSubmitText`, `reduceResetText`,
`reduceSetOption` первой версии не нужны: флага правок нет.

**Мотивация.** Вопрос при закрытии считает слова, не правки (AWP-51);
запрет закрытия во время записи — правило спеки на стейте, тест
редьюсера; страховок от повторного нажатия нет — кнопки неактивны по
флагам.

### Хендлер

**Было.** `mate/DatasourceEffectHandler.kt` — три ветки; тексты отказа
через `errorResOf(reason)`, нейронка — `scan_analysis_unavailable`.

**Будет.** Две новые ветки — `hld-awp.md`, «Редьюсер», блок хендлера.
Помощник отказа на слове:

```kotlin
// deniedMsg — отказ по нейронке — «Сервер сейчас
// недоступен, попробуй позже» с «Повторить» (AWP-61.1);
// остальные — текст по причине без повтора (AWP-61.3,
// 61.4, 61.6, 61.8). Новый.
private fun deniedMsg(
    word: String,
    outcome: LexemesOutcome.Denied,
): Msg = when (outcome.reason) {
    LookupDenyReason.LLM_UNAVAILABLE ->
        Msg.LexemesUnavailable(
            word, R.string.scan_server_unavailable,
        )
    LookupDenyReason.NO_ACCESS_KEY,
    LookupDenyReason.UNAUTHORIZED,
    LookupDenyReason.RATE_LIMITED,
    LookupDenyReason.REQUEST_TOO_LARGE,
    LookupDenyReason.UNSUPPORTED_LANGUAGE,
    -> Msg.LexemesDenied(word, errorResOf(outcome.reason))
}
```

Строка лога исхода: `LexemesOutcome.describe()` в `Outcomes.kt`
виджетов вместо `LookupOutcome.describe()`.

**Мотивация.** Тексты и «Повторить» выбирает хендлер по причине, как
у ошибок до окна.

### Чистые функции списков

**Было.** `mate/ScanWordLists.kt` — `toUi`, `withDictionaryMarks`,
`allTexts`, `countInDictionary`, приватный `allItems`.

**Будет.** Плюс `wordOf`, `updateWord`, `removeWord`,
`updateLemmaCards`, `allLemmaCards`, `countPending`, `anyAdding` —
`hld-awp.md`, «Чистые функции». `countPending`:

```kotlin
// countPending — слова, чьи лексемы-кандидаты идут или
// пришли и есть что добавить (AWP-51): карточка
// «загрузка», или «лексемы-кандидаты» с лексемой без
// «уже в словаре». Новая.
fun ScanWordListsUi.countPending(): Int = allItems()
    .distinctBy { it.text }
    .count { it.card?.isPending() == true }

private fun WordCardUi.isPending(): Boolean = when (phase) {
    CardPhase.LOADING -> true
    CardPhase.CANDIDATES -> lemma.lexemes.any { !it.exists }
    CardPhase.SAVED,
    CardPhase.NOT_FOUND,
    CardPhase.ERROR,
    -> false
}
```

**Мотивация.** Списки — значение, функции над ними без состояния;
слово с одним текстом в двух списках считается один раз.

### Действия виджетов и окно

**Было.** `ui/CandidateActions.kt`:

```kotlin
internal fun CandidateAction.toMsg(): Msg = when (this) {
    is CandidateAction.OpenSheet -> Msg.OpenSheet(sheet)
    CandidateAction.CloseSheet -> Msg.CloseSheet
    is CandidateAction.SetCommonGroup ->
        Msg.SetCommonGroup(groupId)
    CandidateAction.RequestClose -> Msg.RequestClose
    is CandidateAction.DeleteCard,
    …
    CandidateAction.Stay,
    -> Msg.NoOperation
}
```

`ui/ScanWordListsContent.kt` — `WordRow(item)`: текст и пометка «в
словаре», без тапа и кнопок; `TopBar` с крестиком всегда активным.

**Будет.** `toMsg()`: правки, листы, «куда класть», своя группа,
`ConfirmClose` и `Stay` — в свои сообщения; `DeleteCard`,
`ToggleComponentMatches`, `ToggleLemmaWord`, `ToggleNotAdded`,
`RequestAdd`, `ConfirmAdd`, `CancelAdd`, `RetryAdd`, `RetryLookup` —
`NoOperation`. Действия строки слова — свой тип в
`ui/WordActions.kt`:

```kotlin
// WordAction — действия строки слова и кнопок карточки.
// Тап превращается в сообщение по видимому состоянию
// строки: карточка есть — свернуть или раскрыть; слово
// «в словаре» без карточки — сохранённые; иначе запрос.
// Новый.
internal sealed interface WordAction {
    data class Tap(val item: ScanWordUiItem) : WordAction
    data class Delete(val text: String) : WordAction
    data class CheckByText(val text: String) : WordAction
    data class RetryLookup(val text: String) : WordAction
    data class Add(val text: String) : WordAction
    data class RetryAdd(val text: String) : WordAction
    data class ToggleMatches(val text: String) : WordAction
}

internal fun WordAction.toMsg(): Msg = when (this) {
    is Tap -> when {
        item.card != null -> Msg.ToggleCard(item.text)
        item.isInDictionary -> Msg.OpenSavedCard(item.text)
        else -> Msg.LoadLexemes(item.text)
    }
    is Delete -> Msg.DeleteWord(text)
    is CheckByText -> Msg.LoadLexemes(text)
    is RetryLookup -> Msg.LoadLexemes(text)
    is Add -> Msg.AddWord(text)
    is RetryAdd -> Msg.RetryAdd(text)
    is ToggleMatches -> Msg.ToggleComponentMatches(text)
}
```

Окно: строка слова кликабельна, под раскрытой строкой блок карточки
по фазе — признак загрузки; `SavedLexemeRows` и «Проверить по
тексту» с «Удалить»; `LemmaCardWidget` без крестика карточки
(`onDeleteCard = null`), с пометкой `scan_exists_mark`, лексемами,
`RemarksSection`, `ComponentMatchesSection(onToggle =
WordAction.ToggleMatches)`, «куда класть», своя группа, `Attribution`,
«Добавить» (активна по `canAdd`, неактивна при `isAdding`) и
«Удалить»; текст «Не найдено» / «Значений нет» с «Удалить»; причина с
«Удалить» и, по флагу, «Повторить»; «не добавилось» (`failureLabel`)
с «Повторить». Крестик неактивен при `state.isAdding` по паттерну
кнопок, `BackHandler` шлёт `RequestClose` всегда — во время записи
его гасит редьюсер (AWP-51.12). Вопрос при закрытии — свой composable
окна `CloseQuestion(pendingWords, onStay, onClose)` по
`isConfirmingClose`: «Закрыть без добавления?», «Не добавлено слов:
N», «Остаться», «Закрыть». Листы правки, опций, «куда класть» и групп
— общие `CandidatesSheets` по `view.cards`.

**Мотивация.** UI рисует стейт и шлёт сообщения по видимому
состоянию строки; решений в composable нет.

### Строки

**Было.** `core-resources`: `photo_add_*` первой версии,
`scan_in_dictionary`, `scan_list_*`, `scan_translation`,
`scan_server_unavailable`, `scan_analysis_unavailable`.

**Будет.** Новые ключи, в `values` английский и в `values-ru-rRU`
русский: `scan_check_by_text` «Check against the text» / «Проверить
по тексту», `scan_in_context` «in the text» / «в тексте»,
`scan_exists_mark` «already in dictionary» / «уже в словаре»,
`scan_saved_title` «In the dictionary» / «В словаре», `scan_not_found`
«Not found» / «Не найдено», `scan_no_lexemes` «No meanings» /
«Значений нет», `scan_add` «Add» / «Добавить», `scan_delete_word`
«Delete» / «Удалить», `scan_close_pending` «Words not added: %1$d» /
«Не добавлено слов: %1$d». «Повторить» — прежний `photo_add_retry`,
вопрос и кнопки закрытия — прежние `photo_add_close_question`,
`photo_add_stay`, `photo_add_close`. Изменённые:
`photo_add_target_existing` → «%1$s · %2$s · %3$s · %4$s»,
`photo_add_target_no_groups` → «All» / «Все»; `photo_add_exists_mark`
не трогается — он у диалога. Снятые: `photo_add_target_groups`,
`photo_add_button`, `photo_add_button_plain`, `photo_add_confirm`,
`photo_add_count_in`, `photo_add_not_added`, `photo_add_nothing`,
`photo_add_nothing_to_add`, `photo_add_not_found`, `photo_add_error`,
`photo_add_no_lexemes` — каждый после `grep -rn` по `R.string.` в
`wordcard` и `candidates`: что зовёт диалог, остаётся.

**Мотивация.** Тексты экрана — ключи по спеке в обоих языках; кнопки
окна первой версии ушли; диалог карточки слова свои тексты не
меняет.

## Э10. Покрытие, ручники, устройство

ВЫПОЛНЕН 2026-10-28, кроме прогона ручников: кейсы 11–21 написаны в
`manual_tests.md` (заголовок на оба этапа, словарь логов этапа 2),
прогон на устройстве — следующий шаг; dev-сборка поставлена и запущена.
Три прогона покрытия с кодом 0: AWP — открыты только строки AWP-58 и
AWP-59 «отложено», WCL — без нарушений, статусы не трогались, LFL сторона
«устройство» — открытых нет. Статусы «реализовано» поставлены по
покрытию: AWP как в плане (32 тезиса), LFL-103, LFL-104, LFL-23 — в
спеке бэкенда, уедут с его следующим кодовым коммитом; LFL-113 уже
«реализовано» сервером. Разошлось: `AWP-07_*` у ключей текста слова
переехали в `AWP-39_2`, `AWP-39_3` (пометка «в словаре»), у ключей
значений — в `AWP-50_1`, `AWP-50_3`–`AWP-50_5`; `AWP-04_6`, `AWP-07_10`,
`AWP-07_5` у диалога сняты без замены; `AWP-13_*` и `AWP-09_4` у диалога
остались до его захода; `AWP-32_13` добавлен в `WordsBatchesTest`;
`LFL-104_3` добавлен к тесту обрезки слова; инструментальные
`awp20_9`, `awp20_10`, `awp21_2` написаны и прогнаны на Pixel 7 Pro
вместе с прежними — 13 тестов, падений нет; `-PMINIFY_DEBUG=true` —
перед PR, после ручников.

Тезисы: все строки этапа 2. Проверка: `scripts/spec-coverage.sh` по
AWP, LFL (сторона «устройство») и WCL без открытых строк у тезисов
«реализовано»; инструментальные тесты базы; ручники на устройстве
против прода с `lexemes/from-lemma`.

### Адреса первой версии

**Было.** В тестах живут адреса v1, которых нет в AWP v2 (Э5):
AWP-04, 07, 09, 12, 13, 19, 22, 23, 25 — `LemmaCardsTest`,
`CountPhrasesTest`, `WordTextKeyTest`, `LexemeTextKeyTest`,
`CandidatesReducerTest:277`, `WordCardUseCaseImplTest:592`,
`ScanCandidatesUseCaseImplTest`, `Is532WordsBatchTest`; прогон AWP
держит их через `SPEC_KNOWN=…/spec-v1.md`.

**Будет.** Переименования по смыслу строки: AWP-07 (пометка «есть»)
→ AWP-50 у сравнения по ядру; AWP-04 (карточка на лемму) → снято
вместе с `buildLemmaCards`; AWP-12 (не добавить) → снято, слово с «не
найдено» — AWP-55; AWP-19 (флаг правок) → снято; AWP-22, 23, 25
(вопрос и снекбар) → AWP-51 или снято; AWP-09, 13 — у диалога
карточки слова (WCL-19), остаются до его захода. Адреса v1 спек WCL и
LFL в тестах приложения: WCL-03 → LFL-104 и WCL-18 (Э7); LFL-20,
LFL-49, LFL-63 → сняты или LFL-23 (Э7); `LemmasMappersTest.kt:124`
— адрес LFL-63 → LFL-23 по смыслу. Прогоны покрытия в Э10 — три:
AWP как в Э5; WCL —
`SPEC_FILE=docs/handbook/specs/word-card-lexemes/spec.md
SPEC_PREFIX=WCL SPEC_KNOWN=docs/handbook/specs/word-card-lexemes/spec-v1.md
./scripts/spec-coverage.sh`; LFL —
`SPEC_FILE=backend/docs/handbook/specs/lexemes-from-lemma/spec.md
SPEC_PREFIX=LFL SPEC_SIDE=устройство
SPEC_KNOWN=backend/docs/handbook/specs/lexemes-from-lemma/spec-v1.md
SPEC_MANUALS=docs/features/IS536_llm_values/manual_tests.md
./scripts/spec-coverage.sh` — все три с кодом 0. `SPEC_KNOWN` с v1
остаётся только ради тестов диалога.

**Мотивация.** Адрес закрывает строку спеки, старый адрес у нового
смысла закрыл бы не ту строку.

### Инструментальные тесты базы

**Было.** `core/core-db-impl/src/androidTest/…/Is532WordsBatchTest.kt:157–350`
— `awp20_1_awp20_2_awp20_5_awp20_7_batchWritesWordsLexemesAndMembershipsAtOnce`,
`awp20_3_oneWordTwoLexemesWithDifferentPartsOfSpeech`,
`awp22_1_addLexemeWithComponentsKeepsPartOfSpeechOption`,
`awp21_1_rejectionOnLastLexemeOfBatchWritesNothing`,
`awp21_3_groupDeletedBeforeWriteIsRejectedBeforeFirstInsert`,
`awp21_4_wordDeletedBeforeWriteIsRejected`,
`awp21_4_wordOfAnotherDictionaryIsRejected`,
`awp21_6_typeDisabledBeforeWriteIsRejected`,
`awp21_6_optionOfAnotherTypeOrTemplateMismatchIsRejected`,
`awp07_1_getWordTextsReturnsOnlyThisDictionary`. Строк `AWP-20_9`,
`AWP-20_10`, `AWP-21_2`, `AWP-32_*` нет.

**Будет.** Те же тесты плюс `awp20_9_newWordTwoLexemesCommonGroup`
(новое слово, 2 лексемы, общая группа),
`awp20_10_lexemeIntoExistingWordKeepsGroups` (лексема в существующее
слово, прежние группы на месте),
`awp21_2_retryAfterCauseRemovedWritesOnce` (повтор после устранения
причины — одна запись); `awp22_1_…` → `awp32_9_…` (строка v2 про
опцию части речи); `awp07_1_…` — без адреса, имя
`getWordTextsReturnsOnlyThisDictionary`. Прогон на девайсе:
`:core:core-db-impl:connectedDebugAndroidTest
-Pandroid.testInstrumentationRunnerArguments.class=…Is532WordsBatchTest`.

**Мотивация.** Строки «инструментальный» закрываются только на базе
устройства; «было» — по репо, не по памяти.

### Статусы тезисов

**Было.** AWP: 14 тезисов «реализовано» (этап 1), остальные
«принято», AWP-58 и AWP-59 «отложено»; LFL-103, 104, 23 «принято»;
WCL — 2 «реализовано», 17 «принято».

**Будет.** «Реализовано» по покрытию: AWP-02, 03, 08, 11, 14, 15, 16,
17, 18, 20, 21, 26, 27, 29, 30, 31, 32, 42–52, 55, 57, 61, 62;
LFL-103, LFL-104, LFL-23, LFL-113 — статус у тезисов стороны
устройства ставится по покрытию приложения, у LFL-113 — после
сервера; WCL-18, WCL-01.3 — через адреса в тестах юзкейса, статусы
WCL не трогаются до его захода. AWP-58, AWP-59 — «отложено» как есть.
Строки без теста закрываются кейсами ручников: AWP-27.2, AWP-30.5,
AWP-42.3, AWP-45.3, AWP-52.2 — в списке ниже.

**Мотивация.** Статус — только у тезиса без открытых строк.

### Ручники и устройство

**Было.** `manual_tests.md` — 10 кейсов этапа 1, кейс 2 частично
прогнан.

**Будет.** Кейсы этапа 2 (11+), по спеке: тап нового слова
«friendship» → карточка с «в тексте» у первой, «Замечания»,
«Сопоставление», подпись источника (AWP-42.1, 43.1, 45.1, 45.2);
второй тап сворачивает, третий раскрывает с правками (AWP-42.2,
42.4); «study» в словаре → сохранённые значения, «Проверить по
тексту» → «уже в словаре» у совпавшего, «Добавить» у нового
(AWP-46.1, 49.1, 50.1, 47.6); два «bank» в словаре → лист «куда
класть» с датами и группами, выбор второго меняет сохранённые
(AWP-18.1, 18.8, 18.9, 18.10); «Добавить» → строки нет, слово на
вкладке «Слова» с лексемами и в тренировке (AWP-62.1, 62.2, 52.1,
52.2, 52.3, 52.4); «Удалить» (AWP-48.1, 48.3); правка перевода, опция
части речи, удаление лексемы и значения (AWP-16.1, 16.6, 17.1,
15.1, 15.2); своя группа слова (AWP-08.1, 08.3, 08.5); «accorc» в
«не разобрано» → «Не найдено» (AWP-55.1); сеть выключена на тапе →
«Сервер не ответил», «Повторить» (AWP-61.2, 61.7); крестик с
загруженным словом → вопрос с числом, «Остаться», «Закрыть»
(AWP-51.1, 51.3, 51.4); окно без раскрытых карточек — подписи
источника нет, с карточкой «perro» и 15 рекомендациями — все строки
«Замечаний» видны (AWP-45.3, 30.5); карточка «friendship» та же после
сворачивания и раскрытия (AWP-42.3); поворот после правок (AWP-14.3,
43.6); перезапуск при открытом окне и во время записи (AWP-27.1,
27.2, 27.3); карточка слова → значения по лемме (WCL, как в первой
версии). Прогон — на
`co.lexeme.app.dev` против прода с `lexemes/from-lemma`; перед PR —
`:app:installDebug -PMINIFY_DEBUG=true` и один сетевой кейс по тапу.

**Мотивация.** Ручники закрывают строки «ручной»; сборка с
минификацией ловит выпиленные зеркала новой операции.

## Э11. Плашки под словом

ВЫПОЛНЕН 2026-10-29. Как в плане и `hld-awp.md`, «Этап 3»; разошлось:
«Куда» и своя группа живут на `CardLexemeUiItem` вместе с `isAdding`
и `addFailure` (единый источник, без параллельного списка плашек),
`LemmaCardUiItem.target`/`ownGroup` сняты совсем — диалог карточки
слова их не читал; `canWrite` — свойство лексемы, а не поле стейта;
`emptyLemmaCard` и `lemmaCardOf` держат текст слова списка леммой
карточки; `withCardTarget`/`withCardGroup` → `withLexemeTarget`/
`withLexemeGroup`, `withoutGroup`/`withoutTarget` — по лексемам;
`wordsBatchOf` снята, пакет — `lexemeBatchOf`; `LemmaCardWidget`,
`SavedLexemeRows` и `GroupHint` сняты, вместо них `SavedWordPlate`,
`LexemeChoices`, `LexemeRow` с `onDelete`; у плашки «уже в словаре»
«Куда» и «Группа» не рисуются; `AddWordReducerTest` →
`AddLexemeReducerTest`; строка AWP-15.8 (два быстрых «удалить» у
примера) снята в пользу AWP-64.3 — у значения адрес по номеру, второй
тап по тому же месту снял бы соседа, а страховок от двойного тапа нет;
`Fx.cards` виджета строит карточку по лемме ответа, чтобы адреса правок
в тестах остались по лемме. Тесты: виджет, карточка слова, экран снимка,
`:app:testDebugUnitTest`, `:app:lintDebug` зелёные.

Переделка карточки этапа 2 в плашки по `hld-awp.md`, «Этап 3».
Тезисы: AWP-02, AWP-03, AWP-08, AWP-11, AWP-15, AWP-18, AWP-20,
AWP-21, AWP-26, AWP-42, AWP-43, AWP-45, AWP-46, AWP-47, AWP-48,
AWP-49, AWP-50, AWP-51, AWP-55, AWP-57, AWP-61, AWP-62, AWP-63,
AWP-64. Порядок: типы виджета и чистые функции с тестами, затем
экран — стейт, сообщения, эффекты, атомы, редьюсер, хендлер, UI, —
затем тесты экрана, строки, превью. После этапа:
`:modules:widget:candidates`, `:modules:screen:wordcard`,
`:modules:screen:scancandidates`, `:app:testDebugUnitTest`,
`:app:lintDebug` зелёные.

Допущения: лемма карточки — текст слова списка, лемма ответа другой
текстом — ошибка этапа, окно её не ловит (AWP-43.7 снята, бэклог);
«уже в словаре» и «Куда» — по всем словам словаря с текстом слова;
условия в редьюсере — только ветка закрытия, как было.

### Типы виджета

**Было.** `widget/candidates/CandidatesUi.kt` —
`LemmaCardUiItem(lemma, sourceWords, lexemes, existingCount,
target, ownGroup, formNote)`, `CardLexemeUiItem(key, source,
values, isEnglishFallback, exists, lacksCore, existsInLemma,
isInContext)`, листы `CardGroup(lemma)`, `CardTarget(lemma)`;
`FormUi.kt` — `ExistingLemmaUi`, `SavedLexemeUi`;
`CandidateAction.kt` — `SetCardTarget(lemma, target)`,
`SetCardGroup(lemma, group)`.

**Будет.** Как в HLD, «Стейт» этапа 3: у леммы без `target` и
`ownGroup`; у лексемы-кандидата `target`, `ownGroup`, `isAdding`,
`addFailure` с умолчаниями и `canWrite`; листы и действия с
ключом лексемы; новый `SavedWordUi`.

```kotlin
// SavedWordUi — плашка словаря (AWP-46). Новый.
@Immutable
data class SavedWordUi(
    val wordId: Long,
    val text: String,
    val groupNames: List<String>,
    val addedAt: Long,
    val lexemes: List<SavedLexemeUi>,
)

// CandidateAction: адрес — плашка. Меняются.
data class SetCardTarget(
    val lemma: String,
    val key: String,
    val target: CardTargetUi,
) : CandidateAction

data class SetCardGroup(
    val lemma: String,
    val key: String,
    val group: CardOwnGroupUi,
) : CandidateAction
```

**Мотивация.** «Куда» и группа — выбор на каждую лексему (Д123,
Д126); диалог карточки слова эти поля не читает, умолчания его не
трогают.

### Чистые функции виджета

**Было.** `LemmaCards.kt` — `emptyLemmaCard` с «куда класть» по
самой новой лемме, `lemmaCardOf` с веткой «лемма ответа другая»,
`savedLexemesOf(target)`, `lexemeKeysOf(target)`,
`withExistingLemmas(added)`; `CardTransitions.kt` —
`withCardTarget`, `withCardGroup`, `withoutGroup`, `withoutTarget`
по карточкам; `WordsBatches.kt` — `wordsBatchOf(cards, form,
commonGroupId)`, `LemmaCardUiItem.groupIdOr`.

**Будет.** Как в HLD, «Чистые функции» этапа 3.

```kotlin
// savedWordsOf — плашки словаря по тексту, самое новое
// первым, значения в порядке карточки (AWP-46.3, 46.7).
fun DictionaryFormUi.savedWordsOf(
    text: String,
): List<SavedWordUi> = existingOf(text).map { lemma ->
    SavedWordUi(
        wordId = lemma.wordId,
        text = lemma.text,
        groupNames = lemma.groupNames,
        addedAt = lemma.addedAt,
        lexemes = lemma.lexemes.map { saved ->
            saved.copy(
                values = saved.values.inCardOrder(this),
            )
        },
    )
}

// withLemmasOf — леммы словаря с текстом заменены
// присланными (AWP-63).
fun DictionaryFormUi.withLemmasOf(
    text: String,
    lemmas: List<ExistingLemmaUi>,
): DictionaryFormUi {
    val key = wordTextKey(text)
    val others = existingLemmas
        .filterNot { wordTextKey(it.text) == key }
    return copy(existingLemmas = others + lemmas)
}

// withNewWordRetargeted — «новое слово» → созданное
// (AWP-63.1, 63.3); явный выбор не трогается; null —
// таких лексем нет.
fun List<LemmaCardUiItem>.withNewWordRetargeted(
    lemma: String,
    wordId: Long,
): List<LemmaCardUiItem>? {
    val card = cardOf(lemma) ?: return null
    val fresh = card.lexemes
        .filter { it.target == CardTargetUi.NewWord }
    if (fresh.isEmpty()) return null
    val target = CardTargetUi.Existing(wordId)
    return updateCard(lemma) { current ->
        current.copy(
            lexemes = current.lexemes.map { lexeme ->
                if (lexeme.target == CardTargetUi.NewWord) {
                    lexeme.copy(target = target)
                } else {
                    lexeme
                }
            },
        )
    }
}

// lexemeBatchOf — пакет одной лексемы (AWP-20): новое
// слово с текстом леммы и группой по своей или общей
// или выбранное слово без группы (AWP-20.10); null —
// лексемы нет.
fun lexemeBatchOf(
    card: LemmaCardUiItem,
    key: String,
    form: DictionaryFormUi,
    commonGroupId: Long?,
): WordsBatch? {
    val lexeme = card.lexemes.firstOrNull { it.key == key }
        ?: return null
    val word = when (val target = lexeme.target) {
        CardTargetUi.NewWord -> BatchWord(
            wordId = null,
            text = card.lemma,
            groupId = lexeme.groupIdOr(commonGroupId),
            lexemes = listOf(lexeme.toBatchLexeme(form)),
        )
        is CardTargetUi.Existing -> BatchWord(
            wordId = target.wordId,
            text = card.lemma,
            groupId = null,
            lexemes = listOf(lexeme.toBatchLexeme(form)),
        )
    }
    return WordsBatch(
        dictionaryId = form.dictionaryId,
        words = listOf(word),
    )
}
```

**Мотивация.** Одна лексема — одна транзакция (Д123); группы
существующего слова окно не трогает (Д126); плашки словаря после
записи — из перечитанных лемм (Д124).

### Тесты виджета

**Было.** `LemmaCardsTest` — «куда класть» по самой новой,
`savedLexemesOf`, `lexemeKeysOf`, `withExistingLemmas`;
`CardTransitionsTest` — `withCardTarget`, `withCardGroup`,
`withoutGroup`, `withoutTarget` по карточкам; `WordsBatchesTest` —
`wordsBatchOf` по карточкам (AWP-20, AWP-32, AWP-08);
`SavedLexemeMappersTest` — `savedLexemesOf`; `Fixtures.cards`
через `lexemeKeysOf(card.target)`.

**Будет.** Те же файлы под новые функции: `savedWordsOf`
(`AWP-46_1`, `AWP-46_3`, `AWP-46_7`), `withLemmasOf` (`AWP-63_1`),
`withNewWordRetargeted` (`AWP-63_1`, `AWP-63_3`, `AWP-63_4`),
`withLexemeTarget` / `withLexemeGroup` (`AWP-18_12`, `AWP-08_8`),
`withoutGroup` / `withoutTarget` по лексемам (`AWP-21_3`,
`AWP-21_4`, `AWP-21_5`), `lexemeBatchOf` (`AWP-20_2`, `AWP-20_7`,
`AWP-20_9`, `AWP-20_10`, `AWP-08_1`, `AWP-08_4`, `AWP-32_*` как
были); `Fixtures.cards` — пометки по `lemmaWordKeys`. Прогон
`:modules:widget:candidates:testDebugUnitTest`, затем
`:modules:screen:wordcard:testDebugUnitTest` — диалог не меняется,
кроме снятой строки `cards.all { it.target == NewWord }`.

**Мотивация.** Чистые функции проверяются до экрана: атомы потом
только адресуют.

### Стейт, сообщения, эффекты экрана

**Было.** `State.kt` — `WordCardUi` с `saved:
List<SavedLexemeUi>`, `canAdd`, `addFailure`; `Message.kt` —
`LexemesFound.existing`, `WordAdded`, `Add*Gone(text, failure)`,
`AddFailed(text, failure)`, `DeleteWord`, `AddWord`,
`RetryAdd(text)`, `DeleteLexeme(lemma, key)`,
`SetCardTarget(lemma, target)`, `SetCardGroup(lemma, group)`;
`Effects.kt` — `AddWord(word, batch)`.

**Будет.** Как в HLD, «Стейт» и «Сообщения и эффекты» этапа 3.

```kotlin
// Эффекты записи и перечитки. Новые.
data class CreateWord(
    val word: String,
    val key: String,
    val batch: WordsBatch,
) : DatasourceEffect, RecoverableEffect<Msg> {
    override fun onFail(error: Throwable) = Msg.AddFailed(
        text = word,
        key = key,
        failure = AddFailureUi.Failed,
    )
}

data class TransferLexeme(
    val word: String,
    val key: String,
    val batch: WordsBatch,
) : DatasourceEffect, RecoverableEffect<Msg> {
    override fun onFail(error: Throwable) = Msg.AddFailed(
        text = word,
        key = key,
        failure = AddFailureUi.Failed,
    )
}

data class ReloadLemmas(
    val word: String,
    val dictionaryId: Long,
) : DatasourceEffect, RecoverableEffect<Msg> {
    override fun onFail(error: Throwable) =
        Msg.LemmasReloadFailed(text = word)
}
```

**Мотивация.** Два эффекта записи — два исхода без разбора пакета;
перечитка отдельным эффектом — запись, прошедшая до упавшей
перечитки, остаётся записанной.

### Атомы, редьюсер, хендлер

**Было.** `ScanCandidatesAtoms.kt` — `openSavedCard`,
`startLookup`, `applyLexemes` с `withExistingLemmas`,
`refreshCardMarks` по `lexemeKeysOf(target)` и `canAdd`,
`deleteWord`, `deleteLexeme`, `setCardTarget`, `setCardGroup`,
`refreshLemmaSaved`, `startAddingWord` с `wordsBatchOf`,
`removeAddedWord`, `showAddFailure(text, failure)`,
`clearAddFailure(text)`; `ScanCandidatesReducer.kt` — цепочки
этапа 2; `DatasourceEffectHandler.kt` — ветка `AddWord`.

**Будет.** Как в HLD, «Атомы», «Редьюсер», «Хендлер» этапа 3.

```kotlin
// startAddingLexeme — эффект по типу «Куда» плашки.
fun ScanCandidatesState.startAddingLexeme(
    text: String,
    key: String,
): ScanCandidatesResult {
    val form = form
        ?: return noOp("startAddingLexeme: no form")
    val card = lists.wordOf(text)?.card
        ?: return noOp("startAddingLexeme: no card $text")
    val lexeme = card.lemma.lexemes
        .firstOrNull { it.key == key }
        ?: return noOp("startAddingLexeme: no lexeme $key")
    if (!lexeme.canWrite) {
        return noOp("startAddingLexeme: not writable $key")
    }
    val batch = lexemeBatchOf(
        card.lemma, key, form, commonGroupId,
    ) ?: return noOp("startAddingLexeme: no batch $key")
    logStep(
        "startAddingLexeme",
        "word" to text,
        "key" to key,
    )
    val next = copy(
        isAdding = true,
        lists = lists.updateWord(text) { item ->
            item.copy(
                card = card.copy(isAdding = true)
                    .withLexemeAdding(key),
            )
        },
    )
    val effect = when (lexeme.target) {
        CardTargetUi.NewWord ->
            DatasourceEffect.CreateWord(text, key, batch)
        is CardTargetUi.Existing ->
            DatasourceEffect.TransferLexeme(
                text, key, batch,
            )
    }
    return next to setOf(effect)
}

// removeEmptyWord — слово без плашек нейронки уходит
// (AWP-62); no-op у слова с плашками или без ответа.
fun ScanCandidatesState.removeEmptyWord(
    text: String,
): ScanCandidatesResult {
    val card = lists.wordOf(text)?.card
        ?: return noOp("removeEmptyWord: no card $text")
    if (card.phase != CardPhase.CANDIDATES) {
        return noOp("removeEmptyWord: no answer $text")
    }
    if (card.lemma.lexemes.isNotEmpty()) {
        return noOp("removeEmptyWord: lexemes left $text")
    }
    logStep("removeEmptyWord", "word" to text)
    return copy(lists = lists.removeWord(text)).begin()
}
```

**Мотивация.** Атом — один шаг со своими данными: проверки «есть
ли что делать» остаются no-op, выбор эффекта — по типу «Куда»;
уход слова — в хвосте каждой цепочки, где плашка может исчезнуть.

### UI экрана и виджета

**Было.** `widget/candidates/ui/LemmaCardWidget.kt` —
`LemmaCardWidget` с шапкой и «Куда» карточки, `SavedLexemeRows`,
`LexemeRow` internal с крестиком; `CandidatesSheets.kt` — листы по
лемме с подсказкой о группе; `scancandidates/ui/WordActions.kt` —
`Delete`, `Add`, `RetryAdd(text)`; `ScanWordListsContent.kt` —
`WordCardBlock` с `LemmaCardWidget`, `CandidatesBlock` с «Добавить»
и «Удалить».

**Будет.** Как в HLD, «UI» этапа 3: `SavedWordPlate`, `LexemeRow`
с `onDelete`, `LexemeChoices`, листы по плашке; `WordAction.Write`,
`Dismiss`, `RetryAdd(text, key)`, `Remove`; `WordCardBlock` по
фазе с плашками словаря в любой фазе. Строки в `values` и
`values-ru-rRU`: `scan_remove_word` — «Убрать из списка»,
`scan_dismiss` — «Не нужно», `scan_create_word` — «Создать»,
`scan_transfer` — «Перенести», `scan_candidates_title` —
«Найдено по тексту»; `scan_add`, `scan_delete_word` сняты;
`photo_add_target_group_hint`, `photo_add_old_lexemes` сняты.
Превью экрана — карточка с двумя плашками, у второй «Куда» —
слово словаря.

**Мотивация.** Названия кнопок — Д129; плашки словаря видны всегда,
чтобы проверка и запись шли на глазах у сохранённого.

### Тесты экрана

**Было.** `WordCardReducerTest`, `CardEditsReducerTest`,
`AddWordReducerTest`, `CloseReducerTest`, `ScanCandidatesAtomsTest`,
`ScanCandidatesScenarioTest`, `DatasourceEffectHandlerTest`,
`ScanWordListsTest`, `ui/WordActionsTest`, `ui/CandidateActionsTest`
— адреса спеки v2; `ReducerHelpers.batchOf` по `AddWord`.

**Будет.** Те же файлы под v3: `WordCardReducerTest` — `AWP-42_1`,
`42_2`, `42_4`–`42_7`, `43_1`, `43_4`, `46_1`, `46_3`, `46_4`,
`46_5`, `47_1`, `47_2`, `47_6`–`47_9`, `48_1`, `48_3`–`48_5`,
`49_1`, `49_5`, `49_6`, `50_1`, `50_12`, `50_14`, `50_16`, `55_1`,
`55_7`, `55_10`, `57_1`–`57_4`, `61_7`, `18_2`, `18_3`, `18_12`,
`18_13`, `64_1`, `64_3`–`64_5`; `CardEditsReducerTest` — как были
плюс `AWP-11_5` с уходом слова, `AWP-08_6`, `AWP-08_8`, `18_12`
снят отсюда; `AddWordReducerTest` → `AddLexemeReducerTest` —
`AWP-02_1`–`02_3`, `03_1`–`03_3`, `20_2`, `20_3`, `20_5`, `20_7`,
`20_9`–`20_11`, `21_3`–`21_7`, `26_1`, `26_2`, `47_3`, `47_4`,
`62_1`, `62_3`–`62_6`, `63_1`–`63_6`, `50_15`, `39_5`;
`CloseReducerTest` — как были плюс `AWP-51_13`, `60_2`;
`DatasourceEffectHandlerTest` — `AWP-20_9` (создано → id),
`AWP-20_10`, `AWP-63_1` (перечитка → леммы), `AWP-63_6`
(перечитка упала), отказы с ключом; `ScanCandidatesScenarioTest` —
цепочка «тап → плашки → «Создать» первой → плашка словаря и
«Куда» у второй → «Перенести» → строки нет»; `ScanWordListsTest`
— `countPending` по плашкам; `ui/WordActionsTest` — новые действия.
`ReducerHelpers.batchOf(state, text, key)` по обоим эффектам
записи.

**Мотивация.** Адреса тестов — строки спеки v3; покрытие
`scripts/spec-coverage.sh` без нарушений.

## Э12. Покрытие, ручники, устройство

ВЫПОЛНЕН 2026-10-29, ручники прогнаны 2026-10-30/31: кейсы 11–22 на
живых данных телефона, слова заменены там, где подготовка не совпала;
без сети пропущено по решению юзера, поворот не проверить — экран
прибит к портрету. Покрытие AWP (с
`SPEC_MANUALS` и `SPEC_KNOWN` v1, v2), WCL и LFL без нарушений, открыты
только AWP-58/59 «отложено»; статусы v3-тезисов «реализовано»; кейсы
этапа 2 по v2 заморожены в `manual_tests-v2.md` с их прогонами, в
`manual_tests.md` кейсы 11–22 по плашкам (словарь логов под новые
атомы); `hld-lint` чист; dev-сборка поставлена и запущена.

**Было.** Статусы v3-тезисов «принято»; `manual_tests.md` — кейсы
11–21 этапа 2 по карточке v2.

**Будет.** После зелёных тестов — `spec-coverage.sh` для AWP, WCL,
LFL без нарушений, статусы тезисов этапа 3 → «реализовано», кроме
AWP-58/59 «отложено»; `hld-lint` по `hld-awp.md` и плану чист;
кейсы 11–21 в `manual_tests.md` переписаны под плашки (основные
юзкейсы, шаги атомарные): новое слово → плашки, «Создать» первой →
плашка словаря и «Перенести» у второй, слово «в словаре» → плашки
словаря → «Проверить по тексту» → «уже в словаре», «Не нужно»,
«Убрать из списка», два «bank», сбой сети, закрытие с вопросом;
`:app:installDebug` и запуск на `co.lexeme.app.dev`; таймаут
экрана обратно 600000 после ручников.

**Мотивация.** Статусы и ручники закрывают строки «ручной»;
покрытие держит адреса тестов в согласии со спекой.
