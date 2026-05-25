## Итерация 1 (2026-05-19T17:40:00-0600)

### F001 [architect] critical

**Description:** `RefreshLexeme` reducer безусловно append'ит лексему, но subscriber `observeLexemesByWordId` уже эмитит `RefreshLexemeList` после insert'а — без дедупа по id получаем дубль, нарушающий инв.3 (uniqueness id).

**Status:** approved

**Verdict:** race subscriber vs handler-Msg реально даёт двойной append без дедупа по id — нарушение инв.3.

### F002 [architect] critical

**Description:** `RefreshLexemeList` reducer при `lexeme.translationText == null` ставит `translation = null` напрямую — сносит активный `isEdit=true, edited="..."` пользователя; политика `RefreshTranslation` явно запрещает закрывать активный edit — `RefreshLexemeList` ломает инвариант симметрии.

**Status:** approved

**Verdict:** RefreshLexemeList пишет translation=null безусловно, что сносит активный isEdit — ломает симметрию с политикой RefreshTranslation.

### F003 [architect] critical

**Description:** Противоречие про возврат двух Msg одним handler'ом: `CreateLexeme` failure заявлен как `CreateLexemeFailed` + `ShowNotification` (два Msg от одного effect), но EC2 явно говорит «handler не может вернуть два Msg для одного effect». Механизм не определён.

**Status:** approved

**Verdict:** возврат двух Msg от одного handler-effect технически противоречит контракту EC2 — механизм не определён.

### F004 [architect] minor

**Description:** Асимметрия терминологии «Return Msg: отсутствует» (RemoveWord, RemoveLexeme) с одновременным `ShowNotification` Msg в failure-ветке — на failure Msg всё-таки есть, формулировка вводит в заблуждение.

**Status:** approved

**Verdict:** формулировка «Return Msg: отсутствует» вводит в заблуждение, на failure-ветке Msg ShowNotification есть.

### F005 [architect] minor

**Description:** `UpdateWord` не имеет idempotent-skip ветки. `CommitWordChanges` отправляет effect даже при `edited == value`. Handler сделает бесполезный DB-write + RefreshWord round-trip. Асимметрия с `CommitTranslationEdit` ветвь 2 (no-op при `edited == origin`).

**Status:** approved

**Verdict:** UpdateWord без idempotent-skip действительно асимметричен с CommitTranslationEdit и порождает бесполезный round-trip.

### F006 [architect] minor

**Description:** Маппер `Lexeme.toLexemeState()` упоминается в двух разных контекстах с разной семантикой: после `CreateLexeme` (translation=null, definition=null) и в `RefreshLexemeList` (читает `lexeme.translationText`/`definitionText`). Сигнатура и поведение маппера не зафиксированы единообразно.

**Status:** rejected

**Verdict:** дубликат F013 (analyst escalation); вердикт по сути дан в F013.

### F007 [architect] minor

**Description:** `RefreshLexemeList` subscriber-Msg не сбрасывает `isCreatingLexeme`. Если handler-Msg задержится / зафейлится, FAB останется заблокированным до перезагрузки экрана.

**Status:** rejected

**Verdict:** дубликат F015 (analyst escalation); вердикт по сути дан в F015.

### F008 [architect] minor

**Description:** Forward-ref в contract_ui_msg описывает `WordLoaded(word)` с одним payload; contract_io — `WordLoaded(word, lexemes)` с двумя. Изменение сигнатуры не зафиксировано в разделе «Расхождения spec ↔ code» (там только Refresh*).

**Status:** approved

**Verdict:** расхождение сигнатур WordLoaded(word) vs WordLoaded(word, lexemes) не зафиксировано в разделе расхождений.

### F009 [architect] minor

**Description:** `RefreshLexemeList` не специфицирует ordering-инвариант. Subscriber-эмит идёт в порядке Room-flow, `RefreshLexeme` append'ит в конец — возможен визуальный «прыжок» порядка между путями.

**Status:** approved

**Verdict:** ordering-инвариант действительно неуточнён, append-в-конец vs Room-flow order создаёт визуальный прыжок.

### F010 [architect] minor

**Description:** EC4 (subscriber first emit при NotLoaded) добавляет shortcut-ignore guard в reducer-логику `RefreshLexemeList`, но сам блок reducer-логики этот guard не показывает — pseudocode неполный.

