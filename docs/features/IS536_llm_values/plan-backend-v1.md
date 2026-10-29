# IS536 | План бэкенда

LLD к `hld.md`, раздел 2. Этапы в порядке выполнения, независимое
первым; внутри этапа — триады: было — нынешний код из репо, будет —
код после этапа, мотивация — зачем он меняется. Строки нынешнего кода
перенесены под шестьдесят знаков, текст тот же. Код — по команде.
Этап выполнен — пометка «ВЫПОЛНЕН <дата>» с одной фразой, что
разошлось с планом.

Команды: генерация `task gen`, тесты `task test`, линт `task lint`,
покрытие `task spec:coverage`, прогон с сетью `task test:live`.

## Э1. Контракт и генерация

ВЫПОЛНЕН 2026-10-20. `grammar` в контракте — только строки, поля `llm`
нет; подробности — `plan-errata-v1.md`.

Тезисы: LFL-78, LFL-89, LFL-90, LFL-91, LFL-63.5; контракт 1 и 5.
Проверка: `internal/converters/lookup_test.go` на примерах контракта
и фикстурах `fixtures/`.

### Сгенерированный сервер

**Было.** `pkg/openapi/words/v1` сгенерирован `ogen` по контракту
0.2: `LookupRequest` без дерева, `WordResult.Candidates` со схемами
`Candidate`, `Sense`, `Parameters`, у `CandidateLexeme` поля `Parent`
и `ParentKey`.

**Будет.** `task gen` по 0.3.0-draft: `LookupRequest.Components`,
`WordResult.Remarks`, `LookupResponse.Recommendations` и `LLM`,
`CandidateLexeme.ComponentMatches`, `Grammar`, `Level`, `Synonyms`,
`Antonyms`, `Lemma.Pronunciations`; схем `Candidate`, `Sense`,
`Parameters` нет; ответ `LookupWordsServiceUnavailable` с кодом
`llm_unavailable`.

**Мотивация.** Контракт одобрен (Д26, Д27, Д46, Д54, Д55); от типов
сервера зависят ручка и конвертеры, без генерации остальное не
компилируется.

### Конвертер запроса

**Было.**

```go
func RequestToDomain(
    req *wordsv1.LookupRequest,
    subject domain.Subject,
) domain.LookupRequest {
    return domain.LookupRequest{
        Subject: subject,
        LearningLanguage: domain.Language(
            req.LearningLanguage,
        ),
        TranslationLanguage: domain.Language(
            req.TranslationLanguage,
        ),
        Tokens: TokensToDomain(req.Tokens),
    }
}
```

**Будет.**

```go
// RequestToDomain — запрос контракта в домен: субъект,
// языки, токены как пришли, с повторами, плюс дерево
// компонентов.
func RequestToDomain(
    req *wordsv1.LookupRequest,
    subject domain.Subject,
) domain.LookupRequest {
    return domain.LookupRequest{
        Subject: subject,
        LearningLanguage: language(req.LearningLanguage),
        TranslationLanguage: language(
            req.TranslationLanguage,
        ),
        Tokens: TokensToDomain(req.Tokens),
        Components: ComponentsToDomain(
            req.Components,
        ),
    }
}

// ComponentsToDomain — дерево компонентов контракта в
// домен: корень — компоненты от лексемы, children
// компонента — от компонента, children опции — от опции.
// Пустое поле — пустое дерево (LFL-78.7).
func ComponentsToDomain(
    nodes []wordsv1.Component,
) domain.ComponentTree
```

**Мотивация.** Нейронка сопоставляет значения с компонентами словаря
пользователя, дерево уходит в каждом запросе (Д8, Д28).

### Конвертер ответа

**Было.**

```go
func WordResultToAPI(
    w domain.WordResult,
) wordsv1.WordResult {
    candidates := make(
        []wordsv1.Candidate, 0, len(w.Candidates),
    )
    for _, c := range w.Candidates {
        candidates = append(candidates, CandidateToAPI(c))
    }
    lemmas := make([]wordsv1.Lemma, 0, len(w.Lemmas))
    for _, l := range w.Lemmas {
        lemmas = append(lemmas, LemmaToAPI(l))
    }
    return wordsv1.WordResult{
        Text:       w.Text,
        Status:     wordsv1.WordResultStatus(w.Status),
        Candidates: candidates,
        Lemmas:     lemmas,
    }
}

func CandidateLexemeToAPI(
    x domain.CandidateLexeme,
) wordsv1.CandidateLexeme {
    return wordsv1.CandidateLexeme{
        Key:             x.Key,
        PartOfSpeechKey: nilString(x.PartOfSpeechKey),
        Source:          string(x.Source),
        Translations:    nonNil(x.Translations),
        Gloss:           nilString(x.Gloss),
        Caption:         nilString(x.Caption),
        Notes:           nonNil(x.Notes),
        Examples:        nonNil(x.Examples),
        Parent:          x.Parent,
        ParentKey:       nilString(x.ParentKey),
    }
}
```

**Будет.**

