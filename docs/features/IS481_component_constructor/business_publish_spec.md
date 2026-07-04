# publish_spec

## Опубликовано
- `docs/features-spec/component-constructor.md` — создан
- Имя файла: `component-constructor.md` (взято из META: spec_filename в `business_contract_spec.md`)
- Размер: `~864` строк

## Корректировки от implement

Контракт обогащён по результатам review iter 2-6 (`business_implement_review.md`). Раздел «История ревью» из черновика спеки удалён — это рабочий артефакт контракт-блока, в проектной спеке не нужен. Значимые контрактные правки:

- **F123 — `snackbar` field в State.** В `ComponentsManagerScreenState` / `PerDictionaryComponentsScreenState` добавлено поле `val snackbar: SnackbarState? = null`. Snackbar теперь — explicit state, не теряемый через `UiMsg.Snackbar`-only роутинг. Добавлен `Msg.DismissSnackbar` и `data class SnackbarState(val text: String)`. (См. `business_implement.md` Pass 3 State.kt / Msg.kt.)
- **F124 — `ImpactPreviewLoaded/Failed` с `typeId` correlation.** `Msg.ImpactPreviewLoaded(typeId, impact)` и `Msg.ImpactPreviewFailed(typeId, cause: Throwable?)` — reducer ignore'ит late preview к закрытому/перенесённому dialog'у. Test scenario «Delete — preview failure on closed dialog» добавлен.
- **F125 — `CancellationException` re-throw invariant.** В § IO добавлен handler-level invariant: catch (Throwable) с re-throw для `CancellationException`. Test scenario «CancellationException propagation» добавлен.
- **F128 — `UiMsg.Snackbar.show: Boolean` dropped.** Поле было мёртвым — `UiMsg.Snackbar(val text: String)` без флага видимости.
- **F136 — `epochId` correlation на Result Msg.** `CreateResult`/`RenameResult`/`DeleteResult` получили `val epochId: Long`; соответствующие `DatasourceEffect` (`CreateComponent`/`RenameComponent`/`SoftDeleteComponent`) тоже несут `epochId`. Guard «stale epoch dropped» в reducer. Test scenario «Create — stale result (epoch mismatch)» добавлен.
- **F138 — `Open*Dialog` закрывает другие dialog'и.** Reducer reaction column обновлён: `OpenCreateDialog` → `renameDialog=null, deleteConfirm=null`. Test scenario «Open dialog closes other dialogs (invariant enforced)» добавлен; invariant explicit в § State.
- **F140 — `OpenCreateDialog` сбрасывает `isCreating=false`.** Invariant добавлен (`[transition]`).
- **F142 — `failureLabel()` в shared util.** § IO дополнен «Shared утилиты» с указанием на `:modules:core:tools/ThrowableExt.kt`.
- **F145 — `previewDeletionImpact` null distinct semantics.** UseCase doc-comment: `null` (не найден) → handler emit'ит `Msg.ImpactPreviewFailed(typeId, cause=null)` вместо синтетического exception. `ImpactPreviewFailed.cause: Throwable?` (nullable). Test scenario «Delete — preview not-found» добавлен.
- **F116 — `NameTooLong` validation.** `CreateOutcome.NameTooLong` добавлен в spec sealed (был в design, но не в спеке). `ComponentType.NAME_MAX_LEN = 64` companion const упомянут в § Бизнес-описание и в § UseCase doc-comment. Test scenario «Create — name too long» добавлен. `RenameOutcome.NameTooLong` — out-of-scope, не добавляется (см. TODO в impl).
- **Lifecycle-Msg для `PerDictionaryComponentsScreen`.** Уточнено что есть и `ItemsLoaded(snapshot)`, и `ItemsLoadFailed(cause)` (parity с CM, в черновике было только `ItemsLoaded`).

Игнорировано (имплементационные детали без эффекта на контракт):
- F126 (PerDictionary depends on interface — DIP, не наблюдаемо в контракте)
- F127 (best-effort prefs try/catch shape — внутренность UseCase)
- F129 (snackbar text format "Failed: null")
- F130-F134 (build.gradle / package shapes / dormant placeholder)
- F119 (unused ctor param)
- F120, F147 (test counts / doc hygiene)

## PUML

PUML-схемы в feature dir `docs/features/IS481_component_constructor/` отсутствуют (только `.md` файлы). В проекте PUML-схемы существуют только в feature dir'ах старых фич (`IS445_stale_dictionary_icon`, `IS443_flag_placeholder_widget`), нет shared директории для project-level схем. Шаг копирования пропущен — нечего публиковать.