**Status:** rejected

**Verdict:** дубликат F016 (analyst escalation); вердикт по сути дан в F016.

### F011 [analyst] critical

**Description:** Дубликат F001 — Race `RefreshLexemeList` (subscriber) vs `RefreshLexeme` (handler-Msg) после CreateLexeme: оба append'ят лексему по id, нарушая инв.3 если subscriber-эмит долетел первым.

**Status:** rejected

**Verdict:** дубликат F001.

### F012 [analyst] critical

**Description:** Дубликат F003 — двойной Msg от handler'а на failure-пути противоречит EC2. Касается не только `CreateLexeme`, но и `LoadWord` (ShowNotification + WordNotFound), `UpdateWord`, `RemoveWord`, `UpdateLexemeTranslation`, `RemoveTranslation`, `UpdateLexemeDefinition`, `RemoveDefinition` — везде декларируется двойной Msg-возврат.

**Status:** rejected

**Verdict:** дубликат F003.

### F013 [analyst] critical

**Description:** `RefreshLexemeList` reducer читает `lexeme.translationText`/`definitionText` у `Lexeme`-entity, но `CreateLexeme`-маппер явно постулирует «translation/definition отсутствуют в Lexeme на момент создания». Shape entity несогласован: либо у Lexeme есть join-поля (тогда `CreateLexeme` маппинг неточен), либо нет (тогда `RefreshLexemeList` не сможет прочитать), либо subscriber возвращает другой тип.

**Status:** approved

**Verdict:** shape Lexeme entity несогласован — маппер CreateLexeme постулирует отсутствие join-полей, а RefreshLexemeList читает translationText/definitionText; критично для реализации.

### F014 [analyst] critical

**Description:** Snackbar при `WordNotFound`-через-IO-error недостижим: handler шлёт `ShowNotification` + `WordNotFound`, reducer `WordNotFound` сразу триггерит `ExitScreen` ⇒ ViewModel уничтожается, snackbarState (живущий в state, не в side-channel) уничтожается вместе с ним до отрисовки. Любой `ShowNotification` синхронно с `ExitScreen` страдает тем же.

**Status:** approved

**Verdict:** snackbarState в state уничтожается при ExitScreen синхронно с ShowNotification — нотификация недостижима, требует side-channel или отложенного exit.

### F015 [analyst] critical

**Description:** `RefreshLexemeList`-reducer не сбрасывает `isCreatingLexeme`. Если subscriber после CreateLexeme-success долетел раньше `RefreshLexeme`, `lexemeList` уже содержит новую лексему, но флаг ещё `true`, FAB заблокирован дольше нужного. Plus последующий `RefreshLexeme` — двойной append (F001).

**Status:** approved

**Verdict:** isCreatingLexeme не сбрасывается в RefreshLexemeList; вкупе с F001 даёт двойной append плюс blocked FAB до handler-Msg.

### F016 [analyst] critical

**Description:** EC4 `NotLoaded`-guard для `RefreshLexemeList` декларирован в edge-cases, но основной блок reducer-логики этот guard НЕ включён. Реализация по основному блоку нарушит инв.5 (`lexemeList.isNotEmpty ⇒ Loaded`) на subscriber-first-emit до WordLoaded.

**Status:** approved

**Verdict:** EC4 guard NotLoaded декларирован отдельно, а в основном pseudocode reducer-логики отсутствует — нарушит инв.5 при subscriber-first-emit.

### F017 [analyst] minor

**Description:** Effect-handler триггерит `WordCardNavigationEffect.ExitScreen` напрямую (RemoveWord-success, WordNotFound-handler-path) минуя reducer — нарушает паттерн «навигация — эффект, возвращаемый reducer'ом», усложняет тестируемость. Альтернатива через intermediate Msg `WordRemoved` отвергнута без обоснования.

**Status:** approved

**Verdict:** handler триггерит NavigationEffect напрямую минуя reducer — нарушает паттерн; отвергнутая альтернатива WordRemoved-Msg не обоснована технически.

### F018 [analyst] minor

**Description:** `ShowNotification` объявлен Datasource Msg, но reducer-логика для него в contract_io не описана (только упоминание во forward-ref ui_msg). Без неё реализатор не знает поведение перезаписи `snackbarState.title` при `show=true` подряд.

**Status:** approved

