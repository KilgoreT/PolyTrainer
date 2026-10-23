# IS536 | План приложения

LLD к `hld.md`, раздел 3. Этапы в порядке выполнения, независимое
первым; внутри этапа — триады: было — нынешний код из репо, будет —
код после этапа, мотивация — зачем он меняется. Строки нынешнего кода
перенесены под шестьдесят знаков, текст тот же. Код — по команде.
Этап выполнен — пометка «ВЫПОЛНЕН <дата>» с одной фразой, что
разошлось с планом.

Команды: сборка и тесты только `./scripts/cc-build.sh <задача>`, тесты
по одному модулю: `:modules:screen:wordstab:testDebugUnitTest` и так
далее; на устройство только `co.lexeme.app.dev`.

## Э1. Домен ответа и формы

ВЫПОЛНЕН 2026-10-20. Токены — `LookupToken` с уверенностью и языком, не
строки; правило исхода по слову `wordLookupOf` — в домене, не в
юзкейсе.

Тезисов нет: слой типов (Д57). Проверка: компиляция; фикстуры домена
`LookupFixtures` для тестов следующих этапов.

### Исход и данные ответа

**Было.** В `widget/candidates/PhotoLookup.kt`, не в домене:

```kotlin
sealed interface PhotoLookupOutcome {
    data class Words(
        val dictionaryId: Long,
        val words: List<PhotoWord>,
    ) : PhotoLookupOutcome
    data class Denied(
        val reason: PhotoDenyReason,
    ) : PhotoLookupOutcome
    data object Unavailable : PhotoLookupOutcome
}

data class PhotoWord(
    val text: String,
    val status: PhotoWordStatus,
    val lemmas: List<PhotoLemma>,
)

data class PhotoLexeme(
    val key: String,
    val partOfSpeechKey: String?,
    val source: String,
    val translations: List<String>,
    val gloss: String?,
    val caption: String?,
    val notes: List<String>,
    val examples: List<String>,
    val parent: Boolean,
    val parentKey: String?,
)
```

**Будет.** В `modules/domain/lexeme`, файл `Lookup.kt`:

```kotlin
// LookupOutcome — исход запроса, по типу на случай: слова
// есть, ответ без слов, отказ с причиной, сервер не ответил
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

data class LookupWord(
    val text: String,
    val status: LookupWordStatus,
    val lemmas: List<LookupLemma>,
    val remarks: List<WordRemark>,
)

data class LookupLemma(
    val text: String,
    val formNote: String?,
    val pronunciations: List<String>,
    val lexemes: List<LookupLexeme>,
)

// LookupLexeme — данные записи целиком (Д23) и
// сопоставление; без parent и parentKey (Д37)
data class LookupLexeme(
    val key: String,
    val partOfSpeechKey: String?,
    val source: String,
    val translations: List<String>,
    val gloss: String?,
    val caption: String?,
    val notes: List<String>,
    val examples: List<String>,
    val grammar: Map<String, String>,
    val level: String?,
    val synonyms: List<String>,
    val antonyms: List<String>,
    val componentMatches: List<ComponentMatch>,
)

data class ComponentMatch(
    val componentId: Long,
    val optionId: Long?,
    val field: String?,
)

sealed interface WordRemark {
    data class ComponentUnclear(
        val componentId: Long,
    ) : WordRemark

    data class NoTranslation(
        val lemma: String,
        val lexemeKey: String,
    ) : WordRemark
}

data class ComponentRecommendation(
    val name: String,
    val template: ComponentTemplate,
    val options: List<String>,
)

enum class LookupDenyReason {
    NO_ACCESS_KEY, UNAUTHORIZED, RATE_LIMITED,
    REQUEST_TOO_LARGE, UNSUPPORTED_LANGUAGE,
    LLM_UNAVAILABLE,
}

enum class LookupWordStatus { FOUND, NOT_FOUND, ERROR }

// WordLookupOutcome — исход запроса по слову карточки:
// найдено, ни одной лексемы-кандидата, слово удалено,
// отказ, сервер не ответил (WCL-04.7, WCL-11)
sealed interface WordLookupOutcome {
    data class Found(
        val word: LookupWord,
        val recommendations: List<ComponentRecommendation>,
    ) : WordLookupOutcome

    data object NotFound : WordLookupOutcome
    data object WordMissing : WordLookupOutcome
    data class Denied(
        val reason: LookupDenyReason,
    ) : WordLookupOutcome
    data object Unavailable : WordLookupOutcome
}

// RecognitionOutcome — исход распознавания: токены как
// распознаны или пусто
sealed interface RecognitionOutcome {
    data class Text(
        val tokens: List<String>,
    ) : RecognitionOutcome
    data object Empty : RecognitionOutcome
}
```

**Мотивация.** Порты говорят доменом, один порт служит двум окнам,
исходы живут с данными (Д57).

### Форма словаря

**Было.** В `widget/candidates/PhotoAdd.kt`:

```kotlin
sealed interface PhotoFormOutcome {
    data class Loaded(
        val form: PhotoDictionaryForm,
        val remembered: PhotoMapping?,
    ) : PhotoFormOutcome
    data object DictionaryMissing : PhotoFormOutcome
    data class Failed(val cause: String) : PhotoFormOutcome
}

data class PhotoDictionaryForm(
    val dictionaryId: Long,
    val translationLanguage: String,
    val components: List<PhotoComponent>,
    val partOfSpeechOptions: List<PhotoPartOfSpeechOption>,
    val groups: List<PhotoGroup>,
    val existingLemmas: List<PhotoExistingLemma>,
)
```

**Будет.** В домене, файл `DictionaryForm.kt`:

```kotlin
// FormOutcome — исход чтения формы: форма, словарь удалён,
// чтение упало; без запомненных соответствий (AWP-06)
sealed interface FormOutcome {
    data class Loaded(
        val form: DictionaryForm,
    ) : FormOutcome
    data object DictionaryMissing : FormOutcome
    data class Failed(val cause: String) : FormOutcome
}

// DictionaryForm — все включённые живые компоненты с
// зависимостями, опции всех «Выборов» по id компонента
// (AWP-17.5), группы, леммы словаря
data class DictionaryForm(
    val dictionaryId: Long,
    val translationLanguage: String,
    val components: List<FormComponent>,
    val options: Map<Long, List<FormOption>>,
    val groups: List<FormGroup>,
    val existingLemmas: List<ExistingLemma>,
)

data class FormComponent(
    val id: Long,
    val systemKey: BuiltInComponent?,
    val name: String?,
    val template: ComponentTemplate,
    val isCore: Boolean,
    val isMultiple: Boolean,
    val position: Int,
    val dependsOn: DependencyTarget,
)
```

