# IS536 | HLD — черновик

Высокоуровневый план фичи «Лексемы-кандидаты через нейронку»: как
устроено решение целиком. Что править и в каком файле — в LLD.

## 1. Рамка

- **Цель** — пользователь получает до трёх значений слова из словаря, уже
  сопоставленных с его компонентами. Отбирает и сопоставляет нейронка на
  сервере, сервер проверяет её ответ (Д8, Д10, Д12, Д20).
- **Вход** — карточка слова, одно слово, и снимок, все слова текста: они
  же контекст. Ручка одна (Д22, Д26).
- **Документы** — [бриф](brief.md), [решения](decisions.md), спека
  бэкенда LFL (флоу Ш1–Ш8, В1, В2), контракт на примерах `contract.md` и
  `api/openapi.yaml` 0.3.0-draft, спека снимка TFP, экранные спеки AWP и
  WCL.

## 2. Бэкенд: схема и код

### Условные знаки

![Условные знаки](/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS536_llm_values/hld-legend.a02d0bb8.png)

### Бэкенд: слои по C4

В коробке имя, тип в скобках, одна фраза ответственности. На стрелках
намерение связи. Справа от ручки сноска: ответы и ошибки по контракту.
Что меняет IS536 — цветом на схеме и пометками в коде. Источник —
`hld-backend-layers-c4.dot`.

Номер на стрелке — шаг флоу спеки LFL: Ш1–Ш8. Ш2.1 и подобные — вызов
порта внутри шага, по порядку. Отбор и сопоставление, Ш6 и Ш7, идут одним
вызовом нейронки, поэтому у отбора и проверки номер Ш6–7. Слева направо
всё идёт по возрастанию шагов: блоки логики, подписи, порты, адаптеры.
Отбор и проверка повторяются для каждой пачки слов. «Реализован в» и
HTTPS без номеров: это не шаги, а чем реализован порт.

![Бэкенд: слои по C4](/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS536_llm_values/hld-backend-layers-c4.13a364b9.png)

Код под схемой, в порядке вызова: ручка с хуком ключа и обработчиком
ошибок, сценарий, потом сервисы, которые он зовёт. Реализованы
публичные функции, всё, что они зовут, дано сигнатурами с говорящими
именами, без тел, под своей функцией. Стиль — как в нынешних
`internal/api/words/v1/lookup.go` и `internal/service/lookup`.
Комментарий у шага — его номер во флоу LFL.

Имена переименованы: структура ручки `wordsHandler` вместо `api`,
сценарий `LookupScenario` вместо сервиса `LookupService`, его реализация
`lookupScenario` вместо `service`, поле в ручке `words` вместо `lookup`.
Было `a.lookup.Lookup()`, стало `h.words.Lookup()`: ручка слов просит
сценарий найти значения слов. Сервис «Доступ» разделён на `Auth`, кто
ты, и `Quota`, сколько тебе можно (Д47).

Слои кода (Д48), описание для всего бэкенда —
`backend/docs/handbook/architecture.md`. Три слоя, соседи друг в друга
не ходят:

- **Сценарий** — один на операцию контракта, знает порядок шагов и
  ветки. Его зовёт только ручка. Он зовёт только сервисы.
- **Сервис** — один шаг. Его зовут только сценарии. Он зовёт только
  порты. Сервисы между собой не общаются, сценарии между собой тоже.
- **Порт** — интерфейс к хранилищу или внешней системе. Его зовут только
  сервисы.

Как это легло в код ниже:

- **Сценарий `LookupScenario`** в пакете `scenario/lookup`, бывший
  сервис запроса. Зовёт `Quota`, Нормализацию, Леммы, Значения, Отбор и
  Проверку ответа нейронки. Единственный, кто зовёт сервисы.
- **Токены модели списывает сценарий, не Отбор.** `Select` отдаёт расход
  всех вызовов нейронки в отборе, сценарий после него зовёт
  `quota.ChargeModel` (Ш6.3). Проверка, списание и откат — в одном месте.
  Отбор про `Quota` не знает, субъект ему не нужен.
- **Хук ключа — точка входа, не сценарий.** `HandleAccessKey` зовёт
  `Auth` одним вызовом. Сценарий из одного шага ради формы не заводится.
- **Общего кода у двух сервисов не бывает.** Если двум сервисам нужна
  одна функция, нарезка неверная: функция уходит в один из них, или
  появляется третий сервис, который зовёт сценарий. В домен уходят
  только понятия, не помощники.
- **Счётчики после решения — лог, не ошибка.** `Quota.Refund` и
  `Quota.ChargeModel` зовутся, когда ответ клиенту уже решён: сбой
  хранилища пишется в лог, наверх не идёт, ответ не ломается. До решения
  наоборот: сбой чтения расхода в `Check` и списания в `Charge` — 500.

### Ручка

Сначала проверка ключа: её зовёт сгенерированный из контракта сервер,
до ручки, на каждый запрос. Отказ здесь — 401, ручка не зовётся.

```go
// HandleAccessKey — Ш2, первая проверка. Сервер зовёт её до
// LookupWords с ключом из заголовка Authorization: Bearer.
// Только HTTP-часть: ключ из заголовка, субъект у сервиса
// Auth, субъект в контекст, по нему ручка найдёт субъекта
// через subjectFrom. Ошибку не мапит: отдаёт серверу как
// есть, тот зовёт ErrorHandler, блок после ручки, там
// ErrUnauthorized становится 401, всё остальное — 500.
// Меняется: раньше ходила в хранилище субъектов сама.
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

// withSubject — копия контекста с субъектом внутри, пара к
// subjectFrom. Есть в нынешнем хуке строкой, выносится в
// функцию.
func withSubject(
    ctx context.Context,
    subject domain.Subject,
) context.Context
```

Что зовёт проверка ключа. Сервис Auth, порт `AuthService`, новый:

```go
// SubjectByKey — Ш2, правило ключа: пустой, неизвестный или
// отозванный ключ — ErrUnauthorized, иначе субъект:
// владелец ключа с лимитами. Правило живости ключа
// переезжает сюда из запроса к Postgres. Другие способы
// входа, Google и Telegram, добавятся сюда же своими
// методами.
SubjectByKey(
    ctx context.Context,
    key domain.AccessKey,
) (domain.Subject, error)
```

Потом сама ручка:

```go
// LookupWords — Ш1 и Ш8: субъект из контекста, запрос в
// домен, сценарий, ответ в контракт. Субъект уже в
// контексте: его положил HandleAccessKey, блок выше, 401
// отдан ещё там. Отказы по лимитам, языку и 503 —
// типизированные ответы, сбой сервера — error наверх, 500
// через ErrorHandler. 413 и 400 по форме запроса — там же,
// до ручки.
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
        // конвертер теперь переносит и дерево компонентов
        converters.RequestToDomain(req, subject),
    )
    logRequest(ctx, subject, req, resp, started, err)

    switch {
    case err == nil:
        // конвертер теперь отдаёт и сопоставление,
        // замечания, грамматику
        return converters.ResponseToAPI(resp), nil
    // новая ветка: нейронка не ответила ни разу, В2
    case errors.Is(err, domain.ErrLLMUnavailable):
        return llmUnavailable(), nil
    // лимит субъекта, потолок сервера, теперь и лимит
    // нейронки
    case errors.Is(err, domain.ErrRateLimited):
        return rateLimited(), nil
    case errors.Is(err, domain.ErrUnsupportedLanguage):
        return unsupportedLanguage(), nil
    default:
        return nil, err
    }
}

// subjectFrom — владелец ключа доступа, с которым пришёл
// запрос: имя и его лимиты. Сервер пользователей не знает,
// учёт и лимиты идут по владельцу ключа; сейчас ключ один
// на всё приложение. Нашёл его по ключу сервис Auth, в
// контекст положил HandleAccessKey, блок выше. Субъект в
// контексте есть всегда: без него сервер до ручки не
// доходит. Сборка без хука ловится интеграционным тестом:
// запрос без ключа → 401. Меняется: флаг «нашёлся» снят.
func subjectFrom(ctx context.Context) domain.Subject

// llmUnavailable — ответ 503 с кодом llm_unavailable:
// вызовы нейронки были, ни один не ответил. Новая.
func llmUnavailable() *wordsv1.LookupWordsServiceUnavailable

// rateLimited — ответ 429 с кодом rate_limited: лимит
// субъекта, потолок сервера или лимит нейронки. Вынесена из
// нынешней ручки, новая.
func rateLimited() *wordsv1.LookupWordsTooManyRequests

// unsupportedLanguage — ответ 400 с кодом
// unsupported_language: изучаемого языка нет среди языков
// базы слов. Вынесена из нынешней ручки, новая.
func unsupportedLanguage() *wordsv1.LookupWordsBadRequest
```

Что зовёт ручка:

```go
// logRequest — одна строка лога на запрос: субъект, язык,
// число токенов и слов, время, ошибка. Вынесена из нынешней
// ручки, новая.
func logRequest(
    ctx context.Context,
    subject domain.Subject,
    req *wordsv1.LookupRequest,
    resp domain.LookupResponse,
    started time.Time,
    err error,
)

// RequestToDomain — запрос контракта в домен: субъект,
// языки, токены как пришли, с повторами, плюс дерево
// компонентов. Пакет converters, меняется.
func RequestToDomain(
    req *wordsv1.LookupRequest,
    subject domain.Subject,
) domain.LookupRequest

// ResponseToAPI — ответ домена в контракт: леммы с
// лексемами, сопоставление, замечания, грамматика, уровень,
// синонимы, произношение; без candidates. Пакет converters,
// меняется.
func ResponseToAPI(
    resp domain.LookupResponse,
) *wordsv1.LookupResponse
```

Если хук или ручка вернули `error`, сервер зовёт `ErrorHandler`: наш
код в `errors.go` пакета ручки. Ошибки до ручки, ключ и схема запроса,
и всё, что не описано в контракте, становятся кодом ответа только здесь.
Хук свои ошибки не мапит никогда.

```go
// ErrorHandler — сервер зовёт его с error от хука, от
// разбора запроса по схеме или от ручки. Ошибка → код HTTP
// и код контракта, 500 ещё и в лог. Меняется: сбой хука не
// по ключу — 500, не 401.
func ErrorHandler(
    ctx context.Context,
    w http.ResponseWriter,
    r *http.Request,
    err error,
) {
    status, code := classify(err)
    if status == http.StatusInternalServerError {
        logger.Error(ctx, "request failed", zap.Error(err))
    }
    writeError(w, status, code)
}

// classify — какая ошибка, такой и код. Ошибка хука: по
// ключу — 401, иначе 500. Ошибка схемы: слишком много
// токенов — 413, иначе 400. Остальное — 500.
func classify(err error) (int, wordsv1.ErrorCode) {
    var security *ogenerrors.SecurityError
    if errors.As(err, &security) {
        // ключ пустой, неизвестен или отозван
        if errors.Is(err, domain.ErrUnauthorized) {
            return http.StatusUnauthorized,
                wordsv1.ErrorCodeUnauthorized
        }
        // хук упал не по ключу, например легла база
        return http.StatusInternalServerError,
            wordsv1.ErrorCode("internal")
    }
    var decode *ogenerrors.DecodeRequestError
    var params *ogenerrors.DecodeParamsError
    if errors.As(err, &decode) || errors.As(err, &params) {
        if tooManyTokens(err) {
            return http.StatusRequestEntityTooLarge,
                wordsv1.ErrorCodeRequestTooLarge
        }
        return http.StatusBadRequest,
            wordsv1.ErrorCodeInvalidRequest
    }
    return http.StatusInternalServerError,
        wordsv1.ErrorCode("internal")
}
```

Что зовёт `ErrorHandler`:

```go
// writeError — JSON {code, message} с нужным статусом.
// Есть.
func writeError(
    w http.ResponseWriter,
    status int,
    code wordsv1.ErrorCode,
)

// tooManyTokens — ошибка схемы именно про число токенов,
// больше предела (LFL-28). Есть.
func tooManyTokens(err error) bool
```

### Сценарий

Сценарий `LookupScenario`, один на операцию `lookup` контракта. Знает
порядок шагов и ветки, зовёт сервисы, сам в порты не ходит (Д48).