**Verdict:** reducer-логика ShowNotification в contract_io не описана — поведение перезаписи snackbarState.title при повторном show=true не определено.

### F019 [analyst] minor

**Description:** `RefreshTranslation`/`RefreshDefinition` reducer-логика при `lexemeList.none { it.id == lexemeId }` (лексема параллельно удалена) — описана как no-op map. Не покрывает: что если `UpdateLexemeTranslation` шлёт `RefreshTranslation` для уже удалённой лексемы (FK-violation в БД или нет)? Контракт молчит.

**Status:** approved

**Verdict:** контракт молчит про FK-violation/no-op при Update для уже удалённой лексемы — реальный пробел в edge-cases.

### F020 [analyst] minor

**Description:** `RefreshLexeme`-payload `Lexeme` против дефолтного маппера `lexeme.toLexemeState()`: маппер выставляет `translation = null, definition = null`. Если в будущем БД вернёт `Lexeme` с непустыми полями, маппер их потеряет. Сейчас гипотетический кейс, но контракт маппера стоит выровнять.

**Status:** approved

**Verdict:** маппер lexeme.toLexemeState() при будущем расширении entity потеряет translation/definition; стоит выровнять контракт маппера явно.

### F021 [analyst] minor

**Description:** `CommitWordChanges` использует optimistic-apply, contract_io признаёт «при failure state временно расходится с БД». Нарушает симметрию с pessimistic-политикой translation/definition (F002 в contract_ui_msg). Архитектурный кандидат backlog.

**Status:** approved

**Verdict:** optimistic UpdateWord vs pessimistic translation/definition — реальная асимметрия, корректно отмечен как backlog-кандидат.

### F022 [analyst] minor

**Description:** Subscriber `observeLexemesByWordId` использует `collectLatest`, но `RefreshLexemeList` reducer идемпотентен — `collect` (без latest) или `distinctUntilChanged` был бы безопаснее: `collectLatest` отменит предыдущую reduce-обработку при новом эмите, что в Mate-цикле (синхронный reduce) бессмысленно.

**Status:** approved

**Verdict:** collectLatest для синхронной идемпотентной reduce-логики бессмысленен и потенциально опасен (cancellation в неудачный момент); collect/distinctUntilChanged безопаснее.

---

## Итерация 2 (2026-05-19T18:00:00-0600)

Артефакт переписан с нуля БЕЗ subscriber'а по conductor-решению. Большинство critical из ит.1 аннулированы как «надуманные».

### F023 [architect] critical

**Description:** `UpdateLexemeTranslation`/`UpdateLexemeDefinition` effects объявлены с `lexemeId: Long` non-null, но текущий UseCase `addLexemeTranslation(wordId, lexemeId: Long?, ...)` имеет nullable lexemeId (handler конвертит -1→null). Расхождение spec↔code не зафиксировано в таблице — либо API меняется на non-null (мёртвая ветка после F021), либо effect сохраняет nullable.

**Status:** approved

**Verdict:** реальный mismatch — API change для `addLexemeTranslation/addLexemeDefinition` не зафиксирован в таблице расхождений.

### F024 [architect] minor

**Description:** `WordLoaded` reducer безусловно переписывает `wordState` без проверки текущего состояния — если по баге `LoadWord` сработает повторно при уже `Loaded`, потеряются локальные `isEditMode`/`edited`/`showWarningDialog`; defensive guard `wordState is NotLoaded` не добавлен.

**Status:** approved

**Verdict:** defensive guard `wordState is NotLoaded` отсутствует, дёшево фиксится.

### F025 [architect] minor

**Description:** `RefreshLexemeList` ветка `WordState.NotLoaded` — комментарий «невозможно: непустой lexemeList ⇒ Loaded (инв. 5)» семантически кривой: после `RemoveLexeme` последней лексемы `lexemeList` пустой, инв.5 не нарушен. Defensive код корректен, но обоснование ошибочно.

**Status:** approved

**Verdict:** обоснование defensive-ветки инвертирует логику инварианта — переформулировать.

### F026 [architect] minor

**Description:** Сноска про backlog `F021 ит.1` в UpdateWord-секции — F021 в `contract_ui_msg` относится к политике «Create*-Msg не шлют effect», не к optimistic-UpdateWord. Ссылка путает.

**Status:** rejected

