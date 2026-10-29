# IS536 | HLD LFT — леммы из текста, черновик

План бэкенда операции «леммы из текста» по спеке LFT: токены снимка →
доступ, язык и работа → один вызов нейронки → текст, перевод, три
списка словарных форм и «не разобрано». Что править и в каком файле —
в LLD. Соседние планы в этой папке: `hld-lfl.md` — значения слова по
тапу, `hld-acc.md` — ключ, `hld-lim.md` — лимиты, `hld-tfp.md` — снимок
и токены на устройстве, `hld-awp.md` — экран. Первая версия фичи
целиком — `hld-v1.md`.

## 1. Рамка

- **Цель** — после снимка пользователь через секунды видит слова
  текста в словарной форме тремя списками и перевод текста; значений у
  слов нет, они грузятся по тапу (Д68–Д75, Д84).
- **Вход и выход** — устройство после распознавания шлёт токены в
  порядке текста, с повторами, с уверенностью и языком; назад уходят
  исправленный текст, перевод, списки `lexical`, `proper`, `function` и
  `unparsed`. Показ — экран AWP; тап по слову — операция LFL с текстом и
  переводом как контекстом.
- **Что новое на сервере** — операция `lemmas/from-tokens`, сценарий под неё,
  сервисы «Извлечение» и «Проверка пар», второй вызов нейронки у
  клиента: инструкция, схема ответа, разбор.
- **Что чужое и здесь только зовётся** — ключ и аккаунт (`hld-acc.md`),
  начало и конец работы (`hld-lim.md`), изучаемый язык и учёт чужих
  (сервис «Языки», `hld-lfl.md`).
- **Второй вход** — вставленный текст: свой маршрут
  `POST /v1/lemmas/from-text` с телом `text` и тем же ответом; телефон
  текст не режет, нумерует сервер. Сейчас — только имя, делается вместе
  с экраном вставки (Д113).
- **Документы** — [бриф](brief.md), [решения](decisions.md), спека LFT
  и её `contract.md`, спека TFP, экранная спека AWP, контрольный набор
  `control_set.md` в бэкенде.

## 2. Схема и код

### Условные знаки

![Условные знаки](/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS536_llm_values/hld-legend.a02d0bb8.png)

### Слои по C4

В коробке имя, тип в скобках, одна фраза ответственности. На стрелках
намерение связи. Под ручкой сноска: ответы и ошибки по контракту. Что
меняет фича — цветом на схеме и пометками в коде. Источник —
`hld-lft-layers-c4.dot`.

Номер на стрелке — шаг флоу LFT Ш1–Ш5; ACC Ш2 — ключ, LIM Ш2 — расход.
Ш3.1 — вызов порта внутри шага. Сверху вниз всё идёт по возрастанию
шагов: сценарий, два ряда сервисов, порты, адаптеры.

![Слои по C4](/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS536_llm_values/hld-lft-layers-c4.2e841ae1.png)

Код под схемой, в порядке вызова: ручка, сценарий, потом сервисы,
которые он зовёт. Реализованы публичные функции, всё, что они зовут,
дано сигнатурами с говорящими именами, без тел, под своей функцией.
Стиль — как в нынешних `internal/api/words/v1/lookup.go` и
`internal/scenario/lookup`. Комментарий у шага — его номер во флоу
LFT.

Слои те же, что у всего бэкенда: ручка → сценарий → сервис → порт,
соседи друг в друга не ходят (Д48). Как в них легла операция:

- **Свой сценарий.** `LemmasScenario` в пакете `scenario/lemmas` ведёт
  текст по шагам LFT и собирает списки сам, как `lookup` собирает
  слово. Зовёт «Языки», `Quota`, «Извлечение», «Проверка пар».
- **Два новых сервиса.** «Извлечение» — один вызов нейронки и его
  исходы, «Проверка пар» — пары против номеров токенов, чистая логика.
