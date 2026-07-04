## Задача

Нужен пользовательский конструктор компонентов словаря.

Пользователь должен иметь возможность:
- создавать новые компоненты в словаре (имя, шаблон, scope, cardinality);
- редактировать (rename);
- удалять (soft-delete, с warning сколько values станет скрыто).

## Контекст

Сейчас generic component API уже есть (миграция M11→M12), но пользователь не может **сам** создавать новые компоненты для словаря. Built-in `translation` есть везде, definition появляется только если миграция нашла существующие данные. Для новых словарей user-defined компонент = недостижим.

## Концепция фичи

Детали проработаны заранее в директории [`concept/`](concept/). Эти документы — обязательный input для всех последующих шагов flow:

- [`concept/ui_placement.md`](concept/ui_placement.md) — где экраны (Settings tab для глобального view + icon-button «молоток» в `DictionaryAppBar` для per-dictionary), что показывают, операции (create / rename / soft-delete).
- [`concept/template_model.md`](concept/template_model.md) — domain (`Primitive`, `Field`, `ComponentTemplate`, `TemplateValues`), БД (`is_multi`, timestamps, soft-delete), миграция M12→M13 (composite JSON rewrite + drop UNIQUE + `removed_at`), принципы type safety, 11 закрытых open questions.
- [`concept/typed_views.md`](concept/typed_views.md) — финальный дизайн typed views per template (sealed `TemplateValues` + конкретные `*Values` data classes; парсер JSON в `core-db-impl` сразу выдаёт typed, без Map в domain).
- [`concept/deletion_concept.md`](concept/deletion_concept.md) — soft-delete концепция (helmark `removed_at`, скрыты из активных queries, без recovery в этой фиче — корзина + background TTL отложены в `Backlog.md`).

Все шаги flow (scope_analysis, business_walkthrough, business_contract, business_design_tree, ui_layout, ui_design_tree, ui_implement и др.) обязаны читать concept-документы как часть input.

_model: claude-opus-4-7[1m]_
