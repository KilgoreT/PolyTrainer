# Code review: IS482 (Lexeme domain unification)

Branch: `IS482_lexeme_domain_module` | Base: `cacb151` | Date: 2026-05-30

3 параллельных subagent'а: Architecture / Bugs / YAGNI. Каждый finding — `Verify:` через встроенные Claude tools.

## Сводка

| Направление | Critical | Minor |
|---|---|---|
| Architecture | 1 | 4 |
| Bugs | 0 | 0 (PASS) |
| YAGNI | 0 | 3 |
| **Итого** | **1** | **7** |

---

## Architecture

### F-A1 [critical] `QuizGameImpl` force-unwrap `!!.value` — риск NPE при IS481-shim

**Где:** `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/quiz/QuizGameImpl.kt:463, 492, 501, 503`

**Что не так:** После выноса `Lexeme.translation: Translation?` / `Lexeme.definition: Definition?` в cross-module `me.apomazkin.lexeme`, Kotlin smart-cast перестал работать (public property из другого модуля не считается стабильной). Решение через `!!.value` × 4 — runtime force-unwrap. По плану IS481 эти поля станут computed properties (shim над `components`), и `lexeme.translation != null` → `lexeme.translation!!.value` потеряет атомарность: computed может вернуть разные значения между обращениями. NPE при IS481-shim — почти неизбежен.

**Почему важно:** force-unwrap, который business_summary прямо называет «smart-cast workaround» — техдолг с заминированным значением. Идиоматическая фраза `val translation = lexeme.translation` + `translation?.let { ... }` решает обе проблемы (smart-cast и идемпотентность computed) бесплатно.

**Предложение:** заменить шесть `lexeme.translation!!.value` / `lexeme.definition!!.value` на локальные `val translation = lexeme.translation` + `?.let`. 5 минут, убирает риск.

**Verify:** `Grep "lexeme\.(translation|definition)!!" -- modules/screen/quiz/chat/` → 4 hits в QuizGameImpl.kt:463/492/501/503.

**Triage:** → **закрыть в фиче**. Фикс применён: в `WriteQuiz.toQuizItem` вытащены локальные `val translation = lexeme.translation` + `val definition = lexeme.definition` сразу после `val last = ...`. Все 6 `!!.value` заменены на smart-cast по локальным переменным. `./gradlew :modules:screen:quiz:chat:testDebugUnitTest` → EXIT:0. `./gradlew assembleDebug` → EXIT:0. Также добавлено правило **R-CS-001** в `docs/guides/code-style.md` (запрет `!!` в production-коде) — корневой фикс на уровне convention.

---

### F-A2 [minor] Kotlin-plugin version skew: domain/lexeme на 1.9.10, Android-модули на 2.0.20

**Где:** `settings.gradle.kts:16, 21`

**Что не так:** Android-модули — `kotlin.android` 2.0.20; pure-Kotlin (`modules/core/logger`, `modules/domain/lexeme`) — `kotlin.jvm` 1.9.10. infra_summary: «не выравниваем, следуем logger-прецеденту». Domain — публичный контракт между модулями; value-class binary layout у `@JvmInline` менялся 1.9 → 2.0.

**Почему важно:** domain — новая категория, задаёт шаблон для следующих domain-модулей. Если оставить 1.9.10 по прецеденту logger, все будущие domain-модули унаследуют техдолг.

**Предложение:** поднять `org.jetbrains.kotlin.jvm` до 2.0.20 в `settings.gradle.kts:21` (для всех kotlin.jvm-модулей) либо override в `modules/domain/lexeme/build.gradle.kts`.

**Verify:** `Grep "jetbrains.kotlin.jvm"` → `settings.gradle.kts:21: id("org.jetbrains.kotlin.jvm") version "1.9.10"`.

**Triage:** → **закрыть в фиче**. Bump до `2.0.20` в `settings.gradle.kts:21`. Lint/test/build все EXIT:0.

---

