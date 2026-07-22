# Component Constructor — конструктор компонентов словаря

> **SUPERSEDED (IS486, 2026-07-20).** Эта спека заменена единой спекой
> компонентов и иерархии: **[docs/features/IS486_choice_component/spec.md](../../../features/IS486_choice_component/spec.md)**.
> Ниже — только сводка отличий от прежней (IS481 phase 1/2) картины; за любым
> контрактом идти в IS486-спеку.

## Что изменилось относительно IS481-картины

- **Глобальных компонентов в продукте нет** (решение 2026-07-17): builtin —
  пословарные, seed при создании словаря; `Scope.Global` и multi-dict путь
  живут в коде как консервация (IS486 spec §20).
- **Manager-экран (`ComponentsManagerScreen`) недостижим из навигации** —
  вход из Settings выпилен (IS486 фаза 4); модуль остаётся в коде как
  консервированный контракт (IS486 spec §20).
- **Единственная точка входа** — per-dictionary конструктор:
  `DictionaryAppBar → «молоток» → PerDictionaryComponentsScreen`. Builtin-строки
  **показываются** (одна лента с кастомными): у builtin только рубильник
  `enabled`; user-defined — полный CRUD.
- **Иерархия зависимостей**: каждый компонент зависит от узла
  (лексема | компонент | опция CHOICE); ядро — флаг у зависимых от лексемы;
  degraded — вычисляемое состояние; закон доступности, каскады, ацикличность —
  IS486 spec §6–§9.
- **Шаблон CHOICE** с опциями (`component_options`), CRUD опций в Edit-диалоге,
  запрет мульти для CHOICE.
- **Рубильник `enabled`** (мягкий): выключенный компонент не предлагается для
  новых значений; запрет терять последнее включённое ядро словаря (§7.8) на
  всех трёх путях: disable / soft-delete / перепривязка.

## Что осталось валидным из IS481

Soft-delete семантика (`removed_at`, preview impact, каскад quiz-конфигов и
prefs), уникальность имени (`userdefined_identity_invariant`, two-prong SELECT),
template immutability, cardinality-downgrade guard, Removed-ветки outcome,
epoch-корреляция диалогов — всё перенесено и уточнено в IS486 spec §5, §7, §14.
