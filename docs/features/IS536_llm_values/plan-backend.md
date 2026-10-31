# IS536 | План бэкенда

LLD к `hld-lft.md`, `hld-lim.md`, `hld-acc.md` и `hld-lfl.md`. Этап 1
фичи — IS537 «леммы из текста»: ветка `IS537_lemmas_from_tokens` от
`master`, PR в `master`, тег v0.1.32; Д121 — патч v0.1.36 до этапа 2;
этап 2 — своя ветка от `master` после закрытия первого, тег v0.1.37.
Этапы в порядке выполнения, независимое первым; каждый этап
собирается и проходит тесты сам. Внутри этапа — триады: было —
нынешний код из репо, будет — код после этапа, мотивация — зачем он
меняется. Строки нынешнего кода перенесены под шестьдесят знаков,
текст тот же. Код — по команде. Этап выполнен — пометка «ВЫПОЛНЕН
<дата>» с одной фразой, что разошлось с планом. План первой версии —
`plan-backend-v1.md`.

Команды: генерация `task gen`, моки `task mockery:gen`, тесты
`task test`, с Postgres `task test:integration`, линт `task lint`,
покрытие `task spec:coverage`, проба на модели `lexeme probe text`.

Что входит в этап 1 (Э1–Э8): аккаунт вместо субъекта (Д115), сервис
«Языки» с учётом (LFL-114), лимиты-прокладка (Д106), операция
`lemmas/from-tokens` целиком, пробник извлечения, контракт 0.4.0.
`words/lookup` снаружи не меняется, кроме одного: пустой текст токена
даёт 400 по схеме; внутри — вызовы языков и лимитов. Этап 2 (Э9–Э14):
словарная форма ровно в одном списке (Д121); операция
`lexemes/from-lemma` вместо `words/lookup` (Д114, Д46) — лемма с
контекстом, одно слово в ответе, предел 4000 слов кодом (Д111),
запасной поиск леммы по связям, один вызов нейронки без пачек и без
перевода контекста, без пар (Д63, Д64), пометка «в тексте», пробник
по лемме, контракт 0.5.0, тег v0.1.37. Порядок: сначала домен и
порты, затем сервисы отбора, проверки и клиент, потом сценарий, ручка
и сборка — каждый этап собирает свои пакеты, весь модуль зелёный
после ручки. Приложение этапа 2 — `plan-app.md`, Э6–Э10.

## Э1. Аккаунт вместо субъекта

ВЫПОЛНЕН 2026-10-27. Как в плане; имена ограничений и
последовательности сверены с первой миграцией, интеграционные тесты
накатили переименование на чистую схему.

Тезисы: ACC-05, ACC-19, ACC-26, ACC-31, ACC-77 — текст тот же, меняется
слово. Проверка: `task test`, `task lint`, `task test:integration`;
имена тестов со словом subject переименовываются, адреса строк те же;
`grep -rn -i subject` по репозиторию после этапа — пусто, кроме
истории в `agents/` и `CHANGELOG`.

Допущения: `tokens` в `usage` и `daily_limit` у аккаунта остаются в
базе и не читаются до политики лимитов (бэклог); имена ограничений и
последовательности — умолчания Postgres по `202610100001_access.sql`,
перед накатом сверяются `\d subjects` на проде.

### Домен

**Было.** `internal/domain/access.go`:

```go
// Subject — человек, которому выписан ключ; у субъекта
// свои лимиты.
type Subject struct {
    Name   string
    Limits Limits
}

// Limits — суточные пределы в токенах (LIM-29); 0 —
// предела нет. Лимит токенов модели (LIM-96) один на
// всех, из конфига сервера: он у Quota.
type Limits struct {
    SubjectDaily int
    ServerDaily  int
}
```

**Будет.**

```go
// Account — кому выдан ключ доступа и у кого считаются
// лимиты; сейчас один на сборку приложения (Д115).
type Account struct {
    // Name — имя аккаунта, его выписывает владелец
    // сервера командой key.
    Name string
    // Limits — пределы аккаунта из базы.
    Limits Limits
}

// Limits — пределы аккаунта; 0 — предела нет. Пока
// лимиты — прокладка, не читаются (LIM-110).
type Limits struct {
    // AccountDaily — столбец daily_limit, в токенах.
    AccountDaily int
}
```

В `internal/domain/doc.go` строка глоссария «Субъект → Subject» —
«Аккаунт → Account»; в `docs/handbook/speaking-code.md` примеры с
`req.Subject` — `req.Account`.

**Мотивация.** Слово «субъект» не читалось (Д115); потолок сервера из
лимитов уходит вместе с токенами запроса (Э3).

### Сервисы и порты

**Было.** `internal/service/service.go`, `ports.go`:

```go
type AuthService interface {
    SubjectByKey(
        ctx context.Context,
        key domain.AccessKey,
    ) (domain.Subject, error)
}

type SubjectRepository interface {
    ByKey(
        ctx context.Context,
        key domain.AccessKey,
    ) (domain.Subject, error)
}

type KeyStore interface {
    EnsureSubject(
        ctx context.Context,
        name string,
        limits domain.Limits,
    ) error
    SaveKey(
        ctx context.Context,
        subjectName string,
        key domain.AccessKey,
    ) error
    RevokeKey(
        ctx context.Context,
        key domain.AccessKey,
    ) error
}
```

**Будет.** Те же методы с аккаунтом: `AuthService.AccountByKey`,
`AccountRepository.ByKey`, `KeyStore.EnsureAccount`, `SaveKey(ctx,
accountName, key)`; `KeyService.Issue(ctx, accountName, dailyLimit)`.
Пакет `service/auth`: поле `accounts`, метод `AccountByKey`. Моки —
`task mockery:gen`: `mock_account_repository.go` вместо
`mock_subject_repository.go`; конфиг `.mockery.yaml` — имена
интерфейсов. Комментарий в
`internal/service/selection/selection.go:135` — «аккаунт».

**Мотивация.** Имя сущности в каждом порту; код говорит словом
глоссария.

### Ручка и конвертеры

**Было.** `internal/api/words/v1/security.go`:

```go
type subjectKey struct{}

func (h *wordsHandler) HandleAccessKey(
    ctx context.Context,
    _ wordsv1.OperationName,
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

func withSubject(
    ctx context.Context,
    subject domain.Subject,
) context.Context

func subjectFrom(ctx context.Context) domain.Subject
```

**Будет.** `accountKey{}`, `h.auth.AccountByKey`, `withAccount`,
`accountFrom` — тела те же. В `lookup.go` переменная `account`,
`logRequest(ctx, account, …)` с полем лога `account`. В
`converters/lookup.go` `RequestToDomain(req, account)` кладёт
`Account: account`. В `domain/lookup.go` поле `LookupRequest.Account`.

**Мотивация.** Одно имя сущности от ручки до хранилища.

### Хранилище и миграция

**Было.** `internal/repository/postgres/repository.go`:

```go
const (
    tableSubjects   = "subjects"
    tableAccessKeys = "access_keys"
    tableUsage      = "usage"

    colID         = "id"
    colName       = "name"
    colDailyLimit = "daily_limit"
    colKey        = "key"
    colSubjectID  = "subject_id"
    colRevokedAt  = "revoked_at"
    colDay        = "day"
    colTokens     = "tokens"
)
```

Запросы руками в `subjects.go` и `usage.go`:

```go
const query = `INSERT INTO access_keys (key, subject_id)
SELECT $1, s.id FROM subjects s WHERE s.name = $2`
```

Тексты ошибок `subject %q not found` в `subjects.go:71` и
`usage.go:107`.

**Будет.** Миграция `migrations/202610270001_accounts.sql` — таблица,
столбцы, индекс, ограничения и последовательность, данные те же:

```sql
-- +goose Up
-- Субъект становится аккаунтом (Д115): одно слово в базе,
-- как в коде; данные на месте.
ALTER TABLE subjects RENAME TO accounts;
ALTER TABLE accounts
    RENAME CONSTRAINT subjects_pkey TO accounts_pkey;
ALTER TABLE accounts
    RENAME CONSTRAINT subjects_name_key
    TO accounts_name_key;
ALTER SEQUENCE subjects_id_seq RENAME TO accounts_id_seq;
ALTER TABLE access_keys
    RENAME COLUMN subject_id TO account_id;
ALTER TABLE access_keys
    RENAME CONSTRAINT access_keys_subject_id_fkey
    TO access_keys_account_id_fkey;
ALTER INDEX access_keys_subject_idx
    RENAME TO access_keys_account_idx;
ALTER TABLE usage RENAME COLUMN subject_id TO account_id;
ALTER TABLE usage
    RENAME CONSTRAINT usage_subject_id_fkey
    TO usage_account_id_fkey;

-- +goose Down
ALTER TABLE usage
    RENAME CONSTRAINT usage_account_id_fkey
    TO usage_subject_id_fkey;
ALTER TABLE usage RENAME COLUMN account_id TO subject_id;
ALTER INDEX access_keys_account_idx
    RENAME TO access_keys_subject_idx;
ALTER TABLE access_keys
    RENAME CONSTRAINT access_keys_account_id_fkey
    TO access_keys_subject_id_fkey;
ALTER TABLE access_keys
    RENAME COLUMN account_id TO subject_id;
ALTER SEQUENCE accounts_id_seq RENAME TO subjects_id_seq;
ALTER TABLE accounts
    RENAME CONSTRAINT accounts_name_key
    TO subjects_name_key;
ALTER TABLE accounts
    RENAME CONSTRAINT accounts_pkey TO subjects_pkey;
ALTER TABLE accounts RENAME TO subjects;
```

`usage_pkey` имени сущности не несёт — остаётся. Файлы `subjects.go`
→ `accounts.go`, константы `tableAccounts`, `colAccountID`, строка
`accountRow`, `accountToDomain`; запросы руками — `accounts` и
`account_id`; `EnsureAccount`; тексты ошибок `account %q not found`.
Пакет `memory`: `NewAccounts`. Интеграционный тест репозитория — те
же сценарии с новыми именами.

**Мотивация.** В базе то же слово, что в коде и доках; переименование
Postgres без копирования данных.

### Конфиг, команды, сборка, доки

**Было.** `internal/config/env/llm.go`:

```go
// SubjectDaily — токенов модели в сутки на субъекта
// (LIM-96); 0 — лимита нет.
SubjectDaily int `env:"LLM_SUBJECT_DAILY" envDefault:"0"`
```

`cmd/lexeme/key.go`:

```go
const keyUsage = `usage:
  lexeme key issue <subject> [--limit N]   выписать …
  lexeme key revoke <key>                  отозвать ключ`
```

Слово «субъект» ещё в `deploy/.env.example:45–46`
(`LLM_SUBJECT_DAILY=0`), `Taskfile.yml:157` (описание `key:issue`),
`docs/handbook/deploy.md` (переменная и строка лога
`lookup words … subject=…`), `.github/workflows/deploy.yml:4`.

**Будет.** `AccountDaily int env:"LLM_ACCOUNT_DAILY"`, метод
`AccountDaily()`; `di.go` читает его. Usage команды — `<account>`,
комментарий «на аккаунт». В `probe_llm.go` —
`domain.Account{Name: "probe"}`. `deploy/.env.example` —
`LLM_ACCOUNT_DAILY=0` с комментарием «на аккаунт (LIM-96)»;
`Taskfile.yml` — «Выписать ключ аккаунту… <аккаунт>»; `deploy.md` —
переменная и строка лога `account=`; комментарий в `deploy.yml`. В
`deploy/.env` на сервере переменная переименовывается перед тегом.

**Мотивация.** Переменная прода называется по сущности; старое имя
после тега перестаёт читаться — без правки `.env` лимит нейронки
выключится молча, это шаг выкатки, и шаблон `.env.example` не должен
подсказывать старое имя.

## Э2. Языки

ВЫПОЛНЕН 2026-10-27. Как в плане; сверх плана: `LFL-30.1` на моке
«Языков» лежит в `scenario/lookup/language_test.go`, помощник
`languageAllows` в `suite_test.go`; интеграционные тесты команд делят
сборку бинарника (`cmd/lexeme/binary_integration_test.go`).

