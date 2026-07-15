# Approved findings: scope_analysis (итерация 1)

Все approved findings обязаны быть закрыты в новой версии артефакта.

## РЕШЕНИЕ ПОЛЬЗОВАТЕЛЯ по F003+F004 (2026-07-09, зафиксировано — вшить в скоуп как принятое, НЕ как open question)

- **Онбординг** (`showAppBar=false`, AppBar нет): внутренний заголовок «Новый словарь» + подзаголовок — как в макете.
- **Create/Edit** (AppBar есть): значение заголовка уезжает в AppBar (`titleResId` в `DictionaryAppBar`, вместо текущего generic-тайтла; для edit — свой wording, определит UI sub-flow), внутренний дубль заголовка НЕ рисуется.
- **Подзаголовок** «Выберите язык и назовите словарь» — всегда в контенте, в обоих вариантах.
- **AppBar остаётся** в create/edit, навигация «назад» — как сейчас (в AppBar). Осознанная адаптация: в create/edit экран отличается от макета (заголовок в AppBar, не крупный в контенте) — макет рисован под онбординг-кейс.

## F003 [critical] — «заголовка нет» — ложь для create/edit; DictionaryAppBar пропущен в скоупе

Scope утверждает «у экрана появился собственный заголовок (в текущем UI его нет)», но в режимах create/edit это неверно: `DictionaryFormScreen.kt:54-59` рендерит `DictionaryAppBar` при `showAppBar` (default true), а `RootRouter.kt:81-85` (DICTIONARY_CREATE) вызывает экран БЕЗ `showAppBar=false` → AppBar с title включён. Новый in-content заголовок «Новый словарь» даст двойной заголовок. `DictionaryAppBar.kt`, `showAppBar` и `RootRouter.kt` полностью отсутствуют в скоупе.

**Verdict:** DICTIONARY_CREATE не передаёт showAppBar=false, DictionaryAppBar реально рендерится в create/edit — исправить утверждение и включить недостающие файлы/решение в скоуп.

## F004 [critical] — судьба AppBar и «назад» в create/edit не решена

Onboarding (`RootRouter.kt:65`) идёт с `showAppBar=false`, create/edit (`RootRouter.kt:81`) — с AppBar (кнопка назад + title). Макет 5027:1108 AppBar не содержит. Скоуп обязан решить (или явно вынести в Open questions с best-guess): остаётся ли AppBar, убирается ли, и как тогда работает навигация «назад» в create/edit.

**Verdict:** без этого решения имплементация упрётся; зона ответственности scope_analysis.

## F001 [minor] — атрибуция потребителей LexemeTextFieldWidget неточна

В риск-листе указан потребитель wordcard — прямых использований в wordcard НЕТ. Фактические внешние потребители: component_widgets (4 файла) + внутренняя обёртка PrimaryTextFieldWidget в core:ui. Поправить атрибуцию.

## F002 [minor] — атрибуция потребителей PrimaryFullButtonWidget неточна

Указан потребитель dictionarypicker — не подтверждён. Фактические: quiz/chat (ChatWidget), component_widgets (4 файла), dictionary/list, core:ui ErrorStateWidget. Поправить атрибуцию.
