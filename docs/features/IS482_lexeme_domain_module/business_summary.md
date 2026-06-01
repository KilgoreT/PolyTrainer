---
status: done
---

# Summary — business (IS482)

## Что сделано

- **Domain modul `modules/domain/lexeme`** заполнен реальными типами: `LexemeId` (value-class над `Long`), `Translation` / `Definition` (value-классы над `String`), `Lexeme` (data class с полями `lexemeId`, `wordId: Long`, `translation?`, `definition?`, `addDate`, `changeDate?`). Pure-Kotlin, без Android. Placeholder `Lexeme.kt` переписан финальным shape.
- **Общий API → domain mapper** `LexemeApiEntity.toDomain(): Lexeme` создан в `app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt`. Single-expression body. Заменяет три копии: два top-level `toDomainEntity` в wordcard/quiz-chat UseCaseImpl + inline-блок в dictionaryTab.
- **Domain → UI mapper** `Lexeme.toUiItem(): LexemeUiItem` добавлен в `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/entity/LexemeUiItem.kt` — рядом с UI-моделью.
- **Миграция консьюмеров на общий domain:**
  - wordcard main: `Term.kt`, `deps/WordCardUseCase.kt` (7 сигнатур), `mate/State.kt`, `mate/Message.kt` — переключены импорты `wordcard.entity.Lexeme` → `me.apomazkin.lexeme.Lexeme`. Удалён старый `wordcard/entity/Lexeme.kt`.
  - quiz/chat: `WriteQuiz.kt` импорт переключён, `QuizGameImpl.kt` правка `.lexeme.id` → `.lexeme.lexemeId.id` плюс four `!!.value` для smart-cast после кросс-модульного переноса. Удалён старый `quiz/chat/entity/Lexeme.kt`.
  - dictionaryTab UseCaseImpl: inline-маппинг `Api → UI` заменён на двойной `Api → domain → UI`. Публичный контракт UseCase (возврат UI-моделей `TermUiItem` / `LexemeUiItem`) сохранён.
- **Три `UseCaseImpl` в `app/`** мигрированы: `WordCardUseCaseImpl` (удалён локальный `toDomainEntity` для Lexeme, 7 call-sites переключены на `.toDomain()`), `QuizChatUseCaseImpl` (удалён локальный mapper, call-site переключён), `DictionaryTabUseCaseImpl` (двойной маппинг). `TermApiEntity.toDomainEntity` оставлен — Term не унифицируется в IS482.
- **21 unit-тест** (LexemeMapperTest 11 + LexemeUiItemTest 10) — ВСЕ зелёные. Покрыты nullable propagation, value-class wrap/unwrap, passthrough, контрактное игнорирование `wordClass`/`options`.
- **Спека** `docs/features-spec/lexeme-domain.md` опубликована (~165 строк); META-комментарий и раздел «Тестовые сценарии» из черновика убраны.

## Ключевые решения

- **Pure-Kotlin domain** (без Android-зависимостей) — модуль готов как первый житель новой категории `modules/domain/`.
- **`category` / `wordClass` / `options` ИСКЛЮЧЕНЫ** из domain `Lexeme` по YAGNI: `category` всегда `null` в маппере wordcard, `wordClass`/`options` не читает ни один потребитель. 0 readers `.category` в коде.
- **`wordId: Long` non-null** — соответствует `LexemeApiEntity.wordId: Long`; нужно dictionaryTab при маппинге в `LexemeUiItem`.
- **`LexemeId(Long)` value-class** — типобезопасность; quiz/chat и dictionaryTab мигрируют с сырого `Long`.
- **Mapper в `app/`** (не в `modules/domain/lexeme`) — соответствует `data-layer.md` § «API → Domain в UseCase модуле», Dependency Rule (domain не знает API).
- **DictionaryTabUseCase сохраняет публичный UI-контракт** (`LexemeUiItem` не удалён) — двойной маппинг локализован внутри Impl. Альтернатива (возврат domain) выводила бы за scope (нужно переписывать mate-консьюмеров).
- **Все value-классы в одном `Lexeme.kt`** — совместное использование с data class, малый размер (~25 строк), соответствие текущему стилю проекта.
- **`Lexeme.toUiItem()` рядом с `LexemeUiItem`** — UI-маппинг живёт в UI-слое; симметрия с расположением общего API→domain mapper'а в UseCase-слое.
- **`chain_extensions_location` (TextValueState.enableEdit)** НЕ перенесены в общий domain — остаются в wordcard mate (см. возражение из scope).
- **Smart-cast workaround в QuizGameImpl** (`!!.value` в 4 местах) — естественное следствие cross-module переноса публичных `Lexeme.translation: Translation?` / `Lexeme.definition: Definition?`. Минимально-инвазивный фикс, семантика идентична.

## Артефакты

- `business_walkthrough.md` — факты о трёх формах Lexeme (wordcard / quiz-chat / dictionaryTab) + три места маппинга, сравнительная матрица полей.
- `business_contract.md` — Domain shape / Mapper signature / UseCase changes; обоснования по полям и локации mapper'а.
- `business_contract_review.md` — verdict: approved.
- `business_contract_spec.md` — черновик публичной спеки (META `spec_filename`).
- `business_design_tree.md` — DAG 19 узлов: 1 создание (mapper), 16 изменений, 2 удаления; топологический порядок без циклов.
- `business_test.md` — TDD тесты (21 кейс), framework JUnit 4 + `org.junit.Assert.*`.
- `business_implement.md` — реализация + build/test SUCCESS, заметки по smart-cast и test-литералам.
- `business_publish_spec.md` — опубликовано в `docs/features-spec/lexeme-domain.md` as-is относительно черновика; META и «Тестовые сценарии» убраны.

## Что НЕ делалось

- **mate-слой** (State / Msg / Effect/IO) — НЕ менялся (рефакторинг, не feature). Меняли только импорты типа `Lexeme`.
- **Composables / UI** — без изменений (`ui_touched=false`).
- **БД / data слой / `core-db-api` / `core-db-impl`** — без изменений; `LexemeApiEntity` остаётся source of truth для I/O.
- **CI workflow** — не трогался (infra-решение).
- **ProGuard / Manifest** — не нужно для pure-Kotlin domain.
- **`wordClass` / `options` / `category`** — не пробрасывались в domain (out of scope).
- **`Term` миграция в общий domain** — Term остаётся в wordcard feature-модуле; унификация только `Lexeme` и его value-классы.
- **dictionaryTab tech-debt отсутствия domain-слоя** — не решался; UseCase продолжает возвращать UI-модели.
- **PUML-схемы** — в feature dir нет, шаг копирования пропущен.

## log_messages

- IS482 business_summary создан со `status: done`; все 8 business-шагов (walkthrough → contract → contract_review → contract_spec → design_tree → test → implement → publish_spec) выполнены, build/tests зелёные.
- Контракт IS482 выполнен полностью: единый domain-модуль `modules/domain/lexeme`, общий mapper в app/, миграция трёх feature-модулей и трёх UseCaseImpl, два старых `Lexeme.kt` удалены, 21 unit-тест проходят.
- Out-of-scope позиции зафиксированы как осознанные исключения (`wordClass`/`options`/`category`, Term unification, dictionaryTab UI→domain переход) — основа для будущих задач (в т.ч. IS481).

_model: Opus 4.7 (1M context)_