```go
// ResponseToAPI — ответ домена в контракт: слова,
// замечания ко всему ответу, признак нейронки.
// CandidateToAPI удалена.
func ResponseToAPI(
    resp domain.LookupResponse,
) *wordsv1.LookupResponse {
    return &wordsv1.LookupResponse{
        Attribution:     resp.Attribution,
        Words:           wordsToAPI(resp.Words),
        Recommendations: recommendationsToAPI(resp),
        LLM:             resp.LLM,
    }
}

// WordResultToAPI — слово: текст, статус, леммы, замечания
// по слову; candidates нет (Д27, Д46).
func WordResultToAPI(
    w domain.WordResult,
) wordsv1.WordResult {
    return wordsv1.WordResult{
        Text:    w.Text,
        Status:  wordsv1.WordResultStatus(w.Status),
        Lemmas:  lemmasToAPI(w.Lemmas),
        Remarks: remarksToAPI(w.Remarks),
    }
}

// CandidateLexemeToAPI — лексема-кандидат: данные записи
// целиком (Д23) и сопоставление; parent и parentKey сняты
// (Д37).
func CandidateLexemeToAPI(
    x domain.CandidateLexeme,
) wordsv1.CandidateLexeme {
    return wordsv1.CandidateLexeme{
        Key:              x.Key,
        PartOfSpeechKey:  nilString(x.PartOfSpeechKey),
        Source:           string(x.Source),
        Translations:     nonNil(x.Translations),
        Gloss:            nilString(x.Gloss),
        Caption:          nilString(x.Caption),
        Notes:            nonNil(x.Notes),
        Examples:         nonNil(x.Examples),
        Grammar:          grammarToAPI(x.Grammar),
        Level:            nilString(x.Level),
        Synonyms:         nonNil(x.Synonyms),
        Antonyms:         nonNil(x.Antonyms),
        ComponentMatches: matchesToAPI(x.ComponentMatches),
    }
}

// LemmaToAPI — лемма: плюс транскрипции из записи.
func LemmaToAPI(l domain.Lemma) wordsv1.Lemma {
    return wordsv1.Lemma{
        Text:           l.Text,
        FormNote:       nilString(l.FormNote),
        Pronunciations: nonNil(l.Pronunciations),
        Lexemes:        lexemesToAPI(l.Lexemes),
    }
}
```

**Мотивация.** Устройство раскладывает лексему по компонентам по
`componentMatches`, показывает замечания и рекомендации, а не
`candidates` с полями (Д18, Д23, Д27).

## Э2. Домен

ВЫПОЛНЕН 2026-10-20. `WordResult.Candidates` остался внутренним полем,
отбор по словам, рекомендации от нейронки, произношение объектом.

Тезисы: LFL-71, LFL-78, LFL-81. Проверка: компиляция; тесты Э1 и
Э3 работают на новых типах.

### Запрос и ответ

**Было.**

```go
type LookupRequest struct {
    Subject             Subject
    LearningLanguage    Language
    TranslationLanguage Language
    Tokens              []Token
}

type LookupResponse struct {
    Attribution string
    Words       []WordResult
}

type WordResult struct {
    Text       string
    Status     Status
    Candidates []Candidate
    Lemmas     []Lemma
}
```

**Будет.**

```go
// LookupRequest — плюс дерево компонентов словаря (Д28).
type LookupRequest struct {
    Subject             Subject
    LearningLanguage    Language
    TranslationLanguage Language
    Tokens              []Token
    Components          ComponentTree
}

// ComponentTree — дерево компонентов из запроса: корень от
// лексемы, дети компонента и опции (Д28, LFL-78).
type ComponentTree struct {
    Roots []ComponentNode
}

type ComponentNode struct {
    ID       int64
    Key      *string
    Name     *string
    Template string
    Core     bool
    Multiple bool
    Options  []ComponentOption
    Children []ComponentNode
}

type ComponentOption struct {
    ID       int64
    Key      *string
    Label    *string
    Children []ComponentNode
}

// LookupResponse — плюс рекомендации компонентов и признак,
// что ответ собран нейронкой.
type LookupResponse struct {
    Attribution     string
    Words           []WordResult
    Recommendations []Recommendation
    LLM             bool
}

// WordResult — без Candidates (Д27), плюс замечания по
// слову.
type WordResult struct {
    Text    string
    Status  Status
    Lemmas  []Lemma
    Remarks []WordRemark
}
```

**Мотивация.** Один домен для сценария, сервисов и конвертеров;
`Candidate`, `Sense`, `Parameters` ушли из контракта (Д27).

### Лексема-кандидат и лемма

**Было.**

```go
type Lemma struct {
    Text     string
    FormNote *string
    Lexemes  []CandidateLexeme
}

type CandidateLexeme struct {
    Key             string
    PartOfSpeechKey *string
    PartOfSpeech    string
    Unit            string
    Source          SourceID
    Translations    []string
    Gloss           *string
    Caption         *string
    Notes           []string
    Examples        []string
    Parent          bool
    ParentKey       *string
    Register        []string
    Entry           int
    Order           int
}
```

**Будет.**

