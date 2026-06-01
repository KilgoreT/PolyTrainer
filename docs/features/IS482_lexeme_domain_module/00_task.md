## Задача

Вынести доменную сущность `Lexeme` в новый общий модуль **`modules/domain/lexeme`** (новая категория `modules/domain/` наряду с `modules/screen/`, `modules/widget/`, `modules/core/`).

Что входит в модуль:
- **Domain entities:** `Lexeme`, `LexemeId`, value classes которые останутся (`Translation`, `Definition`). После IS481 добавятся `ComponentValue`, `ComponentValueId`.
- **Computed extensions:** `Lexeme.translation`, `Lexeme.definition` (после IS481 — как compatibility shim над `components`), `Lexeme.builtIn(key)`.
- **Domain operations:** chain extensions для state mutations над Lexeme (типа `enableEdit()` для `TextValueState` — сейчас в `wordcard/mate/State.kt`, но они общие).
- **Один маппер** `LexemeApiEntity → Lexeme`.

Все три feature-модуля переключаются на этот общий модуль, свои `Lexeme.kt` удаляют:
- `modules/screen/wordcard/entity/Lexeme.kt`
- `modules/screen/quiz/chat/entity/Lexeme.kt`
- `modules/screen/dictionaryTab/entity/LexemeUiItem.kt`

## Что НЕ делаем

- Не выносим **Quiz** domain в общий модуль (отдельная история — над quiz есть логика в mate, не только сущности).
- Не делаем функционал IS481 (компоненты) — отдельная фича.
- Никаких изменений UI / UX.

## Контекст

**GitHub issue:** https://github.com/KilgoreT/PolyTrainer/issues/482

**Зависимость с IS481:** желательно **до** IS481 (#481) — иначе при миграции придётся плодить `@Deprecated` computed extensions в трёх местах. Если **после** — больше tech debt в трёх копиях, потом единый refactor выпиливает их.

**Источник решения:**
- `docs/Backlog.md` § ВекторныйПиздеж — запись «Domain unification: `modules/domain/lexeme`».
- `docs/features/IS481_lexeme_component_constructor/05_migration_strategy_review.md` Impact #4.

**Затронутые модули:**
- Новый: `modules/domain/lexeme` (создаётся в этой фиче).
- Изменяются: `modules/screen/wordcard`, `modules/screen/quiz/chat`, `modules/screen/dictionaryTab`.
- Возможно: `app` (Gradle dependency wiring).

## Объём

Средний: новый модуль + миграция трёх feature-модулей + проверка тестов в каждом.

_model: Opus 4.7 (1M context)_
