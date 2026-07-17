# Code review: redesign_create_dictionary (IS485)

Commit: uncommitted (ветка IS485_redesign) | Date: 2026-07-10

Обзор выполнен conductor'ом в трёх перспективах (Architecture / Bugs / YAGNI) без суб-агентов — по указанию пользователя. Скоуп диффа: 6 изменённых + 5 новых Kotlin/res-файлов (см. `git diff --stat`), Mate-слой не тронут.

## Architecture

### [minor] FQN Arrangement вместо импорта

**Где:** `modules/screen/dictionary/.../form/widget/FlagGridWidget.kt:73-74`
**Что не так:** `androidx.compose.foundation.layout.Arrangement.spacedBy(...)` полным именем в параметрах LazyVerticalGrid — в остальном коде проекта Arrangement импортируется.
**Почему важно:** стилевая непоследовательность, шум при чтении.
**Предложение:** `import androidx.compose.foundation.layout.Arrangement` + короткие вызовы.
**Verify:** Grep "Arrangement" FlagGridWidget.kt → `:73 androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)`
**Triage:** → закрыть в фиче — исправлено: импорт Arrangement добавлен

Слойность/паттерны: чисто — все правки в UI-слое, Msg-набор прежний, derived «ничего не найдено» вычисляется в composable по зафиксированному решению брифа, стейт не трогался. Границы sub-flow артефактов соблюдены (business/data не запускались обоснованно).

## Bugs

### [minor] IconButton 48dp в пилюле высотой 45dp

**Где:** `modules/screen/dictionary/.../form/widget/SearchPillWidget.kt:81-90` (Surface height 45, `:45`)
**Что не так:** дефолтный minimum touch target `IconButton` — 48dp, что выше контейнера 45dp: клип иконки/ripple краями Surface (Surface клипует по shape).
**Почему важно:** возможен подрезанный ripple/иконка clear-кнопки; touch-зона урезается.
**Предложение:** задать `Modifier.size(32.dp)` IconButton'у (или увеличить высоту пилюли до 48).
**Verify:** Read SearchPillWidget.kt:45,81 → `height(45.dp)` + `IconButton(` без size-модификатора
**Triage:** → закрыть в фиче — исправлено: `Modifier.size(32.dp)` у IconButton

### [minor] Позиция бейджа-галочки — внутри bounding box, а не на краю круга

**Где:** `modules/screen/dictionary/.../form/widget/FlagGridWidget.kt:126-141` (badge в Box с padding(4) item'а)
**Что не так:** бейдж выравнивается по BottomEnd Box'а с внешним padding 4dp — визуально сидит на 4dp внутрь от угла флага; в Figma бейдж лежит на краю круга с выносом −2px (node 5027:1153).
**Почему важно:** мелкое визуальное расхождение с макетом.
**Предложение:** сместить бейдж `Modifier.offset(x = 2.dp, y = 2.dp)` либо убрать внешний padding из расчёта — подобрать на девайсе вместе с тенью кнопки.
**Verify:** Read FlagGridWidget.kt:99-141 → `Box(modifier = Modifier.padding(4.dp))` + `align(Alignment.BottomEnd)` без offset
**Triage:** → закрыть в фиче — исправлено: `offset(x=2.dp, y=2.dp)` у бейджа

### [minor] Двойной верхний отступ в create/edit — существовал до фичи

**Где:** `modules/screen/dictionary/.../form/DictionaryFormScreen.kt:61-69`
**Что не так:** контент получает и `padding(paddings)` от Scaffold (высота topBar включая статусбар), и `statusBarsPadding()` — в режимах с AppBar это двойной отступ сверху.
**Почему важно:** лишние ~24-40dp пустоты под AppBar; НО поведение идентично до-фичевому (цепочка модификаторов не менялась) — это не регресс редизайна.
**Предложение:** вне скоупа фичи; если чинить — убирать `statusBarsPadding()` при showAppBar=true отдельным изменением с визуальной проверкой обоих режимов.
**Verify:** Read DictionaryFormScreen.kt:61-69 → `.padding(paddings)...statusBarsPadding()` — та же цепочка, что в git-версии до фичи
**Triage:** → rejected — не регресс фичи (цепочка существовала до неё), экран визуально принят пользователем на девайсе как есть; при желании чинится отдельной правкой

Race/leak/cancellation: не найдено — корутин/Flow в диффе нет, все колбэки — синхронные sendMsg с существующими Msg.

## YAGNI

Проёбов не обнаружено: новых абстракций/интерфейсов нет (плоские private composables), dead-параметров нет (все параметры новых виджетов используются), defensive-кода на невозможные кейсы нет; константы виджетов локальные private. Проверено чтением всех 5 новых файлов + диффа изменённых.

## Triage Summary

- Закрыть в фиче: 3 findings (все исправлены: импорт Arrangement, size(32) у clear-кнопки, offset бейджа; компиляция модуля SUCCESS)
- В backlog: 0
- Rejected: 1 (двойной statusBar-отступ — существовал до фичи, экран принят пользователем на девайсе)

_model: claude-fable-5_
