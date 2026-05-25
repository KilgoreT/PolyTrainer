# Review шага scope_analysis

**Артефакт на ревью:** `02_scope.md`
**Ревьюверы:** `architect`, `qa_engineer` (заменил несуществующего `analyst` — см. backlog.md)
**Прогон ревью:** 1 (итерация 1 — будет 2-й после правок)
**Инквизиция:** проведена ретроспективно (proper order: review → inquisitor → accept → re-review)
**Дата:** 2026-05-18

## Резюме итерации 1

10 findings от ревьюверов. После инквизиции: 9 valid + 1 partial. Все правки применены, кроме одной — Q7 свёрнут до маркера-аспекта в `02_scope.md` (раздел «Аспекты», `lifecycle_after_modal_removal`). Детальные вопросы — работа `contract_state` / `contract_io`.

Ключевые правки в `02_scope.md`:
- Путь к strings.xml исправлен: `core/core-resources/src/main/res/values/strings.xml` (+ `values-ru-rRU/`).
- `infra_touched: false → true` — chip-стиль и новая кнопка касаются `core/ui` / `core/theme`.
- Список затронутых тестов расширен.
- Добавлен аспект `lifecycle_after_modal_removal` с явными вопросами.

## Findings architect

### [critical] Несуществующий путь к строковым ресурсам

**Резолюция:** accept.

**Что сделано:**
- В `02_scope.md` путь заменён на `core/core-resources/src/main/res/values/strings.xml` + `values-ru-rRU/strings.xml`.
- Сам architect указал НЕВЕРНЫЙ реальный путь (`core/ui/...`) — это [critical] проёб роли, зафиксирован в `backlog.md`.
- Методология scope_analysis sub-agent (угадал по конвенции, не грепнул) — зафиксирована в `backlog.md`.

### [critical] Инфра-слой ошибочно помечен «нет»

**Резолюция:** accept.

**Что сделано:**
- `infra_touched: false → true` в `context_output` (`02_scope.md` и `plan.yml`).
- В раздел «Затронутые слои → Infrastructure» добавлено обоснование: chip-стиль и новая кнопка (`9154-82532`) — потенциальный новый виджет в `core/ui` или ревизия токенов `core/theme`. Если Infra sub-flow увидит что Material chip справляется без обёртки — закроется быстро.
- В таблицу «Sub-flow для запуска» Infrastructure переведён в «да».
- Добавлен аспект `chip_component_in_core_ui`.

### [minor] Неполный список тестов, конструирующих `AddLexemeBottomState`

**Резолюция:** accept.

**Что сделано:** в `02_scope.md` поимённый список расширен: добавлены `WordLoadedTest`, `mate/ext/LoadingExtTest`, `mate/ext/LexemeExtTest`, `mate/ext/SpecializedLexemeExtTest`.

### [minor] Двусмысленность с переименованием `AddLexemeBottomState`

**Резолюция:** accept.

**Что сделано:** в `02_scope.md` две альтернативы заменены на нейтральную формулировку: «форма `AddLexemeBottomState` теряет смысл (флаг `show` несовместим с inline-механикой); конкретный шейп нового суб-стейта определяется на шаге `contract_state`».

## Findings qa_engineer

### [critical] Несуществующий путь к ресурсам

**Резолюция:** accept (дубликат architect F1, qa-агент указал реальный путь корректно — `core/core-resources/`).

### [critical] Не упомянут `WordLoadedTest`

**Резолюция:** accept (дубликат architect F3).

### [critical] Не описан граничный сценарий «частичный ввод + поворот / отмена / возврат»

**Резолюция:** accept.

**Что сделано:**
- В `02_scope.md` раздел «Аспекты» расширен аспектом `lifecycle_after_modal_removal` со списком открытых вопросов: (а) поведение при `NavigateBack` с открытым inline и заполненными чекбоксами; (б) сохранение/сброс на configuration change; (в) поведение при ошибке `CreateLexeme`; (г) что считать «открытием/закрытием» inline (когда `show`-флаг становится бессмысленным).
- Методологическая часть (правило для scope_analysis: «при переходе модальное → inline обязательно покрывать lifecycle») — в `backlog.md`.