- **Нейронка получает только токены с номерами и языки.** Склеенного
  текста, уверенности и языка токенов во входе нет: текст она собирает
  сама (Д87).
- **Словарные формы сервер не правит.** Принимает или отбрасывает пару
  целиком, списки собирает по пунктам (LFT-01, LFT-11).
- **Счётчики после решения — лог, не ошибка.** Конец работы зовётся,
  когда ответ клиенту уже решён: сбой хранилища уходит в лог.

### Домен

Новые понятия операции, пакет `domain`; что не названо — как в v1:

```go
// TokensRequest — запрос lemmas/from-tokens после хука
// ключа. Новый.
type TokensRequest struct {
    // Account — владелец ключа, с которым пришёл запрос;
    // по нему лимиты и лог.
    Account Account
    // LearningLanguage — изучаемый язык словаря.
    LearningLanguage Language
    // TranslationLanguage — язык перевода словаря.
    TranslationLanguage Language
    // Tokens — токены снимка как пришли: в порядке
    // текста, с повторами, с уверенностью и языком.
    Tokens []Token
}

// NumberedToken — токен с номером: так его видят
// нейронка и проверка пар. Новый.
type NumberedToken struct {
    // N — номер с 1 в порядке текста.
    N int
    // Text — токен как распознан.
    Text string
}

// ExtractionPrompt — вход извлечения: всё, что уходит
// нейронке. Новый.
type ExtractionPrompt struct {
    // Tokens — токены с номерами; уверенность и язык
    // токена не уходят.
    Tokens []NumberedToken
    // LearningLanguage — изучаемый язык словаря.
    LearningLanguage Language
    // TranslationLanguage — язык перевода словаря.
    TranslationLanguage Language
}

// Extraction — ответ нейронки как есть, поля в порядке
// LFT-19. Новый.
type Extraction struct {
    // Text — текст из токенов с исправленными ошибками
    // распознавания.
    Text string
    // Translation — перевод текста на язык перевода.
    Translation string
    // Pairs — пара на каждое вхождение слова.
    Pairs []Pair
}

// Pair — слово текста → его словарная форма. Новый.
type Pair struct {
    // Tokens — номера токенов слова, подряд; обычно один.
    Tokens []int
    // Lemma — словарная форма; у обрывка пусто.
    Lemma string
    // List — список формы; обрывок — ListNone.
    List ListKind
}

// ListKind — список словарной формы. Новый.
type ListKind string

const (
    // ListLexical — смысловые
    ListLexical ListKind = "lexical"
    // ListProper — имена собственные
    ListProper ListKind = "proper"
    // ListFunction — служебные
    ListFunction ListKind = "function"
    // ListNone — «не слово», обрывок
    ListNone ListKind = "none"
)

// Checked — пары после проверки сервера. Новый.
type Checked struct {
    // Pairs — принятые пары.
    Pairs []Pair
    // Unparsed — токены без принятой пары, в порядке
    // текста.
    Unparsed []NumberedToken
    // Rejected — отброшенные пары с причиной, для лога.
    Rejected []RejectedPair
}

// RejectedPair — отброшенная пара и почему. Новый.
type RejectedPair struct {
    // Pair — пара как пришла от нейронки.
    Pair Pair
    // Reason — причина словами: нет номеров, номер не из
    // 1…n, номера не подряд, форма пуста.
    Reason string
}

// Lists — три списка словарных форм и «не разобрано»,
// как уходят устройству. Новый.
type Lists struct {
    // Lexical — смысловые, в порядке текста.
    Lexical []string
    // Proper — имена собственные.
    Proper []string
    // Function — служебные слова.
    Function []string
    // Unparsed — токены без пары и обрывки, как есть.
    Unparsed []string
}

// LemmasResponse — ответ Ш5 устройству. Новый.
type LemmasResponse struct {
    // Text — текст как дала нейронка.
    Text string
    // Translation — перевод как дала нейронка.
    Translation string
    // Lists — четыре списка, встроены.
    Lists
}
```

