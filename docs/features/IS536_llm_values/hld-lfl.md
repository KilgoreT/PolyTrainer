# IS536 | HLD LFL — лексемы леммы, черновик

План бэкенда операции «лексемы леммы» по спеке LFL: лемма с текстом
снимка и переводом или без них → доступ, размер, язык и работа → лемма
в базе → значения → один вызов нейронки: отбор и сопоставление → слово
с лексемами-кандидатами. Что править и в каком файле — в LLD. Соседние
планы в этой папке: `hld-lft.md` — списки со снимка, `hld-acc.md` —
ключ, `hld-lim.md` — лимиты, `hld-awp.md` и `hld-wcl.md` — экраны.
Первая версия фичи целиком — `hld-v1.md`; здесь — что против неё
меняется, и то, что нужно, чтобы это прочитать.

## 1. Рамка

- **Цель** — по тапу на слово снимка пользователь получает до трёх
  значений, первое — то, в котором слово употреблено в тексте; из
  карточки слова — три самых употребительных. Значения сопоставлены с
  компонентами его словаря (Д8, Д10, Д74, Д90).
- **Вход и выход** — устройство шлёт лемму: по тапу — с исправленным
  текстом снимка и переводом из ответа LFT, из карточки — без них;
  назад уходит одно слово со статусом, леммами и лексемами-кандидатами
  (Д82, Д90).
- **Что меняется на сервере**:
  - запрос леммой с контекстом, ответ одним словом; нормализации
    токенов нет, лемма ищется как прислана (Д99);
  - по тапу — своя лемма со значениями или первая по связи «форма →
    лемма», запасной поиск в лог;
  - размер контекста считает сценарий: больше 4000 слов — 413
    (Д111); язык — до начала работы, чужой — в учёт (Д100, Д101);
  - один вызов нейронки на запрос: без пачек, без перевода контекста
    отдельным вызовом — перевод приходит в запросе;
  - пар двух Викисловарей нет ни в ответе нейронки, ни в коде (Д63,
    Д64); первая лексема по тапу помечена «в тексте»;
  - совет завести компонент — и по знаниям нейронки, только «Выбор»
    (Д110): инструкция, проверка без изменений.
- **Что не меняется** — база слов и её сборка, значения леммы из базы,
  кеш отбора без контекста, проверка ответа по дереву компонентов,
  бюджет 170 секунд.
- **Что чужое и здесь только зовётся** — ключ и аккаунт (`hld-acc.md`),
  начало и конец работы (`hld-lim.md`).
- **Документы** — [бриф](brief.md), [решения](decisions.md), спека LFL
  и её `contract.md`, экранные спеки AWP и WCL, контрольный набор
  `control_set.md` в бэкенде.

## 2. Схема и код

### Условные знаки

![Условные знаки](/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS536_llm_values/hld-legend.a02d0bb8.png)

### Слои по C4

В коробке имя, тип в скобках, одна фраза ответственности. На стрелках
намерение связи. Под ручкой сноска: ответы и ошибки по контракту. Что
меняет фича — цветом на схеме и пометками в коде. Источник —
`hld-lfl-layers-c4.dot`.

Номер на стрелке — шаг флоу LFL Ш1–Ш8; ACC Ш2 — ключ, LIM Ш2 — расход.
Ш3.1 и подобные — вызов порта внутри шага. Отбор и сопоставление,
Ш5–Ш7, идут одним вызовом нейронки. Сверху вниз всё идёт по
возрастанию шагов: сценарий, три ряда сервисов, порты, адаптеры.

![Слои по C4](/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS536_llm_values/hld-lfl-layers-c4.f8ab3cd1.png)

Код под схемой, в порядке вызова: ручка, сценарий, потом сервисы,
которые он зовёт. Реализованы публичные функции, всё, что они зовут,
дано сигнатурами с говорящими именами, без тел, под своей функцией.
Стиль — как в нынешних `internal/api/words/v1/lookup.go` и
`internal/scenario/lookup`. Комментарий у шага — его номер во флоу
LFL.

Слои те же, что в v1: ручка → сценарий → сервис → порт, соседи друг в
друга не ходят (Д48). Как в них легла v2:

- **Сценарий `LookupScenario`** в пакете `scenario/lookup` ведёт лемму
  по шагам LFL, без нормализации и без списаний. Зовёт «Языки»,
  `Quota`, Леммы, Значения, Отбор и Проверку ответа.
- **Размер — в сценарии.** Слова контекста считаются по разобранному
  запросу, поэтому порядок проверок — ключ, схема, размер, язык, работа
  (Д111).
- **Язык — свой сервис «Языки».** Правило операции, а не лимитов
  (Д79): изучаемый язык среди языков базы слов, чужой — в учёт
  (LFL-114). Его же зовёт сценарий лемм из текста.