### [minor] AddLexemeExtTest упомянут без указания конкретики

**Резолюция:** accept.

**Что сделано:** в `02_scope.md` уточнено: «тесты `showAddLexemeBottom` / `hideAddLexemeBottom` / `setTranslationCheck` / `setDefinitionCheck` — подлежат удалению или переписыванию под новые ext-имена (решение в `contract_state` / `design_tree`)».

### [minor] Возможный мёртвый код `PrimaryLongFabWidget` / `LexemeLongFab` в `core/ui`

**Резолюция:** accept.

**Что сделано:** в `02_scope.md` раздел «Затронутые файлы» дополнен пометкой: «`modules/core/ui/.../PrimaryLongFabWidget.kt` и `LexemeLongFab.kt` — после удаления единственного потребителя могут стать мёртвым кодом; решение по их удалению — в UI sub-flow».

### [minor] Расплывчатость в «возможны новые ключи под chip-метки» / cross-usage

**Резолюция:** accept.

**Что сделано:** в `02_scope.md` к абзацу про ресурсы добавлено: «перед удалением/переименованием ключей `word_card_bottom_*` — verify cross-usage в `core/ui` (`PrimaryEditableWidget` использует `word_card_bottom_translation`) и других виджетах».

## Принятые изменения context_output

| Переменная | Было | Стало | Причина |
|---|---|---|---|
| `infra_touched` | `false` | `true` | architect F2 — chip-компонент / токены в `core/ui` / `core/theme` |
| Остальные | — | без изменений | — |

## Что в backlog (методология flow)

Уже было / добавлено в `backlog.md` фичи:
- scope_analysis sub-agent не верифицирует пути grep'ом (F1, F5).
- architect-ревьювер не верифицирует пути grep'ом (F1 sam-finding).
- scope_analysis промпт не требует lifecycle-чеклиста при переходах модальное → inline (F7).
- Несуществующий ревьювер `analyst` в `adaptive.yml`.
- Conductor смешивал findings с backlog — конвенция артефакта review зафиксирована.

## Инквизиция (ретроспективная)

Проведена после применения правок — методологический проёб conductor'а (см. backlog). Каждый finding проверен grep'ом / read'ом кода.

| Finding | Вердикт | Откат |
|---|---|---|
| A1 | valid | — (правильный путь применён по Q5) |
| A2 | valid | — |
| A3 | valid | — |
| A4 | valid | — |
| Q5 | valid | — |
| Q6 | valid | — |
| Q7 | **partial** | **да** — список (а)–(г) свёрнут до маркера-аспекта в `02_scope.md`; детали — работа `contract_state` / `contract_io` |
| Q8 | valid | — |
| Q9 | valid | — |
| Q10 | valid | — |

**Проверки инквизитора (выборочно):**
- A1/Q5: `grep word_card_add_lexeme` → реальное место `core/core-resources/src/main/res/values/strings.xml:95` (+ ru). Architect (`core/ui/...`) — врал.
- A2: `grep AssistChip/FilterChip/SuggestionChip/InputChip` в `modules/screen/wordcard` и `core` → 0 совпадений (кроме `widget/chipPicker`). chip — новый. Плюс `PrimaryLongFabWidget` единственный потребитель — `AddLexemeWidget`. Касание `core/ui` подтверждено.
- A3/Q6: read `WordLoadedTest.kt`, `LoadingExtTest.kt`, `LexemeExtTest.kt`, `SpecializedLexemeExtTest.kt` — все 4 конструируют `AddLexemeBottomState`.
- Q9: `grep PrimaryLongFabWidget LexemeLongFab` → единственный production-потребитель `AddLexemeWidget.kt:20`; плюс preview-stub (не production).
- Q10: `grep word_card_bottom_translation` → 4 потребителя, включая `core/ui/.../text/PrimaryEditableWidget.kt:78` (cross-module).
- Q7 partial: контракт `scope_analysis.md` («Не делай design-решений по конкретным слоям — это работа sub-flow») — детальные вопросы lifecycle нарушают этот пункт. Аспект как маркер — оставить. Подробности — в задел.

## Возражение от инквизитора (учтено)