Вид работы для лимитов `WorkTextLemmas` — в `hld-lim.md`.

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

Ручка одна на обе операции контракта, у неё два сценария; здесь —
операция `lemmas/from-tokens`:

```go
// wordsHandler — ручка слов, одна на обе операции.
// Меняется: плюс сценарий лемм из текста.
type wordsHandler struct {
    // lexemes — сценарий lexemes/from-lemma.
    lexemes scenario.LookupScenario
    // lemmas — сценарий lemmas/from-tokens; новое поле.
    lemmas scenario.LemmasScenario
    // auth — сервис Auth для хука ключа.
    auth service.AuthService
}

// LemmasFromTokens — Ш1 и Ш5: аккаунт из контекста,
// токены в домен, сценарий, ответ в контракт. 401 отдан в
// хуке, 413 и 400 по форме запроса — до ручки. Отказы по
// лимиту, языку и нейронке — типизированные ответы, сбой
// сервера — error наверх, 500 через ErrorHandler. Новая.
func (h *wordsHandler) LemmasFromTokens(
    ctx context.Context,
    req *wordsv1.TokensRequest,
    params wordsv1.LemmasFromTokensParams,
) (wordsv1.LemmasFromTokensRes, error) {
    account := accountFrom(ctx)
    ctx = logger.ContextWithRunID(ctx, params.XRunID.Or(""))
    started := time.Now()
    resp, err := h.lemmas.FromTokens(
        ctx,
        converters.TokensRequestToDomain(req, account),
    )
    logTokensRequest(ctx, account, req, resp, started, err)

    switch {
    case err == nil:
        return converters.LemmasResponseToAPI(resp), nil
    // В2: нейронка не ответила, не успела или ответила
    // не по схеме
    case errors.Is(err, domain.ErrLLMUnavailable):
        return lemmasLLMUnavailable(err), nil
    // лимит токенов нейронки; лимит работ пока пускает
    case errors.Is(err, domain.ErrRateLimited):
        return lemmasRateLimited(), nil
    // изучаемого языка нет в базе слов
    case errors.Is(err, domain.ErrUnsupportedLanguage):
        return lemmasUnsupportedLanguage(
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

// lemmasLLMUnavailable, lemmasRateLimited,
// lemmasUnsupportedLanguage — ответы 503, 429 и 400 этой
// операции, те же коды, что у lookup: у каждой операции
// свои типы ответов. Новые.
func lemmasLLMUnavailable(
    err error,
) *wordsv1.LemmasFromTokensServiceUnavailable

// logTokensRequest — одна строка лога на запрос: аккаунт,
// языки, число токенов, время, ошибка. Новая.
func logTokensRequest(
    ctx context.Context,
    account domain.Account,
    req *wordsv1.TokensRequest,
    resp domain.LemmasResponse,
    started time.Time,
    err error,
)

// TokensRequestToDomain — запрос контракта в домен:
// аккаунт, языки, токены как пришли. Пакет converters,
// новая.
func TokensRequestToDomain(
    req *wordsv1.TokensRequest,
    account domain.Account,
) domain.TokensRequest

// LemmasResponseToAPI — ответ домена в контракт: текст,
// перевод, четыре списка строк. Новая.
func LemmasResponseToAPI(
    resp domain.LemmasResponse,
) *wordsv1.LemmasResponse
```

`ErrorHandler` и `classify` не меняются: ключ — 401, схема запроса —
413 по числу токенов сверх 2000 или 400, остальное — 500. Признак 413
`tooManyTokens` — поле `tokens`, оно теперь только у этой операции.

### Сценарий

Сценарий `LemmasScenario`, пакет `scenario/lemmas`, один на операцию.
Знает порядок шагов и ветки, зовёт сервисы, сам в порты не ходит.