Тезисы: LFL-30, LFL-114, LFT-17 (сервисная часть; ручка LFT — Э7).
Проверка: `internal/service/languages/languages_test.go` — `LFL-30.1`,
`LFL-30.2`, `LFL-30.4`, `LFL-114.2`, `LFL-114.3`, `LFL-114.5`;
`repository/postgres/integration_test.go` — `LFL-114.1`;
`cmd/lexeme/languages_integration_test.go` — `LFL-114.4`;
`scenario/lookup/lookup_test.go` — язык проверяется первым шагом
(`LFL-30.1` на моке «Языков»); `task spec:coverage` с `SPEC_KNOWN`
ниже.

Допущения: языки базы слов — `wikiDumps` в `di.go` (`es`, `en`),
отдельной настройки нет; на этом этапе язык проверяется дважды —
«Языками» первым шагом и внутри `quota.Check`, дубль снимает Э3.

### Сервис

**Было.** Правило языка внутри `quota.Check` (`quota.go:70–72`,
помощник `unsupported`), список языков — `supportedLanguages` в
`di.go`:

```go
var supportedLanguages = []domain.Language{"es", "en"}
```

**Будет.** Пакет `internal/service/languages`:

```go
// Package languages — правило операции: изучаемый язык
// есть среди языков базы слов, чужой — в учёт (LFL-30,
// LFL-114, LFT-17).
package languages

// Options — языки базы слов из app/di.go.
type Options struct {
    // Supported — основные коды языков выгрузок базы.
    Supported []domain.Language
}

type languages struct {
    log     def.LanguageLog
    options Options
}

// NewLanguages — правило поверх учёта.
func NewLanguages(
    log def.LanguageLog,
    options Options,
) *languages
```

`Check` и `supported` — `hld-lfl.md`, раздел «Языки». Интерфейс
`LanguageService` — в `service.go`; порт `LanguageLog` — в
`ports.go`; `domain/language.go` получает `LanguageCount` из
`hld-lfl.md`. Моки — `task mockery:gen`: `mock_language_service.go`,
`mock_language_log.go`.

**Мотивация.** Язык — правило операции с учётом, общее для двух
сценариев (Д79, Д101).

### Хранилище учёта

**Было.** Нет.

**Будет.** Миграция `migrations/202610270002_unsupported_languages.sql`
— таблица из `hld-lfl.md`, «Хранилище». Файл
`repository/postgres/languages.go`:

```go
var _ service.LanguageLog = (*Repository)(nil)

// Record — одной командой: новая строка с единицей или
// плюс один и last_at; кто просил — нет (LFL-114.3).
func (r *Repository) Record(
    ctx context.Context,
    lang domain.Language,
) error {
    const query = `INSERT INTO unsupported_languages
    (language, requests) VALUES ($1, 1)
ON CONFLICT (language) DO UPDATE
SET requests = unsupported_languages.requests + 1,
    last_at = now()`
    _, err := r.pool.Exec(ctx, query, string(lang))
    if err != nil {
        return internal(
            fmt.Errorf("record language: %w", err),
        )
    }
    return nil
}

// List — учёт целиком по языку. Новая.
func (r *Repository) List(
    ctx context.Context,
) ([]domain.LanguageCount, error)
```

Пакет `memory`: `NewLanguageLog()` — карта в памяти с теми же
методами, для пробы и тестов пакета `app`; тесты сценариев берут мок:
импорт `repository` из `scenario` закрыт правилом слоёв.

**Мотивация.** Учёт — факт для владельца сервера: какой язык и сколько
раз просили (Д101).

### Команда просмотра

**Было.** Нет.

**Будет.** `cmd/lexeme/languages.go`: `lexeme languages` — строка на
язык «код, обращений, первый, последний» (LFL-114.4); в `main.go`
ветка команды; в `app` геттер `LanguageLog(ctx)`. `Taskfile.yml`:

```yaml
languages:
  desc: "Учёт языков, которых нет в базе: task languages"
  dotenv: [deploy/.env]
  cmds:
    - ssh -i "${DEPLOY_SSH_KEY:?}" "${DEPLOY_SSH:?}" \
      docker exec lexeme-server /app/lexeme languages
```

**Мотивация.** Просмотр учёта — задачей на сервере, как ключи
(LFL-114.4).

### Сценарий lookup, сборка, пробник

**Было.** `internal/scenario/lookup/lookup.go`:

```go
func (s *lookupScenario) Lookup(
    ctx context.Context,
    req domain.LookupRequest,
) (domain.LookupResponse, error) {
    err := s.quota.Check(ctx, req)
    if err != nil {
        return rejected(err)
    }
    err = s.quota.Charge(
        ctx, req.Subject, tokenCount(req.Tokens),
    )
    …
```

`scenario.go`: `NewScenario(quota, normalizer, resolver, meanings,
selector, verifier, options)`; `app/probe.go`: `LookupProbe` с
`probeQuota{}`.

**Будет.** Первый шаг — «Языки», остальное как было до Э3:

```go
// Lookup — Ш2: изучаемый язык через «Языки», чужой — в
// учёт; дальше лимиты и шаги как были. Меняется: первый
// шаг.
func (s *lookupScenario) Lookup(
    ctx context.Context,
    req domain.LookupRequest,
) (domain.LookupResponse, error) {
    // Ш2: изучаемый язык есть в базе слов
    err := s.languages.Check(ctx, req.LearningLanguage)
    if err != nil {
        return rejected(err)
    }
    err = s.quota.Check(ctx, req)
    …
```

`NewScenario` получает `languages service.LanguageService` первым
параметром, поле `languages` в структуре. `di.go`: геттеры
`LanguageLog(ctx)` → `PgRepository`, `LanguageService(ctx)` →
`languages.NewLanguages(di.LanguageLog(ctx),
languages.Options{Supported: supportedLanguages})`;
`LookupScenario` передаёт его сценарию. `probe.go`: `LookupProbe` —
`languages.NewLanguages(memory.NewLanguageLog(), …)`.

**Мотивация.** Один экземпляр правила на оба сценария; язык — правило
операции, не лимитов (Д79).

### Тесты и покрытие

**Было.** `quota_test.go` — строки `LFL-30.x`; `suite_test.go:52` —
`NewScenario(quota, …)` без языков; `app/wordbase_test.go:93–106` —
сборка сценария в тесте пакета `app`; `Taskfile.yml`:

```yaml
- SPEC_FILE=docs/handbook/specs/\
  lexemes-from-lemma/spec-v1.md \
  ./scripts/spec-coverage.sh
```

`scripts/spec-coverage.sh:89–91` — адрес в тестах, которого нет в
спеке, даёт код 1.

**Будет.** Строки `LFL-30.x` переезжают из `quota_test.go` в
`languages_test.go`; `suite_test.go` и `llm_test.go` — мок «Языков»
в сборке сценария; `wordbase_test.go` — `languages.NewLanguages(
memory.NewLanguageLog(), …)`. В `scripts/spec-coverage.sh` —
переменная `SPEC_KNOWN`: список спек, чьи адреса тоже считаются
существующими; она участвует только в проверке «адреса нет в спеке»,
покрытие считается по `SPEC_FILE`. В Taskfile у LFL две строки:

```yaml
- SPEC_FILE=…/lexemes-from-lemma/spec-v1.md \
  SPEC_KNOWN=…/lexemes-from-lemma/spec.md \
  ./scripts/spec-coverage.sh
- SPEC_FILE=…/lexemes-from-lemma/spec.md \
  SPEC_KNOWN=…/lexemes-from-lemma/spec-v1.md \
  ./scripts/spec-coverage.sh
```

Первая — ворота «реализовано» по v1, вторая показывает покрытие
LFL-30 и LFL-114; вторая уходит, когда lookup перейдёт на v2.

**Мотивация.** В v1 нет LFL-114, в v2 нет адресов тестов v1 (LFL-04,
24, 28, 38, 49): без `SPEC_KNOWN` любой прогон падает на чужих
адресах.

## Э3. Лимиты — прокладка

ВЫПОЛНЕН 2026-10-27. Как в плане; сверх плана: расход аккаунта
«исчерпанный» в HTTP-тесте пишется адаптером `postgres.ChargeModel`,
не голым SQL (та же база, тот же день по UTC); `LIM-110.1`,
`LIM-110.3` и `LIM-79.3` есть и в `quota_test.go`; у подделки нейронки
счётчик всех вызовов `llmCalls` для «0 вызовов» в `LIM-96.1`.

Тезисы: LIM-110, LIM-111, LIM-112, LIM-79, LIM-96. Проверка:
`quota_test.go` — `LIM-96.1`, `LIM-96.2`, `LIM-96.3`, `LIM-96.4`,
`LIM-96.7` на `Begin`, `LIM-79.1`, `LIM-79.2`, `LIM-79.3`, `LIM-79.4`
на `Finish`; `lookup_test.go` — `LIM-110.1` (`Begin` с
`WorkWordLexemes`), `LIM-111.2`, `LIM-112.1`, `LIM-112.2` (нейронка не
ответила — `Finish` с нулём), `LIM-112.3` и `LIM-96.5` (отказ —
`Finish` не зовётся), `LIM-112.4` (отмена клиента после ответа —
расход записан); `llm_test.go` — `LIM-96.6` (лимит раньше кеша);
`postgres/integration_test.go` — `LIM-79.1`, `LIM-79.4` через
`ChargeModel`; `http_integration_test.go` — `LIM-96.1` и
переписанный `LFL-27.2 LFL-27.3`; `task spec:coverage` для LIM — по
`spec.md`.

Допущение: LIM-29 (токены запроса, потолок сервера) снята вместе со
спекой v1 — столбец `usage.tokens` остаётся, не пишется.

### Сервис Quota

**Было.** `internal/service/service.go`, `quota/quota.go`:

```go
type QuotaService interface {
    Check(
        ctx context.Context,
        req domain.LookupRequest,
    ) error
    Charge(
        ctx context.Context,
        subject domain.Subject,
        tokens int,
    ) error
    Refund(
        ctx context.Context,
        subject domain.Subject,
        tokens int,
    ) error
    ChargeModel(
        ctx context.Context,
        subject domain.Subject,
        usage domain.ModelUsage,
    ) error
}

type Options struct {
    ServerDaily        int
    SupportedLanguages []domain.Language
    ModelDaily         int
}

func (q *quota) Check(
    ctx context.Context,
    req domain.LookupRequest,
) error {
    today := q.clock.Now()
    tokens := len(req.Tokens)

    spent, err := q.usage.Spent(ctx, req.Subject, today)
    …
    if unsupported(
        req.LearningLanguage,
        q.options.SupportedLanguages,
    ) {
        return domain.ErrUnsupportedLanguage
    }
    return nil
}
```

**Будет.** Интерфейс `QuotaService` с `Begin` и `Finish`, вид работы
`domain.Work` с `WorkTextLemmas` и `WorkWordLexemes`, `Options` только
с `ModelDaily`, тела `Begin` и `Finish` — `hld-lim.md`, раздел 2.
Помощники `wouldExceed` и `unsupported` сняты, `exceeded` и `internal`
остаются. Файл `domain/work.go`:

```go
// Work — вид работы для лимитов (LIM-111). Новый.
type Work string

const (
    // WorkTextLemmas — словарные формы текста, LFT.
    WorkTextLemmas Work = "text_lemmas"
    // WorkWordLexemes — значения слова, LFL.
    WorkWordLexemes Work = "word_lexemes"
)
```

**Мотивация.** Контракт двух вызовов без политики (Д106, Д78): позже
политика ляжет внутрь сервиса, сценарии не тронутся.

### Порт «Расход»

**Было.** `ports.go`, `UsageRepository`: `Spent`, `SpentTotal`,
`ModelSpent`, `Charge`, `Refund`, `ChargeModel`.

**Будет.** Только `ModelSpent` и `ChargeModel` с аккаунтом
(`hld-lim.md`). В `postgres/usage.go` и `memory/usage.go` методы
`Spent`, `SpentTotal`, `Charge`, `Refund` удалены, константа
`colTokens` снята; `counter` остаётся ради `ModelSpent`.

**Мотивация.** Порт — ровно те методы, которые нужны сервису; мёртвые
методы не держатся.

### Сценарий lookup, сборка, пробник

**Было.** `lookup.go` после Э2: «Языки», затем `quota.Check`,
`quota.Charge`, `refund` при 503, `chargeModel` после отбора:

```go
    picks, err := s.selector.Select(
        ctx,
        promptOf(
            req, found, s.normalizer.Text(req.Tokens),
        ),
    )
    if err != nil {
        s.refund(ctx, req)
        return rejected(err)
    }
    s.chargeModel(ctx, req.Subject, picks.Usage)
```

`di.go:46–50` — константы `responseBudget`, `serverDailyLimit`;
`QuotaService` с тремя настройками.

**Будет.** Начало работы вместо `Check` и `Charge`, конец работы сразу
после нейронки, списаний и отката нет:

```go
// Lookup — Ш2–Ш8: язык, начало работы, нормализация,
// слова в источнике, один отбор нейронкой, конец работы,
// проверка, сборка. Меняется: лимиты — прокладка,
// списаний и отката нет.
func (s *lookupScenario) Lookup(
    ctx context.Context,
    req domain.LookupRequest,
) (domain.LookupResponse, error) {
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
    ctx, finish := withinBudget(ctx, s.options.Budget)
    defer finish()

    words := s.normalizer.Words(
        req.Tokens, s.options.Thresholds,
    )
    if noWords(words) {
        return emptyResponse(), nil
    }
    found := s.lookupEach(ctx, words, languagesOf(req))

    picks, err := s.selector.Select(
        ctx,
        promptOf(
            req, found, s.normalizer.Text(req.Tokens),
        ),
    )
    // LIM Ш2: конец работы — расход нейронки, 0 — не
    // ответила
    s.finishWork(ctx, req.Account, picks.Usage)
    if err != nil {
        return rejected(err)
    }
    picks = s.verifier.Verify(
        picks, found, req.Components,
        req.TranslationLanguage,
    )
    logCorrections(ctx, picks.Corrections)
    return response(
        assembled(found, picks, req.TranslationLanguage),
        picks.Recommendations,
    ), nil
}

// finishWork — LIM Ш2: расход вызова в счётчик аккаунта на
// контексте без отмены: клиент мог отменить запрос после
// ответа нейронки, работа доводится (LIM-112.4). Ответ
// уже решён: сбой хранилища — в лог. Меняется: бывший
// chargeModel.
func (s *lookupScenario) finishWork(
    ctx context.Context,
    account domain.Account,
    usage domain.ModelUsage,
) {
    err := s.quota.Finish(
        context.WithoutCancel(ctx), account, usage,
    )
    if err != nil {
        logger.Error(
            ctx, "model usage not charged",
            zap.String("account", account.Name),
            zap.Error(err),
        )
    }
}
```

`refund`, `tokenCount` сняты. `di.go`: `quota.Options{ModelDaily:
config.AppConfig().LLM.AccountDaily()}`, константа `serverDailyLimit`
снята. `app/probe.go`: `probeQuota` с `Begin` и `Finish`, оба `nil`.

**Мотивация.** Операция зовёт два вызова лимитов (Д78); расход
пишется и после отмены клиента — иначе `ChargeModel` падает на
отменённом контексте и счётчик нейронки врёт (LIM-79).

### Ручка: текст отказа 429

**Было.** `internal/api/words/v1/lookup.go`:

```go
func rateLimited() *wordsv1.LookupWordsTooManyRequests {
    return &wordsv1.LookupWordsTooManyRequests{
        Code:    wordsv1.ErrorCodeRateLimited,
        Message: "daily limit",
    }
}
```

**Будет.** `Message: "daily llm token limit of account exceeded"` —
как в `contract.md`; комментарий: лимит токенов нейронки (LIM-96),
лимит работ пока не отказывает (LIM-110).

**Мотивация.** Единственная причина 429 теперь одна; живой пример
контракта и ответ сервера совпадают.

### Тесты и покрытие

**Было.** Старые `Check`, `Charge`, `Refund`, `Spent`, `SpentTotal`
зовут: `quota_test.go` (строки LIM-29.x, LIM-96.x),
`lookup_test.go:98–109` (`LIM-29.5`, `LIM-29.6`), `llm_test.go`
(моки `Check`/`Charge`/`Refund`/`ChargeModel`, имена «LFL-99.1
LIM-29.7 LFL-99.3», «LIM-96.5 LIM-96.6»), `suite_test.go:57–60`
(`quotaAllows`), `memory/memory_test.go:30–73` (`Charge`, `Spent`,
`SpentTotal`, `Refund` напрямую), `postgres/integration_test.go:81–131`
(«LIM-29 usage counts…», «charge for an unknown subject», «LIM-79.1
LIM-79.4» через `Charge`/`Spent`, «LIM-29.7 refund…»),
`app/wordbase_test.go:93–106` (мок `Check`/`Charge`),
`http_integration_test.go:474` (`LIM-29.1`) и `:497–501`
(`LFL-27.2 LFL-27.3` получают 429 через `daily_limit = 1`).
`Taskfile.yml`:

```yaml
- SPEC_FILE=docs/handbook/specs/limits/spec-v1.md \
  SPEC_PREFIX=LIM ./scripts/spec-coverage.sh
```

**Будет.**
- `quota_test.go`: `LIM-96.1`, `LIM-96.2`, `LIM-96.3`, `LIM-96.4`,
  `LIM-96.7` на `Begin`; `LIM-79.1`, `LIM-79.2`, `LIM-79.3`,
  `LIM-79.4` на `Finish`; строки LIM-29 сняты.
- `lookup_test.go`: `LIM-29.5`, `LIM-29.6` сняты; `LIM-110.1`,
  `LIM-111.2`, `LIM-112.1`, `LIM-112.2`, `LIM-112.3` и `LIM-96.5`
  (`Begin` отказал → `ErrRateLimited` наверх, `Finish` не зовётся),
  `LIM-112.4` — мок отбора отменяет родительский контекст и отдаёт
  расход, мок `Finish` видит `ctx.Err() == nil` и расход.
- `llm_test.go`: моки на `Begin`/`Finish`; адреса LFL-99.x и LIM-79.x
  остаются, `LIM-29.7` снят, `LIM-96.6` остаётся здесь.
- `suite_test.go`: `quotaAllows` на `Begin`/`Finish`.
- `memory_test.go`: тесты `Spent`/`SpentTotal`/`Refund` сняты,
  раздельность счётчиков — через `ChargeModel`/`ModelSpent`.
- `postgres/integration_test.go`: LIM-29 и `LIM-29.7` сняты; «unknown
  account» и `LIM-79.1 LIM-79.4` — через `ChargeModel`.
- `wordbase_test.go`: `Begin`/`Finish` в моке.
- `http_integration_test.go`: `LIM-29.1` снят; в `startServer` —
  `LLM_ACCOUNT_DAILY=100000`; отдельный аккаунт «исчерпанный», у него
  строка `usage` с `model_tokens = 100000` на сегодня через SQL
  (`pgxpool.New(ctx, config.AppConfig().Pg.URI())` после
  `startServer`); на нём `LIM-96.1` и `LFL-27.2 LFL-27.3`: верный ключ,
  исчерпанный лимит нейронки, 2001 токен → 413, кривое дерево → 400.
  «Тестер» до предела не доходит: подделка даёт 120 и 1 230 токенов
  на вызов.
- Taskfile — `limits/spec.md`.

**Мотивация.** Покрытие считается по спеке v2; тесты на снятые тезисы
— ложная правда; все файлы, где звались старые методы, названы —
иначе `task test` не соберётся.

## Э4. Домен LFT и «Проверка пар»

ВЫПОЛНЕН 2026-10-27. Как в плане; сверх плана в `pairs_test.go` —
`LFT-13.3` и тест допущения «не слово с формой — не слово».

Тезисы: LFT-11, LFT-08 (проверка), LFT-13 (без пары). Проверка:
`internal/service/pairs/pairs_test.go` — `LFT-11.1`, `LFT-11.2`,
`LFT-11.4`, `LFT-11.5`, `LFT-11.6`, `LFT-08.3`, `LFT-08.4`, `LFT-13.2`.

Допущение: пара «не слово» с непустой формой принимается как «не
слово» — форма у обрывка не читается.

### Домен

**Было.** Нет; `domain.Token` и `Language` есть.

**Будет.** Файл `internal/domain/lemmas.go` — типы `TokensRequest`,
`NumberedToken`, `ExtractionPrompt`, `Extraction`, `Pair`, `ListKind` с
константами, `Checked`, `RejectedPair`, `Lists`, `LemmasResponse` —
целиком из `hld-lft.md`, раздел «Домен».

**Мотивация.** Понятия операции отдельным файлом, как `lookup.go` у
значений.

### «Проверка пар»

**Было.** Нет.

**Будет.** В `service.go` — интерфейс `PairChecker` из `hld-lft.md`;
пакет `internal/service/pairs`: `NewChecker()`, `Check` и помощники
`invalid`, `rejection`, `mark`, `unpaired` — `hld-lft.md`, «Проверка
пар». Причины `invalid` словами: `no tokens`, `token out of range`,
`tokens not adjacent`, `empty lemma`. Мок — `task mockery:gen`.

**Мотивация.** Чистая логика без хранилищ в своём пакете — подмена в
тестах сценария и ясный адрес тезиса LFT-11.

## Э5. Клиент нейронки и «Извлечение»

ВЫПОЛНЕН 2026-10-27. Как в плане; сверх плана: тесты клиента — в
`extract_test.go` (не в `client_test.go`), золотой файл входа
`testdata/extract-textbook.json`; ответ с `finish_reason: length`
негоден и когда разобрался (LFT-15.5); у пары список не из четырёх —
причина `unknown list` в проверке пар; `logExtraction` пишет сырой
ответ только при сбое разбора.

Тезисы: LFT-05, LFT-19, LFT-16, LFT-15 (кроме бюджета), LIM-79.5.
Проверка: `internal/clients/openrouter/client_test.go` на поддельном
провайдере — `LFT-05.1`, `LFT-05.2`, `LFT-19.1`, `LFT-16.1`, `LFT-16.2`,
`LFT-15.2`, `LFT-15.5`, `LIM-79.5`; `prompt_test.go` — вход модели по
абзацу учебника; `internal/service/extraction/extraction_test.go` —
`LFT-15.1`, `LFT-15.2`, `LIM-79.5`.

Допущение: потолок ответа извлечения — 16 384 токена, предел модели;
число подбирает пробник (Э8).

### Порт и выключенный клиент

**Было.** `ports.go`, `LLMClient`: `Complete`, `Translate`;
`openrouter/disabled.go` отвечает на оба ошибкой «выключена».

**Будет.** `LLMClient` получает `Extract` (`hld-lft.md`, «Клиент
нейронки»); `disabled.go` — `Extract` с той же ошибкой; моки — `task
mockery:gen`. Реализация у боевого клиента — в этом же этапе ниже:
порт и клиент меняются вместе, иначе `var _ service.LLMClient =
(*client)(nil)` не соберётся.

**Мотивация.** Один порт нейронки на две операции.

### Настройки и инструкция

**Было.** `openrouter.Options` с `SystemPrompt`; `llm.go`:

```go
// PromptFile — файл с инструкцией нейронке вместо
// встроенной: промпт тюнится на стенде без выпуска;
// пусто — встроенная.
PromptFile string `env:"LLM_PROMPT_FILE"`
```

`client.go:206–214` — `complete(ctx, messages, schema, maxTokens)`
шлёт `max_tokens` как есть; `di.go:355–361` — `NewClient` с
`SystemPrompt: promptFrom(ctx, cfg.LLM.PromptFile())`;
`promptFrom:369–381` пишет в лог «llm prompt: file/built-in».

**Будет.** Конфиг `llm.go`, тег в одну строку, как у соседей:

```go
// ExtractPromptFile — файл с инструкцией извлечения
// вместо встроенной (LFT Ш3); пусто — встроенная. Тег
// env:"LLM_EXTRACT_PROMPT_FILE".
ExtractPromptFile string
// ExtractMaxTokens — потолок ответа извлечения в токенах
// модели: пары на каждый токен страницы. Тег
// env:"LLM_EXTRACT_MAX_TOKENS" envDefault:"16384".
ExtractMaxTokens int
```

Методы `ExtractPromptFile()` и `ExtractMaxTokens()` в `llmConfig` и в
интерфейсе `config/interfaces.go`. `openrouter.Options` — поля
`ExtractionPrompt string` и `ExtractMaxTokens int`. `di.go`:

```go
di.llmClient = openrouter.NewClient(openrouter.Options{
    BaseURL:   cfg.LLM.ProviderURL(),
    APIKey:    cfg.LLM.APIKey(),
    Model:     cfg.LLM.Model(),
    UserAgent: cfg.Source.UserAgent(),
    SystemPrompt: promptFrom(
        ctx, "selection", cfg.LLM.PromptFile(),
    ),
    ExtractionPrompt: promptFrom(
        ctx, "extraction", cfg.LLM.ExtractPromptFile(),
    ),
    ExtractMaxTokens: cfg.LLM.ExtractMaxTokens(),
})
```

`promptFrom(ctx, name, path)` — имя инструкции в строке лога:
инструкций две. Встроенная инструкция — `extract_prompt.go`: собрать
текст из токенов, исправляя только ошибки распознавания; перевести
на язык перевода; пара на каждое вхождение слова по номерам токенов;
форма как в словаре, род как в словаре (Д107), строчные кроме имён;
обрывок — «не слово»; пример парой сообщений по первому предложению
снимка из `contract.md`. `deploy/.env.example` и
`deploy/llm/README.md` — обе переменные с пояснением.

**Мотивация.** Инструкция тюнится файлом на сервере без тега (правило
релизов); потолок — своя настройка и своё поле клиента, у извлечения
ответ длиннее отбора; без поля в `Options` в запрос ушёл бы
`max_tokens: 0`.

### Вызов, схема, разбор

**Было.** `client.go`: `Complete` → `completeOnce` → `complete(ctx,
messages, schema, maxTokens)`; `schema.go`: `answerSchema(prompt)`;
`answer.go`: `parseAnswer`.

**Будет.** Файлы `extract.go`, `extract_schema.go`, `extract_answer.go`:
`Extract`, `extractionSchema`, `parseExtraction` — `hld-lft.md`,
«Клиент нейронки»; `complete` переиспользуется как есть с
`c.options.ExtractMaxTokens`. Схема: `text`, `translation`, `pairs` в
этом порядке (LFT-19), у пары `tokens` — массив целых от 1 до числа
токенов, `lemma` — строка или null, `list` — одно из четырёх слов. Лог
вызова — `logExtraction`: модель, номер запуска, токенов, пар, время,
ошибка; на `debug` — вход целиком (LFT-16).

**Мотивация.** Форму ответа держит схема по числу токенов (целые, не
перечисление — у провайдера предел на число значений); разбор падает
только на целом ответе, кривую пару снимает проверка.

### «Извлечение»

**Было.** Нет.

**Будет.** В `service.go` — интерфейс `Extractor` из `hld-lft.md`;
пакет `internal/service/extraction`: `NewExtractor(client
def.LLMClient)`, `Extract` — `hld-lft.md`, «Извлечение». Мок — `task
mockery:gen`.

**Мотивация.** Исходы вызова нейронки — в одном месте: 503 и расход
решаются здесь, сценарий их не разбирает.

## Э6. Сценарий лемм из текста

ВЫПОЛНЕН 2026-10-27. Как в плане; сверх плана: в тестах сценария
«Проверка пар» настоящая (`pairs.NewChecker()`), не мок — виден
результат сборки списков; `LFT-16.1` без номера запуска в строке:
тестовый логгер полей контекста не видит, `run_id` кладёт боевой.

Тезисы: LFT-01, LFT-03.2, LFT-08, LFT-12, LFT-13, LFT-14, LFT-15.1,
LFT-15.4, LFT-16, LFT-17, LIM-110.2, LIM-110.3, LIM-111.1, LIM-112.
Проверка: `internal/scenario/lemmas/lemmas_test.go` с моками —
`LFT-01.1`, `LFT-01.2`, `LFT-03.2`, `LFT-08.1`, `LFT-08.2`, `LFT-08.4`,
`LFT-08.5`, `LFT-12.1` (пары во входе перемешаны), `LFT-12.2`,
`LFT-12.3`, `LFT-13.1`, `LFT-13.2`, `LFT-13.3`, `LFT-14.1`, `LFT-14.2`,
`LFT-15.1`, `LFT-15.4`, `LFT-16.1`, `LFT-17.1`, `LFT-17.2`, `LIM-110.2`
(`Begin` отказал → `ErrRateLimited`, извлечения нет), `LIM-110.3`,
`LIM-111.1`, `LIM-112.1`, `LIM-112.2`, `LIM-112.3`, `LIM-112.4` (отмена
клиента после ответа — расход записан), `LIM-79.5`; фикстуры — абзац
учебника и `es_dense_screenshot` из `testdata/ocr/`.

### Интерфейс и сценарий

**Было.** `scenario/scenario.go` — только `LookupScenario`.

**Будет.** `LemmasScenario` с `FromTokens` в `scenario.go`; пакет
`internal/scenario/lemmas`: `Options{Budget}`, структура с
`languages`, `quota`, `extractor`, `pairs`, `NewScenario(…)`;
`FromTokens` и помощники `numbered`, `promptOf`, `logExtraction`,
`emptyLists`, `response`, `withinBudget`, `rejected` — `hld-lft.md`,
«Сценарий». Два помощника точнее, чем в HLD:

```go
// finishWork — LIM Ш2: расход вызова в счётчик аккаунта на
// контексте без отмены: клиент мог отменить запрос после
// ответа нейронки, работа доводится (LIM-112.4). Ответ
// уже решён: сбой хранилища — в лог, наверх не идёт.
// Новая.
func (s *lemmasScenario) finishWork(
    ctx context.Context,
    account domain.Account,
    usage domain.ModelUsage,
)

// listed — Ш4: принятые пары по первому номеру токена,
// устойчиво — две пары одного токена в порядке нейронки;
// одинаковые формы в одном списке — один пункт на месте
// первого вхождения, одна форма в двух списках — по
// пункту в каждом; «не разобрано» — токены без принятой
// пары в порядке текста. Порядок держит сервер, не
// нейронка: схема порядок пар не обещает (LFT-12). Новая.
func listed(checked domain.Checked) domain.Lists
```

**Мотивация.** Своя операция — свой сценарий (Д48); `lookup` не
растёт; порядок текста и расход при отмене — обязанности сервера.

## Э7. Контракт, генерация, ручка, сборка

ВЫПОЛНЕН 2026-10-27. Как в плане; сверх плана: тесты операции — в
`TestHTTP` на том же сервере (второй подъём базы не нужен), помощник
`postTo` с путём; `LFT-04.2` и `LFT-18.2` тоже в HTTP-тесте; LFT в
`task spec:coverage` добавлен здесь, не в Э8.

Тезисы: LFT-02 (сторона сервера), LFT-03, LFT-04, LFT-18, LFT-17.1,
LFT-15.1, LFT-14. Проверка: `internal/converters/lemmas_test.go` на
примерах `contract.md` — `LFT-02.1`, `LFT-02.2`, `LFT-14.1`, `LFT-14.2`;
`internal/app/http_integration_test.go` — `LFT-03.1`, `LFT-03.2`,
`LFT-03.3` и `LFT-18.3` (токен с `"text": ""` → 400), `LFT-03.4`,
`LFT-04.1`, `LFT-04.2`, `LFT-18.1`, `LFT-18.2`, `LFT-17.1`, `LFT-15.1`,
`LIM-96.1` для новой операции на аккаунте «исчерпанный» (Э3);
`task gen` без ручных правок в `pkg/openapi`.

### Контракт

**Было.** `api/openapi.yaml` 0.3.0: один путь `/v1/words/lookup`;
`TokenQuery.text` без `minLength`; описание `lookupWords` — порядок
«ключ, размер, схема, лимит аккаунта, потолок сервера, лимит
нейронки, изучаемый язык», у 503 — «с лимитов токенов ничего не
списано (LIM-29.7)».

**Будет.** Версия 0.4.0. Путь `/v1/lemmas/from-tokens`, `operationId:
lemmasFromTokens`, параметр `X-Run-Id`, тело — схема `TokensRequest`
(`learningLanguage`, `translationLanguage`, `tokens` — `TokenQuery`,
`maxItems: 2000`, `required` все три), ответ 200 — схема
`LemmasResponse` (`text`, `translation`, `lexical`, `proper`,
`function`, `unparsed` — массивы строк, все `required`), отказы 400,
401, 413, 429, 503 с `Error`; описания — по спеке LFT и `contract.md`.
У `TokenQuery.text` — `minLength: 1` для обеих операций (LFT-03.3,
LFT-18.3): устройство пустых токенов не шлёт — `recognize` их
отбрасывает, диалог карточки слова шлёт текст слова, — так что у
прод-сборки ничего не меняется. У `lookupWords` описание: порядок
«ключ, размер, схема, изучаемый язык, лимит нейронки», 429 — только
лимит нейронки (LIM-96), у 503 снято про списание; остальное у
`lookup` — как в 0.3.0 до этапа 2.

**Мотивация.** Один контракт на обе операции; имена схем — как у
типов в HLD (`wordsv1.TokensRequest`, `wordsv1.LemmasResponse`); у
снимка размер считает схема (LFT-18).

### Генерация и конвертеры

**Было.** `pkg/openapi/words/v1` по 0.3.0; `converters/lookup.go`:
`RequestToDomain`, `TokensToDomain`, `ResponseToAPI`; языки
переводятся как `domain.Language(req.LearningLanguage)`.

**Будет.** `task gen` → `wordsv1.TokensRequest`, `LemmasResponse`,
`LemmasFromTokensParams`, `LemmasFromTokensRes` и типы отказов
операции. Файл `converters/lemmas.go`:

```go
// TokensRequestToDomain — запрос контракта в домен:
// аккаунт, языки, токены как пришли. Новая.
func TokensRequestToDomain(
    req *wordsv1.TokensRequest,
    account domain.Account,
) domain.TokensRequest {
    return domain.TokensRequest{
        Account: account,
        LearningLanguage: domain.Language(
            req.LearningLanguage,
        ),
        TranslationLanguage: domain.Language(
            req.TranslationLanguage,
        ),
        Tokens: TokensToDomain(req.Tokens),
    }
}

// LemmasResponseToAPI — ответ домена в контракт: текст,
// перевод, четыре списка строк; пустой список — `[]`, не
// null. Новая.
func LemmasResponseToAPI(
    resp domain.LemmasResponse,
) *wordsv1.LemmasResponse
```

**Мотивация.** Конвертеры — единственное место, где контракт встречает
домен (Д57).

### Ручка

**Было.** `api/words/v1/api.go`:

```go
type wordsHandler struct {
    words scenario.LookupScenario
    auth  service.AuthService
}

func NewHandler(
    words scenario.LookupScenario,
    auth service.AuthService,
) *wordsHandler
```

**Будет.** Структура и `NewHandler(lexemes, lemmas, auth)` —
`hld-lft.md`, «Ручка»; файл `lemmas.go` с `LemmasFromTokens`,
`lemmasLLMUnavailable`, `lemmasRateLimited`,
`lemmasUnsupportedLanguage`, `logTokensRequest` — оттуда же, запрос —
`*wordsv1.TokensRequest`. `errors.go`, `NotFound`, `ErrorHandler` не
меняются: `tooManyTokens` ловит поле `tokens` обеих операций.

**Мотивация.** Одна ручка на контракт, по методу на операцию;
типизированные отказы — свои у каждой операции по генератору.

### Сборка и интеграционный тест

**Было.** `di.go`: `WordsServer` собирает `NewHandler(LookupScenario,
AuthService)`. `http_integration_test.go:139–237` — `fakeLLM`
различает только схему `translation`, остальное считает отбором.

**Будет.** Геттеры `Extractor(ctx)` → `extraction.NewExtractor(
di.LLMClient(ctx))`, `PairChecker(ctx)` → `pairs.NewChecker()`,
`LemmasScenario(ctx)` → `lemmas.NewScenario(di.LanguageService(ctx),
di.QuotaService(ctx), di.Extractor(ctx), di.PairChecker(ctx),
lemmas.Options{Budget: responseBudget})`; `WordsServer` — три
зависимости в ручку. В `fakeLLM` — ветка по `json_schema.name` схемы
извлечения: текст из токенов, перевод, пара на каждый токен входа.