**Мотивация.** Форма — тоже домен: её читают оба окна, а зависимость
и шаблон уже доменные.

## Э2. Сеть: зеркала и транспорт

ВЫПОЛНЕН 2026-10-20. Поля `llm` нет; шаблон и замечание в зеркалах —
строкой и плоско, как на проводе; 503 без кода `llm_unavailable` —
«сервер не ответил».

Тезисы: WCL-11.6; контракт 1 и 5. Проверка: `WordsApiImplTest` плюс
тесты мапперов на JSON из контракта.

### Зеркала

**Было.** `core-network-api/entity`:

```kotlin
sealed interface LookupResult {
    data class Success(
        val attribution: String,
        val words: List<WordResult>,
    ) : LookupResult
    data class Denied(val reason: DenyReason) : LookupResult
    data class Unavailable(val cause: String) : LookupResult
}

data class WordResult(
    val text: String,
    val status: WordStatus,
    val candidates: List<Candidate>,
    val lemmas: List<Lemma> = emptyList(),
)

data class CandidateLexeme(
    val key: String,
    val partOfSpeechKey: String?,
    val source: String,
    val translations: List<String>,
    val gloss: String?,
    val caption: String?,
    val notes: List<String>,
    val examples: List<String>,
    val parent: Boolean,
    val parentKey: String?,
)

data class LookupRequest(
    val learningLanguage: String,
    val translationLanguage: String,
    val tokens: List<TokenQuery>,
)
```

**Будет.**

```kotlin
// LookupResult — исход вызова, остаётся: Success с
// зеркалами внутри, Denied, Unavailable
sealed interface LookupResult {
    data class Success(
        val attribution: String,
        val words: List<WordApiEntity>,
        val recommendations:
            List<ComponentRecommendationApiEntity>,
        val llm: Boolean,
    ) : LookupResult
    data class Denied(val reason: DenyReason) : LookupResult
    data class Unavailable(val cause: String) : LookupResult
}

// WordApiEntity — было WordResult: без candidates (Д46),
// плюс remarks
data class WordApiEntity(
    val text: String,
    val status: WordStatus,
    val lemmas: List<LemmaApiEntity>,
    val remarks: List<WordRemarkApiEntity>,
)

data class LemmaApiEntity(
    val text: String,
    val formNote: String?,
    val pronunciations: List<String>,
    val lexemes: List<CandidateLexemeApiEntity>,
)

// CandidateLexemeApiEntity — было CandidateLexeme: плюс
// grammar, level, synonyms, antonyms, componentMatches;
// без parent и parentKey
data class CandidateLexemeApiEntity(
    val key: String,
    val partOfSpeechKey: String?,
    val source: String,
    val translations: List<String>,
    val gloss: String?,
    val caption: String?,
    val notes: List<String>,
    val examples: List<String>,
    val grammar: Map<String, String>,
    val level: String?,
    val synonyms: List<String>,
    val antonyms: List<String>,
    val componentMatches: List<ComponentMatchApiEntity>,
)

// LookupRequestApiEntity — плюс дерево компонентов
data class LookupRequestApiEntity(
    val learningLanguage: String,
    val translationLanguage: String,
    val tokens: List<TokenApiEntity>,
    val components: List<ComponentNodeApiEntity>,
)

enum class DenyReason {
    NO_ACCESS_KEY, UNAUTHORIZED, RATE_LIMITED,
    REQUEST_TOO_LARGE, UNSUPPORTED_LANGUAGE,
    LLM_UNAVAILABLE,
}
```

**Мотивация.** Контракт 0.3.0 без `candidates` ломает разбор у старой
сборки, совместимость не нужна (Д46); имена — по слоям типов, зеркало
= `*ApiEntity` (Д57).

### Транспорт и маппер

**Было.** `core-network-impl/dto/LookupDto.kt`,
`mapper/LookupMapper.kt`:

```kotlin
internal data class WordResultDto(
    @SerialName("text") val text: String,
    @SerialName("status") val status: String,
    @SerialName("candidates")
    val candidates: List<CandidateDto>,
    @SerialName("lemmas")
    val lemmas: List<LemmaDto> = emptyList(),
)

internal fun LookupRequest.toDto(): LookupRequestDto
internal fun LookupResponseDto.toSuccess():
    LookupResult.Success
private fun WordResultDto.toEntity(): WordResult
private fun CandidateLexemeDto.toEntity(): CandidateLexeme
private fun CandidateDto.toEntity(): Candidate
```

**Будет.** Классы `*Net` в `net/LookupNet.kt` один в один с openapi
0.3.0-draft; `mapper/LookupMappers.kt`:

```kotlin
internal data class WordResultNet(
    @SerialName("text") val text: String,
    @SerialName("status") val status: String,
    @SerialName("lemmas")
    val lemmas: List<LemmaNet> = emptyList(),
    @SerialName("remarks")
    val remarks: List<WordRemarkNet> = emptyList(),
)

// toNet — запрос с деревом компонентов
internal fun LookupRequestApiEntity.toNet():
    LookupRequestNet

// toApiEntity — ответ в зеркала; CandidateDto и SenseDto
// удалены вместе с candidates
internal fun LookupResponseNet.toApiEntity():
    LookupResult.Success
private fun WordResultNet.toApiEntity(): WordApiEntity
private fun LemmaNet.toApiEntity(): LemmaApiEntity
private fun CandidateLexemeNet.toApiEntity():
    CandidateLexemeApiEntity
private fun WordRemarkNet.toApiEntity(): WordRemarkApiEntity
```

**Мотивация.** Транспорт прячет impl, наружу — зеркала; имена
`toApiEntity()` и `toNet()` по слоям типов (Д57).

### Отказ 503

**Было.**

```kotlin
private fun denialOf(
    response: Response<LookupResponseDto>?,
): LookupResult {
    val code = response?.code()
        ?: return unavailableOf(null)
    return when (code) {
        HTTP_UNAUTHORIZED ->
            LookupResult.Denied(DenyReason.UNAUTHORIZED)
        HTTP_TOO_MANY_REQUESTS ->
            LookupResult.Denied(DenyReason.RATE_LIMITED)
        HTTP_PAYLOAD_TOO_LARGE ->
            LookupResult.Denied(
                DenyReason.REQUEST_TOO_LARGE,
            )
        HTTP_BAD_REQUEST -> badRequestOf(response)
        else -> LookupResult.Unavailable(
            cause = "http $code",
        )
    }
}
```