- **Нормализация снята.** Лемма приходит словарной формой, в базе
  ищется в нижнем регистре, при промахе — как прислана.
- **Отбор — один вызов на запрос.** Пачек, параллельных вызовов и
  перевода контекста нет.
- **Пар нет.** Проверка ответа, сборка слова и клиент нейронки больше
  не знают о лексемах второго Викисловаря.
- **Счётчики после решения — лог, не ошибка.** Конец работы и учёт
  языка зовутся, когда ответ клиенту уже решён: сбой хранилища уходит в
  лог, ответ не ломается.

### Домен

Новые и изменённые понятия, пакет `domain`; что не названо — как в v1:

```go
// LookupRequest — запрос lexemes/from-lemma после хука
// ключа. Меняется: лемма и контекст вместо токенов.
type LookupRequest struct {
    // Account — владелец ключа, с которым пришёл запрос;
    // по нему лимиты и лог.
    Account Account
    // LearningLanguage — изучаемый язык словаря.
    LearningLanguage Language
    // TranslationLanguage — язык перевода словаря.
    TranslationLanguage Language
    // Lemma — слово запроса как прислано; новое поле.
    Lemma string
    // Context — текст снимка и перевод по тапу; nil из
    // карточки слова. Новое поле.
    Context *Context
    // Components — дерево компонентов словаря.
    Components ComponentTree
}

// Context — контекст леммы из ответа lemmas/from-tokens.
// Новый.
type Context struct {
    // Text — исправленный текст снимка.
    Text string
    // Translation — его перевод; нет — пусто.
    Translation string
}

// LookupResponse — ответ Ш8 устройству. Меняется: одно
// слово вместо списка.
type LookupResponse struct {
    // Attribution — источник данных для подписи.
    Attribution string
    // Word — слово запроса со статусом и леммами.
    Word WordResult
    // Recommendations — советы завести компонент.
    Recommendations []Recommendation
}

// Word — лемма для поиска в базе. Меняется: исходный
// текст вместо токена, нормализации нет.
type Word struct {
    // Text — лемма в нижнем регистре.
    Text string
    // Original — как прислана: поиск в исходном регистре
    // при промахе.
    Original string
    // TooLong — длиннее 64 знаков, в базу не ходит.
    TooLong bool
}

// LanguageCount — строка учёта языков. Новый.
type LanguageCount struct {
    // Language — основной код языка: «de» у «de-AT».
    Language Language
    // Requests — сколько раз просили.
    Requests int
    // First — когда просили впервые.
    First time.Time
    // Last — когда просили в последний раз.
    Last time.Time
}

// Prompt — вход отбора: всё, что уходит нейронке.
// Меняется: одно слово вместо списка; перевод контекста
// приходит в запросе.
type Prompt struct {
    // Text — текст снимка; без контекста пусто.
    Text string
    // TextTranslation — перевод текста; нет — пусто.
    TextTranslation string
    // Word — слово с леммами и значениями из записи.
    Word WordResult
    // Tree — дерево компонентов словаря.
    Tree ComponentTree
    // LearningLanguage — изучаемый язык словаря.
    LearningLanguage Language
    // TranslationLanguage — язык перевода словаря.
    TranslationLanguage Language
}

// Answer — ответ одного вызова отбора. Меняется: одно
// слово.
type Answer struct {
    // Word — заполненная форма по слову.
    Word WordAnswer
    // Recommendations — советы завести компонент.
    Recommendations []Recommendation
}

// Selection — отбор по слову после вызова и проверки.
// Меняется: без списка слов.
type Selection struct {
    // Lemmas — леммы с отобранными значениями.
    Lemmas []LemmaSelection
    // Remarks — замечания по слову.
    Remarks []WordRemark
    // Recommendations — советы завести компонент.
    Recommendations []Recommendation
    // Usage — расход вызова нейронки.
    Usage ModelUsage
    // Failed — ответ не разобрался, слово «ошибка».
    Failed bool
    // Corrections — что проверка сняла, для лога.
    Corrections []Correction
}

// Pick — одно отобранное значение. Меняется: без пар.
type Pick struct {
    // Key — ключ лексемы записи.
    Key string
    // Matches — что в какой компонент.
    Matches []ComponentMatch
    // Examples — примеры этого значения, до трёх.
    Examples []string
}
```

У лексемы-кандидата `CandidateLexeme` новое поле `InContext` —
употреблена в тексте, пометка «в тексте» (LFL-113); её метод
`WithPairs` снят. Новая ошибка `ErrRequestTooLarge` — контекст больше
предела, у ручки 413. Сняты `Thresholds`, `WordSelection`,
`CorrectionUnknownPair`. Вид работы для лимитов `WorkWordLexemes` — в
`hld-lim.md`.