```go
// Lookup — Ш2–Ш8: лимиты и списание, слова и контекст,
// леммы со всеми значениями, отбор и сопоставление
// нейронкой, токены модели в счётчик, проверка ответа
// нейронки, сборка ответа. Нейронка не ответила ни разу —
// списание откатывается, наверх ErrLLMUnavailable (В2).
// Меняется: бывший сервис запроса, плюс Ш6.3.
func (s *lookupScenario) Lookup(
    ctx context.Context,
    req domain.LookupRequest,
) (domain.LookupResponse, error) {
    // Ш2: лимит субъекта, потолок сервера, лимит нейронки,
    // язык; ключ и размер запроса проверены до ручки
    if err := s.quota.Check(ctx, req); err != nil {
        return rejected(err)
    }
    // Ш2.2: токены запроса списываются до работы
    err := s.quota.Charge(
        ctx, req.Subject,
        tokenCount(req.Tokens),
    )
    if err != nil {
        return rejected(err)
    }
    // 170 с на всё
    ctx, finish := withinBudget(ctx, s.options.Budget)
    defer finish()

    // Ш3: токены → слова для поиска, сырые токены → текст
    // для нейронки
    words := s.normalizer.Words(
        req.Tokens, s.options.Thresholds,
    )
    if noWords(words) {
        return emptyResponse(), nil
    }
    text := s.normalizer.Text(req.Tokens)

    // Ш4–Ш5: леммы и все их значения, 8 слов параллельно, в
    // порядке слов
    found := s.lookupEach(ctx, words, languagesOf(req))

    // Ш6–Ш7: отбор и сопоставление нейронкой; только 503
    // прерывает сценарий
    picks, err := s.selector.Select(
        ctx, found,
        text, req.Components,
    )
    if err != nil {
        // Д43: вернуть списанное; сбой отката Quota пишет в
        // лог
        s.quota.Refund(
            ctx, req.Subject,
            tokenCount(req.Tokens),
        )
        return rejected(err)
    }
    // Ш6.3: токены модели всех вызовов в счётчик нейронки
    // субъекта
    s.quota.ChargeModel(ctx, req.Subject, picks.Usage)
    picks = s.verifier.Verify(picks, found, req.Components)

    // Ш8: лексемы по парам, сопоставление, замечания,
    // статусы слов
    return response(assembled(found, picks)), nil
}
```

Что зовёт сценарий. Сервисы `s.quota`, `s.normalizer`, `s.selector` и
`s.verifier` реализованы ниже, в порядке вызова.

Пакет `scenario/lookup`:

```go
// withinBudget — контекст с дедлайном по бюджету ответа; 0
// — без предела (LFL-55). Есть, не меняется.
func withinBudget(
    ctx context.Context,
    budget time.Duration,
) (context.Context, context.CancelFunc)

// tokenCount — сколько токенов списать: все присланные
// (LIM-29.6). Есть.
func tokenCount(tokens []domain.Token) int

// noWords — после нормализации не осталось ни одного слова.
// Есть.
func noWords(words []domain.Word) bool

// emptyResponse — ответ без слов, атрибуция на месте
// (LFL-54.2). Есть.
func emptyResponse() domain.LookupResponse

// languagesOf — пара языков запроса: изучаемый и перевода.
// Есть.
func languagesOf(req domain.LookupRequest) languages

// lookupEach — Ш4–Ш5: по словам параллельно, у слова леммы
// (сервис Леммы), у леммы все лексемы обоих Викисловарей со
// всеми метками (сервис Значения); не найдено и ошибка
// источника — статус без лемм. Результаты в порядке слов.
// Меняется: без отбора и без кеша источника, отбор уходит в
// selector.
func (s *lookupScenario) lookupEach(
    ctx context.Context,
    words []domain.Word,
    langs languages,
) []domain.WordResult

// assembled — Ш8: по отбору у леммы лексемы в порядке
// отбора, не больше трёх, толкование и грамматика из первой
// пары, примеры со всех пар, сопоставление, замечания по
// слову; лемма с пустым отбором выпадает (Д31); слова
// упавших пачек — «ошибка». Новая.
func assembled(
    found []domain.WordResult,
    picks domain.Selection,
) []domain.WordResult

// response — Ш8: ответ из слов в их порядке плюс замечания
// по запросу. Меняется: плюс замечания.
func response(
    results []domain.WordResult,
) domain.LookupResponse

// rejected — отказ: ответа нет, ошибка наверх. Есть.
func rejected(err error) (domain.LookupResponse, error)
```

### Quota

Сервис «сколько можно», бывший «Доступ». Его зовёт только сценарий:
проверка лимитов, списание и откат токенов запроса, токены модели в
счётчик нейронки.

```go
// Check — Ш2 после ключа и размера, порядок LFL-27: лимит
// субъекта, потолок сервера, лимит нейронки, изучаемый
// язык. Первая осечка — её ошибка. Меняется: плюс лимит
// нейронки.
func (q *quota) Check(
    ctx context.Context,
    req domain.LookupRequest,
) error {
    today := q.clock.Now()
    // списываются все присланные токены, LIM-29.6
    tokens := len(req.Tokens)

    spent, err := q.usage.Spent(ctx, req.Subject, today)
    if err != nil {
        return internal(err)
    }
    limit := req.Subject.Limits.SubjectDaily
    if wouldExceed(spent, tokens, limit) {
        return domain.ErrRateLimited
    }

    spentTotal, err := q.usage.SpentTotal(ctx, today)
    if err != nil {
        return internal(err)
    }
    ceiling := q.options.ServerDaily
    if wouldExceed(spentTotal, tokens, ceiling) {
        return domain.ErrRateLimited
    }

    // новое: лимит нейронки, LIM-96; 0 — лимита нет, Д16
    modelSpent, err := q.usage.ModelSpent(
        ctx, req.Subject,
        today,
    )
    if err != nil {
        return internal(err)
    }
    if exceeded(modelSpent, req.Subject.Limits.ModelDaily) {
        return domain.ErrRateLimited
    }

    languages := q.options.SupportedLanguages
    if unsupported(req.LearningLanguage, languages) {
        return domain.ErrUnsupportedLanguage
    }
    return nil
}

// Charge — Ш2.2: списать токены запроса с суточного лимита
// субъекта. Есть, не меняется.
func (q *quota) Charge(
    ctx context.Context,
    subject domain.Subject,
    tokens int,
) error {
    return q.usage.Charge(
        ctx, subject,
        q.clock.Now(), tokens,
    )
}

// Refund — В2: вернуть списанное при 503, с лимитов ничего
// не ушло (Д43). Ответ клиенту уже решён, поэтому сбой
// хранилища — в лог, наверх не идёт; счётчик поправят по
// логу. Новая.
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

// ChargeModel — Ш6.3: токены модели всех вызовов запроса в
// счётчик нейронки субъекта (LIM-79), одной суммой. Ответ
// уже получен и оплачен, поэтому сбой хранилища — в лог,
// наверх не идёт. Новая.
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

Что зовёт Quota. Порт «Расход», `UsageRepository`:

```go
// Spent — токены запросов субъекта за день. Есть.
Spent(
    ctx context.Context,
    subject domain.Subject,
    day time.Time,
) (int, error)

// SpentTotal — токены запросов всех субъектов за день.
// Есть.
SpentTotal(ctx context.Context, day time.Time) (int, error)

// ModelSpent — токены модели субъекта за день (LIM-79).
// Новый.
ModelSpent(
    ctx context.Context,
    subject domain.Subject,
    day time.Time,
) (int, error)

// Charge — прибавить токены запроса к расходу дня. Есть.
Charge(
    ctx context.Context,
    subject domain.Subject,
    day time.Time,
    tokens int,
) error

// Refund — вычесть токены запроса из расхода дня (Д43).
// Новый.
Refund(
    ctx context.Context,
    subject domain.Subject,
    day time.Time,
    tokens int,
) error

// ChargeModel — прибавить токены модели к расходу дня.
// Новый.
ChargeModel(
    ctx context.Context,
    subject domain.Subject,
    day time.Time,
    usage domain.ModelUsage,
) error
```

Порт «Часы», `Clock`, и помощники пакета `quota`:

```go
// Now — текущее время; в тестах подменяется. Есть.
Now() time.Time

// wouldExceed — потрачено плюс запрос больше предела; 0 —
// предела нет. Есть.
func wouldExceed(spent, tokens, limit int) bool

// exceeded — потрачено не меньше предела; 0 — предела нет.
// Новая.
func exceeded(spent, limit int) bool

// unsupported — изучаемого языка нет среди языков базы
// слов. Есть.
func unsupported(
    lang domain.Language,
    supported []domain.Language,
) bool

// internal — ошибка хранилища как ErrInternal → 500. Есть.
func internal(err error) error
```

### Нормализация

Сервис «токены → слова». Чистая логика без хранилищ, сервис ради
порогов из конфига и подмены в тестах.

```go
// Words — Ш3: токен → слово. Пороги режут токены с низкой
// уверенностью и чужого языка, по умолчанию выключены
// (LFL-39); дальше буквы, края, регистр, длина, повторы.
// Есть, не меняется.
func (n *normalizer) Words(
    tokens []domain.Token,
    thresholds domain.Thresholds,
) []domain.Word {
    kept := dropBelowConfidence(
        tokens, thresholds.MinConfidence,
    )
    kept = dropForeignLanguage(kept, thresholds.Language)

    words := toWords(kept)
    words = foldUnicode(words)
    words = trimEdgesToLettersAndDigits(words)
    words = dropWithoutLetters(words)
    words = dropShorterThan(words, minLetters)
    words = lowercase(words)
    words = markLongerThan(words, maxLength)
    return keepFirstOfDuplicates(words)
}

// Text — Ш3 для нейронки: токены как пришли, с повторами,
// через пробел, до чистки (LFL-80, Д22). Новая.
func (n *normalizer) Text(tokens []domain.Token) string {
    parts := make([]string, 0, len(tokens))
    for _, token := range tokens {
        parts = append(parts, token.Text)
    }
    return strings.Join(parts, " ")
}
```

Что зовёт Нормализация, все есть:

```go
// dropBelowConfidence — убрать токены с уверенностью ниже
// порога; nil — порог выключен.
func dropBelowConfidence(
    tokens []domain.Token,
    minConfidence *float64,
) []domain.Token

// dropForeignLanguage — убрать токены другого языка; nil —
// выключено.
func dropForeignLanguage(
    tokens []domain.Token,
    lang *domain.Language,
) []domain.Token

// toWords — токен в слово, текст как есть.
func toWords(tokens []domain.Token) []domain.Word

// foldUnicode — одна форма записи у составных букв.
func foldUnicode(words []domain.Word) []domain.Word

// trimEdgesToLettersAndDigits — с краёв всё, что не буква и
// не цифра.
func trimEdgesToLettersAndDigits(
    words []domain.Word,
) []domain.Word

// dropWithoutLetters — убрать слова без единой буквы.
func dropWithoutLetters(words []domain.Word) []domain.Word

// dropShorterThan — убрать слова короче минимума букв.
func dropShorterThan(
    words []domain.Word,
    min int,
) []domain.Word

// lowercase — нижний регистр.
func lowercase(words []domain.Word) []domain.Word

// markLongerThan — слово длиннее предела помечается, в
// источник не идёт (LFL-41).
func markLongerThan(
    words []domain.Word,
    max int,
) []domain.Word

// keepFirstOfDuplicates — повторы схлопнуть, порядок
// первого появления.
func keepFirstOfDuplicates(
    words []domain.Word,
) []domain.Word
```

### Отбор

Сервис «Отбор» со схемы слоёв: единственный, кто знает про кеш, пачки
и клиент нейронки. Расход токенов модели отдаёт сценарию в отборе, сам
не списывает и субъекта не знает (Д48).

```go
// Select — Ш6–Ш7 для всех слов: без контекста сначала кеш,
// остальное пачками по N слов, до P вызовов параллельно.
// Был хоть один вызов и ни один не ответил —
// ErrLLMUnavailable; упавшая пачка — её слова «ошибка». В
// отборе расход токенов всех вызовов, списывает сценарий
// (Ш6.3).
func (s *selector) Select(
    ctx context.Context,
    words []domain.WordResult,
    text string,
    tree domain.ComponentTree,
) (domain.Selection, error) {
    // Ш6.1
    cached, pending := s.splitByCache(words, text, tree)
    batches := batchesOf(pending, s.options.BatchSize)
    outcomes := inParallel(
        ctx, s.options.Parallelism,
        batches,
        func(batch []domain.WordResult) batchOutcome {
            return s.selectBatch(ctx, batch, text, tree)
        },
    )
    if len(pending) > 0 && noneAnswered(outcomes) {
        return domain.Selection{}, domain.ErrLLMUnavailable
    }
    return merged(cached, outcomes), nil
}
```

Что зовёт отбор. Пакет `selection`, всё новое:

```go
// splitByCache — Ш6.1: у запроса без контекста слова с
// готовым отбором по ключу «слово, языки, дерево, модель»
// берутся из кеша; остальные в работу. С контекстом кеш не
// читается (LFL-94).
func (s *selector) splitByCache(
    words []domain.WordResult,
    text string,
    tree domain.ComponentTree,
) (cached domain.Selection, pending []domain.WordResult)