```go
// Lemma — плюс транскрипции из записи (Д23).
type Lemma struct {
    Text           string
    FormNote       *string
    Pronunciations []string
    Lexemes        []CandidateLexeme
}

// CandidateLexeme — данные записи целиком: грамматика
// ключами, уровень, синонимы, антонимы (Д23); сопоставление
// от нейронки после проверки; Parent и ParentKey сняты
// (Д37).
type CandidateLexeme struct {
    Key              string
    PartOfSpeechKey  *string
    PartOfSpeech     string
    Unit             string
    Source           SourceID
    Translations     []string
    Gloss            *string
    Caption          *string
    Notes            []string
    Examples         []string
    Grammar          map[string]string
    Level            *string
    Synonyms         []string
    Antonyms         []string
    ComponentMatches []ComponentMatch
    Register         []string
    Entry            int
    Order            int
}

// ComponentMatch — строка сопоставления: компонент и опция
// «Выбора» или поле записи (контракт 5.1, Д54).
type ComponentMatch struct {
    ComponentID int64
    OptionID    *int64
    Field       *string
}

// WordRemark — замечание по слову (контракт 5.6, Д55):
// компонент непонятен или нет перевода у лексемы.
type WordRemark struct {
    Code        RemarkCode
    ComponentID *int64
    Lemma       *string
    LexemeKey   *string
}

// Recommendation — компонент, которого не хватает словарю
// под данные записи (LFL-89).
type Recommendation struct {
    Name     string
    Template string
    Options  []string
}
```

**Мотивация.** Нейронке нужны все метки записи (Д23), устройству —
сопоставление и замечания (Д8, Д17).

### Отбор и ошибки

**Было.** Типов отбора нет; ошибки домена: `ErrUnauthorized`,
`ErrRateLimited`, `ErrUnsupportedLanguage`, `ErrSourceUnavailable`,
`ErrNotFound`, `ErrInternal`.

**Будет.**

```go
// Selection — отбор нейронки по леммам всех слов и расход
// токенов всех вызовов; списывает сценарий (Ш6.3).
type Selection struct {
    Lemmas []LemmaSelection
    Usage  ModelUsage
    Failed []string
}

// LemmaSelection — что нейронка выбрала у леммы: до трёх
// ключей в порядке отбора, пары источников, сопоставление,
// замечания (Д12, Д20, Д24).
type LemmaSelection struct {
    Key    LemmaKey
    Picks  []Pick
    Failed bool
}

type Pick struct {
    Key     string
    Pairs   []string
    Matches []ComponentMatch
    Remarks []WordRemark
}

// ModelUsage — токены запроса и ответа модели (LIM-79).
type ModelUsage struct {
    Prompt     int
    Completion int
}

// Prompt, Answer — вход и выход одного вызова модели.
type Prompt struct {
    Text  string
    Words []WordResult
    Tree  ComponentTree
}

type Answer struct {
    Lemmas []LemmaSelection
}

// SelectionKey — ключ кеша отбора без контекста: слово,
// языки, дерево, модель (LFL-94).
type SelectionKey string

// ErrLLMUnavailable — вызовы нейронки были, ни один не
// ответил; ответ 503 (LFL-63.5).
var ErrLLMUnavailable = errors.New("llm unavailable")
```

**Мотивация.** Отбор — отдельная сущность домена, с ней работают три
сервиса: отбор, проверка, сборка.

## Э3. Слои: сценарий, Auth, Quota

ВЫПОЛНЕН 2026-10-20. `Refund` и `ChargeModel` отдают ошибку, в лог пишет
сценарий; лимит нейронки в `Options` Quota; запрос без заголовка — 401
через ошибку `ogen`.

Тезисы: LFL-27, LIM-29, LFL-30 как есть, новых нет. Проверка: тесты
`service/lookup`, `service/access` переезжают за кодом; `task test`
зелёный до и после; интеграционный тест «запрос без ключа → 401».

### Хук ключа

**Было.**

```go
type subjectKey struct{}

func (a *api) HandleAccessKey(
    ctx context.Context,
    _ wordsv1.OperationName,
    t wordsv1.AccessKey,
) (context.Context, error) {
    key := domain.AccessKey(strings.TrimSpace(t.Token))
    if key == "" {
        return ctx, domain.ErrUnauthorized
    }
    subject, err := a.subjects.ByKey(ctx, key)
    if err != nil {
        return ctx, err
    }
    return context.WithValue(
        ctx, subjectKey{}, subject,
    ), nil
}

func subjectFrom(
    ctx context.Context,
) (domain.Subject, bool) {
    subject, ok := ctx.Value(subjectKey{}).(domain.Subject)
    return subject, ok
}
```

**Будет.**