**Будет.**

```kotlin
// denialOf — плюс 503 с кодом llm_unavailable → Denied
// (LLM_UNAVAILABLE); 503 без кода — Unavailable, как прежде
private fun denialOf(
    response: Response<LookupResponseNet>?,
): LookupResult {
    val code = response?.code()
        ?: return unavailableOf(null)
    return when (code) {
        HTTP_UNAUTHORIZED ->
            LookupResult.Denied(DenyReason.UNAUTHORIZED)
        HTTP_TOO_MANY_REQUESTS ->
            LookupResult.Denied(DenyReason.RATE_LIMITED)
        HTTP_PAYLOAD_TOO_LARGE ->
            LookupResult.Denied(
                DenyReason.REQUEST_TOO_LARGE,
            )
        HTTP_BAD_REQUEST -> badRequestOf(response)
        HTTP_UNAVAILABLE -> unavailableOf(response)
        else -> LookupResult.Unavailable(
            cause = "http $code",
        )
    }
}

// unavailableOf — тело с кодом llm_unavailable → Denied
// (LLM_UNAVAILABLE), иначе Unavailable
private fun unavailableOf(
    response: Response<LookupResponseNet>?,
): LookupResult
```

**Мотивация.** «Сервер сейчас недоступен» — своя причина со своим
текстом и «Повторить» (WCL-11.6, Д53).

## Э3. Юзкейсы в `app`

ВЫПОЛНЕН 2026-10-20 (агент, вместе с Э7). Дерево и форма из
`ComponentTreeLoader`, номер запуска — счётчик в юзкейсе; отклонения в
errata.

Тезисы: WFP-25, WFT-78, WCL-03.5, WCL-04.7, WCL-11.7. Проверка:
`LookupMappersTest` вместо `PhotoLookupMapperTest`, тесты
`ComponentTreeLoader` на словаре с зависимостями, тесты реализаций
на моках `CoreNetworkApi` и `CoreDbApi`.

### Зеркало → домен

**Было.** `app/mapper/PhotoLookupMapper.kt`, в типы виджета:

```kotlin
fun LookupResult.toOutcome(
    dictionaryId: Long,
): PhotoLookupOutcome = when (this) {
    is LookupResult.Success -> PhotoLookupOutcome.Words(
        dictionaryId = dictionaryId,
        words = words.map { it.toPhotoWord() },
    )
    is LookupResult.Denied ->
        PhotoLookupOutcome.Denied(
            reason.toPhotoDenyReason(),
        )
    is LookupResult.Unavailable ->
        PhotoLookupOutcome.Unavailable
}

private fun CandidateLexeme.toPhotoLexeme(): PhotoLexeme =
    PhotoLexeme(
        key = key,
        partOfSpeechKey = partOfSpeechKey,
        source = source,
        translations = translations,
        gloss = gloss,
        caption = caption,
        notes = notes,
        examples = examples,
        parent = parent,
        parentKey = parentKey,
    )
```

**Будет.** `app/mapper/LookupMappers.kt`, в домен:

```kotlin
// toDomain — исход сети → доменный исход: Success → Words,
// или NoWords при пустом ответе; Denied → Denied;
// Unavailable → Unavailable
fun LookupResult.toDomain(
    dictionaryId: Long,
): LookupOutcome = when (this) {
    is LookupResult.Success -> when {
        words.isEmpty() -> LookupOutcome.NoWords
        else -> LookupOutcome.Words(
            dictionaryId = dictionaryId,
            words = words.map { it.toDomain() },
            recommendations =
                recommendations.map { it.toDomain() },
        )
    }
    is LookupResult.Denied ->
        LookupOutcome.Denied(reason.toDomain())
    is LookupResult.Unavailable -> LookupOutcome.Unavailable
}

fun WordApiEntity.toDomain(): LookupWord
fun LemmaApiEntity.toDomain(): LookupLemma
fun CandidateLexemeApiEntity.toDomain(): LookupLexeme
fun WordRemarkApiEntity.toDomain(): WordRemark
fun ComponentRecommendationApiEntity.toDomain():
    ComponentRecommendation
fun DenyReason.toDomain(): LookupDenyReason
```

**Мотивация.** Реализация порта — единственное место, где зеркала
становятся доменом (Д57); «нет слов» решает маппер, не редьюсер.

### Запрос с деревом

**Было.** `WordsTabUseCaseImpl.lookupWords` и `lookupRequestOf`:

```kotlin
override suspend fun lookupWords(
    tokens: List<RecognizedWord>,
    runId: Long,
): PhotoLookupOutcome {
    val dictionary = currentDictionary()
    val request = lookupRequestOf(dictionary, tokens)
    val result = wordsApi.lookup(request, runId)
    return result.toOutcome(dictionaryId = dictionary.id)
}

fun lookupRequestOf(
    dictionary: DictionaryApiEntity,
    tokens: List<RecognizedWord>,
): LookupRequest = LookupRequest(
    learningLanguage = dictionary.learningLanguage,
    translationLanguage = dictionary.translationLanguage,
    tokens = tokens.map { it.toTokenQuery() },
)
```

**Будет.** `ScanCandidatesUseCaseImpl` в `app/di/module/
scancandidates`:

```kotlin
// lookupWords — WFP Ш6: текущий словарь, дерево его
// компонентов (Д28, WFT-78), запрос, исход в домен
override suspend fun lookupWords(
    tokens: List<String>,
): LookupOutcome {
    val dictionary = currentDictionary()
    val components = treeLoader.treeOf(dictionary.id)
    val request = lookupRequestOf(
        dictionary, tokens, components,
    )
    val result = wordsApi.lookup(request, runIdOf())
    return result.toDomain(dictionaryId = dictionary.id)
}

// recognize — WFP Ш4–Ш5: ML Kit по файлу, токены как
// распознаны, с повторами, пустые прочь, потолок 2000 (Д22)
override suspend fun recognize(
    uri: String,
): RecognitionOutcome

// releaseImageAccess — отпустить доступ к файлу галереи,
// закреплённый вкладкой (Д52); у файла камеры — ничего
override suspend fun releaseImageAccess(uri: String)

// ComponentTreeLoader.treeOf — дерево компонентов словаря:
// корень — включённые живые от лексемы; у «Выбора» живые
// опции; children компонента — зависящие от него, children
// опции — от неё; key у встроенных, name у своих (WFT-78)
suspend fun treeOf(
    dictionaryId: Long,
): List<ComponentNodeApiEntity>

fun lookupRequestOf(
    dictionary: DictionaryApiEntity,
    tokens: List<String>,
    components: List<ComponentNodeApiEntity>,
): LookupRequestApiEntity
```