**Verdict:** ссылка «F021 ит.1» однозначно указывает на ит.1 review `contract_io` (F021 — optimistic UpdateWord backlog), не на `contract_ui_msg`.

### F027 [architect] minor

**Description:** Reducer `RefreshTranslation` в ветке `lexeme.translation == null` уничтожает активный edit пользователя — это документировано в EC1, но reducer-блок не упоминает потерю edited-буфера рядом с кодом, читатель не свяжет.

**Status:** approved

**Verdict:** ветка `translation = null` сносит активный edit; EC1 описывает кейс отдельно — связка не очевидна, добавить inline-ремарку.

### F028 [architect] minor

**Description:** Claim «Mate API позволяет несколько вызовов consumer» в EC2 формально верен, но в существующем `DatasourceEffectHandler` паттерн один-Msg-через-when. Переход к multi-consumer паттерну для CreateLexeme failure не зафиксирован в таблице «Изменения в handler-Msg».

**Status:** rejected

**Verdict:** таблица «Изменения в handler-Msg» уже фиксирует multi-consumer переход для CreateLexeme failure.

### F029 [analyst] critical

**Description:** `contract_ui_msg` v3 в forward-ref таблице декларирует `RefreshTranslation(lexemeId, translation: String?)` / `RefreshDefinition(lexemeId, definition: String?)`, а `contract_io` v2 канонизирует payload как полный `Lexeme`. `contract_ui_msg` явно говорит «точные сигнатуры — в contract_io» (line 139) — формально OK, но reducer-логика веток `CommitTranslationEdit` / `RemoveTranslation` в ui_msg оперирует payload-полем `translation?`, которого больше нет — pseudocode в ui_msg не подровнен под `lexeme.translation?.value`.

**Status:** approved

**Verdict:** cross-artifact contradiction — pseudocode в ui_msg не согласован, требует синхронизации (либо изменить io payload на String?, либо feedback в ui_msg).

### F030 [analyst] minor

**Description:** `UpdateWord` action делает двойной БД-запрос (`updateWord` + `getTermById`). Не описано, что делать при failure второго `getTermById` после успешного `updateWord`: handler пошлёт `ShowNotification`, но БД фактически обновилась — UX-рассинхрон.

**Status:** approved

**Verdict:** реальный пробел контракта на edge case «успех write + failure read».

### F031 [analyst] minor

**Description:** `CreateLexeme` Failure path делает `consumer(CreateLexemeFailed); consumer(ShowNotification(...))` — порядок задан, но артефакт не фиксирует гарантию order preservation в Mate.

**Status:** approved

**Verdict:** артефакт опирается на FIFO consumer-вызовов — гарантия должна быть зафиксирована явно.

### F032 [analyst] minor

**Description:** EC4 «concurrent Update*/Refresh*» утверждает «last-write-wins» — не строго истинно, БД сериализует записи, но handler-Msg могут приходить в неправильном порядке. Не критично для translation/definition update (последовательный ввод), но утверждение декларативно.

**Status:** approved

**Verdict:** ослабить формулировку «при последовательном вводе пользователя last-write-wins; concurrent submit out-of-scope».

### F033 [analyst] minor

**Description:** `RemoveTranslation` edge case: после смены сигнатуры `deleteLexemeTranslation(lexemeId): Lexeme?` контракт «что вернёт UseCase impl при delete несуществующей строки» не зафиксирован — Lexeme с translation=null (no-op success) или null (UX-проёб)?

**Status:** approved

**Verdict:** реальный contract gap — специфицировать поведение impl при delete несуществующей строки (рекомендую Lexeme с null-translation, idempotent no-op).

### F034 [analyst] minor

**Description:** `LoadWord` forward-compat: артефакт декларирует «Concurrent LoadWord не возникает», но не описано что делать если возникнет (через будущий рефактор Reload Msg).

**Status:** rejected

**Verdict:** forward-compat-спекуляция; current contract корректен, out-of-scope IS479.

### F035 [analyst] minor

**Description:** `RefreshTranslation` reducer в ветке `l.translation == null` + non-null payload — race при clock-stuttering двух effect'ов: пользователь успел Create+commit между Remove и его Refresh, активный edit с `isEdit=true` исчезает на следующий recompose, инв.9 счётчик уменьшится. Не описано в EC.

**Status:** rejected

**Verdict:** race-of-race chain требует трёх consecutive user-actions; EC1 покрывает базовый transient, дополнительный chain — over-engineered.