// batchesOf — пачки по N слов в порядке слов; N из конфига,
// старт 1 (Д33).
func batchesOf(
    words []domain.WordResult,
    size int,
) [][]domain.WordResult

// inParallel — не больше P пачек одновременно, исходы в
// порядке пачек; что не успело в бюджет — исход «не
// ответила» (Д34).
func inParallel(
    ctx context.Context,
    parallelism int,
    batches [][]domain.WordResult,
    one func([]domain.WordResult) batchOutcome,
) []batchOutcome

// selectBatch — один вызов на пачку: промпт из значений,
// контекста и дерева, вызов модели, лог вызова целиком
// (LFL-101); исход — ответ пачки с расходом токенов или «не
// ответила».
func (s *selector) selectBatch(
    ctx context.Context,
    batch []domain.WordResult,
    text string,
    tree domain.ComponentTree,
) batchOutcome

// noneAnswered — ни одна пачка не дала ответа: модель
// недоступна, не успела или всё негодно (LFL-99).
func noneAnswered(outcomes []batchOutcome) bool

// merged — отбор из кеша и из ответов пачек одним списком
// по словам; слова упавших пачек помечены для статуса
// «ошибка»; отбор запросов без контекста пишется в кеш;
// расход токенов всех пачек — одной суммой.
func merged(
    cached domain.Selection,
    outcomes []batchOutcome,
) domain.Selection
```

Порт, который зовёт `selectBatch`. «Клиент нейронки», `LLMClient`, новый:

```go
// Complete — Ш6.2, один вызов модели из конфига: промпт →
// ответ и расход токенов; ошибка сети, провайдера или
// пустой ответ — error.
Complete(
    ctx context.Context,
    prompt domain.Prompt,
) (domain.Answer, domain.ModelUsage, error)
```

«Кеш отбора», `SelectionCache`, новый, его зовут `splitByCache` и `merged`:

```go
// Get — готовый отбор леммы по ключу, если был.
Get(
    ctx context.Context,
    key domain.SelectionKey,
) (domain.LemmaSelection, bool, error)

// Put — отбор леммы в память до рестарта; сбой кеша ответ
// не ломает.
Put(
    ctx context.Context,
    key domain.SelectionKey,
    selection domain.LemmaSelection,
) error
```

### Проверка ответа нейронки

Сервис «ответ нейронки против записи и дерева». Нейронка только
выбирает из готового (Д10), всё, чего в записи или дереве нет, —
её выдумка и выбрасывается. Пакет `service/verify`, новый.

```go
// Verify — Ш6–7 по каждой лемме отбора: ключи значений
// только из лексем леммы, пары только из её же лексем
// другого Викисловаря, сопоставление только на компоненты
// дерева и их опции и поля записи, строка в включённое ядро
// обязательна — без неё лексема вон (Д49), у «Выбора» без
// признака «несколько» одна опция (Д32), замечания только
// про компоненты дерева. Лемма, у которой после чистки не
// осталось годного, хотя нейронка отвечала, — «ошибка»
// (LFL-85, LFL-97). Новая.
func (v *verifier) Verify(
    picks domain.Selection,
    found []domain.WordResult,
    tree domain.ComponentTree,
) domain.Selection {
    for i, lemma := range picks.Lemmas {
        lexemes := lexemesOf(found, lemma.Key)
        lemma = withKnownKeys(lemma, lexemes)
        lemma = withKnownPairs(lemma, lexemes)
        lemma = withValidComponentMatches(
            lemma, lexemes, tree,
        )
        lemma = withCoreValue(lemma, tree)
        lemma = withSingleOption(lemma, tree)
        lemma = withKnownMessages(lemma, tree)
        if nothingLeft(lemma) {
            lemma = failed(lemma)
        }
        picks.Lemmas[i] = lemma
    }
    return picks
}
```

Что зовёт Проверка, всё новое:

```go
// lexemesOf — все лексемы-кандидаты леммы из найденного, до
// отбора.
func lexemesOf(
    found []domain.WordResult,
    lemma domain.LemmaKey,
) []domain.CandidateLexeme

// withKnownKeys — выбранные значения с ключами не из лексем
// леммы — вон, порядок остальных как у нейронки.
func withKnownKeys(
    lemma domain.LemmaSelection,
    lexemes []domain.CandidateLexeme,
) domain.LemmaSelection

// withKnownPairs — пары не из лексем леммы или того же
// Викисловаря, что выбранное, — вон (Д24).
func withKnownPairs(
    lemma domain.LemmaSelection,
    lexemes []domain.CandidateLexeme,
) domain.LemmaSelection

// withValidComponentMatches — строка сопоставления живёт,
// если компонент есть в дереве, опция принадлежит этому
// компоненту, а поле есть в записи выбранного значения;
// остальное — вон (LFL-85, LFL-87).
func withValidComponentMatches(
    lemma domain.LemmaSelection,
    lexemes []domain.CandidateLexeme,
    tree domain.ComponentTree,
) domain.LemmaSelection

// withCoreValue — Д49: значение без строки сопоставления в
// включённое ядро с непустым текстом — вон целиком, как с
// чужим ключом: ядро обязательно, промпт его требует. Ядро
// в дереве есть всегда.
func withCoreValue(
    lemma domain.LemmaSelection,
    tree domain.ComponentTree,
) domain.LemmaSelection

// withSingleOption — у «Выбора» без признака «несколько»
// остаётся первая опция (Д32).
func withSingleOption(
    lemma domain.LemmaSelection,
    tree domain.ComponentTree,
) domain.LemmaSelection

// withKnownMessages — сообщение «компонент непонятен» про
// компонент не из дерева — вон (LFL-88).
func withKnownMessages(
    lemma domain.LemmaSelection,
    tree domain.ComponentTree,
) domain.LemmaSelection

// nothingLeft — нейронка называла значения, а после чистки
// их нет: ответ негодный (LFL-97). Пустой отбор с самого
// начала — не это, такая лемма в ответ не идёт (Д31).
func nothingLeft(lemma domain.LemmaSelection) bool

// failed — лемма со статусом «ошибка» для сборки ответа.
func failed(
    lemma domain.LemmaSelection,
) domain.LemmaSelection
```

## 3. Приложение: схема и код

### Модули и экран

Что меняется в составе приложения по Д51, до кода. Один новый модуль,
три меняются, остальные как в разделах «Шлюз» и ниже.

- **`screen/scancandidates` — новый, экран лексем-кандидатов со снимка.**
  Открывается с `Uri` снимка, живёт один снимок. Внутри:
  `ScanCandidatesScreen` — composable экрана; `ScanCandidatesViewModel` и
  сборка `ScanCandidatesAssembly` — цикл Mate, как у карточки слова;
  `mate/` — `ScanCandidatesReducer`, `ScanCandidatesState`, сообщения,
  эффекты с хендлерами данных и навигации, атомы `ScanCandidatesAtoms`;
  `deps/ScanCandidatesUseCase` — порт экрана: распознать снимок, запросить
  слова, прочитать форму словаря, записать пакет, отпустить доступ к
  файлу. После гибели процесса экран не восстанавливается (AWP-27):
  маршрут из сохранённого стека навигации выкидывается при старте,
  приложение открывается на вкладке «Слова». Маршрут
  `scanCandidates?uri=…` рядом с карточкой
  слова в графе вкладки «Словарь».
- **`screen/wordstab` — меняется, остаётся вход.** Кнопка-камера,
  диалог `ScanDialog` (бывший `PhotoWordsDialog`, без фаз и ошибок
  запроса — только «Камера», «Галерея», «нет камеры»), `PhotoPicker` с
  камерой и галереей — теперь по эффектам, не по нажатию кнопки,
  закрепление доступа к файлу галереи (Д52), переход на экран, снекбар
  по результату. Уходят: `logic/photo` целиком — атомы окна,
  `PhotoBatch`, `PrepareTokens` переезжают в экран; ветки окна,
  распознавания и запроса в `WordsTabReducer`; эффекты
  `RecognizeWords`, `LookupWords`, `LoadPhotoForm`, `AddScanCandidates`,
  `SavePhotoMapping`; стейт окна из `WordsTabState` — остаётся
  состояние диалога выбора: открыт, ошибка камеры.
- **`widget/candidates` — меняется, общее для экрана и диалога
  карточки.** Типы: карточки, лексемы, значения по компонентам,
  адреса, листы, форма словаря, исходы. Чистые функции: карточки по
  сопоставлению, пометки «есть» и «ядро», сводка кнопки, замечания,
  строки сопоставления, пакет записи. Composable: карточки, листы,
  вопросы «да/нет». UI-двойники ответа сервера `LookupWordUi` и
  компания с `toUi()` в `mapper/`. `PhotoAddWindow` перестаёт быть `Dialog` —
  содержимое экрана; `CandidatesDialog` карточки — как есть.
- **`screen/wordcard` — меняется по IS536 как было.** Диалог
  лексем-кандидатов: значения по компонентам, «Замечания»,
  «Сопоставление»; структура и свой редьюсер не трогаются.
- **`domain/lexeme` — меняется.** Новые типы ответа сервера: исход
  `LookupOutcome` со словом, леммой, лексемой-кандидатом,
  сопоставлением, замечанием, рекомендацией и причиной отказа; исход
  формы `FormOutcome`. Ими говорят порты обоих окон.
- **`core-network-api`, `core-network-impl` — меняются.** Транспорт
  Retrofit `*Net`, зеркала `*ApiEntity` по контракту 0.3.0-draft,
  исход вызова `LookupResult`; `mapper/LookupMappers.kt` с
  `toApiEntity()` и `toNet()`.
- **`app` — меняется.** `ScanCandidatesUseCaseImpl`: ML Kit через
  `library/textrecognition`, запрос через `core-network`, форма из
  Room, запись через `WordsBatchApi`; `WordsTabUseCaseImpl` худеет на
  то же. `app/mapper/LookupMappers.kt`: зеркала → домен, `toDomain()`. Маршрут экрана в `Vocabulary.kt`, навигация в `AppNavTable`:
  открыть экран с аргументами, вернуться на вкладку с результатом.
  Прецедента результата навигации в коде нет — механизм новый, он же
  закроет хвост IS533 «снекбар после переноса».

Экран `ScanCandidatesScreen`, что видно. Сначала индикатор с подписью
фазы: «Распознаю текст», «Ищу слова», «Собираю окно». Потом окно:
карточки лемм, значения по компонентам, «Замечания», «Сопоставление»,
кнопка «Добавить». Поверх — листы снизу (группа, «куда класть»,
опция, правка текста) и два вопроса: «Добавить N значений в M слов?»,
«закрыть?». Ошибки на месте индикатора с «Закрыть»: снимок не
прочитался, слов нет, отказ по ключу или лимиту, форма не собралась;
сеть и «сервер недоступен» — ещё с «Повторить» (Д53). «Назад» —
закрыть, с вопросом при правках.

Редьюсер `ScanCandidatesReducer`, действия коротко.

- **От эффектов.** `TextRecognized` — токены; `TextEmpty`;
  `RecognitionFailed`; `WordsFound` — слова и замечания; `WordsEmpty`;
  `LookupDenied`; `LookupUnavailable`; `FormLoaded`; `FormFailed`;
  `AddFinished` — счёт; `AddGroupGone`, `AddWordGone`,
  `AddComponentGone`, `AddFailed`.
- **От экрана.** `RetryLookup`; карточки: `ToggleCard`, `DeleteCard`,
  `DeleteLexeme`, `DeleteValue`; правка: `OpenSheet`, `CloseSheet`,
  `UpdateDraft`, `SubmitText`, `ResetText`, `SetOption`; куда класть:
  `SetCardTarget`, `SetCardGroup`, `SetCommonGroup`; показ:
  `ToggleComponentMatches`, `ToggleNotAdded`; запись: `RequestAdd`,
  `ConfirmAdd`, `CancelAdd`, `RetryAdd`; закрытие: `RequestClose`,
  `ConfirmClose`, `Stay`.
- **Чего нет.** Номера запуска и его проверки; `SetPhotoMapping`,
  `TogglePhotoMapping`.

Вкладка `WordsTabReducer`, остаток про снимок: `OpenScanDialog`;
`ChooseCamera`, `ChooseGallery` — эффект запуска системного
приложения; `ImagePicked` — закрыть диалог, открыть экран;
`CameraUnavailable`;
`CloseScanDialog`; `ScanCandidatesAdded` — снекбар.

### Приложение: путь фичи

Откуда фича запускается, через какие окна идёт и куда приходит. Два
входа, оба уже есть: кнопка-камера на вкладке «Слова» и кнопка
«Добавить лексемы-кандидаты» в карточке слова, обе только у сборки с
ключом сервера. У снимка путь новый (Д51): вкладка открывает только
диалог скана и системную камеру, после снимка — отдельный экран со
своим редьюсером, от распознавания до записи, и назад на вкладку с
результатом. В коробке — окно или шаг и его компоненты, на стрелке —
шаг спеки TFP, AWP или WCL и что передаётся. Серый блок — системное
приложение, не наше. Общие для обоих входов блоки — бэкенд, форма
словаря из Room, запись пакетом. Источник — `hld-app-path.dot`.

![Приложение: путь фичи](/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS536_llm_values/hld-app-path.b3130865.png)

Что меняется на пути, цветом: жёлтым — новый экран, он забирает у
вкладки распознавание, запрос, форму, окно и запись; синим — запрос
несёт дерево компонентов, ответ — сопоставление, замечания и
рекомендации, форма — опции всех «Выборов» и зависимости, окно и
диалог показывают значения по компонентам с разделами «Замечания» и
«Сопоставление», диалог скана запускает камеру эффектом, вкладка
показывает снекбар по результату навигации. Кнопка, системная камера
и галерея — как есть. Токены перестают схлопываться перед отправкой
(Д22).

### Приложение: слои по C4

Та же фича по слоям кода: путь выше показывает, что за чем идёт, эта
схема — кто кого зовёт. В коробке имя, тип в скобках, одна фраза
ответственности, как у бэкенда. Сверху вниз: экран, логика, порты, их
реализация и навигация, шлюзы, бэкенд снаружи. Внизу справа сноска:
что видит пользователь по спекам AWP и WCL. Источник —
`hld-app-layers-c4.dot`.

Номер на стрелке — шаг экранной спеки AWP: Ш1 подготовка окна, Ш2
окно, Ш3 действие, Ш4 добавление, Ш5 после. У диалога карточки слова
те же шаги WCL. Запрос на сервер — шаг TFP Ш6 у снимка и WCL Ш1 у
карточки. Три редьюсера: вкладка «Слова» держит диалог скана и
переход на экран, экран лексем-кандидатов и диалог карточки — два
окна с общими виджетами и чистыми функциями модуля `widget/candidates`,
у каждого свой стейт. Слои типов по Д57: экран и виджеты — UI-типы,
порты говорят доменом, `app/mapper` переводит зеркала в домен, шлюзы —
транспорт в зеркала. Код ниже — по окнам, диалог карточки отличиями.

![Приложение: слои по C4](/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS536_llm_values/hld-app-layers-c4.542f2f4b.png)

Что меняет IS536 в приложении, по слоям:

- **Экран** — лексема-кандидат показана значениями компонентов по
  сопоставлении (AWP-29), «англ.» по сообщению сервера (AWP-34). Новые
  разделы «Замечания» (AWP-30) и «Сопоставление» (AWP-31). Выбора
  соответствий и «ещё N» нет. Ядро у каждой лексемы заполнено: сервер
  без него лексему не отдаёт (Д49).
- **Логика** — у снимка свой редьюсер в своём экране, без номера
  запуска (Д51). Карточки собираются по сопоставлению, не по полям
  ответа. Соответствия, их умолчания и память сняты. У значения признак
  зависимости: «Род» виден только при опции «существительное»
  (AWP-17.6). По исходу юзкейса хендлер ветвится `when` и шлёт
  сообщение на случай, редьюсер исходы не разбирает: нынешние
  `WordsLookedUp`, `PhotoFormLoaded`, `PhotoAddRejected` с исходом
  внутри заменены.
- **Порты и реализация** — порты говорят доменом, исходы в домене
  (Д57). В запрос уходит дерево компонентов словаря (Д28), исход
  несёт замечания и рекомендации. Форма даёт опции всех «Выборов» и
  зависимости компонентов. Навигация на экран и назад с результатом —
  `AppNavTable`.
- **Шлюз** — Retrofit `*Net` по контракту 0.3.0-draft, зеркала
  `*ApiEntity`, без `candidates` (Д46). Отказ 503 `llm_unavailable` —
  причина со своим текстом (WCL-11.6).

Код по окнам, в порядке чтения: стейт дельтой, редьюсер с его атомами
под ветками, чистые функции карточек. После окон — юзкейсы и шлюз
сигнатурами. Реализованы ветки редьюсера, которые трогает фича.

### Вкладка «Слова»

Вход фичи: кнопка-камера и диалог скана, `screen/wordstab`.
Меняется только это, окно уходит в экран.

#### Стейт

Только дельта к нынешнему коду: новые поля, изменённые (было →
стало) и снятые. Поля без изменений не показаны, на их месте `…`.
Строка с `-` — снято или «было», с `+` — новое или «стало».

Вкладка «Слова»: окно уходит в экран, остаётся диалог выбора
источника.

```diff
 data class WordsTabState(
     …
-    val photoWordsState: PhotoWordsState =
-        PhotoWordsState(),
+    val scanDialogState: ScanDialogState =
+        ScanDialogState(),
     …
 )