### Ручка

Сначала проверка ключа: её зовёт сгенерированный из контракта сервер
до ручки, на каждый запрос. Отказ здесь — 401, ручка не зовётся. Хук
не меняется, его код — `hld-acc.md`.

```go
// HandleAccessKey — ACC Ш2: ключ из заголовка, аккаунт у
// Auth, аккаунт в контекст. Есть, не меняется.
func (h *wordsHandler) HandleAccessKey(
    ctx context.Context,
    op wordsv1.OperationName,
    key wordsv1.AccessKey,
) (context.Context, error)
```

Ручка одна на обе операции контракта; здесь — операция `lexemes/from-lemma`:

```go
// LexemesFromLemma — Ш1 и Ш8: аккаунт из контекста, лемма
// с контекстом в домен, сценарий, слово в контракт.
// Меняется: имя операции — lexemes/from-lemma вместо
// words/lookup; схемы контракта LemmaRequest и
// LexemesResponse; конвертеры — лемма и контекст вместо
// токенов, одно слово вместо списка; новая ветка 413 —
// контекст больше предела, его считает сценарий.
func (h *wordsHandler) LexemesFromLemma(
    ctx context.Context,
    req *wordsv1.LemmaRequest,
    params wordsv1.LexemesFromLemmaParams,
) (wordsv1.LexemesFromLemmaRes, error) {
    account := accountFrom(ctx)
    ctx = logger.ContextWithRunID(ctx, params.XRunID.Or(""))
    started := time.Now()
    resp, err := h.lexemes.Lookup(
        ctx,
        // конвертер теперь переносит лемму и контекст
        converters.RequestToDomain(req, account),
    )
    logRequest(ctx, account, req, resp, started, err)

    switch {
    case err == nil:
        // конвертер теперь отдаёт одно слово
        return converters.ResponseToAPI(resp), nil
    case errors.Is(err, domain.ErrLLMUnavailable):
        return llmUnavailable(err), nil
    // новая ветка: контекст больше 4000 слов
    case errors.Is(err, domain.ErrRequestTooLarge):
        return requestTooLarge(err), nil
    case errors.Is(err, domain.ErrRateLimited):
        return rateLimited(), nil
    case errors.Is(err, domain.ErrUnsupportedLanguage):
        return unsupportedLanguage(
            req.LearningLanguage,
        ), nil
    default:
        return nil, err
    }
}
```

Что зовёт ручка:

```go
// accountFrom — владелец ключа, с которым пришёл запрос.
// Есть.
func accountFrom(ctx context.Context) domain.Account

// requestTooLarge — ответ 413 с кодом request_too_large и
// числом слов в сообщении. Новая.
func requestTooLarge(
    err error,
) *wordsv1.LexemesFromLemmaRequestEntityTooLarge

// llmUnavailable, rateLimited, unsupportedLanguage —
// ответы 503, 429 и 400. Есть, не меняются.

// logRequest — строка лога запроса. Меняется: лемма,
// число слов контекста и статус слова вместо числа
// токенов и слов ответа.
func logRequest(
    ctx context.Context,
    account domain.Account,
    req *wordsv1.LemmaRequest,
    resp domain.LookupResponse,
    started time.Time,
    err error,
)

// RequestToDomain — запрос контракта в домен. Меняется:
// лемма без пробелов по краям и контекст вместо токенов.
func RequestToDomain(
    req *wordsv1.LemmaRequest,
    account domain.Account,
) domain.LookupRequest

// ResponseToAPI — ответ домена в контракт. Меняется: одно
// слово, у лексемы inContext.
func ResponseToAPI(
    resp domain.LookupResponse,
) *wordsv1.LexemesResponse
```

`ErrorHandler` и `classify` не меняются: ключ — 401, схема запроса —
400, остальное — 500. 413 по схеме у этой операции больше нет: токенов
в запросе нет, размер контекста считает сценарий.

### Сценарий

Сценарий `LookupScenario`, пакет `scenario/lookup`, один на операцию.
Меняется: лемма вместо токенов, размер в коде, нормализации и списаний
нет, слово одно.