### F036 [analyst] minor

**Description:** `RemoveWord`/`RemoveLexeme` success — silent (без snackbar). Inconsistent с failure которые всегда показывают snackbar. UX-обоснование «слово удалено» feedback отсутствует.

**Status:** approved

**Verdict:** UX-асимметрия — обосновать silent Remove* success или ввести snackbar.

### F037 [analyst] minor

**Description:** Дубликат F023 — `addLexemeTranslation(wordId, lexemeId: Long?, ...)` nullable mismatch с effect `lexemeId: Long`. Также мёртвый fallback `if (effect.lexemeId > -1) ... else null` в handler.

**Status:** rejected

**Verdict:** дубликат F023.

---

## Итерация 3 (2026-05-19T18:35:00-0600)

Точечные правки под ит.2 approved. Architect: 3 minor. Analyst: 1 critical + 3 minor.

### F038 [analyst] critical

**Description:** `CreateLexeme` handler ловит только null-возврат; exception из `addLexeme()` убивает корутину без отправки `CreateLexemeFailed` — `isCreatingLexeme` застревает в `true`, FAB заблокирован до выхода с экрана. Не симметрично с LoadWord/UpdateWord, где stuck-state не возникает.

**Status:** approved

### F039 [architect] minor

**Description:** `deleteLexeme(lexemeId): List<Lexeme>?` не получил idempotent no-op contract по аналогии с F033 — EC `RemoveLexeme` допускает оба возврата (`null` или список) при удалении уже отсутствующей лексемы, асимметричный false-failure snackbar при double-tap race.

**Status:** approved

### F040 [architect] minor

**Description:** В блоках `RemoveLexeme` и таблице расхождений написано «UseCase impl должен достать `wordId` из лексемы перед удалением», но entity `Lexeme` не содержит поля `wordId` — implementer'у потребуется отдельный DAO-lookup, контракт вводит в заблуждение.

**Status:** approved

### F041 [architect] minor

**Description:** `UpdateWord` обосновывает повторный `getTermById` как «fresh Term с обновлённым `changedDate`», но `WordState.Loaded` хранит только `added`, не `changedDate` — реальная цель (resync `value` после нормализации БД) расходится с обоснованием.

**Status:** approved

### F042 [analyst] minor

**Description:** F036 декларирует «failure показывает snackbar потому что визуально ничего не изменилось», но handler `RemoveWord` явно отказывается обрабатывать failure и не шлёт snackbar — внутреннее противоречие обоснования и реализации в одном артефакте.

**Status:** approved

### F043 [analyst] minor

**Description:** `RefreshWord` несёт полный `Term` с `lexemeList`, но reducer использует только `value` и `added`, молча игнорируя `term.lexemeList` — нет ни сужения payload до `(value, added)`, ни обоснования почему лексемы из БД отбрасываются.

**Status:** approved

### F044 [analyst] minor

**Description:** `UpdateLexemeTranslation`/`Definition` success-ветка извлекает `lexeme.translation?.value` как `String?`; при гипотетическом null-возврате translation в успешном `Lexeme` reducer выставит `translation=null`, молча уничтожая значение пользователя — нет defensive-проверки и нет документации почему этот сценарий невозможен.

**Status:** approved

---

## Итерация 4 (2026-05-19T19:05:00-0600)

architect: 2 critical + 3 minor. analyst: 2 critical + 3 minor. **Каскад-delete лексемы в data-слое — реальный архитектурный gap.**

### F045 [architect+analyst] critical

**Description:** Существующий `WordCardUseCaseImpl.deleteLexemeTranslation` при отсутствии definition вызывает `lexemeApi.deleteLexeme(id)` — каскадно удаляет ВСЮ лексему. Симметрично `deleteLexemeDefinition`. Контракт `contract_io` v4 не учитывает: после такой ситуации `getLexemeById` вернёт null → handler выдаст snackbar «Не удалось удалить перевод» при бизнес-успехе, и state останется с фантомной лексемой в `lexemeList` до next LoadWord. Текущий код скрывает это через `Msg.LoadingWord` (full reload после delete).

**Status:** approved

**Verdict:** реальный архитектурный gap — pessimistic Remove path в `CommitTranslationEdit/CommitDefinitionEdit` ветвь 1 опирается на `RefreshTranslation(lexemeId, null)`, но cascade-delete делает этот сценарий некорректным.