```

```kotlin
// ScanDialogState — диалог выбора источника снимка:
// камера или галерея. Открыт, и текст ошибки, если камера
// не запустилась. Новое: то, что от PhotoWordsState
// осталось у вкладки
data class ScanDialogState(
    val isOpen: Boolean = false,
    @StringRes val errorRes: Int? = null,
)
```

#### Редьюсер

Что остаётся у вкладки «Слова» в `WordsTabReducer`: вход и переход.
Ветки окна, распознавания и запроса уходят вместе с `logic/photo`.

```kotlin
// OpenScanDialog — UI: кнопка-камера над «+». Без данных.
// Меняется: было OpenPhotoSourceChooser
Msg.OpenScanDialog -> state.showScanDialog()
//   showScanDialog: диалог открыт, без ошибки

// ChooseCamera — UI: «Камера» в диалоге. Без данных.
// Новое: раньше кнопка запускала камеру сама
Msg.ChooseCamera ->
    state.withEffect(UiEffect.LaunchCamera)
//   LaunchCamera: UI-хост открывает системную камеру,
//     снимок — в файл в кеше приложения

// ChooseGallery — UI: «Галерея» в диалоге. Без данных.
// Новое: раньше кнопка запускала галерею сама
Msg.ChooseGallery ->
    state.withEffect(UiEffect.LaunchGallery)
//   LaunchGallery: UI-хост открывает выбор картинки

// ImagePicked — PhotoPicker: камера или галерея вернули
// снимок:
//   uri — файл снимка строкой; у галереи доступ к нему
//     уже закрепил PhotoPicker (Д52)
// Меняется: было PhotoPicked(uri) с распознаванием
is Msg.ImagePicked -> state
    .hideScanDialog()
    .withEffect(
        WordsNavigationEffect.OpenScanCandidates(
            message.uri,
        ),
    )
//   hideScanDialog: диалог закрыт
//   OpenScanCandidates: экран открыт с uri

// отмена в системном приложении ничего не шлёт: диалог
// как был, снова «Камера» или «Галерея»

// CameraUnavailable — хендлер эффекта LaunchCamera: запуск
// вернул отказ — приложения камеры нет, её отключили или
// файл под снимок не создался:
//   errorRes — текст причины
// Меняется: было RecognitionFailed с номером запуска
is Msg.CameraUnavailable ->
    state.showScanDialogError(message.errorRes)
//   showScanDialogError: в диалоге текст и «Закрыть»

// CloseScanDialog — UI: «Отмена» в диалоге. Без данных.
// Меняется: было ClosePhotoWords на любом шаге
Msg.CloseScanDialog -> state.hideScanDialog()
//   hideScanDialog: диалог закрыт

// ScanCandidatesAdded — навигация: экран скана закрылся
// после записи, вкладка получила результат из back stack:
//   lexemeCount — сколько значений
//   wordCount — в сколько слов
// Новое: было веткой PhotoAddFinished с проверкой запуска
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

### Экран лексем-кандидатов

Новый экран, `screen/scancandidates`: от снимка до записи в словарь.

#### Стейт

Экран: `ScanCandidatesState` — это `PhotoWordsState` вкладки,
переехавший в экран. Дельта к нему.

```diff
-data class PhotoWordsState(
+data class ScanCandidatesState(
+    // новое: файл снимка из аргумента маршрута
+    val uri: String,
-    // снято: экран открыт, пока существует
-    val isOpen: Boolean = false,
-    // снято: выбор источника остался у вкладки
-    val isChoosingSource: Boolean = false,
     …
+    // новое: токены снимка, чтобы повторить запрос (Д53)
+    val tokens: List<RecognizedWord> = emptyList(),
+    // новое: рекомендации компонентов ко всему ответу
+    val recommendations: List<ComponentRecommendationUi> =
+        emptyList(),
+    // новое: при ошибке есть «Повторить» (Д53)
+    val hasRetryButton: Boolean = false,
-    // снято: экран живёт один снимок (Д51)
-    val requestId: Long = 0L,
     …
-    // снято: соответствий нет (AWP-09)
-    val mapping: PhotoMapping = PhotoMapping.NONE,
-    // снято: ошибка формы — в errorRes, как остальные
-    val formError: PhotoFormError? = null,
     …
+    // новое: строки раздела «Замечания» (AWP-30)
+    val remarks: List<RemarkUiItem> = emptyList(),
+    // новое: строки раздела «Сопоставление» (AWP-31)
+    val componentMatchLines:
+        List<ComponentMatchLineUiItem> = emptyList(),
     …
-    // было: раскрыт раздел соответствий
-    val isMappingExpanded: Boolean = false,
+    // стало: раскрыт раздел «Сопоставление»
+    val isComponentMatchesExpanded: Boolean = false,
     …
 )
```

Поля `words`, `form`, `cards`, `summary`, `sheet` объявлены как
были, меняются их типы — ниже.

UI-типы полей `cards`, `sheet`, `summary`. Лежат в `widget/candidates`:
диалог карточки слова держит в стейте те же. Элемент списка, что
рисуется рядами, — `*UiItem`, остальное — `*Ui`:

```diff
 // снято: «ещё N» нет, AWP-05 отменён
 data class LemmaCardUiItem(
     …
-    val hiddenCount: Int,
     …
 )

 // стало: вместо полей ответа — значения по компонентам
 // словаря в порядке компонентов (AWP-29); «англ.» теперь
 // по замечанию сервера, не по языку (AWP-34); parent и
 // parentKey сняты (Д37)
 data class CardLexemeUiItem(
     …
-    val translation: EditableTextUiItem,
-    val hasOwnTranslations: Boolean,
-    val gloss: EditableTextUiItem?,
-    val caption: String?,
-    val notes: List<String>,
-    val examples: List<EditableTextUiItem>,
-    val partOfSpeechOptionId: Long?,
-    val parent: Boolean,
-    val parentKey: String?,
+    val values: List<CandidateValueUiItem>,
     …
 )
```

```kotlin
// CandidateValueUiItem — значение одного компонента у
// лексемы. Новое. isActive — зависимость компонента
// выполнена: от лексемы — всегда, от компонента — у того
// есть активное значение, от опции — она выбрана
// (AWP-17.6). Неактивное не показывается и не пишется.
sealed interface CandidateValueUiItem {
    val componentId: Long
    val isActive: Boolean

    // текстовый компонент: один текст, у «несколько» —
    // список; пустым текст ядра бывает только после
    // стирания руками (AWP-11.5); field — поле ответа,
    // откуда взят текст, null у введённого рукой
    data class Text(
        override val componentId: Long,
        val texts: List<EditableTextUiItem>,
        val field: String?,
        override val isActive: Boolean = true,
    ) : CandidateValueUiItem

    // «Выбор»: опции компонента, у «несколько» — все
    // подходящие (Д32); пусто — «без значения»
    data class Choice(
        override val componentId: Long,
        val options: List<Long>,
        override val isActive: Boolean = true,
    ) : CandidateValueUiItem
}

// ValueRefUi — адрес значения: лемма, ключ лексемы,
// компонент, номер в списке значений компонента. Новое.
data class ValueRefUi(
    val lemma: String,
    val key: String,
    val componentId: Long,
    val index: Int,
)

// ComponentRefUi — адрес компонента у лексемы-кандидата.
// Новое.
data class ComponentRefUi(
    val lemma: String,
    val key: String,
    val componentId: Long,
)
```