```go
// Lookup — Ш2–Ш8: размер, язык и начало работы, лемма в
// базе, значения, один вызов нейронки, конец работы,
// проверка ответа, сборка слова. Нейронка не ответила —
// наверх ErrLLMUnavailable (В2). Меняется: лемма с
// контекстом вместо токенов, предел слов, без
// нормализации, без списания, слово одно.
func (s *lookupScenario) Lookup(
    ctx context.Context,
    req domain.LookupRequest,
) (domain.LookupResponse, error) {
    // Ш2: контекст не больше 4000 слов; ключ и схема
    // проверены до ручки; в ошибке — число слов и предел,
    // ручка отдаёт его в сообщении 413
    if n := wordsIn(req.Context); n > s.options.MaxWords {
        return rejected(tooLarge(n, s.options.MaxWords))
    }
    // Ш2: изучаемый язык есть в базе слов
    err := s.languages.Check(ctx, req.LearningLanguage)
    if err != nil {
        return rejected(err)
    }
    // Ш2: начало работы по LIM, пока всегда «можно»
    err = s.quota.Begin(
        ctx, req.Account,
        domain.WorkWordLexemes,
    )
    if err != nil {
        return rejected(err)
    }
    // 170 с на всё
    ctx, finish := withinBudget(ctx, s.options.Budget)
    defer finish()

    // Ш3–Ш4: лемма как прислана, в базе без учёта
    // регистра; с текстом — одна лемма, без — все
    found := s.lookupOne(
        ctx,
        wordOf(req.Lemma), languagesOf(req),
        req.Context != nil,
    )

    // Ш5–Ш7: один вызов нейронки, отбор и сопоставление;
    // только 503 прерывает сценарий
    picks, err := s.selector.Select(
        ctx,
        promptOf(req, found),
    )
    // LIM Ш2: конец работы — расход нейронки, 0 — не
    // ответила или не звалась
    s.finishWork(ctx, req.Account, picks.Usage)
    if err != nil {
        return rejected(err)
    }
    picks = s.verifier.Verify(picks, found, req.Components)
    logCorrections(ctx, picks.Corrections)

    // Ш8: лексемы по отбору, «в тексте», замечание «нет
    // перевода» по языку перевода, статус слова; текст
    // слова в ответе — лемма как прислана
    return response(
        req.Lemma,
        assembled(
            found, picks,
            req.TranslationLanguage, req.Context != nil,
        ),
        picks.Recommendations,
    ), nil
}

// lookupOne — Ш3–Ш4: слишком длинная — не найдено без
// базы; бюджет кончился — ошибка источника; иначе леммы с
// лексемами до отбора, только Викисловаря языка перевода;
// с текстом — своя лемма или первая по связи «форма →
// лемма» со значениями. Меняется: лемма как прислана,
// плюс выбор леммы по контексту.
func (s *lookupScenario) lookupOne(
    ctx context.Context,
    word domain.Word,
    langs languages,
    withContext bool,
) domain.WordResult {
    if word.TooLong {
        return notFound(word)
    }
    if budgetExhausted(ctx) {
        return sourceError(word)
    }
    result, err := s.resolver.Candidates(
        ctx, word,
        langs.learning,
    )
    if err != nil {
        return sourceError(word)
    }
    blocks := s.blocksByLemma(ctx, result, langs)
    result = onlyTranslationWiktionary(
        word,
        withLemmas(result, blocks),
        langs.translation,
    )
    if !withContext {
        return result
    }
    // Ш3 А: своя лемма со значениями — она одна; нет —
    // первая по связи, запасной поиск в лог (LFL-115)
    return lemmaForContext(ctx, word, result)
}
```

Что зовёт сценарий. Сервисы `s.languages`, `s.resolver`, `s.meanings`,
`s.selector` и `s.verifier` реализованы ниже, в порядке вызова;
`s.quota` — `hld-lim.md`:

```go
// Begin — LIM Ш1: можно ли аккаунту начать работу; лимит
// работ пока всегда пускает, лимит токенов нейронки —
// ErrRateLimited. Сервис Quota.
Begin(
    ctx context.Context,
    account domain.Account,
    work domain.Work,
) error

// Finish — LIM Ш2: расход нейронки в счётчик аккаунта;
// 0 — нейронка не ответила или не звалась. Сервис Quota.
Finish(
    ctx context.Context,
    account domain.Account,
    usage domain.ModelUsage,
) error
```

Помощники пакета `scenario/lookup`:

```go
// wordsIn — LFL-106: слов в тексте контекста, по
// пробелам; без контекста — 0. Новая.
func wordsIn(context *domain.Context) int

// tooLarge — ErrRequestTooLarge с числом слов и пределом в
// сообщении: «4001 words in context, limit 4000». Предел
// 4000 — в настройках сценария, 0 — без предела, как у
// бюджета. Новая.
func tooLarge(words, limit int) error

// wordOf — лемма запроса как слово для поиска: нижний
// регистр, исходный текст, признак «длиннее 64». Новая,
// вместо нормализации.
func wordOf(lemma string) domain.Word

// lemmaForContext — Ш3 А: слово «ошибка» — как есть; «не
// найдено» — в лог «lemma fallback» с леммой запроса без
// найденной; у найденного своя лемма — без пометки формы
// — с лексемами остаётся одна; нет — первая в порядке
// статьи лемма по связи «форма → лемма» с лексемами, в
// лог «lemma fallback» с леммой запроса и найденной; нет
// и такой — не найдено, в логе без найденной. В лог —
// лемма как прислана. Новая.
func lemmaForContext(
    ctx context.Context,
    word domain.Word,
    result domain.WordResult,
) domain.WordResult

// promptOf — вход отбора: найденное слово, текст и перевод
// из контекста запроса, дерево, языки. Меняется: контекст
// из запроса, слово одно.
func promptOf(
    req domain.LookupRequest,
    found domain.WordResult,
) domain.Prompt

// finishWork — LIM Ш2: расход вызова в счётчик аккаунта;
// сбой хранилища — в лог. Меняется: бывший chargeModel.
func (s *lookupScenario) finishWork(
    ctx context.Context,
    account domain.Account,
    usage domain.ModelUsage,
)

// assembled — Ш8: not_found и error как есть, без лемм;
// нейронка не дала годного — error без лемм; иначе у леммы
// лексемы в порядке отбора с сопоставлением и примерами
// отбора, у первой с контекстом — InContext; замечания по
// слову, «нет перевода» — по языку перевода (LFL-91);
// лемма с пустым отбором выпадает. Меняется: одно слово,
// без пар, плюс «в тексте».
func assembled(
    found domain.WordResult,
    picks domain.Selection,
    translation domain.Language,
    withContext bool,
) domain.WordResult

// response — Ш8: атрибуция, слово с текстом леммы как
// прислана (контракт 2), рекомендации компонентов.
// Меняется: слово одно, текст — из запроса.
func response(
    lemma string,
    word domain.WordResult,
    recommendations []domain.Recommendation,
) domain.LookupResponse

// withinBudget, languagesOf, rejected, blocksByLemma,
// blocksOf, withLemmas, onlyTranslationWiktionary,
// notFound, sourceError, budgetExhausted,
// noTranslationRemarks — есть, не меняются.
```

Сняты из пакета: `lookupEach`, `inParallel`, `tokenCount`, `noWords`,
`emptyResponse`, `refund`, `chargeModel`, `pairLexemes`, `withPairs`,
`selectionsByWord`. В настройках сценария `Parallelism` и `Thresholds`
сняты, новый `MaxWords` — 4000.

### Языки

Сервис «Языки» — правило операции: изучаемый язык среди языков базы
слов, чужой — в учёт. Зовут оба сценария, этот и лемм из текста.

```go
// LanguageService — изучаемый язык против языков базы слов
// и учёт чужих (LFL-30, LFL-114, LFT-17). Пакет service,
// новый.
type LanguageService interface {
    Check(ctx context.Context, lang domain.Language) error
}

// Check — язык по основному коду есть среди языков базы
// слов — пусто; нет — язык в учёт и ErrUnsupportedLanguage;
// сбой учёта присоединяется к отказу: ручка пишет его в
// лог строкой запроса, отказ уходит как обычно. Новая.
func (l *languages) Check(
    ctx context.Context,
    lang domain.Language,
) error {
    if l.supported(lang) {
        return nil
    }
    err := l.log.Record(ctx, lang.Base())
    if err != nil {
        return errors.Join(
            domain.ErrUnsupportedLanguage, err,
        )
    }
    return domain.ErrUnsupportedLanguage
}
```

Что зовут «Языки». Порт «Учёт языков», `LanguageLog`, новый:

```go
// Record — язык в учёт: новый — строка с одним обращением
// и временем, уже есть — обращений плюс один и время
// последнего; кто просил — не пишется. Новый.
Record(ctx context.Context, lang domain.Language) error

// List — учёт целиком для команды просмотра. Новый.
List(ctx context.Context) ([]domain.LanguageCount, error)
```

Помощник пакета `languages`:

```go
// supported — основной код языка есть среди языков
// выгрузок базы слов из настроек: es и en. Новая, бывший
// unsupported у Quota.
func (l *languages) supported(lang domain.Language) bool
```

### Леммы и Значения

Сервис «Леммы» — лемма в базе слов, леммы-кандидаты по записи и связям
«форма → лемма». Меняется только вход: слово без токена.

```go
// Candidates — Ш3.1: запись леммы, нет — запись по
// исходному тексту, нет и её — не найдено; кандидаты с
// переходом от формы к лемме. Меняется: исходный регистр
// берётся из Original, а не из токена.
Candidates(
    ctx context.Context,
    word domain.Word,
    lang domain.Language,
) (domain.WordResult, error)
```

Сервис «Значения» — все значения леммы из Викисловаря языка перевода
со всем, что запись о них знает. Не меняется:

```go
// Meanings — Ш4.1: блоки значений леммы по частям речи.
// Есть, не меняется.
Meanings(
    ctx context.Context,
    lemma string,
    learning, translation domain.Language,
) ([]domain.MeaningBlock, error)
```

