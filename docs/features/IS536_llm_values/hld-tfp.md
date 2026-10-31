# IS536 | HLD TFP — токены со снимка, черновик

План устройства по спеке TFP: кнопка-камера → снимок → распознавание
на телефоне → токены → один запрос на сервер → ответ в окно. Всё это
работает с первой версии фичи; в v2 меняется конец пути: токены
уходят не за значениями слов, а за леммами текста —
`lemmas/from-tokens` (LFT). Экран, который показывает ответ, —
`hld-awp.md`; сервер — `hld-lft.md`; ключ — `hld-acc.md`, лимиты —
`hld-lim.md`. Этап 1 (IS537) берёт этот план целиком.

## 1. Рамка

- **Цель** — снимок текста превращается в токены на телефоне и одним
  запросом уходит на сервер; сервер снимка не видит (Д35).
- **Вход и выход** — вход: нажатие кнопки-камеры на вкладке «Слова»,
  снимок с камеры или из галереи. Выход: запрос `lemmas/from-tokens` —
  токены как распознаны, с повторами, уверенностью и языком, языки
  словаря снимка, ключ доступа и номер запуска; назад — текст,
  перевод и четыре списка, которые показывает экран (AWP).
- **Что меняется на устройстве** — запрос и ответ: новая операция
  шлюза вместо `lookupWords`, без дерева компонентов; доменный исход
  «леммы текста»; у экрана эффект после распознавания и подпись
  «Анализирую текст…»; текст отказа 503 — «Анализ текста сейчас
  недоступен, попробуй позже» (AWP-53.1). Повтор с теми же токенами —
  как был (TFP-26).
- **Что не меняется** — кнопка и диалог выбора, системная камера и
  галерея, временный файл снимка и доступ к файлу галереи (Д52),
  распознавание ML Kit, подготовка токенов — пустые прочь, потолок
  2000 (TFP-15, TFP-25), номер запуска (TFP-01), перезапуски (TFP-22,
  TFP-23).
- **Документы** — [бриф](brief.md), [решения](decisions.md), спека
  TFP, спека LFT и её `contract.md`, экранная спека AWP.

## 2. Приложение: схема и код

### Модули и экран

- **`screen/wordstab` — как есть.** Кнопка-камера, диалог
  `ScanDialog`, `PhotoPicker` с камерой и галереей, переход на экран
  снимка. Что у вкладки снимается после окна — `hld-awp.md`.
- **`screen/scancandidates` — меняется.** После распознавания экран
  шлёт эффект `ExtractLemmas` вместо `LookupWords`, ответ приходит
  сообщениями про леммы текста. Остальное про экран — `hld-awp.md`.
- **`domain/lexeme` — новое.** Исход запроса `LemmasOutcome` и леммы
  текста `TextLemmas`; токен `LookupToken` и причины отказа
  `LookupDenyReason` — как есть.
- **`core-network-api`, `core-network-impl` — новое.** Операция
  `lemmasFromTokens` у `WordsApi`: зеркала `LemmasRequestApiEntity`,
  `LemmasResult`, транспорт `LemmasRequestNet`, `LemmasResponseNet`,
  адрес в `LexemeService`. Обмен с сервером и разбор кодов — общие
  для обеих операций. `lookup` остаётся: его зовёт диалог карточки
  слова до этапа 2.
- **`app` — меняется.** `ScanCandidatesUseCaseImpl.lemmasFromTokens`
  вместо `lookupWords`, мапперы запроса и ответа в `mapper/`.
- **`library/textrecognition` — как есть.**

### Условные знаки

![Условные знаки](/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS536_llm_values/hld-legend.a02d0bb8.png)

### Путь фичи

Откуда запускается, через какие окна идёт, куда приходит. Вход один —
кнопка-камера на вкладке «Слова», только у сборки с ключом сервера.
В коробке — окно или шаг и его компоненты, на стрелке — шаг спеки
TFP или AWP и что передаётся. Серый блок — системное приложение, не
наше. Источник — `hld-tfp-path.dot`.

![Путь фичи](/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS536_llm_values/hld-tfp-path.07f6b003.png)

Что меняется на пути, синим: запрос несёт только токены и языки,
ответ — текст, перевод и списки; экран на месте «Ищу слова» говорит
«Анализирую текст…» и показывает списки вместо карточек; после окна
вкладка без снекбара. Кнопка, диалог, камера, галерея и
распознавание — как есть.

### Вкладка «Слова»