```diff
 // стало: лист опции любого «Выбора» вместо «Части речи»,
 // лист правки адресует значение; листа соответствий нет
 sealed interface CandidatesSheetUi {
     …
-    data class PartOfSpeech(
-        val lemma: String, val key: String,
-    ) : CandidatesSheetUi
+    data class Option(
+        val ref: ComponentRefUi,
+    ) : CandidatesSheetUi
-    data class MappingTarget(
-        val kind: PhotoDataKind,
-    ) : CandidatesSheetUi
-    data class TextEdit(
-        val lemma: String,
-        val key: String,
-        val field: PhotoTextField,
-        val draft: String,
-    ) : CandidatesSheetUi
+    // canReset — черновик отличается от текста ответа;
+    // ставят атомы openSheet и updateSheetDraft, UI рисует
+    // «сбросить» по флагу и сам не сравнивает (AWP-16.6)
+    data class TextEdit(
+        val ref: ValueRefUi,
+        val draft: String,
+        val canReset: Boolean,
+    ) : CandidatesSheetUi
 }

 // снято: лист правки адресует значение, поле не нужно
-sealed interface PhotoTextField { … }

 // снято: ядро у каждой лексемы есть (Д49), сводка без него
 data class AddSummaryUi(
     …
-    val noCoreMapping: Boolean = false,
     …
 )
```

Разделы «Замечания» и «Сопоставление». Текст строки собирает UI
из ресурсов по виду строки: падежи и число — там.

```kotlin
// RemarkUiItem — строка раздела «Замечания» (AWP-30).
// Новое.
sealed interface RemarkUiItem {
    // «Назначение компонента «<имя>» непонятно, он оставлен
    // пустым»
    data class ComponentUnclear(
        val name: String,
    ) : RemarkUiItem

    // «У N значений нет перевода на <язык>, показано
    // английское толкование»; N — разные лексемы с
    // замечанием
    data class NoTranslation(
        val count: Int,
        val translationLanguage: String,
    ) : RemarkUiItem

    // «У лексем-кандидатов есть «<имя>». Заведите в
    // конструкторе компонент «<имя>» (<шаблон>, опции: …),
    // и значения лягут в него»
    data class ComponentRecommended(
        val name: String,
        val template: ComponentTemplate,
        val options: List<String>,
    ) : RemarkUiItem
}

// ComponentMatchLineUiItem — строка раздела «Сопоставление»
// (AWP-31, Д41). Новое.
sealed interface ComponentMatchLineUiItem {
    // «<поле ответа> → <компонент>»
    data class FieldToComponent(
        val field: String,
        val component: String,
    ) : ComponentMatchLineUiItem

    // «<компонент>: <подпись опции>»
    data class ComponentOption(
        val component: String,
        val option: String,
    ) : ComponentMatchLineUiItem
}
```

Ответ сервера. Домен в `domain/lexeme`: исход и данные, ими говорят
порты обоих окон. Новое: были `PhotoLookupOutcome`, `PhotoWord` и
компания в виджете.

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

// LookupWord — слово ответа (LFL-50): текст, статус, леммы,
// замечания по слову
data class LookupWord(
    val text: String,
    val status: LookupWordStatus,
    val lemmas: List<LookupLemma>,
    val remarks: List<WordRemark>,
)

// LookupLemma — лемма ответа (LFL-76): текст, пометка
// формы, транскрипции из записи, лексемы-кандидаты
data class LookupLemma(
    val text: String,
    val formNote: String?,
    val pronunciations: List<String>,
    val lexemes: List<LookupLexeme>,
)

// LookupLexeme — лексема-кандидат (LFL-71): данные записи
// целиком (Д23) и сопоставление; без parent и parentKey
// (Д37)
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

// ComponentMatch — строка сопоставления: компонент и опция
// «Выбора» или поле ответа (контракт, 5.1)
data class ComponentMatch(
    val componentId: Long,
    val optionId: Long?,
    val field: String?,
)

// WordRemark — замечание по слову (контракт, 5.6)
sealed interface WordRemark {
    data class ComponentUnclear(
        val componentId: Long,
    ) : WordRemark

    data class NoTranslation(
        val lemma: String,
        val lexemeKey: String,
    ) : WordRemark
}

// ComponentRecommendation — рекомендация компонента ко
// всему ответу (контракт, 5.6)
data class ComponentRecommendation(
    val name: String,
    val template: ComponentTemplate,
    val options: List<String>,
)

// LookupDenyReason — причина отказа. Меняется: плюс
// LLM_UNAVAILABLE — 503, нейронка не ответила ни разу →
// «Сервер сейчас недоступен, попробуй позже» (WCL-11.6)
enum class LookupDenyReason {
    NO_ACCESS_KEY, UNAUTHORIZED, RATE_LIMITED,
    REQUEST_TOO_LARGE, UNSUPPORTED_LANGUAGE,
    LLM_UNAVAILABLE,
}

// FormOutcome — исход чтения формы словаря: Loaded с
// формой, DictionaryMissing, Failed. Новое: домен, был
// PhotoFormOutcome; без запомненных соответствий (AWP-06)
```

UI-двойники в `widget/candidates`, поле в поле с доменом, делает
`toUi()` в хендлере: `LookupWordUi`, `LookupLemmaUi`, `LookupLexemeUi`,
`ComponentMatchUi`, `WordRemarkUi`, `ComponentRecommendationUi`,
`LookupWordStatus`. Меняется: были `PhotoWord`, `PhotoLemma`,
`PhotoLexeme` без грамматики, уровня, синонимов, антонимов и
сопоставления, с `parent` и `parentKey`.

Форма словаря, `PhotoAdd.kt`:

```diff
 sealed interface FormOutcome {
     data class Loaded(
         …
-        // снято: соответствий нет (AWP-06)
-        val remembered: PhotoMapping?,
     ) : FormOutcome
     …
 }

 // стало: components — все включённые живые компоненты, не
 // только зависящие от лексемы
 data class DictionaryFormUi(
     …
-    // было: опции одной «Части речи»
-    val partOfSpeechOptions: List<PhotoPartOfSpeechOption>,
+    // стало: опции всех «Выборов» по id компонента
+    // (AWP-17.5)
+    val options: Map<Long, List<FormOptionUi>>,
     …
 )

 data class FormComponentUi(
     …
+    // новое: от чего зависит компонент
+    val dependsOn: DependencyTarget,
 )

 // стало: живая опция любого «Выбора», поля те же