### F-A3 [minor] `Lexeme.wordId: Long` non-null живёт ради одного потребителя (dictionaryTab), wordcard/quiz.chat его не используют

**Где:** `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Lexeme.kt:16`

**Что не так:** `wordId` присутствует в domain `Lexeme` исключительно чтобы `Lexeme.toUiItem()` мог его пробросить в `LexemeUiItem.wordId`. Wordcard читает `wordId` только из `Term` (родителя), quiz/chat — вообще не использует. Это инверсия в дизайне: shape domain-сущности продиктован нуждой UI-маппера одного потребителя.

**Почему важно:** прецедент. Следующий domain-модуль не будет задумываться: «domain — объединение всех потребителей». В реальности `LexemeUiItem.wordId` нужен для navigation `wordId → WordCardScreen`, что UI-связь, а не свойство лексемы.

**Предложение:** оставить в IS482 (бриф запрещает расширение scope), но завести запись в `docs/Backlog.md § ВекторныйПиздеж` — пересмотреть в follow-up: вынести `wordId` в `TermUiItem.children` либо обосновать как доменное.

**Verify:** `Grep "lexeme.wordId" -- modules/screen/quiz/chat/ modules/screen/wordcard/` → 0 чтений в feature-коде. Единственный читатель — `LexemeUiItem.kt:25` (UI-маппер).

**Triage:** → **закрыть в фиче**. После research (git log + grep): поле появилось при миграции legacy `DefinitionOld.wordId` (IS297/IS299, дек.2024), механический перенос FK без переосмысления. Production-читателей `lexeme.wordId` — **0**; везде где нужен `wordId` — он берётся из `Term.wordId` (родителя). Удалено из `LexemeApiEntity`, `Lexeme` (domain), `LexemeUiItem`, `LexemeDbEntity.toApiEntity()`, `LexemeMapper`, `Lexeme.toUiItem()`, 5 тестов, `DataHelper`, `LexemeWidget` preview. `LexemeDb.wordId` (FK) **оставлено** — Room требует. Legacy `Definition*Mapper`-ы не тронуты (работают с `LexemeDb.wordId` напрямую). compile/test/lint/build EXIT:0.

---

### F-A4 [minor] Двойной маппинг `it.toDomain().toUiItem()` — две аллокации Lexeme per item ради «архитектурной чистоты»

**Где:** `app/src/main/java/me/apomazkin/polytrainer/di/module/dictionarytab/DictionaryTabUseCaseImpl.kt:108-109, 129-130`

**Что не так:** `term.lexemes.map { it.toDomain() }.map { it.toUiItem() }` — две аллокации Lexeme на каждую лексему, чтобы потом отбросить domain-инстанс. Если контракт UseCase = UI-модель (как scope зафиксировал), нет смысла материализовывать domain — это видимый перформанс-tax ради абстракции которая никому не нужна.

**Почему важно:** текущее решение хуже обеих чистых альтернатив: оно делает «вид» что domain-слой есть, при этом domain ни в каком state не хранится, ни в каком методе не читается.

**Предложение:** (a) добавить прямой mapper `LexemeApiEntity.toUiItem()` в dictionaryTab; (b) или зафиксировать в backlog как known compromise для будущего рефакторинга dictionaryTab в правильный domain-driven слой.

**Verify:** `Grep ".toDomain().toUiItem()|toDomain() }.map { it.toUiItem()"` → `DictionaryTabUseCaseImpl.kt:108-109, 129-130`.

**Triage:** _(заполнится)_

---

### F-A5 [minor] `mapper/` пакет с одним жителем + naming drift (`toDomain` vs `toDomainEntity`)

**Где:** `app/src/main/java/me/apomazkin/polytrainer/mapper/LexemeMapper.kt:9` + `QuizChatUseCaseImpl.kt:114, 119`, `WordCardUseCaseImpl.kt:199`