### F046 [analyst] critical

**Description:** F040 `deleteLexeme(wordId, lexemeId): List<Lexeme>?` — UseCase impl должен получить список лексем по wordId, но `LexemeApi` не имеет `getLexemesByWord`; единственный путь — `termApi.getTermById(wordId).lexemes` (полный re-fetch term), что противоречит ключевому аргументу контракта «точечный resync без full re-fetch» и не упомянуто в § «Расхождения spec ↔ code».

**Status:** approved

**Verdict:** реальный API gap — либо новый endpoint в LexemeApi, либо признать что resync через getTermById (full term fetch).

### F047 [analyst] minor

**Description:** F044 pessimistic-политика при `lexeme.translation/definition == null` после success Update — state не лечится, но БД фактически в состоянии «translation удалён». Атомарно шлать `RefreshTranslation(lexemeId, null)` — reducer обнулит state в согласие с БД. Минорное противоречие.

**Status:** approved

### F048 [analyst] minor

**Description:** F031 «FIFO consumer-вызовов» формулировка некорректна — Mate не имеет очереди consumer-submission'ов; порядок гарантируется sequential execution внутри одной coroutine handler'а, не «FIFO-обработкой». Терминологическая неточность.

**Status:** approved

### F049 [analyst] minor

**Description:** `addLexeme` impl бросает `IllegalStateException("Dictionary not found")` на отсутствие `CURRENT_DICTIONARY_ID_LONG` в Prefs или dictionary в БД. Formulation edge case «БД-ошибка (UseCase вернул null или бросил exception)» технически ловится try/catch, но не отражает что наиболее частая failure-причина — конфигурация (dictionary не выбран). Пользователю покажется «Не удалось создать лексему», диагностически бесполезный текст.

**Status:** approved

### F050 [architect] minor

**Description:** `RefreshLexemeList` reducer молча принимает порядок из DAO-возврата — порядок UI-списка зависит от ORDER BY в DAO-query, который контракт не фиксирует.

**Status:** approved

### F051 [architect] minor

**Description:** `UpdateWord` пропускает Msg в редком пути `updateWord==true ∧ getTermById==null` — state остаётся с optimistic-применённым значением, никакой Refresh не наступит, ремарка про «следующий LoadWord» нерелевантна — повторный init на том же экране невозможен.

**Status:** approved

---

## Итерация 5 (2026-05-19T19:40:00-0600)

architect: 3 critical + 3 minor. analyst: 1 critical + 4 minor.

### F052 [architect] critical

**Description:** Контракт строит cascade-семантику на ложном инварианте «лексема имеет ≥ 1 суб-сущность» — но `addLexeme` impl создаёт лексему с `translation=null AND definition=null`. Свежесозданная лексема может быть пустой. Cascade — это не enforcement инварианта, а правило «удалить последнюю → удалить целиком».

**Status:** approved

### F053 [architect] critical

**Description:** F033 idempotent no-op конфликтует с cascade. Для лексемы с обоими полями null `canRemoveTranslation() = false` → impl уйдёт в каскад → вернёт `LexemeCascadeRemoved`, не `TranslationRemoved(lexeme)` как требует F033. Граница «idempotent vs cascade» зависит от наличия второй суб-сущности.

**Status:** approved

### F054 [architect] critical

**Description:** F050 ordering `addDate ASC` ссылается на `getLexemesByTermId` DAO-метод, которого нет — лексемы приходят через Room `@Relation` без ORDER BY. Фактический порядок undefined.

**Status:** approved

### F055 [analyst] critical

**Description:** Раздел «Scope артефакта» перечисляет Datasource Msg, но `LexemeCascadeRemoved` пропущен — введён в § «Datasource Msg (канон)» без обновления scope-списка.

**Status:** approved

### F056 [architect] minor

**Description:** EC `RemoveTranslation` содержит dangling reference «если пользователь успел — см. ниже» — продолжения нет, обрыв мысли.

**Status:** approved

### F057 [architect] minor

**Description:** Forward-ref таблица `contract_ui_msg` v3 перечисляет 7 Datasource Msg, но `contract_io` v5 канонизирует 10 — отсутствуют `RefreshWord`, `RefreshLexemeList`, `LexemeCascadeRemoved`, `NavigateBack`. Таблица как inventory устарела.

