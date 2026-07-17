# Review: scope_analysis

## Итерация 1 (2026-07-09T01:58:00)

### F001 [architect] minor

**Description:** в риск-листе потребителей `LexemeTextFieldWidget` указан wordcard — прямых использований в wordcard нет (фактические: component_widgets ×4 + PrimaryTextFieldWidget в core:ui); вывод аспекта не меняется, но атрибуция неточна

**Status:** approved

**Verdict:** Проверено поиском по всему репо — прямых вызовов LexemeTextFieldWidget в wordcard нет (фактические потребители: component_widgets ×4 + PrimaryTextFieldWidget в core:ui), риск-лист в аспекте shared_widget_consumers_risk атрибутирует ложно.

### F002 [architect] minor

**Description:** в риск-листе потребителей `PrimaryFullButtonWidget` указан dictionarypicker — использование там не подтверждено (фактические: quiz/chat ChatWidget, component_widgets ×4, dictionary/list); вывод аспекта не меняется

**Status:** approved

**Verdict:** PrimaryFullButtonWidget в dictionarypicker не используется ни разу (фактические: quiz/chat, component_widgets, dictionary/list, ErrorStateWidget) — атрибуция в скоупе не соответствует коду.

### F003 [qa_engineer] critical

**Description:** Scope утверждает «у экрана появился собственный заголовок (в текущем UI его нет)», но это ложь для режимов create/edit: DictionaryFormScreen.kt:54-59 рендерит DictionaryAppBar при showAppBar (default true), RootRouter.kt:81-85 (DICTIONARY_CREATE) вызывает экран без showAppBar=false → AppBar с title включён; новый in-content заголовок «Новый словарь» даст двойной заголовок; DictionaryAppBar.kt и обработка showAppBar полностью отсутствуют в скоупе

**Status:** approved

**Verdict:** DICTIONARY_CREATE в RootRouter.kt:81-85 не передаёт showAppBar=false (default true в DictionaryFormScreen.kt:30), DictionaryAppBar с title реально рендерится в create/edit — скоуп повторяет ложное «заголовка нет» и полностью пропускает DictionaryAppBar/showAppBar, что даст двойной заголовок.

### F004 [qa_engineer] critical

**Description:** Не разрешена судьба AppBar при редизайне: onboarding (RootRouter.kt:65) идёт с showAppBar=false, create/edit (RootRouter.kt:81) — с AppBar (кнопка назад + title); макет 5027:1108 AppBar не содержит; скоуп не решает — остаётся ли AppBar, убирается ли, и как тогда навигация «назад» в create/edit

**Status:** approved

**Verdict:** Судьба AppBar (оставить/убрать) и навигация «назад» в create/edit — решение, без которого имплементация упрётся, а RootRouter.kt/showAppBar/DictionaryAppBar отсутствуют в скоупе целиком — прямая зона ответственности scope_analysis.

### F005 [qa_engineer] minor

**Description:** Тестовые последствия неполны: упомянуты только unit-тесты reducer, но не превью-функции композаблов — @PreviewWidget есть в DictionaryFormWidget.kt:131-140, FlagGridWidget.kt:76-86, DictionaryFormScreen.kt:78-86, все придётся переписывать под новые сигнатуры/состояния

**Status:** rejected

**Verdict:** Превью-функции живут внутри файлов, уже перечисленных в скоупе как правимые (DictionaryFormWidget, FlagGridWidget, DictionaryFormScreen) — ни пропущенного файла, ни блокера нет, переписывание превью — деталь имплементации UI sub-flow.

### F006 [qa_engineer] minor

**Description:** Пропущен edge-case «пустое имя + placeholder-буква»: name.firstOrNull() берёт первый символ как есть (строчный/emoji/пробел), превью-буква на новом двойном кольце — QA-риск, не помечен в скоупе

**Status:** rejected

**Verdict:** Закрытое решение брифа №3 явно фиксирует «сохранить текущее поведение» плейсхолдера (первый символ как есть) — поведение не меняется редизайном, требование пометить его как риск противоречит step.input.

### F007 [qa_engineer] minor

**Description:** Empty-state «ничего не найдено» — derived-условие живёт только в UI без флага в state, недетерминируемо через reducer-тесты; needs_tests=false оставляет бранч без автоматической проверки — в скоупе не оговорено как сознательный пробел покрытия

**Status:** rejected

**Verdict:** Derived-условие без флага в стейте — закрытое решение брифа (2026-07-08) с явным обоснованием, воспроизведённое в скоупе; требование дополнительно аннотировать «сознательный пробел покрытия» — вкусовщина, оспаривающая уже закрытое на входе шага решение.