**Мотивация.** Дерево компонентов — то, по чему нейронка
сопоставляет (Д8, Д28); распознавание и токены уходят из вкладки в
порт экрана (Д51).

### Порт карточки

**Было.** `WordCardUseCase`:

```kotlin
suspend fun lookupWord(
    wordId: Long,
    runId: Long,
): me.apomazkin.candidates.PhotoLookupOutcome?

suspend fun loadCandidatesForm(
    wordId: Long,
    lemmaTexts: List<String>,
): me.apomazkin.candidates.PhotoFormOutcome
```

**Будет.**

```kotlin
// lookupWord — WCL Ш1: один токен — текст слова, языки и
// дерево словаря слова; случаи типом вместо null
suspend fun lookupWord(
    wordId: Long,
    runId: Long,
): WordLookupOutcome

suspend fun loadCandidatesForm(
    wordId: Long,
    lemmaTexts: List<String>,
): FormOutcome
```

**Мотивация.** Порт говорит доменом; «слово удалено» — случай, а не
null (К12, Д57).

## Э4. `widget/candidates`: UI-типы и функции

ВЫПОЛНЕН 2026-10-20 (типы и функции — оркестратор, composable и тесты —
агент). Строки «Замечаний» и «Сопоставления» несут id, не имена;
`summaryOf` без формы; флага `hasRetryButton` в виджете нет.

Тезисы: AWP-04, AWP-07, AWP-10, AWP-11, AWP-13, AWP-15, AWP-16,
AWP-17, AWP-29, AWP-30, AWP-31, AWP-32, AWP-34; код отменённых AWP-05,
AWP-06, AWP-09, AWP-28, AWP-33 снят. Проверка: тесты чистых функций
на фикстурах из контракта `perro`, `corrían`, `banco`, `zarpó`;
`PhotoMappingRulesTest` удалён.

### Карточка и лексема

**Было.** `PhotoWindowState.kt`:

```kotlin
data class PhotoCard(
    val lemma: String,
    val sourceWords: List<String>,
    val lexemes: List<PhotoCardLexeme>,
    val hiddenCount: Int,
    val existingCount: Int,
    val target: PhotoCardTarget,
    val ownGroup: PhotoOwnGroup,
    val isExpanded: Boolean,
    val formNote: String? = null,
)

data class PhotoCardLexeme(
    val key: String,
    val source: String,
    val translation: PhotoEditableText,
    val hasOwnTranslations: Boolean,
    val isEnglishFallback: Boolean,
    val gloss: PhotoEditableText?,
    val caption: String?,
    val notes: List<String>,
    val examples: List<PhotoEditableText>,
    val partOfSpeechOptionId: Long?,
    val parent: Boolean,
    val parentKey: String?,
    val exists: Boolean,
    val lacksCore: Boolean,
    val existsInLemma: Boolean = false,
)
```

**Будет.** Имена без приставки (Д56), по роли (Д57):

```kotlin
// LemmaCardUiItem — карточка леммы; без hiddenCount,
// «ещё N» нет (AWP-05 отменён)
data class LemmaCardUiItem(
    val lemma: String,
    val sourceWords: List<String>,
    val lexemes: List<CardLexemeUiItem>,
    val existingCount: Int,
    val target: CardTargetUi,
    val ownGroup: CardOwnGroupUi,
    val isExpanded: Boolean,
    val formNote: String? = null,
)

// CardLexemeUiItem — вместо полей ответа значения по
// компонентам словаря в порядке компонентов (AWP-29);
// «англ.» по замечанию сервера (AWP-34)
data class CardLexemeUiItem(
    val key: String,
    val source: String,
    val values: List<CandidateValueUiItem>,
    val isEnglishFallback: Boolean,
    val exists: Boolean,
    val lacksCore: Boolean,
    val existsInLemma: Boolean = false,
)

// CandidateValueUiItem — значение одного компонента;
// isActive — зависимость выполнена (AWP-17.6)
sealed interface CandidateValueUiItem {
    val componentId: Long
    val isActive: Boolean

    data class Text(
        override val componentId: Long,
        val texts: List<EditableTextUiItem>,
        val field: String?,
        override val isActive: Boolean = true,
    ) : CandidateValueUiItem

    data class Choice(
        override val componentId: Long,
        val options: List<Long>,
        override val isActive: Boolean = true,
    ) : CandidateValueUiItem
}

data class ValueRefUi(
    val lemma: String,
    val key: String,
    val componentId: Long,
    val index: Int,
)

data class ComponentRefUi(
    val lemma: String,
    val key: String,
    val componentId: Long,
)
```

**Мотивация.** Окно показывает то, что сервер сопоставил, а не
раскладывает само (Д8, Д18).

### Листы и сводка

**Было.**

```kotlin
sealed interface PhotoSheet {
    data object CommonGroup : PhotoSheet
    data class CardGroup(val lemma: String) : PhotoSheet
    data class CardTarget(val lemma: String) : PhotoSheet
    data class PartOfSpeech(
        val lemma: String, val key: String,
    ) : PhotoSheet
    data class MappingTarget(
        val kind: PhotoDataKind,
    ) : PhotoSheet
    data class TextEdit(
        val lemma: String,
        val key: String,
        val field: PhotoTextField,
        val draft: String,
    ) : PhotoSheet
}

data class PhotoAddSummary(
    val lexemeCount: Int = 0,
    val wordCount: Int = 0,
    val existingCount: Int = 0,
    val lacksCoreCount: Int = 0,
    val noCoreMapping: Boolean = false,
    val isEnabled: Boolean = false,
)
```

**Будет.**

```kotlin
sealed interface CandidatesSheetUi {
    data object CommonGroup : CandidatesSheetUi
    data class CardGroup(
        val lemma: String,
    ) : CandidatesSheetUi
    data class CardTarget(
        val lemma: String,
    ) : CandidatesSheetUi
    // опция любого «Выбора», не только «Части речи»
    data class Option(
        val ref: ComponentRefUi,
    ) : CandidatesSheetUi
    // правка по адресу значения; canReset ставят атомы,
    // UI сам не сравнивает (AWP-16.6)
    data class TextEdit(
        val ref: ValueRefUi,
        val draft: String,
        val canReset: Boolean,
    ) : CandidatesSheetUi
}

// AddSummaryUi — без noCoreMapping: ядро у каждой лексемы
// есть (Д49)
data class AddSummaryUi(
    val lexemeCount: Int = 0,
    val wordCount: Int = 0,
    val existingCount: Int = 0,
    val lacksCoreCount: Int = 0,
    val isEnabled: Boolean = false,
)
```