**Мотивация.** Ленивые геттеры, как у всего контейнера; бюджет общий
с `lookup`; без ветки подделки новые тесты получали бы 503.

## Э8. Пробник, выкатка, доки

ВЫПОЛНЕН 2026-10-27 до тега: пробник `lexeme probe text` написан
(`cmd/lexeme/probe_text.go`, тесты `probe_text_test.go`), прогнан на
сервере — инструкция v1 и v2 по три раза на учебнике и по разу на
плотной странице, итог и открытые промахи — `llm-journal.md`,
«Извлечение»; встроенная инструкция — v2. Доки: CHANGELOG LFT, LIM,
ACC, LFL — «v0.1.32»; `deploy.md` — переменные, «Леммы из текста»,
«Учёт языков», откат; `architecture.md` «Доступ»; статусы
«реализовано» — LFT ×14, LIM ×5, LFL-30, LFL-114. Тег v0.1.32 — по
решению пользователя после коммита.

Тезисы: LFT-06, LFT-07, LFT-09, LFT-10, LFT-20 — контрольный набор,
пробником на модели; LFT-04 — число по прогонам. Проверка:
`cmd/lexeme/probe_test.go` — разбор аргументов; прогон
`lexeme probe text` по абзацу учебника и `es_dense_screenshot` против
`control_set.md` — руками, итог в `llm-journal.md`.

### Пробник

**Было.** `cmd/lexeme/probe.go` — `probe source`, `probe llm`;
`app/probe.go` — `LookupProbe`.

**Будет.** `lexeme probe text --file <testdata/ocr/*.lines.txt> |
--text "<текст>" [--learning es] [--translation ru] [--runs N]`: токены
из файла по строкам и пробелам как у распознавания; печатает текст,
перевод, четыре списка и отброшенные пары с причиной; `app/probe.go`
получает `LemmasProbe(ctx)`: сценарий лемм с `probeQuota`, «Языки» с
учётом в памяти, боевой клиент.

**Мотивация.** Инструкция и потолок ответа проверяются на модели до
тега (правило: проверять на модели до деплоя).

### Выкатка, откат, доки

**Было.** Прод v0.1.31; образ `deploy/docker/Dockerfile` несёт бинарь
и миграции, вниз goose не катает; `deploy.md` с `LLM_SUBJECT_DAILY`;
`architecture.md:85–92` — язык, потолок, списание и откат числятся у
`Quota`; CHANGELOG спек без тега.

**Будет.** Порядок: `task test`, `task test:integration`, `task lint`,
`task spec:coverage` (LFT добавлен в Taskfile: `SPEC_FILE=…/lemmas-
from-text/spec.md SPEC_PREFIX=LFT`); пробник на модели; на сервере в
`deploy/.env` — `LLM_ACCOUNT_DAILY`, `LLM_EXTRACT_PROMPT_FILE` с файлом
инструкции; коммит с доками, тег v0.1.32, CI деплоит, миграции
накатываются при подъёме. Откат: после миграции образ v0.1.31 ищет
`subjects`, и хук ключа падает не по ключу — каждый запрос старой
сборки получает 500; поэтому откат — Down двух миграций руками через
`docker exec lexeme-pg psql` в обратном порядке, их строки удалить из
`goose_db_version`, потом поднять образ v0.1.31. Доки: CHANGELOG LFT,
LIM, ACC, LFL — запись «v0.1.32»; `deploy.md` — переменные, команда
`languages`, откат; `architecture.md`, раздел «Доступ»: язык — сервис
«Языки», у `Quota` — `Begin` и `Finish`. Тезисы LFT со статусом
«черновик» (LFT-04) и «принято» → «реализовано» по покрытию.

**Мотивация.** Бэкенд уезжает раньше мобилки (Д116): старая сборка
операцию не зовёт, ничего не ломается; путь назад записан до того,
как понадобится.

## Э9. Словарная форма в одном списке (Д121)

ВЫПОЛНЕН 2026-10-28, отдельным патчем v0.1.36 до этапа 2. Как в
плане; разошлось: помощника `emptyLists` нет — имя занято ответом на
пустые токены, литерал на месте; обрывок (`ListNone`) форму не
занимает; тест `LFT-12_3` — две пары одного токена 24, в абзаце
учебника «have» один раз; сверх плана строка LFT-12.4 («Study» (1) и
«study» (46) в разных списках) с тестом и тест без адреса на
отсутствие строки лога при повторе в одном списке. На проде повтора
формы не было — случай из ревью спеки (Д109).

Независим от остального этапа 2: правит операцию `lemmas/from-tokens`.

Тезисы: LFT-12 (строка 12.3 заново, 12.1 и 12.2 как были), LFT-16
(строка лога). Проверка: `internal/scenario/lemmas/lemmas_test.go` —
`LFT-12_3` переписан: «have» в смысловые от первого токена и в
служебные от второго → один пункт «have» в смысловых, в служебных
нет, в логе строка `lemma in two lists`; `LFT-12_1`, `LFT-12_2`,
`LFT-08_1 LFT-12_2` как были. После этапа: `task test`, `task lint`
зелёные; статус LFT-12 → «реализовано».

### Сборка списков

**Было.** `scenario/lemmas/lemmas.go:93–121`:

```go
// listed — Ш4: принятые пары по первому номеру токена,
// устойчиво — две пары одного токена в порядке нейронки;
// одинаковые формы в одном списке — один пункт на месте
// первого вхождения, одна форма в двух списках — по
// пункту в каждом; «не разобрано» — токены без принятой
// пары в порядке текста. …
func listed(checked domain.Checked) domain.Lists {
    pairs := slices.Clone(checked.Pairs)
    slices.SortStableFunc(
        pairs,
        func(a, b domain.Pair) int {
            return a.Tokens[0] - b.Tokens[0]
        },
    )
    lists := domain.Lists{
        Lexical: []string{}, Proper: []string{},
        Function: []string{}, Unparsed: []string{},
    }
    for _, pair := range pairs {
        switch pair.List {
        case domain.ListLexical:
            lists.Lexical = appendOnce(
                lists.Lexical, pair.Lemma,
            )
        case domain.ListProper:
            lists.Proper = appendOnce(
                lists.Proper, pair.Lemma,
            )
        case domain.ListFunction:
            lists.Function = appendOnce(
                lists.Function, pair.Lemma,
            )
        case domain.ListNone:
        default:
        }
    }
    …
}
```

`lemmas_test.go:490` — `LFT-12.3 have given as lexical and as
function → an item have in both lists`.

**Будет.** Форма помнится по первому вхождению: пара с формой, которая
уже стоит в любом списке, пункта не даёт; если список другой — строка
лога с номером запуска, формой, списком первого вхождения и списком
повтора. `listed` получает контекст ради лога:

```go
// listed — Ш4: принятые пары по первому номеру токена,
// устойчиво; одинаковые формы — один пункт на месте
// первого вхождения, в списке первого вхождения; та же
// форма в другом списке — повтор снят, в лог «lemma in
// two lists» (LFT-12.3, Д121); «не разобрано» — токены без
// принятой пары в порядке текста. Меняется: один список
// на форму.
func listed(
    ctx context.Context,
    checked domain.Checked,
) domain.Lists {
    pairs := sortedByToken(checked.Pairs)
    lists := emptyLists()
    seen := map[string]domain.List{}
    for _, pair := range pairs {
        if first, ok := seen[pair.Lemma]; ok {
            if first != pair.List {
                logTwoLists(ctx, pair, first)
            }
            continue
        }
        seen[pair.Lemma] = pair.List
        lists = withPair(lists, pair)
    }
    for _, token := range checked.Unparsed {
        lists.Unparsed = append(
            lists.Unparsed, token.Text,
        )
    }
    return lists
}

// logTwoLists — одна форма в двух списках: провал
// извлечения, строка Warn «lemma in two lists» с формой и
// обоими списками (LFT-16). Новая.
func logTwoLists(
    ctx context.Context,
    pair domain.Pair,
    first domain.List,
)
```

`sortedByToken`, `emptyLists`, `withPair` — помощники из тела
нынешней `listed`, `appendOnce` снята. Заметка `pairs` журнала и
`logExtraction` — как были.

**Мотивация.** Д121: одно слово в двух списках — ошибка нейронки, а
не два пункта; в оценке списков повтор — лишний пункт (RUN).

## Э10. Домен, контракт, порты, конвертеры LFL v2

ВЫПОЛНЕН 2026-10-28. Как в плане; разошлось: `Context` лежит в
`domain/lookup.go` рядом с запросом; `WordAnswer` остался типом слова
ответа нейронки; кеш `repository/memory` переведён на
`domain.Selection` здесь же, иначе пакет не собирался; тесты
конвертеров — `LFL-113_1 LFL-113_2` одним тестом, `LFL-105_1`–`105_3`
на `LemmaRequest` с контекстом и `translation: null`.

Тезисы: LFL-105 (105.1–105.3 конвертерами, 105.4–105.7 схемой),
LFL-113 (конвертер), LFL-50.1, LFL-50.2, LFL-50.6, LFL-54, LFL-90.6,
LFL-90.7. Проверка: `internal/converters/lookup_test.go` на примерах
`contract.md` — `LFL-105_1`, `LFL-105_2`, `LFL-105_3`, `LFL-113_1`,
`LFL-113_2`, `LFL-50_2`, `LFL-50_6`, `LFL-54_1`, `LFL-90_6`,
`LFL-90_7`; `task gen` без ручных правок в `pkg/openapi`; `task
mockery:gen` после смены портов, устаревший `mock_normalizer.go`
удалён руками (mockery старые файлы не трёт). После этапа: `go test`
по `internal/domain`, `internal/converters`, `internal/service/...`
(кроме `selection`, `verify` — Э11), `internal/repository/...`,
`internal/clients/wiktionary`, `internal/clients/converters` зелёный;
`scenario/lookup`, `clients/openrouter`, `api/words/v1`, `app`, `cmd`
не собираются до Э11–Э13 — `task test` и `task lint` зелёные после
Э13.

Допущение: `Token` и `TokensToDomain` остаются у лемм из токенов;
`OperationWordsLookup` в домене остаётся — старые записи журнала и
команды `runs` его читают.

### Домен

**Было.** `internal/domain/lookup.go`:

```go
type LookupRequest struct {
    Account             Account
    LearningLanguage    Language
    TranslationLanguage Language
    Tokens              []Token
    Components          ComponentTree
}

type LookupResponse struct {
    Attribution     string
    Words           []WordResult
    Recommendations []Recommendation
}

type Thresholds struct {
    MinConfidence *float64
    Language      *Language
}
```

`word.go` — `Word{Text, Source Token, TooLong}`; `selection.go` —
`Prompt{Text, TextTranslation, Words, Tree, LearningLanguage,
TranslationLanguage}`, `WithWords`, `Answer{Words, Recommendations}`,
`WordAnswer`, `Pick{Key, Pairs, Matches, Examples}`,
`Selection{Words []WordSelection, Recommendations, Usage,
Corrections}`, `WordSelection`, `CorrectionUnknownPair`;
`candidate.go` — `CandidateLexeme` без `InContext`, `WithPairs`,
`mergedExamples`; `error.go` без `ErrRequestTooLarge`.

**Будет.** `hld-lfl.md`, «Домен»: `LookupRequest` с `Lemma` и
`Context`, `Context`, `LookupResponse.Word`, `Word{Text, Original,
TooLong}`, `Prompt.Word`, `Answer.Word`, `Selection{Lemmas, Remarks,
Recommendations, Usage, Failed, Corrections}`, `Pick{Key, Matches,
Examples}`, `CandidateLexeme.InContext`, `ErrRequestTooLarge`. Сняты:
`Thresholds`, `WithWords`, `WordAnswer` в списке, `WordSelection`,
`CorrectionUnknownPair`, `WithPairs`, `mergedExamples`.

**Мотивация.** Одно слово, лемма как прислана, пар нет (Д114, Д99,
Д63).

### Порты, моки, резолверы