Вход фичи, `screen/wordstab`. Код не меняется: ветки `OpenScanDialog`,
`ChooseCamera`, `ChooseGallery`, `ImagePicked`, `CameraUnavailable`,
`CloseScanDialog` редьюсера и атомы диалога — как в первой версии.
Что зовётся, сигнатурами:

```kotlin
// launchCamera — Ш2–Ш3: открыть системную камеру,
// снимок — в новый файл в кеше приложения; false —
// приложения камеры нет, его отключили или файл под
// снимок не создался (TFP-07). Порт ScanLauncher,
// реализация PhotoPicker. Есть.
fun launchCamera(): Boolean

// launchGallery — Ш2–Ш3: системный выбор снимка; доступ
// к файлу галереи закрепляется до сообщения, чтобы
// пережить гибель процесса (TFP-22.4). Есть.
fun launchGallery()

// OpenScanCandidates — Ш3: экран снимка с файлом; после
// гибели процесса маршрут не восстанавливается (TFP-23).
// Навигация вкладки, есть.
data class OpenScanCandidates(
    // uri — файл снимка строкой.
    val uri: String,
) : WordsNavigationEffect
```

### Экран снимка: от файла до запроса

Экран `screen/scancandidates` открывается с файлом снимка и первым
эффектом распознаёт его. Здесь ветки шагов Ш4–Ш6 и В2–В4; окно и его
состояние — `hld-awp.md`.

#### Редьюсер

```kotlin
// TextRecognized — хендлер эффекта RecognizeText: ML Kit
// отдал текст:
//   tokens — токены как распознаны, с повторами, пустые
//     прочь, не больше 2000
// Меняется: эффект запроса другой
is Msg.TextRecognized ->
    state.startAnalyzing(message.tokens)
//   startAnalyzing: токены в стейт, фаза «анализирую
//     текст», ошибка снята, эффект ExtractLemmas

// TextEmpty — хендлер RecognizeText: на снимке ни одного
// слова. Без данных. Есть, не меняется
Msg.TextEmpty -> state.showNoWords()
//   showNoWords: фаза «слов не найдено», только «Закрыть»

// RecognitionFailed — хендлер RecognizeText, эффект упал:
// снимок не прочитался или ML Kit бросил исключение:
//   errorRes — «Не удалось распознать текст»
// Есть, не меняется
is Msg.RecognitionFailed ->
    state.showError(message.errorRes)
//   showError: фаза «ошибка», текст, без «Повторить»

// RetryLemmas — UI: «Повторить» на ошибке сети, сервера
// или нейронки. Без данных. Меняется: было RetryLookup
Msg.RetryLemmas -> state.startAnalyzing(state.tokens)
//   startAnalyzing: те же токены из стейта, без нового
//     снимка и распознавания (TFP-26)
```

Атом и эффект:

```kotlin
// startAnalyzing — Ш6: токены в стейт, фаза «анализирую
// текст», прежняя ошибка снята; эффект запроса рождается
// здесь. Зовётся после распознавания и по «Повторить».
// Меняется: был startLooking с эффектом LookupWords.
fun ScanCandidatesState.startAnalyzing(
    tokens: List<LookupToken>,
): ScanCandidatesResult {
    logStep("startAnalyzing", "tokens" to tokens.size)
    val next = copy(
        phase = ScanPhase.ANALYZING,
        tokens = tokens,
        errorRes = null,
        hasRetryButton = false,
    )
    return next to setOf(
        DatasourceEffect.ExtractLemmas(tokens),
    )
}

// ExtractLemmas — Ш6: один запрос на сервер списком
// токенов. Сбой эффекта — «Сервер не ответил» с
// «Повторить». Новый, вместо LookupWords.
data class ExtractLemmas(
    // tokens — токены как распознаны, с повторами.
    val tokens: List<LookupToken>,
) : DatasourceEffect,
    RecoverableEffect<Msg> {
    override fun onFail(error: Throwable) =
        Msg.LemmasUnavailable(
            errorRes = R.string.photo_words_error_server,
        )

    override fun toString(): String =
        "ExtractLemmas(tokens=${tokens.size})"
}
```

Хендлер: ветка `ExtractLemmas` зовёт юзкейс и шлёт сообщение на
каждый исход; редьюсер исходов не видит. Ветка `RecognizeText` — как
была.

