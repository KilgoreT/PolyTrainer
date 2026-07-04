# publish_spec

## Опубликовано
- `docs/features-spec/wordcard.md` — обновлён (overwrite)
- Имя файла: `wordcard.md` (META `spec_filename: wordcard.md` из `business_contract_spec.md`)
- Размер изменений: с 491 до 569 строк (+~78 строк); основные дельты — переход от translation/definition-specific API к generic component API, добавление `hasDefinitionComponent` поля State, новое поле `componentTypes` в `Msg.WordLoaded`, новая секция § Transitional API (mate refactor shim), новая секция § Domain types (dependency)

## Корректировки от implement

В `business_implement.md` зафиксированы шаги iter 2 (F1–F6 critical-fixes). Проверка спеки на необходимость sync:

- **F1 (`updateComponentValue` dead null/null branch)** — внутренняя impl-деталь `WordCardUseCaseImpl`; контракт интерфейса `WordCardUseCase.updateComponentValue` в спеке не меняется. Спека описывает контракт, не impl; **спека not affected**.
- **F2 (`LexemeMapper.toDomain` debug-check tautology)** — удалён debug-check блок. В спеке секция `### Mapper (shim consistency)` упоминает «debug-only assertion» как тестовый сценарий мапппера. Решение оставить упоминание as-is: спека описывает желаемый параметризованный тест-сценарий (комбинации translation-only / translation+definition / user-defined-only / empty); фактическая impl-реализация этого инварианта может быть либо через assertion в маппере, либо через тест без runtime check — оба варианта удовлетворяют контракту «invariant выполняется». Закрытие invariant в mate refactor (B4/C2 backlog) явно отмечено.
- **F3 (`addLexemeWithBuiltInComponent` missing UPDATE branch для existing lexemeId)** — impl-деталь; контракт «atomic INSERT lexeme + write_quiz + первый component_value» сохраняется для null-lexemeId path. Для non-null lexemeId секция Atomicity contracts не специфицирует ветвление insert vs update — это допустимое impl-улучшение. **Спека not affected**.
- **F4 (`deleteComponentValue` null on non-last)** — impl-деталь с явным TODO о необходимости DAO-level `getLexemeIdByComponentValueId`. Контракт sealed `RemoveComponentResult` (`ComponentRemoved(lexeme)` / `LexemeCascadeRemoved`) сохранён. Текущий semi-stub возвращает `null` для non-last вместо `ComponentRemoved` — это известное ограничение data sub-flow (см. секцию «Известные ограничения» в `business_implement.md`). Generic `deleteComponentValue` не вызывается callers в IS481 (все идут через `deleteDefinitionComponent`/`deleteLexemeTranslation` shim'ы). **Спека not affected** — контракт правильный, impl временно stub до data sub-flow.
- **F5 (`resolveDictionaryIdForLexeme` KDoc mismatch)** — impl-деталь, KDoc внутреннего private метода. **Спека not affected**.
- **F6 (copy-paste refactor delete translation/definition)** — internal helper extraction в `WordCardUseCaseImpl`. Контракт публичных методов сохранён byte-for-byte. **Спека not affected**.

**Итог корректировок:** без изменений в опубликованной спеке относительно `business_contract_spec.md`. Все iter 2 fixes — impl-details / internal refactor; публичные контракты `WordCardUseCase` interface, `Msg`, `State`, `DatasourceEffect`, `UiEffect`, mappers — стабильны.

Дополнительно: одно известное ограничение data-слоя (LexemeApiImpl semi-stub через synthetic component ids) находится out-of-scope business sub-flow и будет закрыто отдельным data sub-flow согласно METAS `00_task.md`.

## PUML

В feature dir `docs/features/IS481_lexeme_component_constructor/` PUML-схем не создавалось. В проекте присутствуют PUML-файлы в других местах (`modules/screen/wordcard/word_card.puml`, `modules/widget/dictionaryappbar/dictionary_appbar.puml`, `modules/screen/dictionary/dictionary_form.puml`, `modules/screen/dictionaryTab/dictionary_tab.puml`, `docs/features/IS445_stale_dictionary_icon/architecture.puml`, `docs/features/IS443_flag_placeholder_widget/architecture.puml`), но для IS481 architecture-диаграмма не была создана на этапах business design. Копировать в `docs/features-spec/` нечего.

_model: claude-opus-4-7[1m]_