**Мотивация.** Соответствий нет (AWP-09), лист правки адресует
значение, флаг «сбросить» в стейте, не в UI.

### Чистые функции

**Было.** `PhotoCards.kt`, `PhotoAddMappers.kt`,
`PhotoMappingRules.kt`:

```kotlin
fun buildPhotoCards(
    words: List<PhotoWord>,
    form: PhotoDictionaryForm,
): PhotoCardsBuild

fun List<PhotoCard>.withExistsMarks(
    keys: Set<String>,
): List<PhotoCard>

fun visibleLexemes(
    lexemes: List<PhotoLexeme>,
): List<PhotoLexeme>

fun wordsBatchOf(
    cards: List<PhotoCard>,
    form: PhotoDictionaryForm,
    mapping: PhotoMapping,
    commonGroupId: Long?,
): WordsBatch

fun PhotoCardLexeme.componentsFor(
    mapping: PhotoMapping,
    form: PhotoDictionaryForm,
): List<BatchComponent>

fun PhotoDictionaryForm.resolveMapping(
    remembered: PhotoMapping?,
): PhotoMapping
```

**Будет.** Сигнатуры и комментарии — в HLD, «Чистые функции
карточек»: `buildLemmaCards(words, form): CardsBuildUi`,
`withActiveValues`, `withExistsMarks(form)`, `withCoreMarks`,
`summaryOf`, `remarksOf`, `componentMatchLinesOf`,
`wordsBatchOf(cards, form, commonGroupId)`; `visibleLexemes` и
`PhotoMappingRules.kt` удалены. Плюс `mapper/`:

```kotlin
// toUi — поле в поле: домен → UI-двойник
fun LookupWord.toUi(): LookupWordUi
fun LookupLemma.toUi(): LookupLemmaUi
fun LookupLexeme.toUi(): LookupLexemeUi
fun ComponentRecommendation.toUi():
    ComponentRecommendationUi
fun DictionaryForm.toUi(): DictionaryFormUi
fun AddWordsBatchOutcome.Rejected.toUi(): AddFailureUi
```

**Мотивация.** Сборка, пометки и пакет идут по сопоставлению, не по
соответствиям; домен в редьюсер и composable не попадает (Д57).

### Composable

**Было.** `ui/PhotoAddWindow.kt` — окно как `Dialog` на весь экран;
`PhotoCardWidget` рисует перевод, толкование, примеры, лист «Части
речи»; `PhotoSheets` — листы по полю; «ещё N».

**Будет.** `PhotoAddWindow` → `ScanCandidatesContent` без `Dialog`,
его рисует экран; карточка рисует `values` рядами по компонентам:
текст с «Сбросить» по `canReset`, «Выбор» с опцией; пометка «англ.»;
разделы «Замечания» из `remarks` и «Сопоставление» из
`componentMatchLines`, раскрытие по `isComponentMatchesExpanded`;
листы `Option` и `TextEdit` по адресу; кнопка «Повторить» по
`hasRetryButton`.

**Мотивация.** Экран рисует стейт без толкования: есть флаг — есть
кнопка: флаги UI — явные поля стейта.

## Э5. Экран `screen/scancandidates`

ВЫПОЛНЕН 2026-10-20 (агент). Фаза `NO_WORDS` вместо `isEmptyResult`,
закрытие `NavigationEffect.Back`, без `UiHost`; отклонения в errata.

Тезисы: WFP-10, WFP-13, WFP-16, WFP-20, WFP-21, WFP-24, WFP-26,
AWP-03, AWP-12, AWP-14, AWP-19, AWP-20, AWP-21, AWP-23, AWP-24,
AWP-25, AWP-26. Проверка: тесты редьюсера по ветке `testReduce`,
атомов, хендлера «исход → сообщение»; фикстуры домена из Э1.

### Стейт

**Было.** `PhotoWordsState` внутри `WordsTabState`:

```kotlin
data class PhotoWordsState(
    val isOpen: Boolean = false,
    val isChoosingSource: Boolean = false,
    val phase: PhotoPhase = PhotoPhase.IDLE,
    val words: List<PhotoWord> = emptyList(),
    val isEmptyResult: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val requestId: Long = 0L,
    val dictionaryId: Long? = null,
    val form: PhotoDictionaryForm? = null,
    val formError: PhotoFormError? = null,
    val mapping: PhotoMapping = PhotoMapping.NONE,
    val commonGroupId: Long? = null,
    val cards: List<PhotoCard> = emptyList(),
    val notAdded: List<PhotoNotAdded> = emptyList(),
    val summary: PhotoAddSummary = PhotoAddSummary(),
    val isDirty: Boolean = false,
    val isAdding: Boolean = false,
    val addFailure: PhotoAddFailure? = null,
    val isConfirmingAdd: Boolean = false,
    val isConfirmingClose: Boolean = false,
    val isMappingExpanded: Boolean = false,
    val isNotAddedExpanded: Boolean = false,
    val sheet: PhotoSheet? = null,
)
```

**Будет.** Корневой стейт экрана, `mate/State.kt`:

```kotlin
data class ScanCandidatesState(
    val uri: String,
    val phase: ScanPhase = ScanPhase.RECOGNIZING,
    val tokens: List<String> = emptyList(),
    val dictionaryId: Long? = null,
    val words: List<LookupWordUi> = emptyList(),
    val recommendations: List<ComponentRecommendationUi> =
        emptyList(),
    val isEmptyResult: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val hasRetryButton: Boolean = false,
    val form: DictionaryFormUi? = null,
    val commonGroupId: Long? = null,
    val cards: List<LemmaCardUiItem> = emptyList(),
    val notAdded: List<NotAddedWordUiItem> = emptyList(),
    val remarks: List<RemarkUiItem> = emptyList(),
    val componentMatchLines:
        List<ComponentMatchLineUiItem> = emptyList(),
    val summary: AddSummaryUi = AddSummaryUi(),
    val isDirty: Boolean = false,
    val isAdding: Boolean = false,
    val addFailure: AddFailureUi? = null,
    val isConfirmingAdd: Boolean = false,
    val isConfirmingClose: Boolean = false,
    val isComponentMatchesExpanded: Boolean = false,
    val isNotAddedExpanded: Boolean = false,
    val sheet: CandidatesSheetUi? = null,
)
```

**Мотивация.** Экран живёт один снимок: `isOpen`, выбор источника и
номер запуска не нужны (Д51); ошибка формы — текстом в `errorRes`,
как остальные.