**Было.** `service/service.go:69–75` — порт `Normalizer` с
`domain.Thresholds`; `:93–100` — `Verifier.Verify(picks, found
[]WordResult, tree, translation)`; `service/ports.go:98–109` —
`LLMClient` с `Translate`; `:113–118` — `SelectionCache` на
`domain.WordSelection`; моки `service/mocks/mock_normalizer.go`,
`mock_verifier.go:24`, `mock_selection_cache.go:26`,
`mock_llm_client.go:154` — по старым сигнатурам (`.mockery.yaml`,
`all: true`). Резолверы читают исходный регистр из токена:
`service/baselookup/resolver.go:63, 66` и
`service/resolve/resolve.go:36, 67, 70` — `word.Source.Text`; тесты
строят `domain.Word{Source: domain.Token{…}}`:
`baselookup_test.go:52, 66, 79, 93`, `resolve_test.go:44, 48`,
`sourceswitch/switch_test.go:19`,
`clients/wiktionary/resolve_fixtures_test.go:63`, `live_test.go:31`;
на токенах стоят `scenario/lookup/suite_test.go:77–90, 145–186`,
`app/wordbase_test.go:23, 39–51, 100–133`,
`app/wordbase_lemmas_test.go:365–372`.

**Будет.** В этом же этапе: `service.go` — `Normalizer` снят,
`Verifier.Verify(picks domain.Selection, found domain.WordResult, tree
domain.ComponentTree)`; `ports.go` — `LLMClient` без `Translate`,
`SelectionCache.Get/Put` на `domain.Selection`; `task mockery:gen`,
`git rm internal/service/mocks/mock_normalizer.go`; резолверы
`baselookup` и `resolve` читают `word.Original`, их тесты и тесты
`sourceswitch`, `wiktionary`, `app/wordbase*_test.go`,
`scenario/lookup/suite_test.go` строят `domain.Word{Text, Original,
TooLong}`; пакет `service/normalize` с тестами удалён.

**Мотивация.** Пакет `service` собирается сразу после смены домена,
моки не врут сигнатурами; исходный регистр живёт в слове, не в
токене (Д99).

### Контракт

**Было.** `api/openapi.yaml` 0.4.0: путь `/v1/words/lookup`,
`operationId: lookupWords`; схема `LookupRequest` с `tokens`
(`maxItems: 2000`) и `components`; `LookupResponse{attribution, words,
recommendations}`; `WordResult.text` — «после нормализации»;
`CandidateLexeme` без `inContext`; 413 — «больше 2000 токенов».

**Будет.** Версия 0.5.0. Путь `/v1/lexemes/from-lemma`, `operationId:
lexemesFromLemma`, `X-Run-Id`; схема `LemmaRequest`:
`learningLanguage`, `translationLanguage`, `lemma` (`minLength: 1`),
`context` — объект `LemmaContext{text, translation}`: оба поля в
`required`, `text` с `minLength: 1`, `translation` nullable — пустой
контекст схема не пропускает; `components`; у запроса `required` —
языки и лемма; ответ
`LexemesResponse{attribution, recommendations, word}`;
`WordResult.text` — лемма как прислана; у `CandidateLexeme` поле
`inContext: boolean` в `required`. Описания: 413 — в `context.text`
больше 4000 слов, считает сервер после разбора (LFL-106); 400
`invalid_request` — пустая лемма, дерево не по схеме, два компонента с
одним `id` (LFL-105.4–105.7); порядок проверок — ключ, схема, размер,
работа (LFL-27); 429 — только лимит нейронки. Путь
`/v1/lemmas/from-tokens` — как был. Схемы `LookupRequest`,
`LookupResponse` сняты.

**Мотивация.** Имена схем — по типам HLD; живые примеры —
`contract.md`.

### Генерация и конвертеры

**Было.** `internal/converters/lookup.go`:

```go
func RequestToDomain(
    req *wordsv1.LookupRequest,
    account domain.Account,
) domain.LookupRequest {
    return domain.LookupRequest{
        Account: account,
        LearningLanguage: domain.Language(
            req.LearningLanguage,
        ),
        TranslationLanguage: domain.Language(
            req.TranslationLanguage,
        ),
        Tokens:     TokensToDomain(req.Tokens),
        Components: ComponentsToDomain(req.Components),
    }
}

func ResponseToAPI(
    resp domain.LookupResponse,
) *wordsv1.LookupResponse
```

`CandidateLexemeToAPI` без `inContext`.

**Будет.** `task gen` → `wordsv1.LemmaRequest`, `LemmaContext`,
`LexemesResponse`, `LexemesFromLemmaParams`, `LexemesFromLemmaRes` и
типы отказов операции. Конвертеры:

```go
// RequestToDomain — запрос контракта в домен: аккаунт,
// языки, лемма без пробелов по краям, контекст, если
// есть, дерево. Меняется: лемма и контекст вместо
// токенов.
func RequestToDomain(
    req *wordsv1.LemmaRequest,
    account domain.Account,
) domain.LookupRequest {
    return domain.LookupRequest{
        Account: account,
        LearningLanguage: domain.Language(
            req.LearningLanguage,
        ),
        TranslationLanguage: domain.Language(
            req.TranslationLanguage,
        ),
        Lemma:      strings.TrimSpace(req.Lemma),
        Context:    contextToDomain(req.Context),
        Components: ComponentsToDomain(req.Components),
    }
}

// contextToDomain — контекст запроса; поля нет — nil;
// перевод null — пусто. Новая.
func contextToDomain(
    ctx wordsv1.OptLemmaContext,
) *domain.Context

// ResponseToAPI — ответ домена в контракт: атрибуция,
// одно слово, рекомендации. Меняется.
func ResponseToAPI(
    resp domain.LookupResponse,
) *wordsv1.LexemesResponse
```

`CandidateLexemeToAPI` переносит `InContext`; `TokensToDomain`
остаётся у `TokensRequestToDomain`.

**Мотивация.** Конвертеры — единственное место встречи контракта и
домена (Д57).

## Э11. Отбор, проверка ответа, клиент нейронки

ВЫПОЛНЕН 2026-10-28. Как в плане; разошлось: `selectionOf(word,
answer, usage)` сверяет текст слова ответа с запросом — чужое слово даёт
отбор без лемм; строка не из лексемы смысла поля не даёт, `fieldOfLine`
и `withGloss` сняты вместе с парами; тест `LFL-85_14` без адреса
(строка снята), `LFL-100_1` → `LFL-100_2`, `LFL-85_23/24` →
`LFL-97_15`; разделители строк Юникода в `safe` записаны явными
` ` и ` `; золотой файл промпта перезаписан.

Тезисы: LFL-109, LFL-85 (без строк о парах), LFL-97, LFL-99.1,
LFL-99.5, LFL-99.6, LFL-94, LFL-87, LFL-88, LFL-89, LFL-101, LFL-93,
LFL-98. Проверка: `service/selection/selection_test.go` — `LFL-94_1`,
`LFL-94_2`, `LFL-94_3`, `LFL-94_5`, `LFL-99_1`, `LFL-99_5`,
`LFL-97_2`, `LFL-109_3`, `LFL-109_6`; `service/verify/verify_test.go`
— адреса LFL-85, LFL-88, LFL-89 как были на одном слове, тесты пар
(`LFL-85_7`, `LFL-85_15`, `LFL-85_16`, `LFL-85_20`, `LFL-95_*`) сняты;
`clients/openrouter/prompt_test.go` — `LFL-109_1`, `LFL-109_2`,
`LFL-109_4`, `LFL-109_5`, `LFL-109_7`, `LFL-80_1`, `LFL-80_2` сняты;
`client_test.go` — `LFL-101_1` (текст и перевод в логе),
`LFL-101_2`–`LFL-101_4`, `LFL-93_*`, `LFL-97_7`–`LFL-97_17`
переписаны на одно слово, обрыв ответа режется перед
`"recommendations"`; золотой файл `testdata/prompt-perro.json`
перезаписан флагом `-update`, `export_test.go` — `PromptOfExample` по
`input.Word`; `notes_test.go` — как был; `memory/memory_test.go:57–58`
— кеш на `domain.Selection`. После этапа: `go test` по
`service/selection`, `service/verify`, `clients/openrouter`,
`repository/memory` зелёный. Контрольный набор — Э14.

### Отбор

**Было.** `service/selection/selection.go` — `Options{BatchSize,
Parallelism, Model}`; `Select` через `splitByCache`, `translated`,
`batchesOf`, `inParallel`, `selectBatch`, `noneAnswered`,
`freshWords`, `merged`, `wordSelectionOf`, `remember`; порт
`SelectionCache` хранит `domain.WordSelection`.

**Будет.** `hld-lfl.md`, «Отбор»: `Select` одним вызовом, `fromCache`,
`keyOf(prompt)`, `selectionOf`, `failed`, `remember`;
`Options{Model}`. В кеш идёт отбор с нулевым `Usage` и пустыми
`Corrections`: ответ из кеша счётчик нейронки не растит (LFL-94,
LIM-79), тест `LFL-94_1` проверяет `second.Usage ==
domain.ModelUsage{}`. Порт `SelectionCache` на `domain.Selection` —
Э10; `repository/memory/selection.go` и `memory_test.go:57–58` — тот
же тип.

**Мотивация.** Один вызов на запрос (Д33 снято пачками); перевод
контекста приходит в запросе (LFL-109.6); расход списывается только
за вызов.

### Проверка ответа

**Было.** `service/verify/verify.go:34–72` — `Verify(picks, found
[]WordResult, tree, translation)` по словам, внутри `withKnownPairs`,
`pairLexemes`; `:203` — `fieldsOf(own.WithPairs(pairLexemes(lexemes,
pick.Pairs)), pronunciations)`; тесты пар `verify_test.go:145, 251,
276` (`LFL-100_1`, `LFL-85_14`, `LFL-85_23`, `LFL-85_24`) и
`LFL-85_7`, `LFL-85_15`, `LFL-85_16`, `LFL-85_20`, `LFL-95_*`.

**Будет.** `hld-lfl.md`, «Проверка ответа нейронки»: `Verify(picks,
found WordResult, tree)` по леммам одного слова; `withKnownPairs`,
`pairLexemes` сняты; `fieldsOf(own, pronunciations)`; порт
`Verifier` без `translation` — Э10; тесты пар и снятых строк сняты.

**Мотивация.** Пар нет (Д63); слово одно.

### Клиент нейронки

**Было.** `clients/openrouter/prompt.go` — `systemPrompt` v17 со
входом `words` и `textTranslation` «made by a separate call»,
`exampleInput` и `exampleAnswer` с двумя словами, `promptJSON{… Words
[]wordJSON}`, `translationPrompt`, `translationMessages`; `client.go`
— `Complete`, `completeOnce`, `withoutExamples` по словам, `Translate`,
`parseTranslation`, `maxTranslationTokens`, `logCall` со списком слов;
`schema.go` — `answerSchema(prompt)` по словам, `translationSchema`,
`inTranslationSchema`; `answer.go` — `parseAnswer` по `words`,
`pickOf` с двойником пары (`twin`).

**Будет.** `hld-lfl.md`, «Клиент нейронки». Вход вызова — `word`
объектом вместо `words`, `textTranslation` из запроса (`promptJSON`,
`wordJSONOf`); по `prompt.Word` вместо `prompt.Words` переходят
`answerSchema`, `lemmaSchemas` (`schema.go:145`), `enumBudgetOf`
(`:512`), `inTranslationSchema` (по словам перевода из запроса),
`parseAnswer`, `lexemesOf` (`answer.go:363`), `pickOf` без двойника
пары, `withoutExamples`, `wordTexts` (`client.go:417`), `pickLines`;
`decodeAnswer` режет оборванный ответ перед `,"recommendations"` и
дописывает `}`, без целого `word` разбор падает целиком (LFL-97.9),
`wordCuts` снят; `logCall` — поля `word`, `context` и `translation`
из `prompt.TextTranslation` (LFL-101.1). Инструкция v18 — правками
строк v17 в `prompt.go`: Input — «"word": the word with its lemmas»
вместо «"words": each word…», «"textTranslation" (the same text in
translationLanguage, from the request)» вместо «made by a separate
call»; правило 1 — без «one entry per input word»; правило 2a — «the
word may stand in "text" in another form ("correr" as "corrían"):
find that form and render it»; правило 6 — форма `{"word": {…},
"recommendations": […]}`; пример парой сообщений — одно слово «link»
с леммой «link», `formNote: null`, текст «Study links early
friendships with sibling bonds.», ответ `inTranslation`
["связывает"], `role` «verb — связывать», как у «links» в v17. Сняты:
`Translate`, `translationPrompt`, `translationMessages`,
`translationSchema`, `parseTranslation`, `maxTranslationTokens`. Файл
инструкции на сервере (`LLM_PROMPT_FILE`) — тем же текстом v18 или
снять, чтобы взялась встроенная.