```kotlin
// ExtractLemmas — Ш6: исход юзкейса → сообщение. Отказ по
// нейронке — свой текст с «Повторить» (AWP-53.1);
// остальные отказы — текст по причине, без повтора
// (AWP-53.2–53.4, 53.8); сервер не ответил — «Сервер не
// ответил» с «Повторить» (AWP-53.5). Новая ветка.
is DatasourceEffect.ExtractLemmas -> withContext(io) {
    val outcome = useCase.lemmasFromTokens(effect.tokens)
    logger.d(
        tag = LogTags.SCAN,
        message = "lemmas outcome=${outcome.describe()}",
    )
    when (outcome) {
        is LemmasOutcome.Found -> Msg.LemmasFound(
            dictionaryId = outcome.dictionaryId,
            text = outcome.lemmas.text,
            translation = outcome.lemmas.translation,
            lists = outcome.lemmas.toUi(),
        )
        LemmasOutcome.NoWords -> Msg.LemmasEmpty
        is LemmasOutcome.Denied ->
            when (val reason = outcome.reason) {
                LookupDenyReason.LLM_UNAVAILABLE ->
                    Msg.LemmasUnavailable(
                        R.string.scan_analysis_unavailable,
                    )
                LookupDenyReason.NO_ACCESS_KEY,
                LookupDenyReason.UNAUTHORIZED,
                LookupDenyReason.RATE_LIMITED,
                LookupDenyReason.REQUEST_TOO_LARGE,
                LookupDenyReason.UNSUPPORTED_LANGUAGE,
                -> Msg.LemmasDenied(errorResOf(reason))
            }
        LemmasOutcome.Unavailable ->
            Msg.LemmasUnavailable(
                R.string.photo_words_error_server,
            )
    }
}
```

Что зовёт хендлер:

```kotlin
// describe — строка исхода для лога: dict, число слов в
// каждом списке; no words; denied=…; unavailable. Новая.
private fun LemmasOutcome.describe(): String

// toUi — леммы текста → четыре списка окна, hld-awp.md.
// Новая.
fun TextLemmas.toUi(): ScanWordListsUi

// errorResOf — текст отказа по причине: «Нет доступа к
// серверу», «Лимит на сегодня исчерпан», «Слишком много
// слов», «Язык словаря не поддерживается». Есть.
fun errorResOf(reason: LookupDenyReason): Int
```

Объявления сообщений — `hld-awp.md`.

### Юзкейсы

Порт экрана `ScanCandidatesUseCase` говорит доменом; исход каждого
вызова — типом на случай.

```kotlin
// recognize — Ш4–Ш5: ML Kit по файлу снимка; токены как
// распознаны, с повторами и в исходном регистре, пустые
// прочь, первые 2000; файл камеры удаляется в любом
// исходе (TFP-08), файл галереи не трогается (TFP-09).
// Есть.
suspend fun recognize(uri: String): RecognitionOutcome

// releaseImageAccess — отпустить доступ к файлу галереи,
// закреплённый вкладкой (Д52); у файла камеры ничего не
// делает. Есть.
suspend fun releaseImageAccess(uri: String)

// lemmasFromTokens — Ш6: один запрос с токенами и языками
// словаря снимка; исход — леммы текста, слов нет, отказ с
// причиной, сервер не ответил. Новый, вместо lookupWords.
suspend fun lemmasFromTokens(
    tokens: List<LookupToken>,
): LemmasOutcome
```

Домен, пакет `domain/lexeme`:

```kotlin
// LemmasOutcome — исход запроса лемм текста (LFT Ш1–Ш5).
// Новый.
sealed interface LemmasOutcome {
    // Found — ответ со словарными формами или
    // «не разобрано».
    data class Found(
        // dictionaryId — словарь снимка (AWP-01).
        val dictionaryId: Long,
        // lemmas — текст, перевод и списки как прислал
        // сервер.
        val lemmas: TextLemmas,
    ) : LemmasOutcome

    // NoWords — ни словарной формы, ни «не разобрано»
    // (TFP-19.2).
    data object NoWords : LemmasOutcome

    data class Denied(
        // reason — причина отказа, текст выбирает экран.
        val reason: LookupDenyReason,
    ) : LemmasOutcome

    // Unavailable — сети нет или сервер не ответил.
    data object Unavailable : LemmasOutcome
}

// TextLemmas — ответ сервера на токены снимка. Новый.
data class TextLemmas(
    // text — текст из токенов с исправленными ошибками
    // распознавания; контекст для запроса по слову.
    val text: String,
    // translation — перевод текста на язык перевода.
    val translation: String,
    // lexical — смысловые словарные формы в порядке
    // текста.
    val lexical: List<String>,
    // proper — имена собственные.
    val proper: List<String>,
    // function — служебные слова.
    val function: List<String>,
    // unparsed — токены без пары и обрывки, как есть.
    val unparsed: List<String>,
)
```