### Отбор

Сервис «Отбор» — Ш5–Ш7, один вызов нейронки на запрос. Единственный,
кто знает про кеш и клиент нейронки; токены не списывает.

```go
// Select — для одного слова: нечего отбирать — вызова нет;
// без контекста сначала кеш; один вызов нейронки; не
// ответила — ErrLLMUnavailable; ответила негодно — слово
// «ошибка», расход считается; отбор без контекста — в кеш.
// Меняется: одно слово, без пачек и параллельных вызовов,
// без перевода контекста.
func (s *selector) Select(
    ctx context.Context,
    prompt domain.Prompt,
) (domain.Selection, error) {
    // Ш5: не найдено или без лексем — нейронка не нужна
    if !selectable(prompt.Word) {
        return domain.Selection{}, nil
    }
    // Ш6.1: без контекста — готовый отбор из кеша
    if cached, ok := s.fromCache(ctx, prompt); ok {
        return cached, nil
    }
    // Ш6.2: один вызов
    answer, usage, err := s.client.Complete(ctx, prompt)
    switch {
    case err == nil:
        selection := selectionOf(prompt.Word, answer, usage)
        s.remember(ctx, prompt, selection)
        return selection, nil
    // ответ был, но не разобрался: слово «ошибка»,
    // расход считается (LFL-97.2)
    case errors.Is(err, domain.ErrAnswerUnreadable):
        return failed(usage), nil
    // соединение, код ошибки провайдера или бюджет
    default:
        return domain.Selection{}, domain.ErrLLMUnavailable
    }
}
```

Что зовёт Отбор. Порт «Кеш отбора», `SelectionCache`, — есть, не
меняется. Порт «Клиент нейронки», `LLMClient`:

```go
// Complete — Ш5.1, один вызов отбора. Меняется: во входе
// одно слово.
Complete(
    ctx context.Context,
    prompt domain.Prompt,
) (domain.Answer, domain.ModelUsage, error)

// Translate — снят: перевод контекста приходит в запросе
// из ответа LFT.
```

Помощники пакета `selection`:

```go
// selectable — найденное слово с лексемами у лемм. Есть,
// меняется: одно слово вместо фильтра списка.
func selectable(word domain.WordResult) bool

// fromCache — Ш6.1: готовый отбор по ключу «слово, языки,
// дерево, модель», только без контекста; сбой кеша —
// промах. Новая, бывший splitByCache.
func (s *selector) fromCache(
    ctx context.Context,
    prompt domain.Prompt,
) (domain.Selection, bool)

// keyOf — ключ кеша. Есть, меняется: слово из prompt.Word.
func (s *selector) keyOf(
    prompt domain.Prompt,
) domain.SelectionKey

// selectionOf — отбор из ответа: леммы, замечания,
// рекомендации, расход. Новая, бывший wordSelectionOf.
func selectionOf(
    word domain.WordResult,
    answer domain.Answer,
    usage domain.ModelUsage,
) domain.Selection

// failed — отбор с Failed и расходом: слово «ошибка».
// Новая.
func failed(usage domain.ModelUsage) domain.Selection

// remember — отбор без контекста в кеш с нулевым
// расходом и без исправлений: ответ из кеша счётчик
// нейронки не растит (LFL-94, LIM-79); сбой кеша ответ не
// ломает. Есть, меняется: одно слово, расход обнуляется.
func (s *selector) remember(
    ctx context.Context,
    prompt domain.Prompt,
    selection domain.Selection,
)
```

Сняты: `translated`, `splitByCache`, `batchesOf`, `inParallel`,
`selectBatch`, `noneAnswered`, `freshWords`, `merged`,
`wordSelectionOf`; в настройках сняты `BatchSize` и `Parallelism`,
остаётся `Model` для ключа кеша.

### Проверка ответа нейронки

Сервис «Проверка ответа нейронки» — ответ против записи и дерева:
нейронка только выбирает из готового, чужого в ответе не остаётся.