**Что не так:** `LexemeMapper.kt` в `app/.../mapper/` — единственный житель пакета (lookahead pattern). Остальные mapper'ы (`WordApiEntity.toDomainEntity`, `WriteQuizComplexEntity.toDomainEntity`, `TermApiEntity.toDomainEntity`) живут inline в `di/module/<feature>/`. Naming тоже разъехался: `toDomain` (новый), `toDomainEntity` (старые).

**Почему важно:** конвенция уже неконсистентна. Через год кто-то добавит в `mapper/` что-то непохожее, и пакет потеряет смысл.

**Предложение:** либо переместить `LexemeMapper.kt` обратно в `di/module/common/` (рядом с другими мапперами), либо в `docs/Backlog.md` зафиксировать миграцию остальных мапперов в `mapper/` + унификацию имени `toDomain`.

**Verify:** `Grep "fun .*ApiEntity.*toDomain" -- app/src/main/` → 4 файла: `Lexeme` (`toDomain`), `Word`/`WriteQuiz`/`Term` (`toDomainEntity`).

**Triage:** → **rejected** в IS482 + **backlog** «Repository pattern refactor — mapper API→Domain в core-db-impl, CoreDbApi возвращает domain» (см. `docs/Backlog.md` § Архитектура). Локация в `app/.../mapper/` соответствует current convention `data-layer.md`. Унификация остальных mapper'ов (Word/Term/WriteQuiz) и переход на Repository pattern — отдельная фича, большой refactor контракта data-API.

---

## Bugs

**PASS** — серьёзных багов в изменениях IS482 не найдено.

Изменения чисто механические: единый domain-модуль, общий API→domain mapper, миграция импортов. Поведение mapper'ов сохранено относительно прежних `toDomainEntity` функций (verified diff с HEAD).

Проверены потенциально-опасные места (все чисты):
- `QuizGameImpl.kt:463,492,501,503` smart-cast workaround — `!!` под immediate guard в `when`, ref immutable. Безопасно но хрупко (см. F-A1).
- `LexemeMapper.kt` nullable propagation через `?.let` — корректно, покрыто unit-тестами.
- `WordCardUseCaseImpl.toDomain` — sort preserved, поведение идентично прежнему.
- `DictionaryTabUseCaseImpl` double-mapping — функционально эквивалентно прежнему inline mapping.
- `WriteQuiz.lexeme` set-equality через mutableSetOf — `wordId` стабильный, дедупликация работает идентично.

---

## YAGNI

### F-Y1 [minor] `LexemeId` value-class сразу разворачивается на каждой границе

**Где:** `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Lexeme.kt:6`

**Что не так:** `LexemeId` создаётся только в mapper и читается через `.id` для немедленного unwrap в `Long` на каждом call-site. Все 4 поля `Msg.*.lexemeId`, `DatasourceEffect.*.lexemeId`, `LexemeState.id`, `LexemeUiItem.id`, `QuizItem.QuizInfo.lexemeId`, `state.lexemeIdPendingDelete` — `Long`, не `LexemeId`. Value-class живёт один hop. Также провоцирует `!!.value` workarounds в QuizGameImpl.

**Почему важно:** заявленная цель «типобезопасность» фактически не достигнута — ни одна функция/state/msg не принимает `LexemeId` как параметр. Абстракция-однодневка.

**Предложение:** либо распространить `LexemeId` через `Msg.*` / `Effect.*` / `LexemeState.id` / `QuizItem.QuizInfo.lexemeId`, либо откатить до `val lexemeId: Long` в `Lexeme`. Текущий компромисс «value-class только в domain» = cost без benefit.

**Verify:** `Grep "LexemeId("` → создаётся в LexemeMapper.kt:10 + 7 тестовых; `Grep "lexemeId.id"` → 7 unwrap-сайтов в production.

**Triage:** → **rejected** — пусть остаётся как есть. Type safety материализуется только в domain layer (где конструктор `LexemeId(id)` и `Lexeme.lexemeId` тип); за пределами domain unwrap в `Long` приемлем для current scope.

---

### F-Y2 [minor] `Lexeme.wordId` dead в production

**Где:** `modules/domain/lexeme/src/main/java/me/apomazkin/lexeme/Lexeme.kt:16`