### Редьюсер и атомы

**Было.** Ветки окна в `WordsTabReducer` с проверкой запуска:

```kotlin
is Msg.TokensReady -> when {
    !state.isCurrentPhotoRequest(message.requestId) ->
        state.noOp("tokensReady: foreign run")
    message.tokens.isEmpty() -> state.showNoPhotoWords()
    else -> state
        .startLooking()
        .withEffect(DatasourceEffect.LookupWords(
            tokens = message.tokens,
            requestId = message.requestId,
        ))
}

is Msg.WordsLookedUp -> when {
    !state.isCurrentPhotoRequest(message.requestId) ->
        state.noOp("wordsLookedUp: foreign run")
    else -> when (val outcome = message.outcome) {
        is PhotoLookupOutcome.Words -> when {
            outcome.words.isEmpty() ->
                state.showNoPhotoWords()
            else -> state.startFormLoading(
                dictionaryId = outcome.dictionaryId,
                words = outcome.words,
            )
        }
        is PhotoLookupOutcome.Denied ->
            state.showPhotoError(errorResOf(outcome.reason))
        PhotoLookupOutcome.Unavailable ->
            state.showPhotoError(
                R.string.photo_words_error_server,
            )
    }
}
```

**Будет.** `ScanCandidatesReducer`, ветки целиком — в HLD,
«Экран лексем-кандидатов → Редьюсер». Три для сравнения:

```kotlin
// TextRecognized — хендлер RecognizeText, исход Text:
//   tokens — токены как распознаны, с повторами
is Msg.TextRecognized -> state.startLooking(message.tokens)
//   startLooking: tokens в стейт, фаза «ищу слова»,
//     эффект LookupWords(tokens)

// TextEmpty — хендлер RecognizeText, исход Empty. Без
// данных
Msg.TextEmpty -> state.showNoWords()
//   showNoWords: фаза «слов нет», только «Закрыть»

// WordsFound — хендлер LookupWords, исход Words:
//   dictionaryId, words, recommendations
is Msg.WordsFound -> state.startFormLoading(message)
//   startFormLoading: в стейт, фаза «собираю окно»,
//     эффект LoadForm

// LookupUnavailable — хендлер LookupWords, исход
// Unavailable: errorRes — текст причины
is Msg.LookupUnavailable ->
    state.showRetryableError(message.errorRes)
//   showRetryableError: фаза «ошибка», hasRetryButton

// RetryLookup — UI: «Повторить». Без данных
Msg.RetryLookup -> state.startLooking(state.tokens)
//   startLooking: фаза «ищу слова», эффект LookupWords
```

Атомы `ScanCandidatesAtoms` — переезд из `PhotoStateAtoms` без
номера запуска; новые:

```kotlin
// startLooking — tokens в стейт, фаза «ищу слова», эффект
// LookupWords(tokens)
fun ScanCandidatesState.startLooking(
    tokens: List<String>,
): ScanResult

// startFormLoading — dictionaryId, words, recommendations в
// стейт, фаза «собираю окно», эффект LoadForm
fun ScanCandidatesState.startFormLoading(
    found: Msg.WordsFound,
): ScanResult

// showError — фаза «ошибка», errorRes,
// hasRetryButton = false
fun ScanCandidatesState.showError(
    @StringRes errorRes: Int,
): ScanResult

// showRetryableError — то же, hasRetryButton = true (Д53)
fun ScanCandidatesState.showRetryableError(
    @StringRes errorRes: Int,
): ScanResult

// closeWithResult — эффект навигации назад с результатом
fun ScanCandidatesState.closeWithResult(
    lexemeCount: Int,
    wordCount: Int,
): ScanResult

// close — эффект навигации назад без результата
fun ScanCandidatesState.close(): ScanResult
```

**Мотивация.** Редьюсер ветвится только по стейту; исход разбирает
хендлер; чужой ответ в новый редьюсер прийти не может (Д51, К12,
К13).

### Хендлер и сборка

**Было.** `DatasourceEffectHandler` вкладки: эффект `RecognizeWords`
с `requestId`, `LookupWords` → `Msg.WordsLookedUp(outcome,
requestId)` с исходом целиком.

**Будет.** Хендлер экрана — `when` по доменному исходу прямо в ветке
эффекта, код в HLD, «Редьюсер → Хендлер эффектов». Сборка по образцу
`WordCardAssembly`:

```kotlin
object ScanCandidatesAssembly {
    fun create(
        useCase: ScanCandidatesUseCase,
        logger: LexemeLogger,
        navigationHandler:
            MateEffectHandler<Nothing, NavigationEffect>,
        uri: String,
        uiHost: UiHost,
        coroutineScope: CoroutineScope,
    ): Mate<ScanCandidatesState, Msg, Effect> =
        Mate(
            initState = ScanCandidatesState(uri = uri),
            initEffects = setOf(
                DatasourceEffect.RecognizeText(uri),
            ),
            coroutineScope = coroutineScope,
            reducer = ScanCandidatesReducer(logger),
            effectHandlers = listOf(
                DatasourceEffectHandler(useCase, logger),
                navigationHandler,
                UiEffectHandler(uiHost = uiHost),
            ),
            observers = listOf(
                ErrorLoggingObserver(logger, LogTags.SCAN),
            ),
        )
}
```

**Мотивация.** Первый эффект — распознавание, как `LoadWord` у
карточки; экран закрыли — цикл умер, ответу некуда прийти (Д51).

### Экран

**Было.** `PhotoWordsDialog` — выбор источника, фазы, ошибки в одном
маленьком `Dialog`; окно — `PhotoAddWindow` отдельным `Dialog` поверх
вкладки.

**Будет.** `ScanCandidatesScreen`: по `phase` — индикатор с подписью
«Распознаю текст», «Ищу слова», «Собираю окно»; по `errorRes` —
текст, «Закрыть» и «Повторить», если `hasRetryButton`; по
`isEmptyResult` — «Лексем-кандидатов для «…» нет»; иначе
`ScanCandidatesContent` с карточками, листами и вопросами; системный
«назад» → `Msg.RequestClose`.

**Мотивация.** Один экран от снимка до записи (Д51), ошибки в одном
месте, повтор без нового снимка (Д53).

## Э6. Вкладка `screen/wordstab`

ВЫПОЛНЕН 2026-10-20 (агент). Запуск камеры и галереи — порт
`ScanLauncher` с держателем во ViewModel, не `UiHost`; файл под снимок
создаётся до запуска камеры и удаляется при отказе и отмене.