```go
// Verify — у леммы отбора: ключи только из её лексем,
// повтор — первый; сопоставление только на компоненты
// дерева, их опции и непустые поля самой лексемы, строка в
// включённое ядро обязательна; первые три; замечания
// только про компоненты дерева, рекомендации по одной на
// имя. Годного не осталось, хотя нейронка выбирала, —
// лемма «ошибка»; леммы не из найденного — вон. Меняется:
// одно слово, без пар.
func (v *verifier) Verify(
    picks domain.Selection,
    found domain.WordResult,
    tree domain.ComponentTree,
) domain.Selection {
    if picks.Failed {
        return picks
    }
    components := indexOf(tree)
    log := &report{word: found.Text}
    picks.Lemmas = knownLemmas(
        picks.Lemmas, found.Lemmas,
        log,
    )
    for i, lemma := range picks.Lemmas {
        log.at(lemma.Text)
        lexemes := lexemesOf(found.Lemmas, lemma.Text)
        hadPicks := len(lemma.Picks) > 0
        lemma = withKnownKeys(lemma, lexemes, log)
        lemma = withValidComponentMatches(
            lemma, lexemes,
            pronunciationsOf(found.Lemmas, lemma.Text),
            components, log,
        )
        lemma = withCoreValue(lemma, components, log)
        lemma = firstPicks(lemma, maxPicks)
        if nothingLeft(lemma, hadPicks, lexemes) {
            lemma.Failed = true
            log.add("", domain.CorrectionLemmaFailed,
                "годного не осталось — слово error")
        }
        picks.Lemmas[i] = lemma
    }
    log.at("")
    picks.Remarks = knownRemarks(
        picks.Remarks, components,
        log,
    )
    picks.Recommendations = validRecommendations(
        picks.Recommendations, components,
        log,
    )
    picks.Corrections = log.items
    return picks
}
```

Что зовёт проверка. Помощники пакета `verify`:

```go
// withValidComponentMatches — у значения остаются строки
// на компоненты дерева с опцией этого «Выбора» или
// непустым полем самой лексемы, зависимые — при выбранном
// родителе. Меняется: поля без дополнения парами.
func withValidComponentMatches(
    lemma domain.LemmaSelection,
    lexemes []domain.CandidateLexeme,
    pronunciations []domain.Pronunciation,
    components componentIndex,
    log *report,
) domain.LemmaSelection

// knownLemmas, withKnownKeys, withCoreValue, firstPicks,
// nothingLeft, knownRemarks, validRecommendations,
// validMatches, fieldsOf, indexOf — есть, не меняются.
// withKnownPairs, pairLexemes — сняты.
```

### Клиент нейронки

Адаптер порта «Клиент нейронки», пакет `clients/openrouter`. Вызов
отбора меняется, вызов перевода снят; вызов извлечения — `hld-lft.md`.

```go
// Complete — вызов отбора. Меняется: в JSON вызова одно
// слово, перевод контекста — из промпта, без своего
// вызова перевода.
func (c *client) Complete(
    ctx context.Context,
    prompt domain.Prompt,
) (domain.Answer, domain.ModelUsage, error)

// answerSchema, parseAnswer — есть, меняются: одно слово,
// без двойника в парах.
// Translate, translationSchema, translationMessages,
// parseTranslation — сняты.
```

Инструкция отбора — файлом на сервере, без тега. Правится по
решениям: совет завести компонент — и по знаниям нейронки, только для
«Выбора» (Д110); перевод контекста приходит готовым. Текст — в плане,
не здесь.

### Хранилище

Postgres, пакет `repository/postgres`: аккаунты, ключи, расход — как
были; новая таблица учёта языков.

```sql
-- +goose Up
-- unsupported_languages: учёт изучаемых языков, которых
-- нет в базе слов (LFL-114): основной код, сколько раз
-- просили, первый и последний раз; кто просил — нет.
CREATE TABLE unsupported_languages (
    language TEXT        PRIMARY KEY,
    requests INTEGER     NOT NULL CHECK (requests > 0),
    first_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- +goose Down
DROP TABLE unsupported_languages;
```

```go
// Record — одной командой: вставить строку с единицей или
// прибавить единицу и обновить last_at. Новая.
func (r *Repository) Record(
    ctx context.Context,
    lang domain.Language,
) error

// List — все строки учёта по языку. Новая.
func (r *Repository) List(
    ctx context.Context,
) ([]domain.LanguageCount, error)
```

Просмотр учёта — команда `lexeme languages` на сервере и задача к ней в
`Taskfile`: строка на язык — код, обращений, первый и последний раз
(LFL-114.4).

## 3. Контракт

Дельта к `api/openapi.yaml` 0.3.0, версия становится 0.4.0:

- **Маршрут** — `POST /v1/lexemes/from-lemma` вместо `words/lookup`,
  `operationId` `lexemesFromLemma` (Д114).
- **Запрос** — схема `LemmaRequest`: `lemma` и `context` с `text` и
  `translation` вместо `tokens`; `components` как был; у контекста
  оба поля обязательны, `text` непустой, `translation` может быть
  `null`. Размер контекста — 4000 слов — считает сценарий после
  разбора по схеме, 413 — типизированный ответ ручки с числом слов.
- **Ответ** — схема `LexemesResponse`: `word` вместо `words`, одно
  `WordResult` с текстом леммы как прислана; у `CandidateLexeme`
  новое `inContext`.
- **Снято** — `tokens`, `words`, описание нормализации и
  порогов в схемах.