**Status:** approved

### F058 [architect] minor

**Description:** Reducer `LexemeCascadeRemoved` молча игнорирует случай когда cascade приходит для лексемы которой уже нет в `lexemeList` (race с параллельным `RemoveLexeme`) — `filterNot` no-op'ит, но edge case явно не зафиксирован в EC.

**Status:** approved

### F059 [analyst] minor

**Description:** F045 не специфицирует как impl `deleteLexemeTranslation/Definition` должен сконструировать `lexeme: Lexeme` в `*Removed(lexeme)` — после `update*` нужен дополнительный `getLexemeById`, что меняет нагрузку на БД (2 query вместо 1). Impl-детали умолчаны.

**Status:** approved

### F060 [analyst] minor

**Description:** EC1 (transient window после pessimistic Remove) описывает только `RefreshTranslation(lexemeId, null)` — для лексемы с единственной суб-сущностью тот же race приводит к `LexemeCascadeRemoved(lexemeId)` (исчезает вся лексема), что более агрессивно и должно быть отдельным под-случаем.

**Status:** approved

### F061 [analyst] minor

**Description:** F033 idempotent no-op не покрывает race «cascade удалил лексему + параллельный `deleteLexemeTranslation` на уже несуществующую» — impl выполнит `deleteLexeme` второй раз и должен вернуть `LexemeCascadeRemoved`, контракт явно не закрепляет.

**Status:** approved

---

## Итерация 6 (2026-05-19T20:30:00-0600)

Архитектурный пересмотр под NOT_IN_DB. architect: 3 critical + 0 minor. analyst: 3 critical + 2 minor.

### F062 [architect+analyst] critical

**Description:** Race повторного Commit на NOT_IN_DB-лексеме — два `UpdateLexemeTranslation(lexemeId=null)` подряд (до Refresh) → handler делает два независимых insert → две лексемы в БД вместо одной. Single-flight guard отсутствует.

**Status:** approved

### F063 [architect] critical

**Description:** Race `CommitTranslationEdit` + `CommitDefinitionEdit` на одной NOT_IN_DB-лексеме — оба effect'а летят с `lexemeId=null` → handler делает два независимых insert лексемы → одна локальная превращается в две БД (translation в одной, definition в другой).

**Status:** approved

### F064 [architect] critical

**Description:** UseCase impl `addLexemeTranslation(lexemeId=null, ...)` не требует транзакционности `addLexeme + updateLexemeTranslation` — если addLexeme прошёл, а update упал, остаётся пустая лексема в БД, нарушающая БД-инвариант F052. Транзакционность не зафиксирована в контракте.

**Status:** approved

### F065 [analyst] critical

**Description:** `Msg.NavigateBack` объявлен и в `contract_ui_msg` v3.1 (UI Msg), и в `contract_io` v6 (Datasource Msg) внутри одного `sealed interface Msg` — два `data object` с одинаковым именем в одном sealed не компилируются. Нужно явно зафиксировать что Msg один (shared).

**Status:** approved

### F066 [analyst] critical

**Description:** Reducer `RefreshTranslation`/`RefreshDefinition` (в ui_msg v3.1) безусловно переименовывает любую `NOT_IN_DB`-лексему в `lexemeId` из payload — если в state одновременно есть реальная лексема с `id == lexemeId` (refresh после Update существующей) и независимая `NOT_IN_DB` (создана FAB не закоммичена) — обе ветки сработают, дубль id, нарушение инв.3 уникальности.

**Status:** approved

### F067 [analyst] minor

**Description:** EC8 trade-off противоречив — выбранный «принятый» вариант (после failure `edited=""`, `origin=""`) теряет введённое значение, альтернатива тоже теряет; обе одинаково плохие, выбор без преимущества.

**Status:** approved

### F068 [analyst] minor

**Description:** F054 undefined ordering Room `@Relation` влияет на merge-by-id `RefreshLexemeList` — после `RemoveLexeme` остальные лексемы визуально могут переупорядочиться. Не описан в EC.

**Status:** approved

---

## Итерация 7 (2026-05-19T21:15:00-0600)

Мини-патч isPendingDbOp. architect: 2 critical + 3 minor. analyst: 3 critical + 3 minor.

### F069 [architect+analyst] critical