```go
// HandleAccessKey — Ш2, первая проверка: ключ из заголовка,
// субъект у сервиса Auth, субъект в контекст. Правило
// пустого ключа ушло в Auth.
func (h *wordsHandler) HandleAccessKey(
    ctx context.Context,
    op wordsv1.OperationName,
    key wordsv1.AccessKey,
) (context.Context, error) {
    accessKey := domain.AccessKey(
        strings.TrimSpace(key.Token),
    )
    subject, err := h.auth.SubjectByKey(ctx, accessKey)
    if err != nil {
        return ctx, err
    }
    return withSubject(ctx, subject), nil
}

// withSubject — копия контекста с субъектом внутри.
func withSubject(
    ctx context.Context,
    subject domain.Subject,
) context.Context

// subjectFrom — субъект из контекста; есть всегда, без него
// сервер до ручки не доходит. Флаг «нашёлся» снят.
func subjectFrom(ctx context.Context) domain.Subject
```

**Мотивация.** Хук — только HTTP; кто ты — решает сервис Auth, и
другие входы, Google и Telegram, придут туда же (Д47, К4).

### Сервис Auth

**Было.** Сервиса нет: хук сам ходит в `SubjectRepository.ByKey`
и сам знает правило пустого ключа.

**Будет.**

```go
// AuthService — кто ты. Порт в service/ports.go. Новый.
type AuthService interface {
    // SubjectByKey — пустой, неизвестный или отозванный
    // ключ — ErrUnauthorized, иначе субъект с лимитами.
    SubjectByKey(
        ctx context.Context,
        key domain.AccessKey,
    ) (domain.Subject, error)
}

// Пакет service/auth.
func (a *auth) SubjectByKey(
    ctx context.Context,
    key domain.AccessKey,
) (domain.Subject, error) {
    if key == "" {
        return domain.Subject{}, domain.ErrUnauthorized
    }
    return a.subjects.ByKey(ctx, key)
}
```

**Мотивация.** «Кто ты» и «сколько можно» — разные сервисы (К6,
Д47).

### Ручка

**Было.**

```go
type api struct {
    lookup   service.LookupService
    subjects service.SubjectRepository
}

func (a *api) LookupWords(
    ctx context.Context,
    req *wordsv1.LookupRequest,
    params wordsv1.LookupWordsParams,
) (wordsv1.LookupWordsRes, error) {
    subject, ok := subjectFrom(ctx)
    if !ok {
        return unauthorized(), nil
    }
    ctx = logger.ContextWithRunID(ctx, params.XRunID.Or(""))
    started := time.Now()
    resp, err := a.lookup.Lookup(
        ctx, converters.RequestToDomain(req, subject),
    )
    logger.Info(ctx, "lookup words",
        zap.String("subject", subject.Name),
        zap.String(
            "learning_language", req.LearningLanguage,
        ),
        zap.Int("tokens", len(req.Tokens)),
        zap.Int("words", len(resp.Words)),
        zap.Duration("took", time.Since(started)),
        zap.Error(err),
    )
    switch {
    case err == nil:
        return converters.ResponseToAPI(resp), nil
    case errors.Is(err, domain.ErrRateLimited):
        return &wordsv1.LookupWordsTooManyRequests{
            Code:    wordsv1.ErrorCodeRateLimited,
            Message: "daily limit",
        }, nil
    case errors.Is(err, domain.ErrUnsupportedLanguage):
        return &wordsv1.LookupWordsBadRequest{
            Code:    wordsv1.ErrorCodeUnsupportedLanguage,
            Message: "learning language",
        }, nil
    case errors.Is(err, domain.ErrUnauthorized):
        return unauthorized(), nil
    default:
        return nil, err
    }
}
```

**Будет.**

```go
type wordsHandler struct {
    words service.LookupScenario
    auth  service.AuthService
}

// LookupWords — Ш1 и Ш8: субъект из контекста, запрос в
// домен, сценарий, ответ в контракт. 401 отдан в хуке;
// мёртвая ветка ErrUnauthorized снята.
func (h *wordsHandler) LookupWords(
    ctx context.Context,
    req *wordsv1.LookupRequest,
    params wordsv1.LookupWordsParams,
) (wordsv1.LookupWordsRes, error) {
    subject := subjectFrom(ctx)
    ctx = logger.ContextWithRunID(ctx, params.XRunID.Or(""))
    started := time.Now()
    resp, err := h.words.Lookup(
        ctx,
        converters.RequestToDomain(req, subject),
    )
    logRequest(ctx, subject, req, resp, started, err)

    switch {
    case err == nil:
        return converters.ResponseToAPI(resp), nil
    case errors.Is(err, domain.ErrLLMUnavailable):
        return llmUnavailable(), nil
    case errors.Is(err, domain.ErrRateLimited):
        return rateLimited(), nil
    case errors.Is(err, domain.ErrUnsupportedLanguage):
        return unsupportedLanguage(), nil
    default:
        return nil, err
    }
}

// llmUnavailable — 503 с кодом llm_unavailable. Новая.
func llmUnavailable() *wordsv1.LookupWordsServiceUnavailable

// rateLimited — 429: лимит субъекта, потолок сервера, лимит
// нейронки. Вынесена из ручки.
func rateLimited() *wordsv1.LookupWordsTooManyRequests

// unsupportedLanguage — 400 unsupported_language. Вынесена.
func unsupportedLanguage() *wordsv1.LookupWordsBadRequest

// logRequest — одна строка лога на запрос. Вынесена.
func logRequest(
    ctx context.Context,
    subject domain.Subject,
    req *wordsv1.LookupRequest,
    resp domain.LookupResponse,
    started time.Time,
    err error,
)
```