Реализация порта, `app`:

```kotlin
// lemmasFromTokens — Ш6: языки текущего словаря, токены
// как пришли, номер запуска; дерева компонентов в запросе
// нет — значений эта операция не даёт. Исключений от сети
// нет: слой отдаёт исход. Новая, вместо lookupWords.
override suspend fun lemmasFromTokens(
    tokens: List<LookupToken>,
): LemmasOutcome {
    val dictionary = currentDictionary()
    val request = lemmasRequestOf(dictionary, tokens)
    return wordsApi
        .lemmasFromTokens(
            request,
            lastRunId.incrementAndGet(),
        )
        .toDomain(dictionaryId = dictionary.id)
}
```

Мапперы `app/mapper`, граница «слой сети → домен»:

```kotlin
// lemmasRequestOf — запрос lemmas/from-tokens: языки
// словаря, токены как есть и в том же порядке. Новая.
fun lemmasRequestOf(
    dictionary: DictionaryApiEntity,
    tokens: List<LookupToken>,
): LemmasRequestApiEntity = LemmasRequestApiEntity(
    learningLanguage = dictionary.learningLanguage,
    translationLanguage = dictionary.translationLanguage,
    tokens = tokens.map { it.toApiEntity() },
)

// toDomain — исход слоя сети → доменный исход. Четыре
// пустых списка — «слов нет» (TFP-19.2). Новая.
fun LemmasResult.toDomain(
    dictionaryId: Long,
): LemmasOutcome = when (this) {
    is LemmasResult.Success -> when {
        isEmpty() -> LemmasOutcome.NoWords
        else -> LemmasOutcome.Found(
            dictionaryId = dictionaryId,
            lemmas = toTextLemmas(),
        )
    }
    is LemmasResult.Denied ->
        LemmasOutcome.Denied(reason.toDomain())
    is LemmasResult.Unavailable ->
        LemmasOutcome.Unavailable
}

// isEmpty — все четыре списка пусты. Новая.
private fun LemmasResult.Success.isEmpty(): Boolean

// toTextLemmas — поля ответа в домен один в один. Новая.
private fun LemmasResult.Success.toTextLemmas(): TextLemmas

// toApiEntity — токен запроса: текст, уверенность, язык.
// Есть.
private fun LookupToken.toApiEntity(): TokenApiEntity

// toDomain — причина отказа слоя сети → доменная. Есть.
fun DenyReason.toDomain(): LookupDenyReason
```

### Шлюз

Зеркала контракта, `core-network-api`:

```kotlin
// lemmasFromTokens — один запрос lemmas/from-tokens
// (LFT-02). Ждёт ответ до 180 секунд, ходит только по
// HTTPS; пустой ключ — Denied без обращения к сети
// (ACC-19). WordsApi, новый метод.
suspend fun lemmasFromTokens(
    request: LemmasRequestApiEntity,
    runId: Long,
): LemmasResult

// LemmasRequestApiEntity — запрос без дерева компонентов.
// Новое.
data class LemmasRequestApiEntity(
    // learningLanguage — изучаемый язык словаря, BCP-47.
    val learningLanguage: String,
    // translationLanguage — язык перевода словаря.
    val translationLanguage: String,
    // tokens — как распознаны, без нормализации.
    val tokens: List<TokenApiEntity>,
)

// LemmasResult — исход запроса, ровно один из трёх.
// Новое.
sealed interface LemmasResult {
    // Success — 200, поля ответа как в контракте.
    data class Success(
        // text — текст с исправленными ошибками
        // распознавания.
        val text: String,
        // translation — перевод текста.
        val translation: String,
        // lexical — смысловые словарные формы.
        val lexical: List<String>,
        // proper — имена собственные.
        val proper: List<String>,
        // function — служебные слова.
        val function: List<String>,
        // unparsed — «не разобрано».
        val unparsed: List<String>,
    ) : LemmasResult

    // Denied — 401, 429, 413, 400 и 503 с кодом в теле
    // или ключ не задан.
    data class Denied(
        // reason — причина, как у lookup.
        val reason: DenyReason,
    ) : LemmasResult

    // Unavailable — сети нет, ответа нет 180 секунд, код
    // вне контракта или тело не разбирается.
    data class Unavailable(
        // cause — для лога, не для пользователя.
        val cause: String,
    ) : LemmasResult
}
```