**Что не так:** `wordId` в domain `Lexeme` non-null «нужно dictionaryTab». Но `LexemeUiItem.wordId` тоже dead — `LexemeWidget.kt` показывает только translation/definition, `wordId` никто на UI не читает. Каскад: dead в UI → dead в domain.

**Почему важно:** каждый новый член data class — нагрузка на passthrough-тесты + шум в конструкторах. По YAGNI domain shape должна быть строго тем что реально читают потребители.

**Предложение:** удалить `wordId` из `Lexeme` (+ `LexemeUiItem` — но это вне scope, в backlog). Если понадобится — добавить тогда.

**Verify:** `Grep "lex.wordId|lexeme.wordId|ui.wordId|item.wordId"` → только два чтения в `LexemeUiItemTest.kt:94,140` (тесты passthrough); 0 production reads. `LexemeWidget.kt:18-41` использует только translation/definition.

(Дублирует F-A3 с другой точки зрения.)

**Triage:** → **закрыть в фиче** (закрыто вместе с F-A3 — это один и тот же фикс).

---

### F-Y3 [minor] Over-testing passthrough — половина из 21 теста дублирует roundtrip

**Где:** `app/src/test/.../LexemeMapperTest.kt:97-132, 139-145` + `dictionaryTab/.../LexemeUiItemTest.kt:89-124`

**Что не так:** В `LexemeMapperTest` 4 отдельных теста «X passed through» (wordId/addDate/changeDate × 2) плюс id wrap, translation null/non-null, definition null/non-null. Затем full-roundtrip покрывает все эти passthrough заново. Тест «wordClass and options ignored» (`:139-145`) проверяет тавтологию — domain не имеет этих полей, compile-time контракт.

**Почему важно:** business_summary гордится числом «21 тест зелёные», но половина — boilerplate без incremental coverage. При добавлении поля → 2-3 теста на одно поле. Тест ignore wordClass/options проверяет несуществующее.

**Предложение:** оставить nullable propagation (translation null/non-null, definition null/non-null, changeDate null/non-null = 6) + по одному full roundtrip с реальными значениями (×2). Итог ~16 тестов вместо 21. Удалить тавтологию о wordClass/options.

**Verify:** Read `LexemeMapperTest.kt:97-132` (4 passthrough), `:148-168` (full roundtrip покрывает), `:139-145` (тавтология).

**Triage:** → **rejected** — после F-A3 fix (удаление `wordId`) тесты уже сократились. Оставшиеся «passthrough X» + «full roundtrip» — canonical TDD pattern. Cleanup ради cleanup не стоит времени.

---

## Triage Summary

- **Закрыть в фиче (3):**
  - F-A1 — force-unwrap `!!.value` в QuizGameImpl → локальные переменные + smart-cast. Добавлено R-CS-001 в `code-style.md` (запрет `!!` в production).
  - F-A2 — Kotlin version skew → `kotlin.jvm` 1.9.10 → 2.0.20 в `settings.gradle.kts:21`.
  - F-A3 / F-Y2 — `Lexeme.wordId` zombie field → удалён из API/Domain/UI/Mapper/tests/preview. `LexemeDb.wordId` (FK) оставлен.

- **Backlog (1):**
  - F-A5 — Repository pattern refactor (mapper в `core-db-impl`, `CoreDbApi` возвращает domain) → запись в `docs/Backlog.md § Архитектура`.

- **Rejected (3):**
  - F-A4 — двойной маппинг `API→domain→UI` это feature не bug: domain как source of truth + UI value-class как stable Compose type.
  - F-Y1 — `LexemeId` value-class в domain, Long везде в state/UI — type safety материализуется только в domain layer; за пределами unwrap приемлем.
  - F-Y3 — passthrough + full-roundtrip это canonical TDD pattern; cleanup ради cleanup не оправдан.

**Build/Lint/Test после всех фиксов:** все три EXIT:0.

_model: Opus 4.7 (1M context)_