**Мотивация.** В ручке только возможные ошибки: 401 до неё не
доходит; 503 — новый ответ нейронки (LFL-63.5).

### Сценарий вместо сервиса запроса

**Было.**

```go
// Пакет service/lookup.
type service struct {
    access     def.AccessService
    normalizer def.Normalizer
    resolver   def.Resolver
    meanings   def.MeaningsSource
    cache      def.CacheRepository
    options    Options
}

func (s *service) Lookup(
    ctx context.Context,
    req domain.LookupRequest,
) (domain.LookupResponse, error) {
    err := s.access.Check(ctx, req)
    if err != nil {
        return rejected(err)
    }
    err = s.access.Charge(
        ctx, req.Subject, tokenCount(req.Tokens),
    )
    if err != nil {
        return rejected(err)
    }
    thresholds := s.options.Thresholds
    words := s.normalizer.Words(req.Tokens, thresholds)
    if noWords(words) {
        return emptyResponse(), nil
    }
    budget := s.options.Budget
    results := s.lookupEach(
        ctx, budget, words, languagesOf(req),
    )
    return response(results), nil
}
```

**Будет.**

```go
// Пакет scenario/lookup. Сценарий зовёт сервисы по шагам,
// сам в порты не ходит (Д48).
type lookupScenario struct {
    quota      service.QuotaService
    normalizer service.Normalizer
    resolver   service.Resolver
    meanings   service.MeaningsSource
    selector   service.Selector
    verifier   service.Verifier
    options    Options
}

func (s *lookupScenario) Lookup(
    ctx context.Context,
    req domain.LookupRequest,
) (domain.LookupResponse, error) {
    if err := s.quota.Check(ctx, req); err != nil {
        return rejected(err)
    }
    err := s.quota.Charge(
        ctx, req.Subject,
        tokenCount(req.Tokens),
    )
    if err != nil {
        return rejected(err)
    }
    ctx, finish := withinBudget(ctx, s.options.Budget)
    defer finish()

    words := s.normalizer.Words(
        req.Tokens, s.options.Thresholds,
    )
    if noWords(words) {
        return emptyResponse(), nil
    }
    text := s.normalizer.Text(req.Tokens)

    found := s.lookupEach(ctx, words, languagesOf(req))

    picks, err := s.selector.Select(
        ctx, found,
        text, req.Components,
    )
    if err != nil {
        s.quota.Refund(
            ctx, req.Subject,
            tokenCount(req.Tokens),
        )
        return rejected(err)
    }
    s.quota.ChargeModel(ctx, req.Subject, picks.Usage)
    picks = s.verifier.Verify(picks, found, req.Components)

    return response(assembled(found, picks)), nil
}
```

**Мотивация.** Один сценарий на операцию знает порядок шагов и ветки;
сервисы — по шагу, друг в друга не ходят (Д48).

### Quota вместо Доступа

**Было.**

```go
// Пакет service/access.
func (a *access) Check(
    ctx context.Context,
    req domain.LookupRequest,
) error {
    today := a.clock.Now()
    spent, err := a.usage.Spent(ctx, req.Subject, today)
    if err != nil {
        return internal(err)
    }
    limit := req.Subject.Limits.SubjectDaily
    if wouldExceed(spent, req.Tokens, limit) {
        return domain.ErrRateLimited
    }
    spentTotal, err := a.usage.SpentTotal(ctx, today)
    if err != nil {
        return internal(err)
    }
    ceiling := a.options.ServerDaily
    if wouldExceed(spentTotal, req.Tokens, ceiling) {
        return domain.ErrRateLimited
    }
    languages := a.options.SupportedLanguages
    if unsupported(req.LearningLanguage, languages) {
        return domain.ErrUnsupportedLanguage
    }
    return nil
}

func (a *access) Charge(
    ctx context.Context,
    subject domain.Subject,
    tokens int,
) error {
    today := a.clock.Now()
    return a.usage.Charge(ctx, subject, today, tokens)
}
```

**Будет.** Пакет `service/quota`: `Check` как было плюс лимит
нейронки между потолком сервера и языком (код в HLD, «Quota»);
`Charge` как было; новые `Refund` и `ChargeModel`:

```go
// Refund — В2: вернуть списанное при 503 (Д43). Ответ уже
// решён, сбой хранилища — в лог.
func (q *quota) Refund(
    ctx context.Context,
    subject domain.Subject,
    tokens int,
) {
    err := q.usage.Refund(
        ctx, subject,
        q.clock.Now(), tokens,
    )
    if err != nil {
        logger.Error(ctx, "refund failed", zap.Error(err))
    }
}

// ChargeModel — Ш6.3: токены модели всех вызовов в счётчик
// нейронки субъекта (LIM-79).
func (q *quota) ChargeModel(
    ctx context.Context,
    subject domain.Subject,
    usage domain.ModelUsage,
) {
    err := q.usage.ChargeModel(
        ctx, subject,
        q.clock.Now(), usage,
    )
    if err != nil {
        logger.Error(
            ctx, "model usage not charged",
            zap.Error(err),
        )
    }
}
```