-data class PhotoPartOfSpeechOption(
+data class FormOptionUi(
     …
 )
```

Зависимость компонента `dependsOn` — доменный `DependencyTarget`
конструктора как есть: лексема, компонент или опция. Своего UI-типа у
неё нет, это значение.

Снято целиком — соответствий нет (AWP-06, AWP-09, AWP-28):

```diff
-enum class PhotoDataKind { … }
-sealed interface PhotoTarget { … }
-data class PhotoMapping( … )
```

#### Редьюсер

Экран снимка живёт в своём модуле со своим циклом Mate (Д51):
`ScanCandidatesReducer`, стейт `ScanCandidatesState`, атомы `ScanCandidatesAtoms`.
Цикл создаётся с `Uri` снимка и словарём, первый эффект —
распознавание, через `initEffects`, как `LoadWord` у карточки слова.
Номера запуска нет: экран живёт один снимок, закрыли экран — цикл и
корутины эффектов умерли, ответу некуда прийти.

Юзкейс отдаёт исход типом на каждый случай, хендлер эффекта
превращает каждый случай в своё сообщение, редьюсер исходы не
разбирает и ветвится только по стейту. Условий в ветках два: «текст сменился» перед флагом правок и
`isDirty` при закрытии — инварианты спеки на стейте, с тестом
редьюсера.

Ниже ветки старта и ветки, которые трогает IS536. Ветки карточек,
групп и «куда класть» переезжают из вкладки как есть, без приставки.
Страховок от повторного нажатия нет: кнопки, листы и крестик
неактивны по флагам стейта, второго нажатия UI не даёт. Цепочка —
атомы по порядку, сводка кнопки последней. Под веткой — что делает
каждый её атом: что меняется в стейте и какой эффект уходит.

```kotlin
// TextRecognized — хендлер RecognizeText, исход Text:
//   tokens — токены как распознаны, с повторами, без
//     пустых, потолок 2000 (TFP Ш4–Ш5, prepareTokens)
// Новое: было TokensReady вкладки с номером запуска
is Msg.TextRecognized -> state.startLooking(message.tokens)
//   startLooking: tokens в стейт, фаза «ищу слова»,
//     эффект LookupWords(tokens)

// TextEmpty — хендлер RecognizeText, исход Empty: на снимке
// ни одного слова. Без данных.
// Новое: было пустым списком внутри TokensReady
Msg.TextEmpty -> state.showNoWords()
//   showNoWords: фаза «слов нет», только «Закрыть»

// RecognitionFailed — хендлер RecognizeText, onFail: снимок
// не прочитался:
//   errorRes — текст причины
// Меняется: без номера запуска; «нет камеры» — у вкладки
is Msg.RecognitionFailed ->
    state.showError(message.errorRes)
//   showError: фаза «ошибка», errorRes,
//     hasRetryButton = false

// WordsFound — хендлер LookupWords, исход Words: сервер
// нашёл слова по снимку:
//   dictionaryId — словарь, куда добавляем
//   words — слова ответа с лексемами-кандидатами
//   recommendations — рекомендации компонентов ко
//     всему ответу
// Новое вместо WordsLookedUp
is Msg.WordsFound -> state.startFormLoading(message)
//   startFormLoading: dictionaryId, words,
//     recommendations в стейт, фаза «собираю окно»,
//     эффект LoadForm(dictionaryId, тексты лемм ответа)

// WordsEmpty — хендлер LookupWords, исход NoWords: сервер
// слов не нашёл. Без данных.
// Новое, было веткой WordsLookedUp
Msg.WordsEmpty -> state.showNoWords()
//   showNoWords: фаза «слов нет», только «Закрыть»

// LookupDenied — хендлер LookupWords, исход Denied: отказ
// по ключу или лимиту:
//   errorRes — текст причины, выбрал хендлер
// Новое, было веткой WordsLookedUp; «Повторить» нет (Д53)
is Msg.LookupDenied -> state.showError(message.errorRes)
//   showError: фаза «ошибка», errorRes,
//     hasRetryButton = false

// LookupUnavailable — хендлер LookupWords, исход
// Unavailable или onFail: сети нет или сервер недоступен:
//   errorRes — текст причины
// Новое: с «Повторить» рядом с «Закрыть» (Д53)
is Msg.LookupUnavailable ->
    state.showRetryableError(message.errorRes)
//   showRetryableError: фаза «ошибка», errorRes,
//     hasRetryButton = true

// RetryLookup — UI: «Повторить» на ошибке. Без данных.
// Новое (Д53): запрос заново с токенами из стейта
Msg.RetryLookup -> state.startLooking(state.tokens)
//   startLooking: фаза «ищу слова», эффект LookupWords

// FormLoaded — хендлер LoadForm, исход Loaded: форма
// словаря прочитана:
//   form — компоненты с опциями и зависимостями, группы,
//     леммы, что уже есть в словаре
// Меняется: без запомненных соответствий, три новых атома
is Msg.FormLoaded -> state
    .begin<ScanCandidatesState, Effect>()
    .then { it.applyForm(message.form) }
    .then { it.applyRemarks() }
    .then { it.applyComponentMatchLines() }
    .then { it.refreshActiveValues() }
    .then { it.refreshExistsMarks() }
    .then { it.refreshCoreMarks() }
    .then { it.refreshSummary() }
//   applyForm: form в стейт, карточки из слов по
//     сопоставлению, фаза «окно»
//   applyRemarks: notes — строки «Замечаний» из
//     замечаний слов и ответа (AWP-30)
//   applyComponentMatchLines: componentMatchLines — строки
//     «Сопоставления» по всем лексемам, без повторов, в
//     порядке компонентов (AWP-31, Д41)
//   refreshActiveValues: isActive у значений по
//     зависимостям компонентов (AWP-17.6, 17.7)
//   refreshExistsMarks: exists у лексем — ядро совпало с
//     ядром лексемы словаря, параметры не сравниваются
//     (AWP-07, Д42)
//   refreshCoreMarks: lacksCore у лексем — ни одно
//     включённое ядро не имеет непустого текста (AWP-11)
//   refreshSummary: summary кнопки — лексемы без «есть»,
//     слова, число без ядра, активна ли (AWP-13)

// FormFailed — хендлер LoadForm, исход DictionaryMissing
// или Failed: форма не прочиталась:
//   errorRes — текст: словарь удалён или чтение упало
// Новое, было веткой PhotoFormLoaded; ошибка формы — в
// errorRes, как остальные
is Msg.FormFailed -> state.showError(message.errorRes)
//   showError: фаза «ошибка», errorRes,
//     hasRetryButton = false

// DeleteValue — UI: крестик у значения в карточке
// (AWP-15.2, 15.7):
//   ref — адрес значения: лемма, ключ, компонент, номер
// Меняется: было DeletePhotoExample
is Msg.DeleteValue -> state
    .begin<ScanCandidatesState, Effect>()
    .then { it.deleteValue(message.ref) }
    .then { it.refreshActiveValues() }
    .then { it.markDirty() }
    .then { it.refreshExistsMarks() }
    .then { it.refreshCoreMarks() }
    .then { it.refreshSummary() }
//   deleteValue: значение по адресу убрано; компонент
//     без значений уходит; ушёл последний текст
//     последнего ядра — без ядра нет лексемы, она
//     удалена целиком (AWP-11.5)
//   refreshActiveValues: ушёл последний текст компонента,
//     от которого зависит другой, — зависимый гаснет
//     (AWP-17.6)
//   markDirty: isDirty = true
//   refreshExistsMarks, refreshCoreMarks, refreshSummary:
//     пометки и сводка заново

// SubmitText — UI: «Сохранить» в листе правки (AWP-16):
//   ref — адрес значения: лемма, ключ, компонент, номер
//   text — текст из листа правки
// Меняется: было без данных, редьюсер читал лист из стейта
is Msg.SubmitText -> reduceSubmitText(state, message)
//   reduceSubmitText: ниже — applyTextEdit,
//     dropEmptyTexts, refreshActiveValues, closeSheet,
//     markDirty если текст сменился, пометки и сводка

// ResetText — UI: «Сбросить» в листе правки (AWP-16.6):
//   ref — адрес значения
// Меняется: адрес вместо леммы, ключа и поля
is Msg.ResetText -> reduceResetText(state, message.ref)
//   reduceResetText: ниже — resetText, markDirty если
//     текст отличался, пометки и сводка

// SetOption — UI: опция в листе «Выбора» (AWP-17):
//   ref — адрес компонента: лемма, ключ, компонент
//   option — id опции, null — «без значения»
// Меняется: было SetPhotoPartOfSpeech
is Msg.SetOption -> reduceSetOption(state, message)
//   reduceSetOption: ниже — setOption,
//     refreshActiveValues, closeSheet, markDirty если
//     опция сменилась

// ToggleComponentMatches — UI: заголовок раздела
// «Сопоставление» (AWP-31.4). Без данных.
// Меняется: было TogglePhotoMapping
Msg.ToggleComponentMatches ->
    state.toggleComponentMatchesExpanded()
//   toggleComponentMatchesExpanded:
//     isComponentMatchesExpanded наоборот

// снято: SetPhotoMapping — соответствий нет (AWP-09)

// AddComponentGone — хендлер AddWords, отказ
// TypeUnavailable: компонент выключили (AWP-21.6):
//   failure — причина с id выключенного компонента
// Новое, было веткой PhotoAddRejected; AddGroupGone,
// AddWordGone, AddFailed — так же
is Msg.AddComponentGone -> state
    .begin<ScanCandidatesState, Effect>()
    .then { it.dropComponent(message.failure.typeId) }
    .then { it.refreshActiveValues() }
    .then { it.refreshCoreMarks() }
    .then { it.refreshSummary() }
    .then { it.showAddFailure(message.failure) }
//   dropComponent: компонент убран из формы и из
//     значений всех лексем
//   refreshActiveValues, refreshCoreMarks,
//     refreshSummary: пометки и сводка заново
//   showAddFailure: isAdding = false, addFailure =
//     причина — окно активно с «не добавилось»

// AddFinished — хендлер AddWords, исход Added: записано
// (AWP-23):
//   lexemeCount — сколько значений
//   wordCount — в сколько слов
// Меняется: снекбар показывает вкладка по результату
// навигации, экран закрывается
is Msg.AddFinished -> state.closeWithResult(
    message.lexemeCount, message.wordCount,
)
//   closeWithResult: эффект навигации — назад на
//     вкладку с результатом, стейт не меняется

// RequestAdd — UI: кнопка «Добавить» (AWP-13). Без данных.
// Меняется: без страховок — кнопка неактивна по стейту
Msg.RequestAdd -> state.askAddConfirm()
//   askAddConfirm: isConfirmingAdd = true — вопрос
//     «Добавить N значений в M слов?»

// ConfirmAdd — UI: «Да» в вопросе (AWP-20). Без данных.
// Меняется: без страховок
Msg.ConfirmAdd -> state
    .begin<ScanCandidatesState, Effect>()
    .then { it.closeAddConfirm() }
    .then { it.startAdding() }
//   closeAddConfirm: isConfirmingAdd = false
//   startAdding: isAdding = true — окно неактивно,
//     эффект AddWords(пакет из карточек, wordsBatchOf)

// RetryAdd — UI: «Повторить» в «не добавилось» (AWP-26).
// Без данных. Меняется: без страховок
Msg.RetryAdd -> state
    .begin<ScanCandidatesState, Effect>()
    .then { it.clearAddFailure() }
    .then { it.startAdding() }
//   clearAddFailure: addFailure = null
//   startAdding: isAdding = true, эффект AddWords

// OpenSheet — UI: нажатие на группу, «куда класть»,
// «Выбор» или текст значения:
//   sheet — какой лист: общая группа, группа карточки,
//     «куда класть», опция «Выбора», правка текста
// Меняется: без страховки
is Msg.OpenSheet -> state.openSheet(message.sheet)
//   openSheet: sheet = лист; у правки canReset = false

// UpdateDraft — UI: ввод в листе правки, каждое изменение:
//   text — текст поля
// Меняется: атом ставит canReset, UI сам не сравнивает
is Msg.UpdateDraft -> state.updateSheetDraft(message.text)
//   updateSheetDraft: draft листа = ввод, canReset = ввод
//     отличается от текста ответа (AWP-16.6)

// RequestClose — UI: крестик или системный «назад»
// (AWP-19, AWP-25); во время записи UI не шлёт
// (AWP-19.5). Без данных. Меняется: без страховок;
// выбор по isDirty остаётся
Msg.RequestClose -> when {
    state.isDirty -> state.askCloseConfirm()
    else -> state.close()
}
//   askCloseConfirm: isConfirmingClose = true — вопрос
//     «закрыть?»
//   close: эффект навигации — назад на вкладку без
//     результата

// ConfirmClose — UI: «Закрыть» в вопросе (AWP-25).
// Без данных. Меняется: без страховки
Msg.ConfirmClose -> state.close()
//   close: эффект навигации — назад на вкладку
```

Что зовёт редьюсер экрана, кроме атомов. Ветки правки вынесены в
функции: `when` в `when` не влезает в ширину.

```kotlin
// reduceSubmitText — правка из листа: обрезать, пустой
// параметр убрать, зависимые от опустевшего компонента
// погасить, лист закрыть, флаг правок только если
// текст сменился (AWP-19.9), пометки и сводка. Условие
// по стейту, инвариант спеки, тест редьюсера. Новая.
private fun reduceSubmitText(
    state: ScanCandidatesState,
    message: Msg.SubmitText,
): ScanCandidatesResult {
    val dirty = state.textChanged(message.ref, message.text)
    return state
        .begin<ScanCandidatesState, Effect>()
        .then {
            it.applyTextEdit(message.ref, message.text)
        }
        .then { it.dropEmptyTexts(message.ref) }
        .then { it.refreshActiveValues() }
        .then { it.closeSheet() }
        .then { if (dirty) it.markDirty() else it.begin() }
        .then { it.refreshExistsMarks() }
        .then { it.refreshCoreMarks() }
        .then { it.refreshSummary() }
}

// reduceResetText — то же для «сбросить»: флаг, если текст
// отличался от текста ответа — условие по стейту, как у
// правки; пометки и сводка. Новая.
private fun reduceResetText(
    state: ScanCandidatesState,
    ref: ValueRefUi,
): ScanCandidatesResult

// reduceSetOption — опция по ссылке, зависимости, лист
// закрыть, флаг, если опция сменилась. «Есть» и ядро не
// трогает: «Выбор» — не ядро. Новая.
private fun reduceSetOption(
    state: ScanCandidatesState,
    message: Msg.SetOption,
): ScanCandidatesResult {
    val dirty = state.optionChanged(
        message.ref, message.option,
    )
    return state
        .begin<ScanCandidatesState, Effect>()
        .then { it.setOption(message.ref, message.option) }
        .then { it.refreshActiveValues() }
        .then { it.closeSheet() }
        .then { if (dirty) it.markDirty() else it.begin() }
}

// textChanged — текст значения по ссылке отличается от
// ввода с обрезанными краями; значения нет — false. Новая.
fun ScanCandidatesState.textChanged(
    ref: ValueRefUi,
    draft: String,
): Boolean

// optionChanged — у «Выбора» по ссылке сейчас другая
// опция. Новая.
fun ScanCandidatesState.optionChanged(
    ref: ComponentRefUi,
    option: Long?,
): Boolean
```

Хендлер эффектов экрана `DatasourceEffectHandler`: зовёт юзкейс,
получает доменный исход и ветвится по нему `when` прямо в ветке
эффекта: на каждый случай своё сообщение, данные внутри — через
`toUi()`. Отдельного маппера исхода нет. Провал эффекта — через
`RecoverableEffect.onFail`, как сейчас у вкладки.

```kotlin
// Распознавание. Меняется: эффект экрана, без номера
// запуска; доступ к файлу галереи отпускается после
is DatasourceEffect.RecognizeText -> withContext(io) {
    val outcome = useCase.recognize(eff.uri)
    useCase.releaseImageAccess(eff.uri)
    when (outcome) {
        is RecognitionOutcome.Text ->
            Msg.TextRecognized(outcome.tokens)
        RecognitionOutcome.Empty -> Msg.TextEmpty
    }
}

// Запрос на сервер. Меняется: исход доменный, разбор здесь,
// слова и рекомендации в UI через toUi()
is DatasourceEffect.LookupWords -> withContext(io) {
    when (val outcome = useCase.lookupWords(eff.tokens)) {
        is LookupOutcome.Words -> Msg.WordsFound(
            outcome.dictionaryId,
            outcome.words.map { it.toUi() },
            outcome.recommendations.map { it.toUi() },
        )
        LookupOutcome.NoWords -> Msg.WordsEmpty
        is LookupOutcome.Denied ->
            Msg.LookupDenied(errorResOf(outcome.reason))
        LookupOutcome.Unavailable -> Msg.LookupUnavailable(
            R.string.scan_server_unavailable,
        )
    }
}

// Форма словаря. Меняется: исход доменный, разбор здесь;
// ошибка — текстом, как у запроса
is DatasourceEffect.LoadForm -> withContext(io) {
    val outcome = useCase.loadForm(eff.dictionaryId)
    when (outcome) {
        is FormOutcome.Loaded ->
            Msg.FormLoaded(outcome.form.toUi())
        FormOutcome.DictionaryMissing -> Msg.FormFailed(
            R.string.scan_dictionary_missing,
        )
        is FormOutcome.Failed -> Msg.FormFailed(
            R.string.scan_form_failed,
        )
    }
}

// Запись. Меняется: по сообщению на причину отказа, было
// одно PhotoAddRejected на все
is DatasourceEffect.AddWords -> withContext(io) {
    when (val outcome = useCase.addWords(eff.batch)) {
        is AddWordsBatchOutcome.Added -> Msg.AddFinished(
            outcome.lexemeCount, outcome.wordCount,
        )
        is AddWordsBatchOutcome.GroupMissing ->
            Msg.AddGroupGone(outcome.toUi())
        is AddWordsBatchOutcome.WordMissing ->
            Msg.AddWordGone(outcome.toUi())
        is AddWordsBatchOutcome.TypeUnavailable ->
            Msg.AddComponentGone(outcome.toUi())
        // остальные причины — в панель, как есть
        else -> Msg.AddFailed(outcome.toUi())
    }
}
```

Сообщения экрана. Приставки `Photo` нет: экран и так про снимок.

```kotlin
// новое: было TokensReady(tokens, requestId) вкладки
data class TextRecognized(
    val tokens: List<RecognizedWord>,
) : Msg

// новое: было пустым списком в TokensReady
data object TextEmpty : Msg

// меняется: без номера запуска
data class RecognitionFailed(
    @StringRes val errorRes: Int,
) : Msg

// новое: было WordsLookedUp(outcome, requestId) с исходом
// целиком — теперь по сообщению на случай
data class WordsFound(
    val dictionaryId: Long,
    val words: List<LookupWordUi>,
    val recommendations: List<ComponentRecommendationUi>,
) : Msg

data object WordsEmpty : Msg

// новое: отказ по ключу или лимиту, «Повторить» нет (Д53)
data class LookupDenied(
    @StringRes val errorRes: Int,
) : Msg

// новое: сеть или сервер недоступен, с «Повторить» (Д53)
data class LookupUnavailable(
    @StringRes val errorRes: Int,
) : Msg

// новое (Д53)
data object RetryLookup : Msg

// меняется: было PhotoFormLoaded(outcome, requestId)
// с исходом целиком
data class FormLoaded(val form: DictionaryFormUi) : Msg

data class FormFailed(@StringRes val errorRes: Int) : Msg

// меняется: было PhotoAddRejected(failure, requestId) с
// разбором причины в редьюсере — теперь по сообщению на
// причину; у трёх с данными причина типом
data class AddGroupGone(
    val failure: AddFailureUi.GroupMissing,
) : Msg

data class AddWordGone(
    val failure: AddFailureUi.WordMissing,
) : Msg

data class AddComponentGone(
    val failure: AddFailureUi.TypeUnavailable,
) : Msg

data class AddFailed(val failure: AddFailureUi) : Msg

// меняется: было PhotoAddFinished(…, requestId)
data class AddFinished(
    val lexemeCount: Int,
    val wordCount: Int,
) : Msg

// меняется: было SubmitPhotoText без данных, редьюсер читал
// открытый лист из стейта — теперь адрес значения и текст
data class SubmitText(
    val ref: ValueRefUi,
    val text: String,
) : Msg

// меняется: было DeletePhotoExample(lemma, key, index)
data class DeleteValue(val ref: ValueRefUi) : Msg

// меняется: было ResetPhotoText(lemma, key, field)
data class ResetText(val ref: ValueRefUi) : Msg

// меняется: было SetPhotoPartOfSpeech(lemma, key,
// optionId); option null — «без значения»
data class SetOption(
    val ref: ComponentRefUi,
    val option: Long?,
) : Msg

// меняется: было TogglePhotoMapping
data object ToggleComponentMatches : Msg

// снято: SetPhotoMapping(kind, target)

// как есть, без приставки: ToggleCard, DeleteCard,
// DeleteLexeme, OpenSheet, CloseSheet, UpdateDraft,
// SetCardTarget, SetCardGroup, SetCommonGroup,
// ToggleNotAdded, RequestAdd, ConfirmAdd, CancelAdd,
// RetryAdd, RequestClose, ConfirmClose, Stay
```

#### Чистые функции карточек

Модуль `widget/candidates`, их зовут атомы экрана и диалога. Сборка,
пометки, сводка и пакет идут по сопоставлению. Соответствий нет.

```kotlin
// buildLemmaCards — по карточке на лемму в порядке первого
// появления (AWP-04). У лексемы values по сопоставлению в
// порядке компонентов формы: строка с полем → Text из поля
// ответа, список без «несколько» — одной строкой через
// «, » (AWP-29.9); строка с опцией → Choice; строки на
// компоненты и опции не из формы отбрасываются (AWP-29.12,
// 29.13); «англ.» — у слова есть no_translation с её леммой
// и ключом (AWP-34). Ядро у каждой лексемы заполнено,
// сервер без него лексему не отдаёт (Д49). Меняется.
fun buildLemmaCards(
    words: List<LookupWordUi>,
    form: DictionaryFormUi,
): CardsBuildUi

// withActiveValues — isActive у значений по зависимостям
// компонентов формы (AWP-17.6). Новая.
fun List<LemmaCardUiItem>.withActiveValues(
    form: DictionaryFormUi,
): List<LemmaCardUiItem>

// withExistsMarks — exists по активным ядрам (AWP-07, Д42).
// Меняется.
fun List<LemmaCardUiItem>.withExistsMarks(
    form: DictionaryFormUi,
): List<LemmaCardUiItem>

// withCoreMarks — lacksCore: нет активного ядра с непустым
// текстом (AWP-11). Меняется: без соответствий.
fun List<LemmaCardUiItem>.withCoreMarks(
    form: DictionaryFormUi,
): List<LemmaCardUiItem>

// summaryOf — сводка кнопки (AWP-13). Меняется: без
// соответствий.
fun summaryOf(
    cards: List<LemmaCardUiItem>,
    form: DictionaryFormUi?,
): AddSummaryUi

// remarksOf — строки «Замечания» из замечаний слов и
// рекомендаций (AWP-30); имя компонента из формы, нет в
// форме — строки нет. Новая.
fun remarksOf(
    words: List<LookupWordUi>,
    recommendations: List<ComponentRecommendationUi>,
    form: DictionaryFormUi,
): List<RemarkUiItem>

// componentMatchLinesOf — строки «Сопоставление» по
// сопоставлениям всех лексем без повторов, в порядке
// компонентов словаря (AWP-31, Д41). Новая.
fun componentMatchLinesOf(
    words: List<LookupWordUi>,
    form: DictionaryFormUi,
): List<ComponentMatchLineUiItem>

// wordsBatchOf — пакет из окна (AWP-20, AWP-32): у лексем
// без «есть» активные значения с непустым текстом — Text в
// TextValues, в компоненте «текст с подписью» значение из
// ответа с подписью «Wiktionary»; Choice в ChoiceValues, у
// «несколько» по значению на опцию. Меняется: по значениям,
// не по соответствиям.
fun wordsBatchOf(
    cards: List<LemmaCardUiItem>,
    form: DictionaryFormUi,
    commonGroupId: Long?,
): WordsBatch

// снято: PhotoMappingRules.kt целиком — targetsFor,
// defaultTarget, resolveMapping, choose, hasNoCore,
// revalidate; visibleLexemes и VISIBLE_LEXEMES — «ещё N»
// нет (AWP-05).
```

Домен → UI, `widget/candidates/mapper/LookupMappers.kt` и
`FormMappers.kt`, их зовут хендлеры обоих окон:

```kotlin
// toUi — поле в поле: LookupWord → LookupWordUi, с леммами,
// лексемами, сопоставлением и замечаниями внутри. Новая.
fun LookupWord.toUi(): LookupWordUi
fun ComponentRecommendation.toUi():
    ComponentRecommendationUi

// toUi — форма словаря: компоненты с опциями и
// зависимостями, группы, леммы, что уже есть. Новая.
fun DictionaryForm.toUi(): DictionaryFormUi

// toUi — отказ записи по причине: группа, слово, компонент
// исчезли; прочее — как есть для панели. Новая.
fun AddWordsBatchOutcome.Rejected.toUi(): AddFailureUi

// errorResOf — текст отказа по причине. Меняется: плюс
// LLM_UNAVAILABLE → «Сервер сейчас недоступен, попробуй
// позже».
fun errorResOf(reason: LookupDenyReason): Int
```

### Диалог карточки слова

`WordCardReducer` и `CandidatesAtoms` повторяют окно над
`CandidatesState`; стейт — те же UI-типы из `widget/candidates`.
Здесь только отличия от экрана выше. Диалог живёт в ViewModel
карточки, поэтому проверка номера запуска у него остаётся.

#### Стейт

- `CandidatesState` получает те же новые поля: `recommendations`,
  `remarks`, `componentMatchLines`, `isComponentMatchesExpanded`,
  `hasRetryButton`. Поля `mapping` и `isMappingExpanded` уходят.
  `CandidatesView` для общих виджетов — так же.

#### Редьюсер

- **Ответ** — `applyCandidatesLookup(outcome)` с разбором исхода внутри
  атома уходит. Хендлер ветвится `when` по доменному
  `WordLookupOutcome` порта `lookupWord`: `Found` → `CandidatesFound`
  с леммами через `toUi()` и рекомендациями; `NotFound` — ни одной
  лексемы-кандидата → `CandidatesNotFound`; `WordMissing`, `Denied` →
  `CandidatesLookupFailed` с текстом причины; `Unavailable`,
  `Denied(LLM_UNAVAILABLE)` и `WordFailed` (слово ответа `error`,
  WCL-11.7) → `CandidatesLookupUnavailable` с «Повторить» (Д53,
  WCL-11.9). Атомы:
  `startCandidatesForm(found)`, `showCandidatesError(errorRes)`,
  `showCandidatesRetryableError(errorRes)`.
- **Форма** — хендлер по `FormOutcome`: `Loaded` →
  `CandidatesFormLoaded(form.toUi())`, остальное →
  `CandidatesFormFailed(errorRes)`. Отказ записи — по сообщению на
  причину, как у экрана: `WordMissing` → `CandidatesWordGone`,
  `TypeUnavailable` → `CandidatesComponentGone`, прочие →
  `CandidatesAddFailed`. `applyCandidatesForm(form)` без
  запомненных соответствий. Цепочка после неё: `applyCandidatesRemarks`,
  `applyCandidatesComponentMatchLines`, `refreshCandidatesActiveValues`
  — новые, затем `refreshCandidatesExists`,
  `refreshCandidatesLemmaExists`, `refreshCandidatesCore`,
  `refreshCandidatesSummary` как есть.
- **Действия** — `deleteCandidateExample` → `deleteCandidateValue(ref)`,
  `applyCandidateTextEdit(ref, text)`, `dropCandidateEmptyExamples` →
  `dropCandidateEmptyTexts(ref)`, `resetCandidateText(ref)`,
  `setCandidatePartOfSpeech` → `setCandidateOption(ref, option)`;
  `refreshCandidatesActiveValues` в цепочке после `setCandidateOption`,
  `deleteCandidateValue` и `dropCandidateEmptyTexts`, как у экрана.
  `setCandidatesMapping` снят, `toggleCandidatesMapping` →
  `toggleCandidatesComponentMatches`.
- **Общие действия виджетов** — `CandidateAction`: `DeleteExample` →
  `DeleteValue(ref)`, `SubmitText` → `SubmitText(ref, text)` с данными
  из листа, `ResetText(ref)`, `SetPartOfSpeech` →
  `SetOption(ref, option)`, `SetMapping` снят, `ToggleMapping` →
  `ToggleComponentMatches`.
- **Запись** — `startAddingCandidates` собирает пакет
  `wordsBatchForWord` и `wordsBatchForTransfer` по значениям, без
  соответствий. `dropCandidatesComponent(typeId)` снимает и значения.
- **Страховки** — в `reduceCandidateAction` проверки «запись идёт»,
  «кнопка неактивна», «уже спрашиваем» уходят, как у экрана; выбор по
  `isDirty` при закрытии остаётся. «Назад» и нажатие мимо во время
  записи (WCL-12.4) глушит UI, не редьюсер: у диалога
  `DialogProperties` по `isAdding`, у экрана снимка `BackHandler` шлёт
  `RequestClose` только не во время записи (AWP-19.5).
- **Без изменений** — переименование и перенос в лемму-форму
  (WCL-15), пометки «есть в «correr»», вопрос при закрытии, повтор
  записи.

### Юзкейсы

Порты экранов говорят доменом; реализует их `app`. Экран
лексем-кандидатов, `ScanCandidatesUseCase` — новый порт:

```kotlin
// recognize — TFP Ш4–Ш5: ML Kit по файлу, токены как
// распознаны, с повторами, пустые прочь, потолок 2000
// (Д22). Исход типом: текст с токенами или пусто. Файл
// камеры удаляется в finally в любом исходе; сбой
// удаления исход не портит. Новое: было эффектом вкладки
// RecognizeWords + prepareTokens
suspend fun recognize(uri: String): RecognitionOutcome

// releaseImageAccess — отпустить доступ к файлу галереи,
// закреплённый вкладкой (Д52); у файла камеры ничего не
// делает. Зовётся в любом исходе распознавания, повтор и
// чужой файл — не ошибка, исключений наружу нет. Новое
suspend fun releaseImageAccess(uri: String)

// lookupWords — TFP Ш6: токены, языки и дерево компонентов
// текущего словаря (LFL-78). Исход типом на каждый случай:
// слова с рекомендациями, нет слов, отказ с причиной,
// сервер не ответил. Исключений нет. Номер запуска для
// заголовка запроса — счётчик процесса внутри юзкейса
// (TFP-01), у экрана его нет. Меняется: было у вкладки с
// номером запуска в стейте; плюс дерево, случай «нет
// слов», причина 503
suspend fun lookupWords(
    tokens: List<LookupToken>,
): LookupOutcome

// loadForm — AWP Ш1: форма словаря снимка — включённые
// живые компоненты всех зависимостей в порядке дерева
// (компонент, зависимые от его опций по порядку опций,
// зависимые от него самого), опции всех «Выборов», группы,
// леммы словаря с текстами лемм ответа. Меняется: без
// запомненных соответствий
suspend fun loadForm(
    dictionaryId: Long,
    lemmaTexts: List<String>,
): FormOutcome

// addWords — AWP Ш4: пакет одной транзакцией (AWP-20). Как
// было addPhotoWords у вкладки
suspend fun addWords(
    batch: WordsBatch,
): AddWordsBatchOutcome
```

Вкладка «Слова», `WordsTabUseCase`: уходят `lookupWords`,
`loadPhotoForm`, `addPhotoWords`, `prepareTokens`, `savePhotoMapping`.
Остаётся то, что до снимка: словарь, список слов.

Карточка слова, `WordCardUseCase`:

```kotlin
// lookupWord — WCL Ш1: один токен — текст слова без краёв,
// языки и дерево словаря слова (WCL-03.5). Исход типом на
// каждый случай: Found с леммами и рекомендациями; NotFound
// — ни одной лексемы-кандидата: not_found или found с
// пустыми леммами (LFL-100.2, WCL-04.7); WordMissing —
// слово или словарь удалены; Denied с причиной;
// Unavailable — сервер не ответил, сюда же слово ответа
// «ошибка» без лемм (WCL-11.7). Меняется: исход в домене,
// плюс дерево, случаи вместо null, причина 503
suspend fun lookupWord(
    wordId: Long,
    runId: Long,
): WordLookupOutcome

// loadCandidatesForm — форма словаря слова: как у экрана;
// леммы — слово диалога и слова с текстами лемм-форм.
// Меняется: без соответствий
suspend fun loadCandidatesForm(
    wordId: Long,
    lemmaTexts: List<String>,
): FormOutcome

// addCandidates — запись в слово диалога (WCL-10). Есть,
// не меняется. Снято: saveCandidatesMapping
```

Реализации в `app`, что они зовут:

```kotlin
// ComponentTreeLoader.treeOf — дерево компонентов словаря
// для запроса (Д28, LFL-78): корень — включённые живые
// компоненты от лексемы; у «Выбора» живые опции; children
// компонента — зависящие от него, children опции — от неё;
// key у встроенных, name — имя своего или встроенного из
// ресурсов. Словарь без включённых компонентов — пусто,
// поля в запросе нет (LFL-78.7). Новая
suspend fun treeOf(
    dictionaryId: Long,
): List<ComponentNodeApiEntity>

// lookupRequestOf — зеркало запроса: языки словаря, токены,
// дерево. Меняется: плюс дерево
fun lookupRequestOf(
    dictionary: DictionaryApiEntity,
    tokens: List<String>,
    components: List<ComponentNodeApiEntity>,
): LookupRequestApiEntity

// toDomain — app/mapper/LookupMappers.kt: исход сети →
// доменный исход: Success → Words с LookupWord, или NoWords
// при пустом ответе; Denied → Denied; Unavailable →
// Unavailable. Меняется: было toOutcome в типы виджета
fun LookupResult.toDomain(
    dictionaryId: Long,
): LookupOutcome

// CandidatesFormLoader.formOf и formForWord — доменная
// форма: компоненты всех зависимостей с dependsOn, опции
// каждого «Выбора», группы, леммы. Меняется
suspend fun formOf(
    dictionary: DictionaryApiEntity,
    lemmaTexts: List<String>,
): DictionaryForm

// снято: PhotoMappingStore — память соответствий (AWP-06.8)
```

### Шлюз

`core-network-api`: `WordsApi.lookup(request, runId): LookupResult`
есть и не меняется. Меняются зеркала по обе стороны провода и разбор
кодов отказа; имена — по слоям типов (Д57).

```kotlin
// LookupRequestApiEntity — меняется: плюс дерево
// компонентов (контракт, 1); было LookupRequest
data class LookupRequestApiEntity(
    val learningLanguage: String,
    val translationLanguage: String,
    val tokens: List<TokenApiEntity>,
    val components: List<ComponentNodeApiEntity>,
)

// ComponentNodeApiEntity, ComponentOptionNodeApiEntity —
// узлы дерева, как Component и ComponentOption в openapi:
// id, key, name, template, core, multiple, options,
// children; у опции id, key, label, children. Новое

// LookupResult — исход вызова: Success, Denied,
// Unavailable. Success — меняется: плюс recommendations.
// WordApiEntity — меняется: плюс remarks, без candidates
// (Д27, Д46); было WordResult. LemmaApiEntity — плюс
// pronunciations; было Lemma. CandidateLexemeApiEntity —
// плюс grammar, level, synonyms, antonyms, componentMatches
// из ComponentMatchApiEntity; без parent и parentKey (Д37);
// было CandidateLexeme. Candidate, Sense — снято

// DenyReason — меняется: плюс LLM_UNAVAILABLE, 503 с кодом
// llm_unavailable (LFL-63.5)
```

`core-network-impl`: Retrofit-классы `*Net` один в один с openapi
0.3.0-draft, были `*Dto`; `mapper/LookupMappers.kt` — `toApiEntity()`
в ответ, `toNet()` в запрос.

```kotlin
// denialOf — код отказа → исход. Меняется: 503 с кодом
// llm_unavailable → Denied(LLM_UNAVAILABLE); 503 без кода
// и прочие коды — Unavailable, как прежде
private fun denialOf(
    response: Response<LookupResponseNet>?,
): LookupResult

// Меняется в Net: в запросе ComponentNet и
// ComponentOptionNet; в ответе LookupResponseNet.
// recommendations, у WordResultNet remarks и нет
// candidates, LemmaNet.pronunciations, у CandidateLexemeNet
// grammar, level, synonyms, antonyms, componentMatches из
// ComponentMatchNet и нет parent, parentKey; WordRemarkNet
// плоский — code, componentId, lemma, lexemeKey;
// ComponentRecommendationNet — code, name, template,
// options
```

## 4. Контракт

- **Запрос** — новое поле `components`, дерево компонентов словаря.
- **Ответ** — у лексемы новые `componentMatches`, грамматика, уровень, синонимы и
  антонимы, у леммы `pronunciations`, у слова `remarks`, у ответа
  `recommendations`.
- **Снято** — `candidates` у слова со схемами `Candidate`, `Sense`,
  `Parameters` (Д27), `parent` и `parentKey` у лексемы (Д37).
- **Отказы** — новый 503 `llm_unavailable`, 429 теперь и по лимиту
  нейронки.

## 5. Ключевые решения

- **Сопоставление нейронкой на сервере по дереву пользователя** (Д8).
  Отвергнуто: каталог параметров и привязка на сервере (Д2), встроенный
  «Род» (Д4), соответствия на устройстве (Д5–Д7).
- **Нейронка только выбирает, сервер проверяет** (Д10). Отвергнуто:
  тексты от нейронки с правкой и сокращением.
- **Отбор и сопоставление одним вызовом, пачками** (Д33). Старт с N = 1: сбой
  бьёт одно слово, цена — повтор контекста и дерева в каждом вызове.
- **Без нейронки ответа нет** (Д38). Отвергнуто: запасной путь с первым
  значением без сопоставления (Д21, Д36).
- **Одна ручка** для карточки и снимка (Д26). Вторая появится, когда у
  одиночного слова будут свои гарантии.
- **Контекст — сами токены, без отдельного поля** (Д22).
- **Значения обоих Викисловарей одним списком, пары списком** (Д24, Д25).
- **Кеш только без контекста, в памяти** (Д45). Отвергнуто: кеш снимков,
  текст почти не повторяется.
- **Старые поля ответа уходят без заглушки** (Д27, Д46). Отвергнуто:
  пустой `candidates` для старых сборок, приложение не в проде.
- **«Кто ты» и «сколько можно» — разные сервисы: Auth и Quota** (Д47).
  Способов входа станет несколько, Google и Telegram, а лимит на
  человека один. Отвергнуто: один сервис «Доступ» с ключом и лимитами.
- **Три слоя: сценарий, сервис, порт, соседи друг в друга не ходят**
  (Д48). Сценарий один на операцию, его зовёт ручка, он зовёт сервисы,
  сервис зовёт только порты. Отвергнуто: сервис, который зовёт сервисы;
  общий код двух сервисов.
- **Ядро обязательно** (Д49). Лексема без строки сопоставления в включённое
  ядро — негодный ответ нейронки, сервер её не отдаёт. Отвергнуто:
  пустое поле ядра в окне для ввода руками (AWP-33).

## 6. Сквозное

- **Отказы** — кто решает:
  - 401 ключа нет, он неизвестен или отозван — `Auth`, до ручки
  - 413 большой запрос, 400 неверный запрос — схема контракта, до ручки
  - 429 лимит субъекта, потолок сервера или лимит нейронки и 400 язык —
    `Quota`, первый шаг сценария
  - 503 нейронка не ответила ни разу — Отбор, сценарий откатывает
    списание
- **Ошибка у слова** — источник не ответил, слово не успело в бюджет или
  нейронка не дала по нему годного.
- **Ядро** — у каждой отданной лексемы строка сопоставления в включённое
  ядро с непустым текстом: промпт требует, проверка без неё лексему не
  пропускает (Д49). У леммы не осталось годных — слово «ошибка».
- **Списание** — всё у `Quota`, зовёт только сценарий. Лимиты и списание
  токенов запроса — до работы, раньше кеша. Токены модели — после отбора,
  одной суммой за запрос; ответ из кеша счётчик нейронки не увеличивает.
  При 503 списанное откатывается (Д43). Сбой отката или счётчика модели —
  лог, ответ не ломает.
- **Бюджет времени** — 170 с на сервере, 180 с на устройстве. Своего
  срока у нейронки нет, что не успело — «ошибка» (Д34, Д38).
- **Лог** — лог вызова нейронки целиком (Д44).
- **Секреты** — ключ нейронки не попадает ни в ответ, ни в лог (LFL-93).
- **Конфиг** — модель и пачки меняются без правки кода. Смена модели
  сбрасывает попадания в кеш (Д15, LFL-94).
- **Данные у провайдера** — текст снимка и имена компонентов могут
  храниться у провайдера (Д35).

## 7. Выкатка

- **Порядок** — сначала бэкенд, деплой по тегу `v*`, потом релиз
  приложения.
- **Старое приложение на новом сервере**:
  - запрос без дерева компонентов сервер принимает: 200, нейронка
    зовётся без дерева (LFL-82.5)
  - новые поля ответа приложение пропускает: разбор с
    `ignoreUnknownKeys`
  - снятые `parent` и `parentKey` у него со значениями по умолчанию
  - 503 показывается как «сервер недоступен»

> [!NOTE]
> В приложении 0.1.17 поле `candidates` у слова обязательное, без
> значения по умолчанию: `WordResultDto` в `LookupDto.kt`. На новом
> сервере эта сборка не разберёт ответ. Это принято: приложение не в
> проде, совместимость со старыми сборками не нужна (Д46). Сборку на
> устройстве обновить сразу после деплоя сервера.

## 8. Риски

- **Старая сборка на устройстве** — после деплоя сервера и до установки
  новой сборки лексемы-кандидаты не приходят (Д46).
- **Провайдер лёг** — без нейронки ответа нет, 503 у всех (Д38), с
  лимитов ничего не списано (Д43).
- **Расход у провайдера** — лимит нейронки выключен (Д16), расход
  токенов модели только считается. Потолок трат держат лимит субъекта и
  потолок сервера по токенам запроса. Включить лимит — конфиг, `Quota`
  уже проверяет.
- **Задержка** — снимок со многими словами упирается в бюджет 170 с, что
  не успело — «ошибка». Долгий снимок задачей с ожиданием — бэклог
  бэкенда (Д34).
- **Цена** — оценка на фикстурах: среднее слово около 740 токенов
  промпта, страница на 50 слов около 37 тысяч. N = 1 повторяет контекст
  и дерево в каждом вызове.
- **Качество отбора** — проверяется контрольным набором `control_set.md`
  с фикстурами в бэкенде.
- **Кеш при деплое** — обнуляется, сразу после выкатки попаданий мало.

## Как перерисовать схемы

Условные знаки — Mermaid, обе схемы слоёв C4 — Graphviz: Mermaid не
ставит сноску сбоку от коробки, если от неё идёт ещё стрелка вниз, и
тасует порядок блоков. Источник в `.mmd` или `.dot` рядом, картинка
рендерится скриптом, в документ идёт картинка
по абсолютному пути: превью студии иначе не показывает. В имени картинки
хеш содержимого схемы: превью кеширует картинку по имени и после
перерисовки показывает старую. Перерисовать все и обновить ссылки здесь:

```
scripts/mmd-render.sh docs/features/IS536_llm_values
```
