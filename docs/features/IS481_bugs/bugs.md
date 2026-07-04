# IS481 — Баги карточки слова (после последних изменений)

**Тикет:** IS481
**Ветка:** `IS481_lexeme_component_constructor` (коммитим сюда же)

После последних изменений в WordCard (generic-компоненты `63cedb8` + rename `isMulti→isMultiple` + миграция-collapse + UI-термины) появились баги. Логируем и чиним по TDD (red-тест → фикс).

## Контекст последних изменений (кандидаты в причины)

- `63cedb8` — переписан WordCard под generic ComponentValue (reducer/handlers/flush-on-back).
- `d267322` — `isMulti → isMultiple` по всем модулям.
- `c563c74` — схлопывание миграций 11→12, колонка `is_multi → is_multiple`.
- `0d002be` — UI-термины (Охват/Общий/Пословарный/Несколько).

## Баги

_TBD — заполнить по описанию. Формат на каждый баг:_

### BUG-1: Нет чипа «Перевод» в новом черновике лексемы (пустой словарь)
- **Шаги:** новый словарь (компонентов нет) → создать слово «book» → открыть карточку → тап FAB «создать лексему».
- **Ожидается:** пустой черновик с чипом «Перевод» (built-in global), можно ввести перевод. (S1 сценарий: «new dictionary loads single translation chip».)
- **Факт:** черновик появляется, но чипов нет — только кнопка «Удалить». Добавить перевод нельзя. (см. `img.png`)
- **Экран/место в коде:** WordCard → `LexemeComponentsBlock`/`ComponentChipsRow` (пустой `availableComponentTypes`). Подозрение: `flowTypesForDictionary` / поток типов не возвращает global translation для нового словаря; reducer-тесты мокают `ComponentTypesLoaded([translation])`, поэтому проходят — реальный data-flow не покрыт.
- **Диагноз (доказан):** НЕ баг кода — dev-only регресс БД. На тестовом девайсе лежала dev-БД v13, приложение после схлопывания миграций стало v12 → Room не нашёл путь 13→12 → destructive fallback стёр всё (поэтому словарь и был «новым»). Seed built-in translation висит на `Callback.onCreate`, а после destructive-пересоздания Room зовёт `onDestructiveMigration`+`onOpen`, но НЕ `onCreate` (проверено по исходнику Room 2.8.4, `RoomConnectionManager.onMigrate`: dropAllTables → callback → createAllTables). Факт с девайса: `user_version=12`, `component_types` — 0 строк. Схема v13 нигде кроме этого девайса не существовала → в проде путь недостижим.
- **Решение:** переустановка приложения (fresh install → `onCreate` → seed). Кода не касаемся. Идея-страховка «seed в `onOpen` (идемпотентный INSERT OR IGNORE)» — в Backlog.
- **Статус:** закрыт (не баг — dev-окружение)

### BUG-2: Диалог удаления лексемы не закрывается после подтверждения
- **Шаги:** лексема есть → тап «Удалить лексему» → модалка подтверждения → тап «Удалить».
- **Ожидается:** лексема удаляется, модалка закрывается.
- **Факт:** лексема удаляется (под окном), но **модалка остаётся открытой**.
- **Место в коде:** `WordCardReducer.RemoveLexeme` не сбрасывал `lexemeIdPendingDelete` (диалог показывается пока `lexemeIdPendingDelete != null`). Reducer-level баг — reducer-тесты должны были поймать, но кейса не было: `UndoDeleteTest` проверял только эффект/список лексем, а существовавшие кейсы диалога покрывали лишь Open/Close (отмену), не путь подтверждения.
- **Фикс (TDD):** red-тесты `RemoveLexeme closes confirm dialog` + `RemoveLexeme NOT_IN_DB closes confirm dialog` в `UndoDeleteTest` (падали: expected null, was 8 / -1) → фикс: оба пути ветки `RemoveLexeme` сбрасывают `lexemeIdPendingDelete = null` → 221/221 green. Сценарий L3.1 дополнен (диалог закрывается по подтверждению, не дожидаясь ответа БД).
- **Статус:** исправлен

---