**Мотивация.** Списание и откат в одном месте, не в отборе (Д47,
Д48); сбой после решения — лог, не ошибка (К11, ждёт слова).

### Порты и хранилище расхода

**Было.**

```go
type UsageRepository interface {
    Spent(
        ctx context.Context,
        subject domain.Subject,
        day time.Time,
    ) (int, error)
    SpentTotal(
        ctx context.Context,
        day time.Time,
    ) (int, error)
    Charge(
        ctx context.Context,
        subject domain.Subject,
        day time.Time,
        tokens int,
    ) error
}
```

**Будет.** В `UsageRepository` плюс `ModelSpent`, `Refund`,
`ChargeModel` (сигнатуры в HLD, «Quota»); в
`repository/postgres/usage.go` и `repository/memory/usage.go` —
реализация: таблица расхода получает колонку токенов модели,
`Refund` вычитает из расхода дня, не ниже нуля. Новые порты
`QuotaService`, `AuthService`, `LookupScenario`, `Selector`,
`Verifier`, `LLMClient`, `SelectionCache` — в `service/ports.go`,
моки — `task mockery:gen`.

**Мотивация.** Расход модели — свой счётчик и свой лимит (LIM-79,
LIM-96); порты — границы слоёв по `architecture.md`.

### Сборка

**Было.**

```go
func (di *diContainer) LookupService(
    ctx context.Context,
) service.LookupService {
    if di.lookupService == nil {
        di.lookupService = lookup.NewService(
            di.AccessService(ctx),
            di.Normalizer(ctx),
            di.Resolver(ctx),
            di.MeaningsSource(ctx),
            di.CacheRepository(ctx),
            lookup.Options{
                Budget: responseBudget,
                Parallelism: config.AppConfig().
                    Source.Parallelism(),
            },
        )
    }
    return di.lookupService
}

handlers := apiv1.NewAPI(
    di.LookupService(ctx), di.SubjectRepository(ctx),
)
```

**Будет.**

```go
func (di *diContainer) LookupScenario(
    ctx context.Context,
) service.LookupScenario {
    if di.lookupScenario == nil {
        di.lookupScenario = lookup.NewScenario(
            di.QuotaService(ctx),
            di.Normalizer(ctx),
            di.Resolver(ctx),
            di.MeaningsSource(ctx),
            di.Selector(ctx),
            di.Verifier(ctx),
            lookup.Options{
                Budget:      responseBudget,
                Parallelism: cfg.Source.Parallelism(),
                Thresholds:  domain.Thresholds{},
            },
        )
    }
    return di.lookupScenario
}

handlers := apiv1.NewHandler(
    di.LookupScenario(ctx), di.AuthService(ctx),
)
```

**Мотивация.** `app` знает всех и собирает слои; сервисы конфиг не
читают.

## Э4. Данные записи целиком

ВЫПОЛНЕН 2026-10-20. Код в `baselookup/meanings.go` и в сервисе Леммы,
не в `clients/converters`; `pattern` не идёт; тесты на выгрузках
`testdata/kaikki`.

Тезисы: LFL-76, LFL-81. Проверка: тесты `clients/converters` на
фикстурах прода `perro`, `banco`, `run`, `corrían`.

### Грамматика и прочие метки

**Было.** `clients/converters/kaikki_rules.go`: `genderOf(tags)`,
`countabilityOf(tags)` и прочие читают теги записи в структуру
грамматики, но дальше `Parameters` она не шла и в
`CandidateLexeme` её нет; синонимы, антонимы, уровень и произношения
не читаются.

**Будет.**

```go
// grammarOf — метки записи ключами: gender, countability,
// transitivity, reflexive, comparable, position, pattern,
// plural, comparison, irregular; пустые не кладутся (Д23).
func grammarOf(tags map[string]bool) map[string]string

// lexemeFrom — лексема из записи: плюс Grammar, Level,
// Synonyms, Antonyms; формы слова не идут.
func lexemeFrom(sense kaikkiSense) domain.CandidateLexeme

// lemmaFrom — лемма: плюс Pronunciations из записи.
func lemmaFrom(entry kaikkiEntry) domain.Lemma
```

**Мотивация.** Нейронка сопоставляет по именам полей и меткам: что
не отдали — не сопоставить (Д23, LFL-81).

## Э5. Нейронка: отбор и сопоставление

ВЫПОЛНЕН 2026-10-20. Порт отбора принимает `Prompt`, кеш по слову, лог
вызова в клиенте, выключатель даёт 503; `task test:live` на нейронке не
гонялся — ключа OpenRouter на ноутбуке нет.

Тезисы: LFL-80, LFL-82, LFL-83, LFL-84, LFL-86, LFL-93, LFL-94,
LFL-100. Проверка: промпт — золотые файлы по фикстурам и
`control_set.md`; отбор — мок `LLMClient`; `task test:live` на
`perro`, `corrían`, `banco`, `zarpó`.