Транспорт, `core-network-impl`:

```kotlin
// lemmasFromTokens — адрес операции; Authorization ставит
// AccessKeyInterceptor, X-Run-Id — номер запуска.
// LexemeService, новый метод.
@POST("v1/lemmas/from-tokens")
suspend fun lemmasFromTokens(
    @Header(RUN_ID_HEADER) runId: String,
    @Body body: LemmasRequestNet,
): Response<LemmasResponseNet>

// LemmasRequestNet — тело запроса один в один с openapi.
// Новое.
@Serializable
internal data class LemmasRequestNet(
    @SerialName("learningLanguage")
    val learningLanguage: String,
    @SerialName("translationLanguage")
    val translationLanguage: String,
    @SerialName("tokens") val tokens: List<TokenNet>,
)

// LemmasResponseNet — тело 200; списки с умолчаниями:
// разбор не падает, если поле не пришло. Новое.
@Serializable
internal data class LemmasResponseNet(
    @SerialName("text") val text: String,
    @SerialName("translation") val translation: String,
    @SerialName("lexical")
    val lexical: List<String> = emptyList(),
    @SerialName("proper")
    val proper: List<String> = emptyList(),
    @SerialName("function")
    val function: List<String> = emptyList(),
    @SerialName("unparsed")
    val unparsed: List<String> = emptyList(),
)
```

`WordsApiImpl`: обмен с сервером и разбор кодов — то, что у операций
одинаково, — уходит в общий помощник; `lookup` переписывается через
него, поведение то же.

```kotlin
// lemmasFromTokens — ключ не задан → отказ без сети; не
// HTTPS → недоступен; сети нет или ответа нет →
// недоступен; код отказа → причина; 200 → зеркало.
// Новая.
override suspend fun lemmasFromTokens(
    request: LemmasRequestApiEntity,
    runId: Long,
): LemmasResult {
    val outcome = call {
        service.lemmasFromTokens(
            runId.toString(),
            request.toNet(),
        )
    }
    return when (outcome) {
        is Outcome.Success ->
            outcome.body.toApiEntity()
        is Outcome.Denied ->
            LemmasResult.Denied(outcome.reason)
        is Outcome.Unavailable ->
            LemmasResult.Unavailable(outcome.cause)
    }
}
```

Что зовёт:

```kotlin
// Outcome — обмен с сервером до разбора тела: 200 с телом,
// отказ с причиной, недоступен с причиной для лога. Общий
// для lookup и lemmasFromTokens. Новый.
private sealed interface Outcome<out T> {
    data class Success<T>(
        // body — тело 200 как разобрал транспорт.
        val body: T,
    ) : Outcome<T>

    data class Denied(
        // reason — по коду HTTP и коду в теле.
        val reason: DenyReason,
    ) : Outcome<Nothing>

    data class Unavailable(
        // cause — для лога.
        val cause: String,
    ) : Outcome<Nothing>
}

// call — всё, что у операций одинаково: ключ не задан →
// Denied без сети (ACC-19); не HTTPS → Unavailable
// (LFL-21); исключение сети или таймаут → Unavailable с
// причиной (LFL-22, LFL-23); 401, 429, 413 → Denied по
// коду, 400 — только unsupported_language, 503 — только
// llm_unavailable, иной код → Unavailable; 200 без тела
// → Unavailable; с телом → Success. Новая, вынесена из
// lookup.
private suspend fun <T> call(
    request: suspend () -> Response<T>,
): Outcome<T>

// toNet — запрос на провод один в один. Новая.
internal fun LemmasRequestApiEntity.toNet():
    LemmasRequestNet

// toApiEntity — тело 200 в зеркало один в один. Новая.
internal fun LemmasResponseNet.toApiEntity():
    LemmasResult.Success
```

## 3. Контракт

Что устройство берёт из `api/openapi.yaml` 0.4.0 — живые примеры в
`contract.md` рядом со спекой LFT:

- **Запрос** `POST /v1/lemmas/from-tokens`: `learningLanguage`,
  `translationLanguage`, `tokens` — те же `text`, `confidence`,
  `language`, что у `lookup`; дерева компонентов нет; заголовки
  `Authorization: Bearer` и `X-Run-Id`.
- **Ответ 200**: `text`, `translation`, `lexical`, `proper`,
  `function`, `unparsed` — строки.