```go
// LemmasScenario — операция lemmas/from-tokens целиком,
// Ш2–Ш5. Пакет scenario, новый.
type LemmasScenario interface {
    FromTokens(
        ctx context.Context,
        req domain.TokensRequest,
    ) (domain.LemmasResponse, error)
}

// FromTokens — Ш2–Ш5 для токенов снимка: язык и начало
// работы, токены с номерами, один вызов нейронки, конец
// работы, проверка пар, списки, ответ. Нейронка не
// ответила, не успела или ответила негодно — наверх
// ErrLLMUnavailable (В2). Вход для вставленного текста —
// FromText, появится с экраном вставки. Новый.
func (s *lemmasScenario) FromTokens(
    ctx context.Context,
    req domain.TokensRequest,
) (domain.LemmasResponse, error) {
    // Ш2: изучаемый язык есть в базе слов; ключ и размер
    // проверены до ручки
    err := s.languages.Check(ctx, req.LearningLanguage)
    if err != nil {
        return rejected(err)
    }
    // Ш2: начало работы по LIM, пока всегда «можно»
    err = s.quota.Begin(
        ctx, req.Account,
        domain.WorkTextLemmas,
    )
    if err != nil {
        return rejected(err)
    }
    // Ш2: токенов нет — списки пусты, нейронка не зовётся
    if len(req.Tokens) == 0 {
        return emptyLists(), nil
    }
    // 170 с на всё
    ctx, finish := withinBudget(ctx, s.options.Budget)
    defer finish()

    // Ш3: токены с номерами с 1 → один вызов нейронки:
    // текст, перевод, пары
    tokens := numbered(req.Tokens)
    extraction, usage, err := s.extractor.Extract(
        ctx,
        promptOf(req, tokens),
    )
    // LIM Ш2: конец работы — расход нейронки, 0 — не
    // ответила
    s.finishWork(ctx, req.Account, usage)
    if err != nil {
        return rejected(err)
    }
    // Ш3: пары под проверкой сервера; промахи нейронки —
    // в лог
    checked := s.pairs.Check(extraction, tokens)
    logExtraction(ctx, tokens, checked)

    // Ш4–Ш5: три списка и «не разобрано», ответ
    return response(extraction, listed(checked)), nil
}
```

Что зовёт сценарий. Сервисы `s.extractor` и `s.pairs` реализованы
ниже; `s.languages` — `hld-lfl.md`, `s.quota` — `hld-lim.md`:

```go
// Check — Ш2: изучаемый язык по основному коду есть среди
// языков базы слов — пусто; нет — язык в учёт и
// ErrUnsupportedLanguage (LFT-17). Сервис «Языки».
Check(ctx context.Context, lang domain.Language) error

// Begin — LIM Ш1: можно ли аккаунту начать работу; лимит
// работ пока всегда пускает, лимит токенов нейронки —
// ErrRateLimited. Сервис Quota.
Begin(
    ctx context.Context,
    account domain.Account,
    work domain.Work,
) error

// Finish — LIM Ш2: расход нейронки в счётчик аккаунта;
// 0 — нейронка не ответила, счётчик не растёт. Сервис
// Quota.
Finish(
    ctx context.Context,
    account domain.Account,
    usage domain.ModelUsage,
) error
```

Помощники пакета `scenario/lemmas`:

```go
// numbered — токены с номерами с 1 в порядке текста, как
// пришли, с повторами. Новая.
func numbered(tokens []domain.Token) []domain.NumberedToken

// promptOf — вход извлечения: токены с номерами и языки
// словаря. Новая.
func promptOf(
    req domain.TokensRequest,
    tokens []domain.NumberedToken,
) domain.ExtractionPrompt

// finishWork — LIM Ш2: расход вызова в счётчик аккаунта.
// Ответ уже решён: сбой хранилища — в лог, наверх не идёт.
// Новая.
func (s *lemmasScenario) finishWork(
    ctx context.Context,
    account domain.Account,
    usage domain.ModelUsage,
)

// listed — Ш4: три списка из принятых пар, одинаковые
// формы в одном списке — один пункт на месте первого
// вхождения, одна форма в двух списках — по пункту в
// каждом; «не разобрано» — токены без принятой пары, как
// есть, в порядке текста. Новая.
func listed(checked domain.Checked) domain.Lists

// logExtraction — LFT-16: номер запуска из контекста,
// число токенов и принятых пар, «не разобрано» с
// номерами, отброшенные пары с причиной. Новая.
func logExtraction(
    ctx context.Context,
    tokens []domain.NumberedToken,
    checked domain.Checked,
)

// emptyLists — ответ на пустой список токенов: текст,
// перевод и списки пусты. Новая.
func emptyLists() domain.LemmasResponse

// response — Ш5: текст и перевод как дала нейронка,
// списки. Новая.
func response(
    extraction domain.Extraction,
    lists domain.Lists,
) domain.LemmasResponse

// withinBudget — контекст с дедлайном по бюджету ответа,
// 170 с (LFT-15). Своя копия в пакете, как у lookup.
func withinBudget(
    ctx context.Context,
    budget time.Duration,
) (context.Context, context.CancelFunc)

// rejected — отказ Ш2 или В2: ответа нет, ошибка наверх.
func rejected(err error) (domain.LemmasResponse, error)
```

### Извлечение

Сервис «Извлечение» — Ш3, один вызов нейронки. Его зовёт только
сценарий.

```go
// Extractor — Ш3: токены с номерами и языки → текст,
// перевод, пары. Пакет service, новый.
type Extractor interface {
    Extract(
        ctx context.Context,
        prompt domain.ExtractionPrompt,
    ) (domain.Extraction, domain.ModelUsage, error)
}

// Extract — один вызов нейронки: ответ как есть и расход.
// Нейронка не ответила, не успела в бюджет или ответ не по
// схеме, в том числе оборван, — ErrLLMUnavailable; расход
// отдаётся и тогда, когда ответ был, но негодный: вызов
// ответил (LIM-79). Новая.
func (e *extractor) Extract(
    ctx context.Context,
    prompt domain.ExtractionPrompt,
) (domain.Extraction, domain.ModelUsage, error) {
    extraction, usage, err := e.client.Extract(ctx, prompt)
    switch {
    case err == nil:
        return extraction, usage, nil
    // ответ был, но не по схеме: для устройства это
    // 503 (LFT-15.2), расход считается
    case errors.Is(err, domain.ErrAnswerUnreadable):
        return domain.Extraction{}, usage,
            domain.ErrLLMUnavailable
    // соединение, код ошибки провайдера или бюджет
    default:
        return domain.Extraction{}, domain.ModelUsage{},
            domain.ErrLLMUnavailable
    }
}
```

Что зовёт «Извлечение». Порт «Клиент нейронки», `LLMClient`, получает
второй метод:

```go
// Extract — Ш3.1, один вызов модели: инструкция
// извлечения, токены с номерами и языки, схема ответа
// «текст, перевод, пары». Ошибка сети, провайдера, пустой
// ответ — error без расхода; ответ есть, но не разбирается
// — ErrAnswerUnreadable с расходом. Новый.
Extract(
    ctx context.Context,
    prompt domain.ExtractionPrompt,
) (domain.Extraction, domain.ModelUsage, error)
```

### Проверка пар

Сервис «Проверка пар» — Ш3, пары против номеров токенов. Чистая
логика без хранилищ, сервис ради одного шага и подмены в тестах.