Допущения: клиент OpenRouter — свой `net/http` без библиотеки; ответ
модели — JSON по контракту, раздел 4.

### Текст для нейронки

**Было.** `normalizer.Words` — единственный выход нормализации:
токены → слова, повторы схлопнуты.

**Будет.**

```go
// Text — Ш3 для нейронки: токены как пришли, с повторами,
// через пробел, до чистки (LFL-80, Д22).
func (n *normalizer) Text(tokens []domain.Token) string {
    parts := make([]string, 0, len(tokens))
    for _, token := range tokens {
        parts = append(parts, token.Text)
    }
    return strings.Join(parts, " ")
}
```

**Мотивация.** Контекст — это текст снимка в порядке и с повторами
(Д22).

### Отбор

**Было.** Отбора нет: `lookupOne` отдаёт все лексемы леммы, устройство
показывает пять и «ещё N».

```go
func (s *service) lookupOne(
    ctx context.Context,
    word domain.Word,
    langs languages,
) domain.WordResult {
    if isTooLong(word) {
        return notFound(word)
    }
    cached, found := s.fromCache(ctx, word, langs.learning)
    if found {
        return cached
    }
    if budgetExhausted(ctx) {
        return sourceError(word)
    }
    result, err := s.resolver.Candidates(
        ctx, word, langs.learning,
    )
    if err != nil {
        return sourceError(word)
    }
    blocks := s.blocksByLemma(ctx, result, langs)
    result = withMeanings(result, blocks)
    result = withLemmas(result, blocks)
    s.toCache(ctx, word, langs.learning, result)
    return result
}
```

**Будет.** `lookupOne` без кеша источника и без `withMeanings`:
леммы со всеми лексемами обоих источников; кеш переезжает на отбор.
Пакет `service/selection`, `Select` и помощники — код в HLD,
«Отбор»: `splitByCache`, `batchesOf`, `inParallel`, `selectBatch`,
`noneAnswered`, `merged`. Порт клиента и его адаптер:

```go
// LLMClient — один вызов модели. Порт, новый.
type LLMClient interface {
    Complete(
        ctx context.Context,
        prompt domain.Prompt,
    ) (domain.Answer, domain.ModelUsage, error)
}

// Пакет clients/openrouter. Complete — POST
// /api/v1/chat/completions с моделью и ключом из конфига,
// системный промпт из prompt.go, ответ JSON → Answer,
// расход из usage ответа; сеть, не 200, пустой или не JSON
// — error.
func (c *client) Complete(
    ctx context.Context,
    prompt domain.Prompt,
) (domain.Answer, domain.ModelUsage, error)

// buildMessages — промпт: правила отбора (до трёх по роли
// без почти-синонимов, пары источников, строка в ядро
// обязательна), текст снимка, дерево компонентов, записи
// обоих источников одним списком с метками (Д12, Д20, Д24,
// Д25, Д49).
func buildMessages(prompt domain.Prompt) []message

// Кеш отбора: repository/memory/selection.go, Get и Put по
// SelectionKey до рестарта (Д45).
```

**Мотивация.** Это и есть фича: отбор по контексту и сопоставление с
компонентами пользователя (Д8–Д11); кеш — чтобы слово без контекста
не ходило в нейронку дважды (Д45).

## Э6. Проверка ответа нейронки

ВЫПОЛНЕН 2026-10-20. `Verify` получает ещё язык перевода; «лексема по
парам» — метод домена, общий для проверки и сборки; рекомендация
компонента из дерева отбрасывается.

Тезисы: LFL-85, LFL-87, LFL-88, LFL-91, LFL-97. Проверка: тесты
`service/verify` на подложных ответах: чужой ключ, чужая опция, нет
строки в ядро, пустой отбор, две опции у «Выбора» без «несколько».

**Было.** Проверять нечего, ответа нейронки нет.

**Будет.** `Verify` и помощники — код в HLD, «Проверка ответа
нейронки»: `lexemesOf`, `withKnownKeys`, `withKnownPairs`,
`withValidComponentMatches`, `withCoreValue`, `withSingleOption`,
`withKnownMessages`, `nothingLeft`, `failed`.

**Мотивация.** Нейронка только выбирает из готового; что выдумала —
вон; без строки в ядро лексема негодная (Д10, Д49).

## Э7. Сборка ответа и путь без нейронки

ВЫПОЛНЕН 2026-10-20. Пути без нейронки нет (LFL-92 отменён, Д38):
`firstPerPartOfSpeech` и `recommended` не делались.

Тезисы: LFL-89, LFL-90, LFL-92, LFL-63.5. Проверка: тесты сценария
с моком отбора: успех, упавшая пачка, полный отказ, выключенная
нейронка.

### Сборка

**Было.**

```go
func response(
    results []domain.WordResult,
) domain.LookupResponse {
    return domain.LookupResponse{
        Attribution: domain.Attribution,
        Words:       results,
    }
}
```

**Будет.**

