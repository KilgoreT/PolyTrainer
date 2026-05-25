# publish_spec

## Опубликовано

- `docs/features-spec/wordcard.md` — **создан** (новый файл, ранее в `docs/features-spec/` спеки для WordCard не было — раздел «Карточка слова (WordCard)» в `README.md` стоял без ссылки).
- Имя файла: `wordcard.md` (из META-комментария `spec_filename: wordcard.md` в шапке `business/contract_spec.md`).
- Размер изменений: ~442 строки (полный финальный документ).
- Дополнительно: `docs/features-spec/README.md` — раздел «По экранам» обновлён, `Карточка слова (WordCard)` теперь содержит ссылку на `wordcard.md`.

## Корректировки от implement

**Без изменений: спека опубликована as-is относительно `business/contract_spec.md` v1.**

`business/impl.md` фиксирует корректировки, внесённые в коде на ит.2 (мини-патч после review):

1. **Reducer guards `!isEdit ⇒ ignore` для `Commit*Edit` / `Cancel*Edit`** (architect F-arch-1, F-arch-2). Это уточнение reducer-логики, **уже декларированное** в спеке: таблица guards содержит `lexeme.translation == null ∨ !lexeme.translation.isEdit` для `CommitTranslationEdit` / `CancelTranslationEdit` (симметрично для Definition).
2. **`OpenLexemeMenu(isShow=true)` закрывает `isMenuOpen` у других лексем** (architect F-arch-3). Уже декларировано — раздел «Reducer — ключевые правила» формулирует `closeAllEditModes()` как обязательный шаг «перед открытием любого нового edit-mode (Enter*/Create*/CreateLexeme/OpenLexemeMenu(isShow=true))».
3. **`catch (e: Throwable) → catch (e: Exception)` + `LexemeLogger` injection** (senior critical) — детали реализации handler'а / UseCaseImpl, не контракт. В спеке не отражается.
4. **Инверсия default в `Msg.isGuardedByPending()`** (architect/senior) — деталь реализации reducer helper, не контракт. В спеке не отражается.

Иными словами: ит.2 не вносила **значимых** изменений контракта (новых Msg, Effect, полей State или методов UseCase) — только уточнения уже декларированных правил и детали реализации. Спека публикуется в редакции черновика v1.

## PUML

**PUML-схем в проекте не найдено** (`find . -name "*.puml"` — пусто как в `docs/features/IS479_wordcard_lexeme_inline/`, так и во всём репозитории за вычетом `node_modules`/`.git`/`build`). Шаг копирования PUML пропущен.

## Лог итераций

### ит.1 (2026-05-21T17:50:00-0600)

1. Прочитан `business/contract_spec.md` v1 (467 строк с META + лог итераций) — финальный черновик спеки.
2. Прочитан `business/impl.md` — выявлены ит.2 корректировки реализации (4 пункта, все либо уже отражены в контракте, либо детали реализации).
3. Создан `docs/features-spec/wordcard.md` — 442 строки. Удалены при копировании:
   - HTML-комментарий `<!-- META: spec_filename: wordcard.md -->` (служебная мета черновика).
   - Раздел `## Лог итераций` с ит.1 черновика (служебный артефакт draft'а, не часть проектной спеки — per `publish_spec.md` prompt § 2.4 «Что НЕ должно попасть в спеку»).
   - Финальный `_model:_` маркер (служебный).
4. Сама структура спеки сохранена 1:1: Бизнес-описание / User Stories / State / UI Messages / IO / UseCase / Тестовые сценарии — формат соответствует декларации в `FORGEFLOW_contract_design.md` § «Формат проектной спеки».
5. Заголовок начинается с `# WordCard — карточка слова` (русский, без префикса IS479) — соответствует output_criteria шага.
6. Обновлён `docs/features-spec/README.md`: пункт `- Карточка слова (WordCard)` заменён на `- [Карточка слова (WordCard)](wordcard.md)`.
7. PUML-шаг пропущен — в проекте `.puml` файлов нет.

---

_model: claude opus 4.7 (1M context)_