**Description:** Cross-contract mismatch начального значения `isPendingDbOp` — state v2.5 декларирует `= false`, io v7 line 185 «false стартово», но ui_msg v3.2 line 163 утверждает «начальное `true`» со ссылкой на state. Один из трёх должен уступить.

**Status:** approved

### F070 [architect+analyst] critical

**Description:** Reducer pseudocode для `WordLoaded`/`WordNotFound`/`RefreshWord`/`RefreshLexemeList`/`LexemeCascadeRemoved` не содержит `isPendingDbOp = false` в `state.copy(...)`, нарушая собственный UX-инвариант ит.7. Только `NavigateBack` и `ShowNotification` обновлены — implementer скопирует pseudocode и оставит UI заблокированным.

**Status:** approved

### F071 [analyst] critical

**Description:** EC8 failure-path final state `translation = TextValueState(origin="", isEdit=false, edited="")` нарушает F037 cross-contract assumption (ui_msg) что этот tuple достижим только из `CreateTranslation` без Commit. После EC8 та же форма после Commit-failure — F037-обоснование stale.

**Status:** approved

### F072 [architect] minor

**Description:** EC4 «Concurrent Update*/Refresh*» (l.683-687) — «пользователь в быстрой последовательности коммитит translation дважды» — архитектурно невозможен из-за `isPendingDbOp` блокировки (F062 закрыт). EC4 stale.

**Status:** approved

### F073 [architect] minor

**Description:** io v7 line 419 декларирует «isEdit/edited не трогаем у существующих лексем» для `RefreshTranslation`/`RefreshDefinition`, но pseudocode в ui_msg v3.2 (lines 956-960, 984-988) конструирует свежий `TextValueState(origin, isEdit=false, edited="")` для ветки `l.id == lexemeId` — затирает активный edit. io v7 не отлавливает противоречие с делегатом.

**Status:** approved

### F074 [architect+analyst] minor

**Description:** Data-loss сценарий: при `RemoveLexeme` реальной лексемы во время существования `NOT_IN_DB`-лексемы с in-progress `edited`-буфером — merge выкидывает `NOT_IN_DB` целиком (включая typed text). v7 acknowledged визуальное исчезновение, но не data loss редактируемого буфера. `isPendingDbOp=false` в этот момент, `isCreatingLexeme` гарантирует только FAB-block, не RemoveLexeme через menu.

**Status:** approved

### F075 [analyst] minor

**Description:** F058 `LexemeCascadeRemoved` reducer pseudocode возвращает `state to emptySet()` через `filterNot` no-op — также пропускает обязательный `isPendingDbOp = false` reset (та же ошибка что F070, но на ветке no-op).

**Status:** approved

---

## Итог contract_io (ит.7 close)

Все 7 findings ит.7 (F069-F075) точечно закрыты conductor-патчем без отдельной ит.8:
- F069 (initial isPendingDbOp false vs true) — формулировка в ui_msg v3.2 line 163 поправлена на `false` (согласовано с state v2.5). UI init заблокирован через `isLoading = true` overlay.
- F070 (reducer pseudocode пропускает `isPendingDbOp = false`) — добавлено в 5 pseudocode-блоков: `WordLoaded`, `WordNotFound`, `RefreshWord`, `RefreshLexemeList`, `LexemeCascadeRemoved`.
- F071 (EC8 vs F037 cross-contract assumption) — ремарка добавлена: форма `(translation != null, isEdit = false, origin = "")` после EC8 только для NOT_IN_DB; Cancel-логика остаётся валидной; для реальной лексемы форма не достижима, F037 future-relax потребует пересмотра.
- F072 (EC4 stale) — отмечен как «closed by ит.7», ссылка на Concurrency policy.
- F073 (RefreshTranslation/Definition reducer затирает активный edit) — pseudocode в ui_msg переписан: для существующей лексемы `l.translation?.copy(origin = ...)` сохраняет `isEdit`/`edited`.
- F074 (data-loss NOT_IN_DB при RemoveLexeme через menu) — accepted out-of-scope ремарка в EC `RemoveLexeme`; backlog-кандидат.
- F075 (LexemeCascadeRemoved no-op ветка) — закрыто вместе с F070 (`isPendingDbOp = false` в обеих ветках).

Шаг `contract_io` закрыт на ит.7 без эскалации (max=7 достигнут, все critical закрыты conductor-патчем).