Тезисы: WFP-04, WFP-05, WFP-06, WFP-07, WFP-08, WFP-09, WFP-18,
WFP-22, WFP-23, AWP-23. Проверка: тесты редьюсера вкладки на диалог
скана и переход; `PhotoWordsReducerTest`, `PhotoStateAtomsTest`,
`PhotoAddMappersTest` — удалить или перенести в Э5.

### Стейт и ветки

**Было.**

```kotlin
data class WordsTabState(
    …
    val photoWordsState: PhotoWordsState =
        PhotoWordsState(),
)

Msg.OpenPhotoSourceChooser -> state.showPhotoSourceChooser()

is Msg.PhotoPicked -> state
    .startRecognizing()
    .let { (next, effects) ->
        next to effects + RecognizeWords(
            uri = message.uri,
            requestId = next.photoWordsState.requestId,
        )
    }

Msg.PhotoPickCancelled -> when {
    !state.photoWordsState.isOpen ->
        state.noOp("photoPickCancelled: dialog closed")
    else -> state.returnToPhotoSourceChooser()
}
```

**Будет.** Стейт диалога и ветки целиком — в HLD, «Вкладка
«Слова»»:

```kotlin
data class WordsTabState(
    …
    val scanDialogState: ScanDialogState =
        ScanDialogState(),
)

data class ScanDialogState(
    val isOpen: Boolean = false,
    @StringRes val errorRes: Int? = null,
)

// OpenScanDialog — UI: кнопка-камера. Без данных
Msg.OpenScanDialog -> state.showScanDialog()
//   showScanDialog: диалог открыт, без ошибки

// ChooseCamera — UI: «Камера» в диалоге. Без данных
Msg.ChooseCamera ->
    state.withEffect(UiEffect.LaunchCamera)
//   LaunchCamera: UI-хост открывает системную камеру

// ChooseGallery — UI: «Галерея» в диалоге. Без данных
Msg.ChooseGallery ->
    state.withEffect(UiEffect.LaunchGallery)
//   LaunchGallery: UI-хост открывает выбор картинки

// ImagePicked — PhotoPicker: камера или галерея вернули
// снимок: uri — файл снимка строкой
is Msg.ImagePicked -> state
    .hideScanDialog()
    .withEffect(
        WordsNavigationEffect.OpenScanCandidates(
            message.uri,
        ),
    )
//   hideScanDialog: диалог закрыт
//   OpenScanCandidates: экран открыт с uri
```

`logic/photo` целиком и `CandidateActionMapper` удаляются.

**Мотивация.** Вкладка знает только вход: кнопку, диалог и переход;
кнопки диалога не решают сами, что запускать (Д51).

### Запуск камеры и доступ к файлу

**Было.** `ui/photo/PhotoPicker.kt`: `launchCamera()` и
`launchGallery()` зовут кнопки диалога напрямую; результат
`PickVisualMedia` отдаётся как есть.

**Будет.**

```kotlin
// UiHost вкладки исполняет эффекты LaunchCamera и
// LaunchGallery через PhotoPicker; «нет камеры» →
// Msg.CameraUnavailable(errorRes)
data object LaunchCamera : UiEffect
data object LaunchGallery : UiEffect

// PhotoPicker: результат галереи — закрепить доступ до
// отправки сообщения (Д52), провал — в лог
val pickImage = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
) { uri ->
    if (uri == null) {
        return@rememberLauncherForActivityResult
    }
    keepReadAccess(uri)
    onPicked(Msg.ImagePicked(uri.toString()))
}

private fun keepReadAccess(uri: Uri) {
    runCatching {
        resolver.takePersistableUriPermission(
            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }.onFailure { logger.w(TAG, "keep access", it) }
}
```

**Мотивация.** Снимок переживает гибель процесса в камере и галерее
(Д52, WFP-22).

### Результат с экрана

**Было.** `PhotoAddFinished` с проверкой запуска прямо во вкладке:

```kotlin
is Msg.PhotoAddFinished -> when {
    !state.isCurrentPhotoRequest(message.requestId) ->
        state.noOp("photoAddFinished: foreign run")
    else -> state
        .hidePhotoWords()
        .withEffect(UiEffect.ShowNotification(
            message = addedMessage(
                message.lexemeCount, message.wordCount,
            ),
        ))
}
```

**Будет.**

```kotlin
// ScanCandidatesAdded — навигация: экран закрылся после
// записи, вкладка прочитала результат из back stack
is Msg.ScanCandidatesAdded -> state.withEffect(
    UiEffect.ShowNotification(
        addedMessage(
            message.lexemeCount, message.wordCount,
        ),
    ),
)
//   ShowNotification: снекбар «Добавлено N значений в
//     M слов», стейт не меняется
```

**Мотивация.** Снекбар остаётся на вкладке, как сейчас (AWP-23), без
номера запуска.

## Э7. Навигация в `app`

ВЫПОЛНЕН 2026-10-20 (агент, вместе с Э3). Результат назад —
псевдоэкран `ScanCandidatesAdded` и `SavedStateHandle`, метка процесса
в `ProcessBound`; ручник гибели процесса — `adb shell am kill`, не «Не
сохранять действия»; отклонения в errata.

Тезисы: AWP-27, WFP-23, AWP-23. Проверка: `PilotScenarioTest` — путь
от кнопки до снекбара на пилоте; ручник гибели процесса через «Не
сохранять действия».

Допущение: результат назад — через `SavedStateHandle` предыдущей
записи стека; прецедента в коде нет, тот же механизм закроет хвост
IS533 «снекбар после переноса».

**Было.**

```kotlin
internal const val WORD_CARD_PATTERN = "wordCard/{wordId}"

composable(
    route = MainRoutes.WORD_CARD_PATTERN,
    arguments = listOf(
        navArgument(WORD_ID_ARG) {
            type = NavType.LongType
        },
    ),
) { navBackStackEntry ->
    val wordId: Long = navBackStackEntry.arguments
        ?.getLong(WORD_ID_ARG)
        ?: throw IllegalArgumentException("Unknown WordId")
    compositionRoot.WordCardScreenDep(wordId = wordId)
}

sealed interface AppScreen : Screen {
    …
    data class WordCard(val wordId: Long) : AppScreen
}

on<WordsNavigationEffect.OpenWordCard> {
    push(AppScreen.WordCard(it.wordId))
}
```

**Будет.**