**Мотивация.** LFL-109, LFL-97.15; текст инструкции проверяется
пробником до тега, журнал приёмов — `llm-journal.md`.

## Э12. Сценарий lookup v2

ВЫПОЛНЕН 2026-10-28. Как в плане; разошлось: предел слов проверяет
`exceeds(n, limit)` — 0 без предела; `ownLemma` — первая лемма без
пометки формы с лексемами; `lemma fallback` пишется уровнем Info;
`response` ставит слову текст леммы как прислана; тесты по тапу — в
`context_test.go` (LFL-107, 108, 115, 113.3), `LFL-55_6` на одном
слове, `LFL-90_2` в `assemble_test.go`; тесты лемм — без
нормализатора, через `resp.Word`.

Тезисы: LFL-27.2, LFL-106, LFL-37, LFL-41, LFL-107, LFL-108, LFL-115,
LFL-44, LFL-48, LFL-50, LFL-51.4, LFL-54, LFL-55.6, LFL-66, LFL-67,
LFL-71, LFL-75, LFL-90, LFL-91, LFL-100, LFL-113, LFL-99.2–99.4;
LIM-79.1, LIM-79.2, LIM-96.5, LIM-96.6, LIM-110.1, LIM-111.2,
LIM-111.3, LIM-112.1–112.4 — тесты лимитов на входе леммой.
Проверка: `internal/scenario/lookup/lookup_test.go` и соседи —
`LFL-27_2`, `LFL-106_1`, `LFL-106_2`, `LFL-106_3`, `LFL-37_1`,
`LFL-37_2`, `LFL-37_3`, `LFL-41_1`, `LFL-41_2`, `LFL-107_1`,
`LFL-107_2`, `LFL-108_1`, `LFL-108_2`, `LFL-108_4`, `LFL-108_5`,
`LFL-115_1`, `LFL-115_2`, `LFL-115_3`, `LFL-113_1`, `LFL-113_2`,
`LFL-113_3`, `LFL-100_2`, `LFL-100_3`, `LFL-90_1`, `LFL-90_2`,
`LFL-90_3`, `LFL-90_4`, `LFL-90_9`, `LFL-91_1`, `LFL-91_4`,
`LFL-91_5`, `LFL-50_1`, `LFL-50_3`, `LFL-50_4`, `LFL-54_3`,
`LFL-55_6`, `LFL-99_2`, `LFL-99_3`, `LFL-99_4`, новые `LFL-51_4`,
`LFL-67_6`, `LFL-71_14`, «London» по тапу → `word.text` «London»
(контракт 2); `LFL-44_1`–`LFL-44_5`, `LFL-48_1`–`LFL-48_4`,
`LFL-66_*`, `LFL-67_*`, `LFL-71_*`, `LFL-75_*`, `LIM-*` — как были,
вход словом вместо токена. Сняты вместе с кодом и строками v1:
`LFL-04_1`, `LFL-04_2` (`lookup_test.go:15, 27`), `LFL-24_*`,
`LFL-38_*`, `LFL-49_*`, `LFL-82_*`, `LFL-86_*`, `LFL-95_*`, тесты на
снятые строки `LFL-54_2`, `LFL-55_1`, `LFL-55_2`, `LFL-55_4`
(`lookup_test.go:323–381`), `LFL-50_5`, `LFL-83_7`, `LFL-98_2`
(`assemble_test.go:151, 181`, `selection_test.go:344`), `LFL-51_1`
(`app/wordbase_test.go:160`, `resolve_fixtures_test.go`,
`resolve_test.go`); `LFL-70_3` (`clients/converters/kaikki_test.go:140`)
→ `LFL-71_13`; `LFL-40_1`, `LFL-40_2` (`article_test.go:95, 101`,
`resolve_fixtures_test.go:155, 165`) и `LFL-53_1`
(`resolve_test.go:314`) — без адреса; `LFL-27_6` — в HTTP-тест Э13,
схему ловит ogen до сценария. После этапа: `go test
./internal/scenario/...` зелёный.

Допущение: «своя лемма» (LFL-107) — лемма ответа без пометки формы:
такую резолвер даёт только самому слову (`baselookup/resolver.go:162–169`).

### Сценарий

**Было.** `scenario/lookup/lookup.go`:

```go
func (s *lookupScenario) Lookup(
    ctx context.Context,
    req domain.LookupRequest,
) (domain.LookupResponse, error) {
    err := s.languages.Check(ctx, req.LearningLanguage)
    if err != nil {
        return rejected(err)
    }
    err = s.quota.Begin(
        ctx, req.Account,
        domain.WorkWordLexemes,
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
    found := s.lookupEach(ctx, words, languagesOf(req))

    picks, err := s.selector.Select(
        ctx,
        promptOf(req, found, s.normalizer.Text(req.Tokens)),
    )
    s.finishWork(ctx, req.Account, picks.Usage)
    if err != nil {
        return rejected(err)
    }
    picks = s.verifier.Verify(
        picks, found,
        req.Components, req.TranslationLanguage,
    )
    logCorrections(ctx, picks.Corrections)
    return response(
        assembled(found, picks, req.TranslationLanguage),
        picks.Recommendations,
    ), nil
}
```

`lookup_each.go` — `lookupEach`, `inParallel`, `lookupOne(ctx, word,
langs)`, в слово ответа идёт `word.Text` (`:142–161`); `scenario.go` —
`Options{Budget, Parallelism, Thresholds}`, `NewScenario` с
`normalizer`; `assemble.go` — `assembled(found []WordResult, picks,
translation)`, `selectionsByWord`, `assembledWord`, `selectedWord`,
`noTranslationRemarks(selected, translation)` (`:67, 138–141`),
`withPairs`, `pairLexemes`; `lemmas.go:357–378` —
`onlyTranslationWiktionary` ставит `notFound(word)` слову с одними
чужими лексемами.

**Будет.** `hld-lfl.md`, «Сценарий»: `Lookup`, `lookupOne(ctx, word,
langs, withContext)`, `wordsIn`, `tooLarge`, `wordOf`,
`lemmaForContext`, `promptOf(req, found)`, `finishWork`,
`assembled(found, picks, translation, withContext)` — язык перевода
нужен `noTranslationRemarks` (LFL-91.1 против LFL-91.5),
`response(lemma, word, recommendations)` — текст слова ответа —
лемма как прислана (контракт 2, «London» → «London»); `Options{Budget,
MaxWords}`, `MaxWords` 0 — без предела, как бюджет; `NewScenario` без
`normalizer`. `lookup_each.go` оставляет `lookupOne`, `withinBudget`,
`blocksByLemma`, `withLemmas`, `blocksOf`, `budgetExhausted`,
`notFound`, `sourceError`, `inParallel` снят; `assemble.go` —
`assembled` для одного слова, `selectedLemma` без пар, у первой
лексемы `InContext` при контексте; `withPairs`, `pairLexemes`,
`selectionsByWord` сняты. Запасной поиск идёт после
`onlyTranslationWiktionary`, поэтому смотрит на статус:

```go
// lemmaForContext — Ш3 А (LFL-107, LFL-108, LFL-115):
// слово «ошибка» — как есть; «не найдено» — в лог «lemma
// fallback» с леммой как прислана, без найденной; у
// найденного своя лемма — без пометки формы — с лексемами
// остаётся одна; нет — первая в порядке статьи лемма с
// лексемами, в лог обе; нет и такой — не найдено, в логе
// без найденной. Новая.
func lemmaForContext(
    ctx context.Context,
    word domain.Word,
    result domain.WordResult,
) domain.WordResult {
    if result.Status == domain.SourceError {
        return result
    }
    if result.Status == domain.NotFound {
        logFallback(ctx, word.Original, "")
        return result
    }
    if own := ownLemma(result.Lemmas); own != nil {
        result.Lemmas = []domain.Lemma{*own}
        return result
    }
    for _, lemma := range result.Lemmas {
        if len(lemma.Lexemes) > 0 {
            logFallback(ctx, word.Original, lemma.Text)
            result.Lemmas = []domain.Lemma{lemma}
            return result
        }
    }
    logFallback(ctx, word.Original, "")
    return notFound(word)
}

// ownLemma — лемма ответа без пометки формы с лексемами;
// нет — nil. Новая.
func ownLemma(lemmas []domain.Lemma) *domain.Lemma

// logFallback — строка «lemma fallback» с номером запуска:
// лемма запроса как прислана и найденная, пусто — не
// найдена (LFL-115). Новая.
func logFallback(ctx context.Context, from, to string)
```

Порядок проверок: `wordsIn` против `MaxWords` → `languages.Check` →
`quota.Begin` (LFL-27, LFL-106); в ошибке предела — число слов и
предел, ручка отдаёт их в сообщении 413 (контракт 3).

**Мотивация.** Лемма приходит словарной формой, нормализация не нужна
(Д99); предел считает сценарий по разобранному запросу (Д111); по
тапу лемма одна (Д82); запасной поиск виден в логе и у «не найдено».

## Э13. Ручка, сборка, пробник, журнал

ВЫПОЛНЕН 2026-10-28. Как в плане; сверх плана: два компонента с одним
`id` ловит ручка до сценария (`duplicateComponentID`, LFL-105.6) —
схема такого не выражает; `probe llm` разбирает флаги в
`parseProbeLLMArgs` с тестами, `--show` снят, отбор у пробника со
своим пустым кешем на прогон — каждый прогон зовёт модель, иначе
карточка без контекста после первого идёт из кеша (LFL-94) и «три
прогона на приём» не прогоны; гоняется на копии базы прода
`~/lexeme/probe/dict`, по сети ключи другие и английского издания
нет; HTTP-тест — `cardRequest`
и `tapRequest`, `fakeLLM` по `word`, записи журнала читаются как JSON
(jsonb переформатирует тело), у `perro` в фикстурах нет русской статьи
— лог вызова проверяется по `corrían`; `wordbase_test.go` — запрос на
каждую лемму, `TestWordBaseTapped` (LFL-108.1, 108.5, 107.2 на базе из
фикстур); `Taskfile` `spec:coverage` у LFL одной строкой с
`SPEC_MANUALS`. `task test`, `task test:integration`, `task lint`,
`task spec:coverage` зелёные.

Тезисы: LFL-105.4–105.7, LFL-27.1, LFL-27.3, LFL-27.6, LFL-106.1,
LFL-106.2, LFL-30.1, LFL-114.1, LFL-99.1, LFL-54.1, LIM-96.1,
RUN-01.1, RUN-01.4, RUN-03.2 у новой операции; ACC-26.1, ACC-26.4,
ACC-77.1, ACC-77.2, LFL-93, LFL-98, LFL-74, LFL-72.7, LFL-87.1,
LFL-89.1, LFL-99.4, LFL-99.5, LFL-101.1 — прежние HTTP-тесты на новом
маршруте. Проверка: `internal/app/http_integration_test.go` — `post`
(`:382–385`), `request`, `requestWithTree` (`:413–427`), `lookupBody`
(`:331–362`) переведены на `/v1/lexemes/from-lemma` с телом `{lemma,
context?, components?}` и ответом `word`; каждый `t.Run` с `post`
(`:461–692`) переносится с прежним адресом; новые `LFL-105_1`,
`LFL-105_4`, `LFL-105_5`, `LFL-105_6`, `LFL-105_7`, `LFL-27_1`,
`LFL-27_3`, `LFL-27_6` (4001 слово плюс опция без `id` → 400),
`LFL-106_1` (413, в сообщении «4001 words in context, limit 4000»),
`LFL-106_2` (бывшие `LFL-28_1`, `LFL-28_2` на `:514, 596`),
`LFL-30_1`, `LFL-99_1`, `LFL-54_1`, `LIM-96_1`, `RUN-01_1`,
`RUN-01_4` (`:824–876`); в `fakeLLM` (`:209–261`) ветка отбора по
имени схемы `card` читает `word` и отвечает `{"word": …}`, ветка
`translation` (`:166–179`) и счётчик `llmTranslations` (`:134–136`)
снимаются; `api/words/v1/journal_test.go:144–162` — `LexemesFromLemma`,
`wordsv1.LemmaRequest{Lemma: "perro"}`,
`domain.OperationLexemesFromLemma`, адреса `RUN-01_4`, `RUN-03_2`;
`cmd/lexeme/probe_test.go` — разбор флагов пробника. После этапа:
`task test`, `task test:integration`, `task lint` зелёные, `task
mockery:gen` без диффа.