```go
// PairChecker — LFT-11: пары нейронки против токенов
// запроса. Пакет service, новый.
type PairChecker interface {
    Check(
        extraction domain.Extraction,
        tokens []domain.NumberedToken,
    ) domain.Checked
}

// Check — пара принимается, когда у неё есть номера, все
// они среди токенов и идут подряд, а форма непуста; «не
// слово» принимается без формы, его токены — в «не
// разобрано»; негодная пара — вон с причиной, её токены
// без других пар — тоже в «не разобрано». Форму и список
// не правит. Новая.
func (c *checker) Check(
    extraction domain.Extraction,
    tokens []domain.NumberedToken,
) domain.Checked {
    accepted := []domain.Pair{}
    rejected := []domain.RejectedPair{}
    paired := map[int]bool{}
    for _, pair := range extraction.Pairs {
        reason := invalid(pair, len(tokens))
        switch {
        case reason != "":
            rejected = append(
                rejected,
                rejection(pair, reason),
            )
        // обрывок: токены остаются без пары
        case pair.List == domain.ListNone:
        default:
            accepted = append(accepted, pair)
            mark(paired, pair.Tokens)
        }
    }
    return domain.Checked{
        Pairs:    accepted,
        Unparsed: unpaired(tokens, paired),
        Rejected: rejected,
    }
}
```

Помощники пакета `pairs`:

```go
// invalid — почему пара негодна: нет номеров, номер не из
// 1…n, номера не подряд, форма пуста при списке не «не
// слово»; годная — пусто. Новая.
func invalid(pair domain.Pair, count int) string

// rejection — отброшенная пара с причиной для лога. Новая.
func rejection(
    pair domain.Pair,
    reason string,
) domain.RejectedPair

// mark — номера пары как занятые. Новая.
func mark(paired map[int]bool, tokens []int)

// unpaired — токены без принятой пары в порядке текста.
// Новая.
func unpaired(
    tokens []domain.NumberedToken,
    paired map[int]bool,
) []domain.NumberedToken
```

### Клиент нейронки

Адаптер порта «Клиент нейронки», пакет `clients/openrouter`: ходит к
провайдеру через OpenAI-совместимый `chat/completions`, пишет лог
вызова. Для извлечения — своя инструкция и своя схема ответа.

```go
// Extract — вызов извлечения: инструкция извлечения,
// пример парой сообщений, JSON с токенами и языками; схема
// ответа — текст, перевод, пары, где номер токена — целое
// от 1 до числа токенов, список — одно из четырёх слов;
// свой потолок длины ответа; расход из usage ответа,
// строка лога на вызов. Новая.
func (c *client) Extract(
    ctx context.Context,
    prompt domain.ExtractionPrompt,
) (domain.Extraction, domain.ModelUsage, error)

// extractionSchema — схема ответа извлечения по числу
// токенов. Новая.
func extractionSchema(
    prompt domain.ExtractionPrompt,
) jsonSchema

// parseExtraction — JSON ответа в Extraction: не JSON,
// без текста или пар, оборван — ошибка целиком; кривая
// пара — мимо, её снимет проверка. Новая.
func parseExtraction(
    content string,
) (domain.Extraction, error)
```

Инструкция извлечения — файлом на сервере, без тега, второе поле
настроек клиента рядом с инструкцией отбора. Что в ней: собрать текст
из токенов, исправляя только ошибки распознавания; перевести; дать
пару на каждое вхождение слова, форма — как в словаре, род — как в
словаре (Д107), имена — с большой буквы, обрывок — «не слово». Текст
инструкции — в плане, не здесь.

## 3. Контракт

Дельта к `api/openapi.yaml`:

- **Новая операция** `POST /v1/lemmas/from-tokens`, `lemmasFromTokens`, с
  заголовком `X-Run-Id`. Запрос `TokensRequest`: `learningLanguage`,
  `translationLanguage`, `tokens` — те же `TokenQuery`, не больше 2000.
  Ответ `LemmasResponse`: `text`, `translation`, `lexical`, `proper`,
  `function`, `unparsed` — списки строк.
- **Отказы** — 401, 413, 400 `unsupported_language` и
  `invalid_request`, 429, 503: те же коды и `Error`, что у `lookup`.
  Порядок проверок: ключ, размер, схема, язык, начало работы.
- Живые примеры — `contract.md` рядом со спекой LFT.

## 4. Ключевые решения