```kotlin
internal const val SCAN_CANDIDATES_PATTERN =
    "scanCandidates?uri={uri}"

composable(
    route = MainRoutes.SCAN_CANDIDATES_PATTERN,
    arguments = listOf(
        navArgument(URI_ARG) { type = NavType.StringType },
    ),
) { navBackStackEntry ->
    val uri = navBackStackEntry.arguments
        ?.getString(URI_ARG)
        ?: throw IllegalArgumentException("Unknown uri")
    compositionRoot.ScanCandidatesScreenDep(uri = uri)
}

sealed interface AppScreen : Screen {
    …
    data class ScanCandidates(val uri: String) : AppScreen
}

on<WordsNavigationEffect.OpenScanCandidates> {
    push(AppScreen.ScanCandidates(it.uri))
}
on<ScanNavigationEffect.CloseWithResult> {
    popWithResult(
        SCAN_ADDED_KEY, it.lexemeCount to it.wordCount,
    )
}
on<ScanNavigationEffect.Close> { pop() }

// popWithResult — результат в SavedStateHandle предыдущей
// записи стека вкладок, затем pop; вкладка читает ключ
// потоком, шлёт ScanCandidatesAdded и стирает ключ
private fun popWithResult(key: String, value: Any)

// При старте приложения верх сохранённого стека с маршрутом
// scanCandidates выкидывается: после гибели процесса экран
// не восстанавливается (AWP-27)
```

**Мотивация.** Экран — отдельный маршрут со своей ViewModel, иначе
Д51 не работает; результат назад — единственное, чего в навигации
нет.

## Э8. Карточка слова `screen/wordcard`

ВЫПОЛНЕН 2026-10-20 (агент). Доработка по четырём эскалациям — тем же
агентом; слово `error` — свой исход домена `WordFailed`; отклонения в
errata.

Тезисы: WCL-02, WCL-04, WCL-10, WCL-11, WCL-15, WCL-17. Проверка:
тесты атомов и редьюсера карточки, фикстуры из домена.

**Было.**

```kotlin
data class CandidatesState(
    val wordId: Long,
    val text: String,
    val dictionaryId: Long,
    val requestId: Long,
    val phase: CandidatesPhase = CandidatesPhase.LOADING,
    @StringRes val errorRes: Int? = null,
    val words: List<PhotoWord> = emptyList(),
    val form: PhotoDictionaryForm? = null,
    val mapping: PhotoMapping = PhotoMapping.NONE,
    val cards: List<PhotoCard> = emptyList(),
    val summary: PhotoAddSummary = PhotoAddSummary(),
    val sheet: PhotoSheet? = null,
    val isMappingExpanded: Boolean = false,
    …
)

fun WordCardState.applyCandidatesLookup(
    outcome: PhotoLookupOutcome?,
): CandidatesResult {
    val dialog = candidates
        ?: return noOp("applyCandidatesLookup: no dialog")
    return when (outcome) {
        null -> showCandidatesError(
            R.string.candidates_word_missing,
        )
        is PhotoLookupOutcome.Denied ->
            showCandidatesError(errorResOf(outcome.reason))
        …
    }
}
```

**Будет.**

```kotlin
data class CandidatesState(
    val wordId: Long,
    val text: String,
    val dictionaryId: Long,
    val requestId: Long,
    val phase: CandidatesPhase = CandidatesPhase.LOADING,
    @StringRes val errorRes: Int? = null,
    val hasRetryButton: Boolean = false,
    val words: List<LookupWordUi> = emptyList(),
    val recommendations: List<ComponentRecommendationUi> =
        emptyList(),
    val form: DictionaryFormUi? = null,
    val cards: List<LemmaCardUiItem> = emptyList(),
    val remarks: List<RemarkUiItem> = emptyList(),
    val componentMatchLines:
        List<ComponentMatchLineUiItem> = emptyList(),
    val summary: AddSummaryUi = AddSummaryUi(),
    val sheet: CandidatesSheetUi? = null,
    val isComponentMatchesExpanded: Boolean = false,
    …
)

// Хендлер карточки: when по WordLookupOutcome, по
// сообщению на случай; applyCandidatesLookup уходит
is DatasourceEffect.LookupCandidates -> withContext(io) {
    val outcome = useCase.lookupWord(eff.wordId, eff.runId)
    when (outcome) {
        is WordLookupOutcome.Found -> Msg.CandidatesFound(
            outcome.word.toUi(),
            outcome.recommendations.map { it.toUi() },
            eff.runId,
        )
        WordLookupOutcome.NotFound ->
            Msg.CandidatesNotFound(eff.runId)
        WordLookupOutcome.WordMissing ->
            Msg.CandidatesLookupFailed(
                R.string.candidates_word_missing, eff.runId,
            )
        is WordLookupOutcome.Denied ->
            Msg.CandidatesLookupFailed(
                errorResOf(outcome.reason), eff.runId,
            )
        WordLookupOutcome.Unavailable ->
            Msg.CandidatesLookupUnavailable(
                R.string.scan_server_unavailable, eff.runId,
            )
    }
}
```

Атомы: `startCandidatesForm(found)`, `showCandidatesError`,
`showCandidatesRetryableError`, `deleteCandidateValue(ref)`,
`setCandidateOption(ref, option)`, `applyCandidatesRemarks`,
`applyCandidatesComponentMatchLines`,
`refreshCandidatesActiveValues`; номер запуска остаётся: диалог живёт
в ViewModel карточки.

**Мотивация.** Второе окно той же фичи: общая логика в
`widget/candidates`, своё — старт, запрос по слову, перенос в
лемму-форму (Д51).

## Э9. Ручники, покрытие, строки

Тезисы: все из Э4–Э8 со стороной «устройство». Проверка: прогон
ручников на `co.lexeme.app.dev`, `scripts/spec-coverage.sh` по трём
спекам.

**Было.** Ручники IS529 и IS532 в `docs/features/IS523_words_from_text/
*/manual_tests.md`; покрытие стороной «устройство» —
`SPEC_FILE=… SPEC_PREFIX=… SPEC_MANUALS=… ./scripts/spec-coverage.sh`;
строки снимка в ресурсах модулей.

**Будет.** `manual_tests.md` фичи: кейсы по AWP, WFP, WCL — снимок,
галерея, повтор при 503, гибель процесса в камере и на экране,
карточка; словарь логов. Покрытие по трём спекам без дыр, черновики →
«реализовано». Строки: «Добавить лексемы-кандидаты», «Сервер сейчас
недоступен, попробуй позже», «Лексем-кандидатов для «…» нет», «У
лексем-кандидатов есть «Род»…», «Повторить» (Д50, Д53).

**Мотивация.** Покрытие по стороне и ручники — ворота к коммиту.