- **Отказы** — коды те же; 429 только по лимиту токенов нейронки;
  порядок проверок: ключ, схема, размер, язык, начало работы.
- Живые примеры — `contract.md` рядом со спекой LFL.

## 4. Ключевые решения

- **Лемма с контекстом или без — одна ручка, одно слово** (Д82, Д90):
  по тапу текст и перевод снимка идут контекстом, из карточки — нет.
- **Лемма как прислана** (Д99): нормализации нет, регистр снимается при
  поиске, промах — исходный регистр.
- **Предел контекста — в словах, кодом** (Д111): 4000 слов, порядок
  проверок — ключ, схема, размер. Отвергнуто: предел в знаках схемой.
- **Язык — правило операции, с учётом** (Д79, Д100, Д101): сервис
  «Языки», таблица учёта, команда просмотра.
- **Без пар, примеры по значению** (Д63, Д64): лексема одного
  Викисловаря, в коде пар больше нет.
- **Отбор одним вызовом, перевод из запроса** (Д33 снято пачками):
  нейронка видит одно слово с его контекстом.
- **«В тексте» ставит сервер** (Д74, LFL-113): первая лексема леммы по
  тапу.
- **Совет компонента и по знаниям нейронки, только «Выбор»** (Д110).
- **Три слоя, два вызова лимитов из сценария** (Д48, Д78).

## 5. Сквозное

- **Отказы** — кто решает:
  - 401 — `Auth`, до ручки
  - 400 не по схеме — схема контракта, до ручки
  - 413 контекст больше 4000 слов — сценарий, первый шаг
  - 400 язык — «Языки», второй шаг, с учётом
  - 429 лимит токенов нейронки — `Quota.Begin`, третий шаг
  - 503 — Отбор: нейронка не ответила; негодный ответ — не отказ, а
    слово «ошибка»
- **Слово «ошибка»** — источник не ответил, лемма не успела в бюджет,
  нейронка не дала по ней годного.
- **Ядро** — у каждой отданной лексемы строка сопоставления в
  включённое ядро с непустым текстом (Д49).
- **Работа и расход** — `Begin` до нейронки, `Finish` после; ответ из
  кеша счётчик не увеличивает; сбой записи — лог.
- **Бюджет времени** — 170 с на сервере, 180 с на устройстве; у
  нейронки своего срока нет, остаток бюджета — её контекст.
- **Лог** — вызов нейронки целиком (LFL-101), запасной поиск леммы
  (LFL-115), исправления проверки.
- **Секреты** — ключ нейронки не попадает ни в ответ, ни в лог
  (LFL-93); ключ доступа — ни в логи, ни в ответы.
- **Конфиг** — модель, инструкция отбора файлом, лимит токенов
  нейронки, предел слов контекста. Смена модели сбрасывает попадания
  в кеш.
- **Данные у провайдера** — текст снимка целиком и имена компонентов
  (Д35).

## 6. Выкатка

- **Порядок** — вместе с остальным бэкендом v2, тег `v0.1.32`, потом
  сборка приложения.
- **Старое приложение на новом сервере** — запрос с `tokens` без
  `lemma` получает 400 `invalid_request`: сборка v1 не в проде,
  совместимость не держится (Д46).
- **База данных** — таблица учёта языков создаётся миграцией при
  подъёме сервера.
- **Инструкция нейронке** — файлом на сервере, до тега проверяется
  пробником `lexeme probe llm` на контрольном наборе: совет по знаниям,
  одно слово в вызове.
- **Кеш отбора** — обнуляется при деплое, сразу после выкатки
  попаданий мало.

## 7. Риски

- **Запасной поиск леммы по тапу** — форма, которую нейронка LFT не
  довела до словарной, находится по связям базы; промахи видны в логе
  `lemma fallback`, лечатся инструкцией извлечения.
- **Провайдер лёг** — без нейронки значений нет: 503, с работ ничего
  не списано.
- **Расход у провайдера** — лимит токенов нейронки выключен по
  умолчанию, расход только считается; включить — конфиг.
- **Цена** — тап: один вызов с контекстом целиком, кеш только без
  контекста; страница из 2000 токенов в каждом вызове.
- **Качество отбора** — проверяется контрольным набором `control_set.md`
  с фикстурами в бэкенде.

## 8. Как перерисовать схемы

Условные знаки — Mermaid, схема слоёв — Graphviz: Mermaid не ставит
сноску рядом с коробкой и тасует порядок блоков. Источник в `.mmd` или
`.dot` рядом, картинка рендерится скриптом, в документ идёт по
абсолютному пути с хешем содержимого в имени: превью студии кеширует
картинку по имени. Перерисовать все схемы папки и обновить ссылки:

```
scripts/mmd-render.sh docs/features/IS536_llm_values
```