- **Один вызов нейронки на снимок** (Д87): текст, перевод и пары
  приходят разом, порядок полей — текст, перевод, пары (LFT-19).
- **Пары на каждое вхождение, «не разобрано» виден** (Д84): сервер
  словарные формы не правит, только проверяет номера; обрывки и токены
  без пары пользователь видит.
- **Нейронке — только токены с номерами** (Д87): склеенный текст она
  собирает сама, исправляя ошибки распознавания.
- **Род — как в словаре** (Д107): «la primera» → «el», «primero».
- **Язык проверяется до работы** (Д100), чужой — в учёт (Д101):
  сервисом «Языки», общим с LFL.
- **Лимиты — прокладка** (Д106): `Begin` всегда пускает, `Finish`
  записывает расход.
- **Три слоя** (Д48): сценарий зовёт сервисы, сервисы — порты.

## 5. Сквозное

- **Отказы** — кто решает:
  - 401 — `Auth`, до ручки
  - 413 больше 2000 токенов, 400 не по схеме — схема контракта, до
    ручки
  - 400 язык — «Языки», первый шаг сценария, с учётом
  - 429 лимит токенов нейронки — `Quota.Begin`, второй шаг
  - 503 — «Извлечение»: нейронка не ответила, не успела или ответила
    не по схеме; на устройстве повтор с теми же токенами
- **Работа и расход** — `Begin` до нейронки, `Finish` после, расход 0 —
  счётчик не растёт; сбой записи — лог.
- **Бюджет времени** — 170 с на сервере, 180 с на устройстве; у
  нейронки своего срока нет, остаток бюджета — её контекст.
- **Лог** — номер запуска, число токенов и пар, «не разобрано» с
  номерами, отброшенные пары с причиной (LFT-16); на `debug` — вход
  модели целиком.
- **Секреты** — ключ нейронки не попадает ни в ответ, ни в лог; ключ
  доступа — ни в логи, ни в ответы.
- **Конфиг** — модель, инструкция извлечения файлом, потолок длины
  ответа; предел 2000 токенов — в схеме, меняется вместе с TFP-15.
- **Данные у провайдера** — текст снимка целиком (Д35).

## 6. Выкатка

- **Порядок** — вместе с остальным бэкендом v2, тег `v0.1.32`, потом
  сборка приложения; у старой сборки этой операции нет.
- **Инструкция нейронке** — до тега подбирается пробником на модели:
  новый `lexeme probe text` с токенами снимка по контрольному набору.
  Предел 2000 токенов и потолок ответа — по его прогонам.

## 7. Риски

- **Извлечение на модели не пробовалось** — качество пар и списков
  неизвестно, инструкцию подбирает пробник.
- **Длина ответа** — плотная страница в 2000 токенов даёт пары на
  каждый токен, это десятки тысяч токенов ответа: потолок выше, чем у
  отбора; оборванный ответ — 503, пользователь повторяет.
- **Схема ответа** — номера токенов целыми от 1 до n, не
  перечислением: у провайдера предел на число значений перечисления.
- **Промахи нейронки** — форма не доведена до словарной: по тапу её
  найдёт запасной поиск LFL, промахи видны в логе; списки — как дала
  нейронка.
- **Провайдер лёг** — без нейронки списков нет: 503, с работ ничего не
  списано.
- **Цена** — один вызов на всю страницу, вход — токены с номерами,
  выход — пары на каждый.

## 8. Как перерисовать схемы

Условные знаки — Mermaid, схема слоёв — Graphviz: Mermaid не ставит
сноску рядом с коробкой и тасует порядок блоков. Источник в `.mmd` или
`.dot` рядом, картинка рендерится скриптом, в документ идёт по
абсолютному пути с хешем содержимого в имени: превью студии кеширует
картинку по имени. Перерисовать все схемы папки и обновить ссылки:

```
scripts/mmd-render.sh docs/features/IS536_llm_values
```
