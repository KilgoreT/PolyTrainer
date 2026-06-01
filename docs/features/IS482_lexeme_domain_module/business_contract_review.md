# Review: business_contract

## Findings

(пустой — блокирующих замечаний нет)

### Проверка спорных точек sub-agent'а

- **disp_1 (`category` исключён, не nullable)** — обосновано.
  Verify: `Lexeme.category` нигде не читается в коде.
  - `wordcard/mate/State.kt:245-261` (`Lexeme.toLexemeState()`) обращается только к `lexemeId.id`, `translation.value`, `definition.value` — `category` не читается.
  - Поиск `.category` в `modules/screen/wordcard/src/`, `modules/screen/quiz/chat/src/`, `modules/screen/dictionaryTab/src/` — нет совпадений.
  - Поиск `lexeme.category` в `app/src/` и `modules/` — нет совпадений.
  - Маппер `WordCardUseCaseImpl.kt:221` единственный, кто пишет в поле, и пишет туда `null`.
  - Вывод: исключение `category` из union-shape по YAGNI валидно. После выноса domain в `modules/domain/lexeme` нет risk-зоны «поле читалось — теперь compile error», потому что поле не читалось.

- **disp_2 (`wordId: Long` non-null)** — обосновано.
  Verify: `LexemeApiEntity.wordId: Long` — non-null (`core/core-db-api/.../LexemeApiEntity.kt:13` из walkthrough § 4). dictionaryTab inline-маппер (`DictionaryTabUseCaseImpl.kt:108-121`, `:140-153`) пробрасывает `defMate.wordId` без null-проверки. Если в domain сделать `wordId: Long?` — потребовалось бы добавлять `?: error()` либо `!!` в маппере dictionaryTab без причины. Non-null корректно, информация не теряется.

- **disp_3 (`DictionaryTabUseCase` контракт сохраняет UI-возврат, двойной маппинг внутри Impl)** — соответствует scope.
  Verify: `02_scope.md` § Аспекты `ui_layer_dictionarytab` (строки 84-86) фиксирует: «Маппинг `domain.Lexeme → LexemeUiItem` живёт в dictionaryTab (mate/reducer либо UseCaseImpl — точное место выбирает business sub-flow)». Контракт `DictionaryTabUseCase` сохраняется по тем же причинам, что и в scope § Sub-flow `UI — нет`: правка публичного контракта потребует переписки `State.termListMap`/reducer'ов dictionaryTab — это out of scope IS482 (бриф «без изменений UI/UX»). Двойной маппинг `Api → domain → UI` — это «цена соответствия» общему mapper'у в `app/` при сохранении текущего стиля dictionaryTab. Решение согласовано со scope.

> 📎 guide: docs/guides/data-layer.md § Маппинг сущностей — трёхслойная схема `Room Entity → API Entity → Domain Entity` через extension-функции; `Api → domain → UI` в dictionaryTab сохраняет этот контракт.

### Проверка согласованности

- **Domain shape ↔ Mapper signature** — согласованы. Domain `Lexeme(lexemeId, wordId, translation, definition, addDate, changeDate)` соответствует полям, которые пишет mapper `LexemeApiEntity.toDomain()`. `wordClass`/`options` из API не пробрасываются — это явное решение в § Domain shape и § Mapper signature § Замечания.

> 📎 guide: docs/guides/data-layer.md § Маппинг сущностей — extension-функция вида `ApiEntity.toDomainEntity()` (здесь `toDomain()`) в UseCase-модуле, маппит из API в domain.

- **UseCase ↔ Domain shape** — согласованы. Wordcard методы возвращают `Lexeme`/`List<Lexeme>?` из общего пакета `me.apomazkin.lexeme`. QuizChat транзитивно через `WriteQuiz.lexeme`. DictionaryTab сохраняет UI-контракт, но внутри Impl использует общий domain.

> 📎 guide: docs/guides/data-layer.md § UseCase — интерфейс в feature-модуле, реализация в app-модуле; возврат domain-сущностей соответствует архитектуре.
>
> 📎 guide: docs/guides/naming.md § Пакеты — `modules/domain/<name>/` (т.е. `me.apomazkin.lexeme`) для общих domain-моделей.

- **Naming** — domain без суффикса (`Lexeme`, `Translation`, `Definition`), UI с префиксом `Ui` (`LexemeUiItem`, `TranslationUiEntity`, `DefinitionUiEntity`), mapper-метод `toDomain()` (не `toDomainEntity()`) согласно отсутствию суффикса `Entity` у domain-классов. `naming.md` применён корректно.

> 📎 guide: docs/guides/naming.md § Сущности по слоям + R-N-011 — Domain без суффикса (`Lexeme`), UI с префиксом `Ui` + тип (`*UiItem`, `*UiEntity`); domain `data class` свой на каждый слой.

- **Data-layer guide** — mapper в `app/` (UseCase-модуль) соответствует `data-layer.md` § «Маппинг сущностей». Scope явно зафиксировал это решение (аспект `mapper_location`).

> 📎 guide: docs/guides/data-layer.md § UseCase — `WordCardUseCaseImpl` живёт в app-модуле и инжектит `CoreDbApi.*Api`; место маппера в `app/` согласовано с этим расположением.

- **Факты walkthrough учтены** — все три формы Lexeme (wordcard / quiz.chat / dictionaryTab.UiItem) отражены; `LexemeApiEntity.wordId`/`wordClass`/`options` рассмотрены явно; миграция `lexeme.id` → `lexeme.lexemeId.id` в `QuizGameImpl.kt:511` упомянута в § State и § Domain shape `LexemeId`.
- **Scope не расширяется** — `category` / `wordClass` / `options` явно отклонены с отсылкой к брифу «без изменений поведения». UI-контракт `DictionaryTabUseCase` не трогаем. State/Msg/Effect/IO помечены N/A и обоснованы.

## Verdict

verdict: approved

_model: Opus 4.7 (1M context)_

## log_messages

- Проверил три спорные точки sub-agent'а: `category` (исключение валидно — поле нигде не читается), `wordId: Long` non-null (обосновано через non-null в API), `DictionaryTabUseCase` контракт (соответствует scope § ui_layer_dictionarytab).
- Verify по `.category`: 0 совпадений в `modules/screen/{wordcard,quiz/chat,dictionaryTab}/src/` и `app/src/` — единственный writer `WordCardUseCaseImpl.kt:221` пишет `null`, ни одного reader.
- Внутренняя согласованность Domain shape ↔ Mapper signature ↔ UseCase — без расхождений; гайды (naming, data-layer) применены корректно.