```go
// assembled — Ш8: у леммы лексемы в порядке отбора, не
// больше трёх; толкование и грамматика из первой пары,
// примеры со всех пар; сопоставление и замечания по слову;
// лемма с пустым отбором выпадает (Д31); слова упавших
// пачек — «ошибка». Новая.
func assembled(
    found []domain.WordResult,
    picks domain.Selection,
) []domain.WordResult

// response — ответ из слов плюс рекомендации компонентов и
// признак нейронки.
func response(
    results []domain.WordResult,
    recommendations []domain.Recommendation,
    llm bool,
) domain.LookupResponse

// recommended — компонент, которого нет в дереве, а в
// записях есть непустое поле под него (Д30, LFL-89).
func recommended(
    found []domain.WordResult,
    tree domain.ComponentTree,
) []domain.Recommendation
```

**Мотивация.** Устройство получает готовые лексемы в порядке отбора,
а не весь источник (Д12, Д18).

### Без нейронки

**Было.** Пути два: ответ источника целиком или ошибка.

**Будет.**

```go
// firstPerPartOfSpeech — В2: нейронка выключена или не
// ответила — по одному значению на часть речи, первое по
// порядку источника, без сопоставления, llm=false (Д21,
// LFL-92). Новая.
func firstPerPartOfSpeech(
    found []domain.WordResult,
) []domain.WordResult
```

В сценарии: `ErrLLMUnavailable` от отбора — откат списания и 503
(LFL-63.5), как в коде Э3; упавшая пачка — её слова «ошибка»;
выключатель в конфиге — сразу `firstPerPartOfSpeech`.

**Мотивация.** Без нейронки устройство получает честный первый ответ,
а не пустоту (Д21).

## Э8. Квоты нейронки и конфиг

ВЫПОЛНЕН 2026-10-20. `Limits.ModelDaily` не заводился: лимит один из
конфига в `Options` Quota; `deploy.md` и `.env.example` дополнены.

Тезисы: LIM-79, LIM-96. Проверка: тесты `service/quota` с подменой
часов и памятью, как у `service/access` сейчас.

### Лимиты субъекта

**Было.**

```go
type Subject struct {
    Name   string
    Limits Limits
}

type Limits struct {
    SubjectDaily int
    ServerDaily  int
}
```

**Будет.**

```go
// Limits — плюс суточный лимит вызовов нейронки; 0 — лимита
// нет (Д16, LIM-96).
type Limits struct {
    SubjectDaily int
    ServerDaily  int
    ModelDaily   int
}
```

`Check` в `service/quota` после потолка сервера читает `ModelSpent`
и при `exceeded(modelSpent, ModelDaily)` отдаёт `ErrRateLimited` —
код в HLD, «Quota».

**Мотивация.** Расход у провайдера без лимита — риск счёта; лимит и
выключатель — страховка (Д16, раздел «Риски» HLD).

### Конфиг

**Было.** `config.go`: `Logger`, `HTTP`, `Source`, `Base`, `Pg` из
`.env`; нейронки нет.

**Будет.**

```go
// LLMConfig — из .env: LLM_ENABLED, LLM_PROVIDER_URL,
// LLM_API_KEY, LLM_MODEL, LLM_BATCH_SIZE (1),
// LLM_PARALLELISM, LLM_SUBJECT_DAILY; ключ в логи не идёт
// (LFL-93).
type LLMConfig struct {
    Enabled      bool
    ProviderURL  string
    APIKey       string
    Model        string
    BatchSize    int
    Parallelism  int
    SubjectDaily int
}
```

`deploy.md` — переменные и где лежит ключ OpenRouter.

**Мотивация.** Провайдер и модель меняются без кода (Д35); старт с
пачкой в одно слово (Д33).

## Э9. Логи, покрытие, релиз

ВЫПОЛНЕН 2026-10-20. Покрытие без нарушений, 51 открытая строка у
принятых тезисов; LFL-27.5 сервером не выполняется (400 вместо 413),
решение за юзером; фикстуры прода не переснимались.

Тезисы: LFL-101. Проверка: `task lint`, `task test`,
`task spec:coverage` без дыр, `task test:live`.

### Лог вызова нейронки

**Было.** Одна строка лога на запрос в ручке; вызовов нейронки нет.

**Будет.**

```go
// logCall — одна строка на вызов модели: модель, слова
// пачки, токены запроса и ответа, длительность, исход
// (LFL-101). Ключ и промпт целиком не пишутся. Новая.
func logCall(
    ctx context.Context,
    model string,
    batch []domain.WordResult,
    usage domain.ModelUsage,
    took time.Duration,
    err error,
)
```

**Мотивация.** Без лога расход нейронки не объяснить и не сверить со
счётом провайдера.

### Покрытие и релиз

**Было.** Покрытие тезисов LFL строками тестов — `task spec:coverage`
по стороне «сервер»; прод v0.1.9; фикстуры прода от 2026-10-15.

**Будет.** Все тезисы из этапов выше со строками в тестах, черновики
→ «реализовано»; тег v0.1.10 и деплой; фикстуры переснять с прода;
ручник IS529 — «Прогон» и кейс «Неверный ключ» пересмотреть.

**Мотивация.** Покрытие по стороне — ворота к коммиту; фикстуры
нужны свежие под новый контракт.
