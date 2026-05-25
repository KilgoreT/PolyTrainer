## Итерация 1 (2026-05-19T05:55:00-0600)

### F001 [architect] minor

**Description:** Forward-ref таблица Datasource Msg не упоминает success-Msg для `DatasourceEffect.RemoveLexeme` — описание `RemoveLexeme` ссылается на «фактический возврат из БД» без указания какой именно Msg удалит лексему из `lexemeList`.

**Status:** rejected

**Verdict:** Out-of-scope. Forward-ref таблица помечена как гипотеза, полный набор Datasource Msg каноном живёт в `contract_io`.

### F002 [architect] minor

**Description:** Несимметрия политики обнуления nullable суб-сущности: `CommitTranslationEdit` ветка 1 сразу `translation = null`, а `RemoveTranslation` ждёт `RefreshTranslation(translation=null)`.

**Status:** approved

**Verdict:** Несимметрия реальна. Два пути к одному результату с разной reducer-политикой; либо обосновать, либо унифицировать.

### F003 [architect] minor

**Description:** `RemoveTranslation` / `RemoveDefinition` не имеют явного guard'а на `translation == null` / `definition == null` (только общая сноска про «лексема не найдена»).

**Status:** approved

**Verdict:** Реально. В таблице guards записей нет, в описании Msg явный guard не указан. Если поле уже null — отправка effect бессмысленна.

### F004 [architect] minor

**Description:** `RemoveWord(wordId)` несёт payload `wordId`, дублирующий `state.wordState.id` — обоснование payload-формы отсутствует.

**Status:** approved

**Verdict:** Реально. `CommitWordChanges` и `CreateLexeme` берут id из state без payload — `RemoveWord` выделяется без обоснования.

### F005 [analyst] minor

**Description:** `RemoveTranslation`/`RemoveDefinition` Msg предполагают наличие пунктов меню «Удалить перевод/определение» в `LexemeState.isMenuOpen`, но макет `9154-82519` явно показывает только общую кнопку «Удалить» лексемы.

**Status:** rejected

**Verdict:** Out-of-scope. `contract_ui_msg` фиксирует Msg + reducer-логику, не финальные пункты UI-меню. Msg валидна минимум для пути «пустой Commit ⇒ удаление».

### F006 [analyst] minor

**Description:** Cancel*Edit ветка `origin.isEmpty()` теоретический недостижимый путь — формальной дыры нет.

**Status:** rejected

**Verdict:** Сам ревьюер признаёт «формальной дыры нет». Ветка покрывает легитимный кейс (Create → Cancel сразу).

---

## Итог

3 approved (все minor), 3 rejected. **approved_critical = []** → `review_passed = true` → выход из repeat-цикла на первой итерации. Шаг done на 1-й итерации.

**Открытый tech debt** (3 minor approved, не блокируют шаг):

- **F002** — Унифицировать политику обнуления nullable суб-сущности (`CommitTranslationEdit` ветка 1 vs `RemoveTranslation`).
- **F003** — Добавить guard `translation == null` / `definition == null` для `RemoveTranslation` / `RemoveDefinition`.
- **F004** — Решить судьбу payload `wordId` в `RemoveWord` (убрать или обосновать).

Адресовать в `contract_io` (там точно зафиксируются обнуления / эффекты) или в `implement` шаге.