### Ручка

**Было.** `api/words/v1/lookup.go`:

```go
func (h *wordsHandler) LookupWords(
    ctx context.Context,
    req *wordsv1.LookupRequest,
    params wordsv1.LookupWordsParams,
) (wordsv1.LookupWordsRes, error) {
    account := accountFrom(ctx)
    runID := params.XRunID.Or("")
    ctx = logger.ContextWithRunID(ctx, runID)
    ctx, notes := journal.WithNotes(ctx)
    started := time.Now()
    resp, err := h.lexemes.Lookup(
        ctx, converters.RequestToDomain(req, account),
    )
    logRequest(ctx, account, req, resp, started, err)
    res, status, outcome := lookupResult(req, resp, err)
    h.record(
        ctx, domain.OperationWordsLookup, runID, started,
        notes, req, res, status, outcome, err,
    )
    if res == nil {
        return nil, err
    }
    return res, nil
}
```

`lookupResult` — 200, 503, 429, 400, иначе 500; `logRequest` с числом
`tokens` и `words`.

**Будет.** Файл `lexemes.go`: `LexemesFromLemma(ctx, req
*wordsv1.LemmaRequest, params wordsv1.LexemesFromLemmaParams)` тем же
ходом, запись с `domain.OperationLexemesFromLemma`; `lexemesResult` —
плюс ветка:

```go
// новая ветка: в контексте больше 4000 слов (LFL-106)
case errors.Is(err, domain.ErrRequestTooLarge):
    return requestTooLarge(err),
        http.StatusRequestEntityTooLarge,
        string(wordsv1.ErrorCodeRequestTooLarge)
```

`requestTooLarge(err)` — 413 `request_too_large` с числом слов в
сообщении; `logRequest` — лемма, слова контекста, статус слова.
`lookup.go` снят; `errors.go` — `tooManyTokens` как есть, у
`lemmas/from-tokens`.

**Мотивация.** Одна ручка на контракт, по методу на операцию; 413 по
словам решает сценарий, ручка только переводит ошибку в код.

### Сборка и пробник

**Было.** `app/di.go` — геттер `Normalizer`, `Selector` с
`BatchSize` и `Parallelism`, `LookupScenario` с `Normalizer`,
`Parallelism`, `Thresholds`; конфиг `config/env/llm.go:18–21, 65–71`
— `BatchSize int \`env:"LLM_BATCH_SIZE" envDefault:"1"\``,
`Parallelism int \`env:"LLM_PARALLELISM" envDefault:"4"\`` и их
методы, `config/interfaces.go:47–50`, `deploy/.env.example:43–44`;
`app/probe.go:46–61` — `LookupProbe` с `Normalizer`, `Parallelism`,
`Thresholds`; `cmd/lexeme/probe_llm.go:27–32` — флаги `--text`,
`--tree`, `--learning` (язык, «en»), `--translation` (язык, «ru»),
`--show`, `--runs`; текст режется на токены.

**Будет.** `di.go`: `Selector` с `selection.Options{Model}`,
`LookupScenario` без `Normalizer` и с `lookup.Options{Budget,
MaxWords: cfg.LLM.MaxContextWords()}`; конфиг: поле `MaxContextWords
int \`env:"LLM_MAX_CONTEXT_WORDS" envDefault:"4000"\`` и метод
`MaxContextWords()` в `llmConfig` и интерфейсе `LLMConfig`,
`BatchSize` и `Parallelism` сняты вместе с методами и строками
`.env.example`; геттер `Normalizer` снят. `LookupProbe`:
`lookup.NewScenario(languages с учётом в памяти, probeQuota{},
NetworkResolver, MeaningsSource, Selector, Verifier,
lookup.Options{Budget: responseBudget, MaxWords:
config.AppConfig().LLM.MaxContextWords()})`. Пробник: `probe llm
--lemma <лемма> [--text "<текст>"] [--text-translation "<перевод>"]
[--learning es] [--translation ru] [--tree components.json] [--runs
N]` — языки как были, контекст и его перевод — свои флаги; один
запрос по лемме, печатает статус, лемму с пометкой формы, лексемы с
ключами, сопоставлением и «в тексте», замечания, рекомендации и
строки `llm verify`; `--show` снят; разбор флагов —
`probe_test.go`. `Taskfile`: `spec:coverage` у LFL — одна строка
`SPEC_FILE=docs/handbook/specs/lexemes-from-lemma/spec.md
SPEC_MANUALS=docs/features/IS536_llm_values/control_set.md
./scripts/spec-coverage.sh`, без `SPEC_KNOWN`; `spec-v1.md` остаётся
замороженным.

**Мотивация.** Пробник — на модели до тега, те же языки, что у
`probe text`; предел слов — в конфиге, один источник для сценария и
пробы.

## Э14. Контрольный набор, выкатка, доки

ВЫПОЛНЕН 2026-10-28. Прод v0.1.37 (lexeme 847939c + 6dc0eee: код
контракта 0.5.0 из `pkg/openapi` не попал в первый коммит, тег
перенесён на второй; первые прогоны CI/Deploy красные, вторые
зелёные), проверен с ноутбука: карточка perro, тап correr с
`inContext`, `words/lookup` → 404, 4001 слово → 413. Разошлось:
пробник гоняется не по сети, а на копии базы прода
(`BASE_DIR=/home/ubuntu/lexeme/probe/dict`) — по сети ключи
`ruwiktionary/1.1` и английского издания нет; инструкция в проде v22 =
v18 + критерий «почти то же», приёмы v19…v21 (род из знаний,
компонент под опцией, `componentNames`, пример с рекомендацией) на
`gpt-4o-mini` не сработали и сняты (Ж-28…Ж-30); по контрольному набору
закрыты LFL-83.2–83.6, 87.2, 88.1–88.3, 89.2, 89.7, 89.8 — LFL-88
«реализовано», LFL-83 и LFL-89 остались «принято»: 83.1 (перевод
примера говорит «бежали», Ж-31), 87.1 и 89.1 (род из знаний), 89.6
(«Пример») — вопросы юзеру; LFL-25 «реализовано», пример [1] — 308
вместо 301 у `POST` (Traefik хранит метод); `scripts/llm-journal.pl`
переведён на строки `lexemes from lemma`; статусы, CHANGELOG LFL про
LFL-25 и контрольный набор по LFL-25 в lexeme не закоммичены — уедут
со следующим кодовым коммитом; `LLM_BATCH_SIZE=1` в серверном `.env`
осталась (игнорируется), снять руками.

Тезисы: LFL-83 (83.1–83.6), LFL-87.1, LFL-87.2, LFL-87.7, LFL-88
(88.1–88.3), LFL-89.1, LFL-89.6–89.8 — контрольный набор пробником на
модели; LFL-25.1, LFL-25.2 — ручной. Проверка: раздел «LFL v2» в
`control_set.md` — кейс на строку с заголовком-адресом (`## LFL-83.4
…`): лемма, текст и перевод, языки, файл дерева `probe/tree-*.json`,
ожидание, команда пробника; прогон `lexeme probe llm` на сервере по
каждому кейсу три раза, строка закрыта при совпадении на каждом
прогоне; итог в `llm-journal.md`; оценка записей журнала `runs
assess` по аспектам `selection` и `matching`; кейсы LFL-25 там же:
`curl -X POST http://api.lexeme.pro/v1/lexemes/from-lemma` с ключом и
леммой → 301, `GET http://api.lexeme.pro/healthz` → 301, затем 403.
`task spec:coverage` считает `control_set.md` ручниками
(`SPEC_MANUALS`, Э13).

### Контрольный набор и журнал нейронки

**Было.** `control_set.md` — ожидания отбора и сопоставления по словам
первой версии, вход токенами: «perro» с текстом (`:14`), «dar» es →
ru (`:25–27`), «corrían» формой (`:23–24`), «zarpó» es → ru
(`:36–37`), одно дерево «Раскладка» (`:81–87`); `llm-journal.md` —
приёмы отбора v1…v17, извлечения v1…v3.

**Будет.** Раздел «LFL v2» кейсами по строкам спеки, вход леммой с
контекстом: LFL-83.1 — «correr» с «Las nubes corrían por el cielo.»
es → ru; 83.2 — «banco» с двумя текстами; 83.3 — «perro» без текста;
83.4 — «zarpar» с «El barco zarpó al amanecer.» es → en, оба
значения; 83.5 — «dar» es → en, ровно три; 83.6 — «correr» с «Los
niños corren por la playa.»; 87.1, 87.2 — «perro» и «correr» с
деревом контракта; 87.7 — «llegar» с деревом со «Стилем» `multiple`;
88.1–88.3 — дерево с компонентом «Жопа» id 7; 89.1 — дерево без
«Рода», «perro»; 89.6 — без «Примера»; 89.7 — без «Транскрипции»;
89.8 — без «Рода», «correr»; деревья — файлы `probe/tree-*.json` в
папке набора. Инструкция v18 — новый приём в журнале с итогами трёх
прогонов; расхождения лечатся приёмами инструкции, модель не
меняется.

**Мотивация.** Строки «контрольный набор» закрываются только на
модели и только адресом в документе набора — иначе статус
«реализовано» не ставится, как у LFT-06, 07, 09, 10, 20 после Э8.

### Выкатка, откат, доки

**Было.** Прод v0.1.36 с `words/lookup`; CHANGELOG LFL — последняя
запись «v0.1.32», LFT — «v0.1.36» (Д121); `deploy.md` — проверки `curl` на
`words/lookup` (`:70–75`, `:225–232`), переменные `LLM_BATCH_SIZE` и
`LLM_PARALLELISM` (`:186–187`), пробник с `--show` (`:211–220`),
строка лога `lookup words … tokens=…` (`:339`), без предела слов
контекста; `architecture.md` — «Нормализация» среди сервисов;
`Taskfile` — два прогона LFL в `spec:coverage`; `contract.md` LFL с
пометками **v2** против схемы 0.3.0, разделы 5 и 6 (сервер ↔
нейронка) со списком `words`.

**Будет.** Порядок: `task format`, `task test`, `task lint`,
`task test:integration`, `task spec:coverage`; пробник на сервере по
контрольному набору; `deploy/.env` — `LLM_MAX_CONTEXT_WORDS` можно не
задавать, `LLM_PROMPT_FILE` — текст v18 или снять, `LLM_BATCH_SIZE` и
`LLM_PARALLELISM` убрать; коммит с доками, тег v0.1.37, CI деплоит;
миграций нет. Совместимость: `words/lookup` отвечает 404; прод-сборки
приложения с ним нет (Д46); dev-сборка на телефоне до сборки этапа 2
получает в диалоге карточки слова «сервер не ответил» — ожидаемо.
Откат — образ v0.1.36, базу не трогать. Доки: CHANGELOG LFL, LIM, ACC,
RUN — «v0.1.37», LFT — Д121 уже в «v0.1.36»; `deploy.md` — переменная предела,
проверки `curl` и строка лога на `lexemes/from-lemma`, пробник по
лемме, без `LLM_BATCH_SIZE` и `LLM_PARALLELISM`; `architecture.md` —
нормализации нет, порядок проверок операции; `contract.md` — пометки
**v2** сняты, ссылка на схему 0.5.0, разделы 5 и 6 на `word`;
статусы LFL «принято» → «реализовано» по покрытию, LFL-25 — по
кейсам набора, LFL-83, 87, 88, 89 — по контрольному набору, LFT-12 —
по тесту Э9; `OperationWordsLookup` в `domain/runs.go` помечен как
прошлая операция для чтения старых записей.

**Мотивация.** Д116: бэкенд в прод раньше мобилки; Д46: старый маршрут
не держится; путь назад — образ, без миграций.
