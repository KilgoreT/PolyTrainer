# Approved findings — infra_implement.md, iteration 1

2 minor (F076, F077). Оба про placeholder-debt чистку.

## F076 [minor] — unused project-deps в новых модулях

В `modules/screen/components_manager/build.gradle.kts` и `modules/screen/per_dictionary_components/build.gradle.kts` объявлены deps:

- `:modules:domain:lexeme`
- `:modules:widget:component_widgets`
- `:modules:core:tools`
- `:modules:core:logger`
- `:core:core-resources`

Все 5 — без единого import в `src/main/`. Используются реально: `core:di`, `core:mate`, dagger, compose, плюс косвенно `core:theme` / `core:ui` (стандартно pre-declared, не F076 target).

**Что исправить:** **удалить** 5 unused deps из обоих build.gradle.kts. Если какой-то из них реально нужен (например `:modules:domain:lexeme` для будущих lexeme-related операций) — pre-declare в `infra_implement.md` § Известные TODO с явным обоснованием pre-declare для business_implement.

Минимальный вариант: удалить **все 5** — добавятся в business_implement когда реально понадобятся.

## F077 [minor] — `@Suppress` костыль в placeholder Screen/ViewModel

В:

- `modules/screen/components_manager/src/main/.../ComponentsManagerScreen.kt`
- `modules/screen/per_dictionary_components/src/main/.../PerDictionaryComponentsScreen.kt`

Костыль `@Suppress("UnusedPrivateMember") val vm = viewModel` — присваивание без использования.

В:

- `ComponentsManagerViewModel.kt`
- `PerDictionaryComponentsViewModel.kt`

`private val navigator` / `private val dictionaryId` — захвачены, не используются.

**Что исправить:** убрать `val vm = viewModel` в обоих Screen — composable не использует ViewModel, ViewModel инстанцируется в DI и attached к graph через factory, в placeholder Screen его не надо присваивать. Если требуется чтобы factory wiring "ожил" в DI — fix infrastructure-level в DI module/factory, не костыль в Screen.

Для placeholder ViewModel'ей: пометить `navigator` / `dictionaryId` через `@Suppress("unused")` с явным TODO-комментарием «used in business_implement when real logic added» либо вообще не передавать пока в business_implement.
