# Review: infra_design_tree.md

## Итерация 1 (2026-06-15T17:00:00-06:00)

### F051 [architect] minor
**Description:** `app/.../route/MainRouter.kt` отсутствует в основном DAG, только в Tier 9 Note как «trivial auto-followup». Создаёт `CompositionRootImpl(...)` с factory'ями — добавление 2 ViewModel factories сделает MainRouter compile-broken.
**Status:** approved
**Verdict:** Должен быть отдельным узлом DAG с depends на AppComponent + CompositionRootImpl.

### F052 [architect] minor
**Description:** Узел id 36 (app/build.gradle.kts) добавлен «постфактум» в § Аудит, а не в основной DAG Tier 0. Header говорит «36 узлов», основной граф — 35.
**Status:** approved
**Verdict:** Должен быть в Tier 0 рядом с id 1-4.

### F053 [architect] minor
**Description:** Tier 9 описание self-contradictory: «сигнатуры host-методов НЕ меняются» + затем «придётся расширить». Запутывает.
**Status:** approved
**Verdict:** Удалить ошибочный первый абзац.

### F054 [architect] minor
**Description:** `internal` модификатор для goToPerDictionaryComponents — отклонение от pattern (все аналоги private).
**Status:** rejected
**Verdict:** Implementation deталь visibility, не DAG-структура.

### F055 [architect] minor
**Description:** Snake_case в gradle module path — отклонение от existing convention.
**Status:** rejected
**Verdict:** Naming стилистика, не корректность DAG; existing convention сама непоследовательна.

### F056 [architect] minor
**Description:** Statistic.kt не содержит backPress extension по parity с Vocabulary/Quiz/Settings.
**Status:** rejected
**Verdict:** Содержимое файла — уровень кода, не DAG.

### F057 [architect] minor
**Description:** RoomModule docstring `user_version < 11` — оставить или обновить?
**Status:** rejected
**Verdict:** Вне scope infra_design_tree — это data_design_tree concern.

## Итоги итерации 1

- **Approved:** 3 minor (F051, F052, F053). 0 critical.
- **Rejected:** 4.
- **Решение:** minor-only iteration. streak=1. По review module — repeat (streak<2). Прогресс: audit checklist сработал, ни одного critical.

---

## Итерация 2 (2026-06-15T17:30:00-06:00)

### F058 [architect] critical
**Description:** id 28 (CompositionRoot.kt interface) не упоминает добавления параметра `onComponentsManagerClick` к `SettingsTabScreenDep`, но id 29 (Impl) override добавляет, id 30 (Settings.kt) передаёт → override не может расширять сигнатуру относительно interface, compile fail.
**Status:** approved
**Verdict:** Interface должен расширять сигнатуру SettingsTabScreenDep параметром onComponentsManagerClick.

### F059 [architect] minor
**Description:** id 36 (app/build.gradle.kts) depends: [3, 4] не включает id 2 (component_widgets/build.gradle.kts).
**Status:** rejected
**Verdict:** Gradle Sync делает include из settings.gradle.kts (id 1), порядок depends в design tree не определяет sync order — overkill.

### F060 [architect] minor
**Description:** app/-side узлы не имеют depends на id 36 → compile может упасть с unresolved reference.
**Status:** rejected
**Verdict:** id 36 — Gradle build file, Gradle Sync происходит до compile в одном билде; depends overkill.

## Итоги итерации 2

- **Approved:** 1 critical (F058). streak counter сбрасывается из-за critical.
- **Rejected:** 2.
- **Решение:** repeat (есть approved critical).

---

## Итерация 3 (2026-06-15T18:00:00-06:00)

### F061 [architect] critical → minor (normalized)
**Description:** id 36 (app/build.gradle.kts) `depends: [3, 4]` пропускает id 2 (component_widgets/build.gradle.kts). app/build.gradle.kts добавляет implementation на 3 модуля включая widget.
**Status:** approved
**Verdict:** depends [3,4] уже коммитит к симметрии build.gradle.kts deps, исключение id 2 — asymmetric. Iter 2 rejection (F059) был ошибкой ревьювера — повторное поднятие архитектором валидно. Severity нормализую до minor (DAG-housekeeping).

## Итоги итерации 3

- **Approved:** 1 minor (F061).
- **Решение:** minor-only, streak=1. Repeat iter 4.

---

## Итерация 4 (2026-06-16T03:29:00-06:00)

### F062 [architect] critical
**Description:** id 29 (CompositionRootImpl) `depends: [21, 22, 27, 28]` не включает id 19/20. id 29 инстанцирует `SettingsNavigatorImpl(...)` с 4 lambda'ми (новый параметр `onOpenComponentsManager` появляется в id 19) и `DictionaryAppBarNavigatorImpl(...)` с 2 lambda'ми (новый параметр `onOpenPerDictionaryComponents` появляется в id 20). Без id 19/20 ctor-аргументы id 29 ссылаются на несуществующие параметры → compile fail.
**Status:** approved
**Verdict:** `depends: [21, 22, 27, 28]` → `depends: [19, 20, 21, 22, 27, 28]`.

### F063 [architect] critical
**Description:** id 32 (Quiz.kt) и id 33 (Statistic.kt) вызывают `navController.goToPerDictionaryComponents(dictId)`. Эта `internal` extension-функция объявляется в id 31 (Vocabulary.kt). Без id 31 — id 32/33 не компилируются. Сейчас оба `depends: [28]`.
**Status:** approved
**Verdict:** id 32 и id 33: `depends: [28]` → `depends: [28, 31]`.

### F064 [architect] critical
**Description:** Узлы `app/`, импортирующие типы из новых модулей (id 21, 22, 23, 25, 27, 29), требуют id 36 (`app/build.gradle.kts` добавляет `implementation(project(...))` на 3 модуля) на compile-classpath. Сейчас никто не зависит от id 36 — он изолированный. Семантика DAG = порядок изменений: id 36 должен идти ДО id 21/22/23/25/27/29, иначе intermediate compile broken.
**Status:** approved
**Verdict:** Добавить id 36 в `depends` узлов 21, 22, 23, 25, 27, 29. (id 24, 26 — DI Components зависят от id 23/25, getting id 36 транзитивно через них — но для явности можно добавить и им; минимально обязательно — 21, 22, 23, 25, 27, 29.)

## Итоги итерации 4

- **Approved:** 3 critical (F062, F063, F064). streak counter сбрасывается из-за critical.
- **Rejected:** 0.
- **Решение:** repeat (есть approved critical).

---

## Итерация 5 (2026-06-16T03:35:00-06:00)

architect: **PASS**. 0 findings.

Проверены: transitive depends (id 21/22/23/25/27 → 36, id 36 → [2,3,4]), отсутствие циклов, inverted direction, missing nodes (37 узлов все на месте), внутренние противоречия (id 28 ↔ id 29 сигнатуры совпадают), orphan nodes, duplicates. Все закрытые findings F051-F064 остаются закрытыми.

## Итоги итерации 5

- **Approved:** 0. **Rejected:** 0. **PASS.**
- require_clean_iteration=true: iter 4 had changes_made=true (F062/F063/F064 fix), iter 5 = clean PASS → **exit step**.
- **Финальное решение:** infra_design_tree → done.