Применить 10 findings без верификации — вторая methodology-ошибка подряд (ревьюверы не грепали, conductor не грепал ревьюверов). Корреляция findings (A1≡Q5, A3≡Q6) должна была триггерить мини-аудит «оба ревьювера согласны — но кто видел код?». В этом случае QA видел, architect нет. Зафиксировано в `backlog.md` фичи как два отдельных проёба.

## Готов к итерации 2

После применения правок и отката Q7-deep — запускается второй прогон ревью на текущей версии `02_scope.md` (см. ниже).

---

# Review шага scope_analysis — итерация 2

**Дата:** 2026-05-18

## Findings итерации 2

| ID | От кого | Категория исходная | После инквизиции | Резолюция |
|---|---|---|---|---|
| F1 | architect/qa | critical / minor | **critical** (повышено — повтор класса проёба) | partial-accept: добавлены `SnackbarExtTest`, `TopBarExtTest`, `WordExtTest` (последний — как представитель неявной категории) |
| F2 | architect | minor | minor | accept: `DatasourceEffectHandler.CreateLexeme` → `DatasourceEffect.CreateLexeme` |
| F3 | qa | minor | minor | partial-accept: переформулировка про cross-usage — `core/ui/PrimaryEditableWidget` только в превью, production blast radius ограничен `screen/wordcard` |
| F4 | qa | minor | minor | accept: упоминание precedent `modules/widget/chipPicker` с Material3 chip |

## Инквизиция итерации 2

- Verify command F1: `grep -c "AddLexemeBottomState(" SnackbarExtTest.kt` = 7, `TopBarExtTest.kt` = 6, `WordExtTest.kt` = 0 (но 2 immutability-assertions).
- Verify F2: `DatasourceEffectHandler.kt:11` `sealed interface DatasourceEffect`, line 26 `data class CreateLexeme(...)`, line 75 `is DatasourceEffect.CreateLexeme ->`. Правильное имя — `DatasourceEffect.CreateLexeme`.
- Verify F3: `Read PrimaryEditableWidget.kt:66-87` — литерал `word_card_bottom_translation` только внутри `@PreviewWidget`-функции. Production cross-module = 0.
- Verify F4: `Read ChipPickerWidget.kt:23-26` — production-использование Material3 `SuggestionChip` + `InputChip`.

## Возражение инквизитора (учтено)

Architect F1 суммировал «конструкции + immutability-assertions» в одну метрику — некорректно. Это разные категории риска (конструкции ломаются при изменении конструктора; assertions ломаются при изменении `equals`/`hashCode` через поля). В правке F1 категории разделены явно. `WordExtTest` отнесён к категории «неявная зависимость через дефолт» с указанием на escape-hatch `grep WordCardState(`.

## Методология flow

Повтор класса проёба «неполный список тестов» две итерации подряд → системная дыра в промпте `scope_analysis`. Зафиксировано в `backlog.md` («scope_analysis промпт не требует grep-доказательства списков»).

---

# Review шага scope_analysis — итерация 3

**Дата:** 2026-05-18

## Findings итерации 3

**Architect:** «Проёбов не обнаружено». Все правки итерации 2 верифицированы grep'ом / read'ом — корректны.

**QA Engineer:** «Проёбов не обнаружено». Verified F1–F4. Counts тестов сходятся (16 файлов конструируют `AddLexemeBottomState(`; 14 поимённо в списке «явные», `WordExtTest` представитель «неявных», escape-hatch grep покрывает остальных — `TranslationManagementTest`, `DefinitionManagementTest`). Возражение про неполное поимённое перечисление — **не finding**, escape-hatch уже покрывает.

## Critical-инвариант

**`count == 0` у обоих ревьюверов.** Выход из review-петли разрешён. Шаг `scope_analysis` готов.

## Метрика review-петли

- Итераций ревью: 3
- Прогонов инквизиции: 2
- Total findings: 10 (ит.1) + 4 (ит.2) = 14
- Accepted: 9 (ит.1) + 2 valid + 2 partial (ит.2) = 13
- Rejected полностью: 0
- Partial accepted: 1 (Q7 ит.1) + 2 (F1, F3 ит.2) = 3
- Полный consensus отсутствия critical достигнут на 3-й итерации