- **Отказы** — те же коды и тело `{ code, message }`, что у `lookup`:
  401, 413, 400 `invalid_request` и `unsupported_language`, 429, 503
  `llm_unavailable`.
- **`words/lookup`** — на этапе 1 остаётся как в 0.3.0 для диалога
  карточки слова.

## 4. Ключевые решения

- **Один запрос на снимок** (Д87): нейронка на сервере собирает текст
  сама, устройство ждёт один ответ — у индикатора одна фаза
  «Анализирую текст…».
- **«Не разобрано» виден** (Д84): список приходит как есть и идёт в
  окно без фильтра.
- **Маршрут по образцу «что получаем / откуда»** (Д113):
  `lemmas/from-tokens`; `lexemes/from-lemma` — этап 2.
- **Этап 1 — весь путь до списков** (Д116): запрос по слову,
  карточка и запись — этап 2.
- **Доступ к файлу галереи закрепляет вкладка** (Д52): как в первой
  версии, экран отпускает его после распознавания.
- **Слои типов** (Д57): порт говорит доменом, `app/mapper` переводит
  зеркала в домен, шлюз — транспорт в зеркала.

## 5. Сквозное

- **Отказы** — кто решает на каком слое: шлюз — код HTTP и код в теле
  → причина `DenyReason`, иное → недоступен; `app/mapper` — четыре
  пустых списка → «слов нет»; хендлер экрана — причина → текст и есть
  ли «Повторить»; редьюсер — фаза. Тексты: 401 и ключ не задан — «Нет
  доступа к серверу», 429 — «Лимит на сегодня исчерпан», 413 —
  «Слишком много слов», 400 язык — «Язык словаря не поддерживается»,
  503 — «Анализ текста сейчас недоступен, попробуй позже», сеть и
  таймаут — «Сервер не ответил» (AWP-53).
- **Повтор** — токены лежат в стейте экрана, «Повторить» шлёт их же с
  новым номером запуска, без снимка и распознавания (TFP-26, TFP-01).
- **Отмена** — «Отменить» на индикаторе закрывает экран; с ним
  умирает цикл и его эффекты, ответу некуда прийти (TFP-21).
- **Перезапуск** — процесс убит в камере или галерее: снимок приходит
  во вкладку при закрытом диалоге и открывает экран (TFP-22); убит на
  экране — маршрут не восстанавливается (TFP-23).
- **Бюджет времени** — 180 секунд на устройстве, 170 — на сервере
  (LFT-15): сервер сдаётся раньше клиента.
- **Секреты** — ключ доступа из сборки ставит перехватчик, в логи
  устройства не попадает (ACC-77).
- **Лог** — строка исхода `###SCAN###` с числом слов в списках; номер
  запуска в `X-Run-Id` склеивает логи устройства и сервера.
- **Строки** — новая `scan_analysis_unavailable`: «Анализ текста
  сейчас недоступен, попробуй позже» и английский вариант; остальные
  тексты отказов — как были.

## 6. Выкатка

- **Порядок** — сначала бэкенд с операцией `lemmas/from-tokens`
  (тег v0.1.32, `hld-lft.md`), потом сборка `co.lexeme.app.dev`:
  сборка без операции на сервере получит 404 → «Сервер не ответил».
- **Старая сборка** — не задета: она зовёт только `words/lookup`,
  который на этапе 1 не меняется.
- **Релиз мобилки** — после всей IS536 одним релизом (Д116).

## 7. Риски

- **Предел 2000** — у устройства (TFP-15) и у сервера (LFT-04) одно
  число; разойдутся — 413 «Слишком много слов» без повтора.
- **Долгий ответ** — плотная страница в 2000 токенов отвечает
  минуту и больше, индикатор без прогресса; 180 секунд — потолок.
- **Форма ответа сервера** — подбирается пробником до тега;
  транспорт с умолчаниями не падает на новом поле, но пропавшее
  поле даст пустой список.
- **Качество распознавания** — как в первой версии: обрывки у края
  кадра уходят в «не разобрано» и видны пользователю (Д84).

## 8. Как перерисовать схемы

Условные знаки — Mermaid, путь — Graphviz: Mermaid тасует порядок
блоков. Источник в `.dot` рядом, картинка рендерится скриптом, в
документ идёт по абсолютному пути с хешем содержимого в имени:
превью студии кеширует картинку по имени. Перерисовать все схемы
папки и обновить ссылки:

```
scripts/mmd-render.sh docs/features/IS536_llm_values
```
