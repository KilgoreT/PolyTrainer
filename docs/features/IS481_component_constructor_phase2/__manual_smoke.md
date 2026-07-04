# Manual smoke run-book — IS481 component_constructor (phase 1 + phase 2)

Документ для прогона ручных smoke-тестов **обеих фаз** фичи user-defined компонентов: phase 1 (базовый CRUD + два экрана + UI entry-points + миграция M12→M13) и phase 2 (Edit + cardinality downgrade + multi-dict scope picker + shared widget module + Removed-semantics + feature-tag logs). Каждый сценарий — самодостаточен, рассчитан на человека который **не знает код**: «куда тапнуть → что увидеть → какие логи появятся → какой ожидаемый результат». Известные блокеры и UX-долги отмечены явно (`⚠ Известные проблемы`, `BLOCKER`).

Покрытие:
- **Phase 1** — Группы 1 (Manager-экран CRUD), 2 (Per-Dictionary экран).
- **Phase 2** — Группы 3 (Edit + downgrade), 4 (Multi-dict scope picker — **BLOCKER**), 5 (Shared widget regression), 6 (Removed semantics), 7 (Feature-tag + migration logs).

Канонический spec: `docs/features-spec/component-constructor.md § Тестовые сценарии` (12 phase 1 + 16 phase 2 acceptance contracts).

> **Локаль:** документ написан под прогон на устройстве с **русской локалью** (`values-ru-rRU`). UI-строки приведены в формате **«русский текст»** (`R.string.key`) — то что юзер видит + ключ для поиска в коде. Snackbar-тексты Reducer'а (`"Updated"`, `"Created N"`, `"N values hidden"` и т.д.) на момент написания **hardcoded на английском** в `Reducer.kt`, не локализованы — это явно помечено `(локализация не завершена)`.

---

## Setup

### 1. Собрать debug APK

Из корня репозитория:

```bash
./scripts/cc-build.sh :app:assembleDebug
```

Артефакт: `app/build/outputs/apk/debug/app-debug.apk`.

`applicationId` debug-сборки: `co.lexeme.app.dev` (production — `co.lexeme.app`). Это важно для фильтра logcat по пакету.

### 2. Поставить на устройство / эмулятор

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Если на устройстве стоит релизный билд с другой подписью — сначала `adb uninstall co.lexeme.app.dev`.

> ⚠ **Важно для Группы 7.1 (migration logs):** если вы хотите проверить migration M12→M13 — поставьте СНАЧАЛА старый билд (с schema v12 или ранее), запустите его, дайте создать БД, затем поставьте свежий phase 2 билд `-r` (без uninstall, иначе данные удалятся). Migration логи появляются при первом запуске нового билда поверх старой БД.

### 3. Запустить logcat с фильтром по фича-тегу

В отдельном терминале:

```bash
adb logcat -c                              # сброс буфера
adb logcat | grep '###ComponentConstructor###'
```

Альтернатива (с pid фильтрацией):

```bash
adb logcat --pid=$(adb shell pidof co.lexeme.app.dev) | grep '###ComponentConstructor###'
```

### 4. Подготовить тестовые данные

Минимум:
- 2-3 словаря с разными language-pair (например `EN→RU`, `DE→RU`, `FR→RU`).
- В каждом — 3-5 лексем с переводами.

Подготовительные команды для «грязных» состояний (например soft-deleted типов для Группы 6) — в текущем UI нет UI-action который позволяет открыть диалог для уже удалённого типа. Поэтому реалистичная имитация — через debug-tooling либо параллельную модификацию БД (`adb shell run-as co.lexeme.app.dev` + sqlite3); если такой возможности нет — сценарий помечается `⏭ skipped`.

### 5. Где искать UI

- **Settings drill-in (Manager-экран):** нижняя навигация → таб Settings → раздел **«Компоненты»** (`R.string.settings_section_components_management`) → row → переход в `ComponentsManagerScreen`. Title — **«Компоненты»** (`R.string.components_manager_title`).
- **PerDictionary экран:** главный экран (Vocabulary / Quiz / Statistic tab) с выбранным словарём → AppBar → icon-button **«молоток»** (visible iff currentDict != null; contentDescription **«Управление компонентами этого словаря»** — `R.string.components_tools_description`) → переход в `PerDictionaryComponentsScreen`. Title — name текущего словаря либо **«Компоненты словаря»** (`R.string.per_dict_components_title`).
- **CRUD-действия:** внутри обоих экранов — **FAB «Создать»** (`R.string.components_create_cta`), **edit-icon (карандаш)** и **trash-icon** на каждой строке user-defined компонента. Built-in компоненты (Translation, Definition) в списках **не отображаются**.

---

## Группа 1: Phase 1 — базовые CRUD user-defined компонента (Manager-экран)

Покрывает phase 1 brief §1-4. Manager-экран — глобальный aggregated view всех user-defined типов из всех словарей. Built-in компоненты в списке не показываются.

### Сценарий 1.1. Create user-defined компонент с scope=Global

**Что проверяем:** базовое создание глобального компонента из Manager-экрана работает; row появляется в списке с chip `global`.

**Откуда требование:** phase 1 brief «Создать new user-defined component»; spec § Тестовые сценарии § Create — happy path.

**Предусловие:**
- Минимум 1 словарь существует.
- Активного user-defined компонента с именем `globalA` нет (ни global, ни per-dict).

**Шаги:**
1. Нижняя навигация → Settings → раздел **«Компоненты»** → row **«Компоненты»**. Открывается `ComponentsManagerScreen`.
2. Тапнуть FAB **«Создать»** (BottomEnd).
3. В диалоге **«Новый компонент»** (`R.string.components_create_dialog_title`):
   - В поле **«Название»** (`R.string.components_create_field_name`) ввести `globalA`.
   - Radio **«Тип значения»** (`R.string.components_create_field_template`) — оставить **«Текст»** (`R.string.components_template_text`, по умолчанию).
   - Checkbox **«Разрешить несколько значений на карточке»** (`R.string.components_create_field_is_multi`) — оставить unchecked.
   - **Радио «Контекст»** (`R.string.components_create_field_scope`) — выбрать **«Общий (все словари)»** (`R.string.components_create_scope_global`).
4. Тапнуть **«Создать»** (`R.string.components_button_create`).

**На экране:**
- После шага 1: список user-defined компонентов либо empty-state (если их нет) с CTA **«Создать компонент»** (`R.string.components_empty_cta`), либо LazyColumn с уже существующими.
- После шага 3: диалог с 4 секциями — name input, template radio group (Текст / Изображение), isMulti checkbox, scope radio + (для PerDictionaries) chip-list словарей.
- После шага 4: диалог закрывается, snackbar **«Created 1»** *(локализация не завершена — hardcoded в Reducer.kt:139)*, в списке появляется row `globalA` с chip `global` (`R.string.components_chip_global`) + chip **«одно»** (`R.string.components_chip_single`) + chip **«Текст»** (`R.string.components_template_text`).

**Логи (`adb logcat | grep '###ComponentConstructor###'`):**
- Под фича-тегом для success-пути create — **логов нет** (см. backlog «feature-tag `###ComponentConstructor###` не дублируется в success-путях public UseCase методов»). Ошибки логируются под module-tag `ComponentsManager` (формат `createUserDefinedComponent failed: <message>`), не под feature-tag.

**Ожидаемый результат:**
- 1 row в `component_types` с `dictionary_id IS NULL`, `system_key IS NULL`, `removed_at IS NULL`, `name = "globalA"`.
- Компонент виден в Manager-экране (этот экран показывает global + все per-dict).

**⚠ Известные проблемы:** entry/exit логи под фича-тегом обещаны brief'ом, но не пишутся для успешных путей public UseCase методов — см. Backlog `[IS481 phase 2: feature-tag не дублируется в success-путях…]`.

---

### Сценарий 1.2. Create — попытка коллизии с built-in (`Перевод`)

**Что проверяем:** ожидаемо — uniqueness invariant с built-in (зарезервированные имена). По факту — **collision НЕ срабатывает**, user-defined с любым именем создаётся рядом с built-in.

**Откуда требование:** phase 1 spec § Create — same-scope collision (расширено: built-in имена тоже резервированы); aspect `userdefined_identity_invariant`.

**Предусловие:** минимум 1 словарь существует.

**Шаги:**
1. Settings → Components → FAB «Создать».
2. Имя — `Перевод` (то, что видишь в UI как built-in; на английской локали — `Translation`). Также попробовать варианты: `перевод` (нижний регистр), `translation` (английский технический ключ).
3. Scope — Global. Submit.

**На экране:**
- **Ожидаемо (по концепту):** диалог не закрывается, под полем Name ошибка «Имя уже занято в этой области» (`R.string.components_name_error_same_scope_collision`).
- **Реально (verified в логах 2026-06-24):** диалог **закрывается**, user-defined компонент `Перевод` **успешно создаётся** рядом с built-in. Snackbar «Создан 1». В Manager-списке появляется новая строка `Перевод`. Если потом удалить эту user-defined строку — в логах `cascade soft-delete: ... removedName=перевод write=false` (cascade пустой потому что built-in в `quiz_configs.component_refs` ссылается на `translation` через системный ключ, не через `user:перевод`).

**Почему так:** collision check (`componentTypeDao.findActiveUserDefinedByName(...)` + `findActiveGlobalByName(...)` в `CoreDbApiImpl.kt`) фильтрует **только user-defined** (`system_key IS NULL`). Built-in row (`system_key='translation'`, `name` либо NULL либо backfilled string) collision check **не видит**. Plus UI отображает built-in через локализованную строку (`R.string.components_builtin_translation_name` = «Перевод» / "Translation"), но в БД его системный ключ `translation` (lowercase) — даже если бы collision проверял built-in, comparison прошёл бы как case-sensitive vs локализованный label.

**Логи:** на create — нет (success-путь create НЕ логируется под фича-тегом — см. backlog). На последующий soft-delete (если делал) — `###ComponentConstructor### cascade soft-delete: configId=N refs=M→M write=false removedName=<имя_что_удалял>`.

**Ожидаемый результат теста:** ❌ FAIL — фича не работает как описано в концепте. user-defined компонент создаётся с зарезервированным именем.

**⚠ Записать в Backlog:**
- **Built-in имена не зарезервированы в collision check user-defined create.** Концепт `ui_placement.md § Built-in / uniqueness` подразумевает что built-in **read-only глобально**, но не описывает явно «имя built-in нельзя занять user-defined». На MVP юзер может создать `Перевод` (либо любое другое имя совпадающее с built-in display label), и в UI появятся **два разных компонента с одним именем** — путаница.
- Решение зависит от продуктового намерения:
  - Если «built-in резервирует имя» (включая локализованные display labels) → расширить collision check: для каждого built-in перебрать его локализованные имена (через `Resources` либо отдельный hardcoded set) и сравнить trim'нутый input.
  - Если «built-in и user-defined существуют параллельно даже с одним именем» (текущее поведение) → обновить концепт и удалить сценарий 1.2 как «N/A» (либо переформулировать на «два user-defined с одинаковым именем — collision»).

---

### Сценарий 1.3. Create — пустое name (NameEmpty)

**Что проверяем:** UseCase-валидация: пустое (`trim().isBlank()`) имя возвращает `CreateOutcome.NameEmpty`.

**Откуда требование:** spec § Create — NameEmpty handling.

**Предусловие:** минимум 1 словарь.

**Шаги:**
1. FAB **«Создать»**.
2. Поле **«Название»** **не трогать** либо ввести только пробелы.
3. Попытаться тапнуть **«Создать»**.

**На экране:**
- Submit-кнопка **disabled** (`canSubmit = name.trim().isNotEmpty() && (...)` — проверка на UI-уровне). Тап не реагирует визуально.
- Если по какой-то причине UI пропустил пустое имя (например через keyboard input edge case) — после Submit диалог не закрывается, под полем — **«Название не может быть пустым»** (`R.string.components_name_error_empty`).

**Логи:** нет (UI-level guard).

**Ожидаемый результат:** ничего не создаётся.

---

### Сценарий 1.4. Rename user-defined компонента

**Что проверяем:** rename меняет name в `component_types` + cascade обновляет ссылки в `quiz_configs.component_refs`.

**Откуда требование:** phase 1 brief «переименовать»; spec § Rename — happy path.

**Предусловие:**
- user-defined `OldName` существует.
- Желательно: `OldName` фигурирует в `quiz_configs.component_refs` хотя бы одного словаря (если включался в quiz picker).

**Шаги:**
1. Manager-экран → в строке `OldName` тапнуть **edit-icon (карандаш)** на trailing slot.

   > ⚠ **Phase 2 поведение:** edit-icon теперь открывает `EditComponentDialog`, **не RenameDialog**. Для phase 1 rename-пути — менять name прямо в EditDialog (см. Сценарий 3.1). Если в текущей сборке остался отдельный RenameDialog как самостоятельный entry — использовать его. Сценарий описан на phase 2 actual поведении через Edit.

2. В EditDialog: стереть `OldName` в поле **«Название»**, ввести `NewName`.
3. Checkbox **«Разрешить несколько значений на карточке»** **не трогать** (для чистоты — изолируем только rename).
4. Тапнуть **«Сохранить»** (`R.string.components_button_save`).

**На экране:**
- Диалог закрывается, snackbar **«Updated»** *(локализация не завершена — hardcoded в Reducer.kt:495)*.
- В списке строка `OldName` стала `NewName`.

**Логи:**
- На каждый `quiz_configs` row у которого `component_refs` содержал `OldName`:
  ```
  ###ComponentConstructor### cascade rename: configId=<id> refs=N→N write=true oldName=OldName newName=NewName
  ```
- На каждый config БЕЗ упоминания: тот же лог с `write=false`.
- Источник: `CoreDbApiImpl.kt:647-648`.

**Ожидаемый результат:**
- `component_types.name = 'NewName'`, `updated_at` свежий.
- Все references `OldName` в JSON `quiz_configs.component_refs` заменены на `NewName`.

**⚠ Известные проблемы:** UseCase entry/exit-логи для rename success — не пишутся под feature-tag (Backlog item).

---

### Сценарий 1.5. Rename — collision (тот же scope)

**Что проверяем:** попытка переименовать в имя, занятое другим активным user-defined в том же scope — диалог не закрывается, показывается inline ошибка.

**Откуда требование:** spec § Rename — collision branches; aspect `userdefined_identity_invariant`.

**Предусловие:**
- Два user-defined компонента `A` и `B` в одном scope (оба global, либо оба per-dict одного словаря).

**Шаги:**
1. Manager-экран → edit-icon у `A` (открывается EditDialog).
2. Изменить **«Название»** на `B`.
3. **«Сохранить»**.

**На экране:**
- Диалог **остаётся открытым**, под полем «Название» — текст **«Компонент с таким названием уже существует в этом контексте»** (`R.string.components_name_error_same_scope_collision`).
- При попытке Save второй раз — snackbar **«Name already taken in this scope»** *(локализация не завершена — hardcoded в Reducer.kt:512)*, если EditDialog уже закрылся — fallback.

**Логи:** error-логи под module-tag `ComponentsManager`, не под feature-tag.

**Ожидаемый результат:** `A.name` не изменился, `B` тоже не тронут.

---

### Сценарий 1.6. Rename — попытка переименовать встроенный (BuiltInProtected)

**Что проверяем:** built-in компоненты (Translation / Definition) не отображаются в Manager-списке, поэтому через UI этот сценарий **в принципе невоспроизводим**. Защита `BuiltInProtected` работает на data-уровне как defence-in-depth.

**Откуда требование:** spec § Rename — built-in protected; phase 1 brief.

**Предусловие:** N/A (built-in отсутствуют в UI-списке).

**Шаги:** не применимо.

**На экране:** built-in нет в LazyColumn, edit-icon для них недоступен.

**Логи:** error-лог если бы вызвалось `renameComponentType` для built-in row — `BuiltInProtected` outcome, snackbar **«Built-in protected»** *(локализация не завершена — Reducer.kt:293)*. Только через debug-action / direct API call.

**Ожидаемый результат:** `⏭ skipped` либо `N/A` для UI-теста (data-level гарантия только).

---

### Сценарий 1.7. Soft-delete user-defined компонента

**Что проверяем:** soft-delete не уничтожает данные; preview impact показывает counters перед confirm; cascade очищает `quiz_configs.component_refs` + сбрасывает quiz_picker prefs.

**Откуда требование:** phase 1 brief «удаление с warning»; spec § Delete — preview + confirm; deletion_concept.md.

**Предусловие:**
- user-defined компонент `DelMe` существует.
- Желательно: у `DelMe` есть active `component_values` (для не-нулевого `impact.valueCount`); фигурирует в `quiz_configs.component_refs` минимум одного словаря; в `quiz_picker_dict_<id>` prefs выбран.

**Шаги:**
1. Manager-экран → в строке `DelMe` тапнуть **trash-icon** на trailing slot.
2. Открывается `DeleteComponentConfirmDialog` с заголовком **«Удалить "DelMe"?»** (`R.string.components_delete_dialog_title`).
3. Дождаться, пока внутри диалога подгрузится preview impact (CircularProgressIndicator → counters).
4. После загрузки видны строки:
   - **«Будет скрыто значений: N»** (`R.string.components_delete_impact_values`, visible iff `valueCount > 0`).
   - **«Затронуто словарей: N»** (`R.string.components_delete_impact_dicts`, visible iff `dictCount > 0`).
   - **«Будет удалён из настроек квиза: N»** (`R.string.components_delete_impact_quiz`, visible iff `quizCount > 0`).
   - **«Сбросит выбор квиза в словарях: N»** (`R.string.components_delete_impact_prefs`, visible iff `prefsCount > 0`).
   - **«Компонент будет помечен как удалённый. Значения остаются в базе, но скрываются.»** (`R.string.components_delete_hint`).
5. Тапнуть **«Confirm»** (alarm-button — красная). *Примечание: текст confirm-кнопки в UI — отдельная строка, см. диалог.*

**На экране:**
- Диалог закрывается, snackbar **«N values hidden»** *(локализация не завершена — hardcoded в Reducer.kt:388; где N = `impact.valueCount`)*.
- В списке `DelMe` пропадает.

**Логи:**
- В транзакции — cascade soft-delete на каждый затронутый config:
  ```
  ###ComponentConstructor### cascade soft-delete: configId=<id> refs=N→M write=true removedName=DelMe
  ```
  (`CoreDbApiImpl.kt:709-710`).
- После транзакции — prefs reset bundle:
  ```
  ###ComponentConstructor### resetQuizPickerPrefs start: count=<N>
  ###ComponentConstructor### resetQuizPickerPrefs ok: dictId=<id>            (per-pref, если ok)
  ###ComponentConstructor### resetQuizPickerPrefs fail: dictId=<id> cause=<msg>  (если failed)
  ###ComponentConstructor### resetQuizPickerPrefs done: ok=<successCount>/<total>
  ```
  (`ComponentsManagerUseCaseImpl.kt:186-214`).

**Ожидаемый результат:**
- `component_types.removed_at` свежий timestamp.
- Refs `DelMe` удалены из всех `quiz_configs.component_refs`.
- `quiz_picker_dict_<id>` prefs сброшены для затронутых словарей.
- `DelMe` пропал из Manager-экрана и per-dict экранов.

---

### Сценарий 1.8. Soft-delete — попытка удалить встроенный (BuiltInProtected)

См. Сценарий 1.6 — built-in компоненты отсутствуют в UI-списке Manager / PerDict, trash-icon недоступен. Защита `DeleteOutcome.BuiltInProtected` работает на data-уровне.

**На экране (defence-in-depth):** snackbar **«Built-in protected»** *(локализация не завершена — Reducer.kt:392)* если бы вызвалось.

**Ожидаемый результат:** `⏭ N/A` для UI-теста.

---

### Сценарий 1.9. Cascade — после soft-delete компонент исчезает из quiz picker'а

**Что проверяем:** cleanup `quiz_configs.component_refs` (DB cascade) + reset `quiz_picker_dict_<id>` prefs (UseCase composition) → удалённый компонент не появляется в quiz picker.

**Откуда требование:** phase 1 spec § Delete — cascade; deletion_concept.md.

**Предусловие:** выполнить Сценарий 1.7 для компонента `DelMe`, который ранее был выбран в quiz_picker какого-то словаря.

**Шаги:**
1. После soft-delete `DelMe` — перейти в Quiz tab затронутого словаря.
2. Открыть quiz session либо picker компонента (доступен через menu чата).
3. Проверить список доступных компонентов (`availableTypes`).

**На экране:**
- `DelMe` **отсутствует** в picker'е.
- Если pref ссылался на `DelMe` — после сброса prefs picker автоматически возвращается к default (translation).

**Логи:** prefs reset логи (см. 1.7) — здесь только consume результата.

**Ожидаемый результат:** quiz session не показывает items типа `DelMe`; ни в одном месте UI нет ссылки на удалённый компонент.

---

## Группа 2: Phase 1 — Per-Dictionary экран

Покрывает phase 1 brief «per-dictionary view через AppBar». Per-Dictionary экран — scoped view: только user-defined компоненты применимые к текущему словарю (global + own per-dict).

### Сценарий 2.1. Открыть Per-Dictionary Components через AppBar «молоток»

**Что проверяем:** entry-point из `DictionaryAppBar` (общий AppBar для 3 табов Vocabulary/Quiz/Statistic) ведёт в `PerDictionaryComponentsScreen` с правильным `dictionaryId`.

**Откуда требование:** phase 1 brief; spec § UI Layout § Точка касания 4.

**Предусловие:** минимум 1 словарь выбран в picker'е (currentDict != null); минимум 1 user-defined компонент применим к нему (global или own per-dict).

**Шаги:**
1. Vocabulary tab → если currentDict нет, выбрать через DictDropDownWidget.
2. В AppBar — найти icon-button **«молоток»** (contentDescription **«Управление компонентами этого словаря»** — `R.string.components_tools_description`).
3. Тапнуть.
4. Повторить шаги 1-3 для Quiz tab и Statistic tab.

**На экране:**
- После шага 2: «молоток» visible iff `currentDict != null` (если currentDict не выбран — icon скрыт, проверить snapshot).
- После шага 3: открывается `PerDictionaryComponentsScreen`, title = name выбранного словаря либо **«Компоненты словаря»** (`R.string.per_dict_components_title`).
- На всех трёх табах icon работает идентично (общий AppBar).

**Логи:** под фича-тегом для open-event — не пишутся (нет log call'а на навигацию). Из ViewModel logger.d при reducer.init возможен под module-tag `PerDictComponents`.

**Ожидаемый результат:** экран открывается с `dictionaryId = currentDict.id`; список фильтрует к global + own per-dict.

---

### Сценарий 2.2. Create user-defined в Per-Dictionary (scope=PerDictionary current)

**Что проверяем:** в PerDict экране диалог Create по умолчанию имеет scope = `PerDictionaries([currentDict.id])` (преднастройка из контекста); scope_slot полностью **скрыт** (multi-dict picker не показывается).

**Откуда требование:** spec § Per-dictionary — create with preselect scope; ui_layout HostVariant.PerDict.

**Предусловие:** открыт `PerDictionaryComponentsScreen` для словаря `D1`.

**Шаги:**
1. FAB **«Создать»**.
2. В диалоге ввести **«Название»** = `perDict_X`.
3. **Внимание:** блок **«Контекст»** (radio + chip-list) в этой версии диалога **отсутствует** — scope hardcoded reducer-side в `PerDictionaries([D1.id])`. Видны только Название / Тип значения / чекбокс / actions.
4. Тапнуть **«Создать»**.

**На экране:**
- Диалог закрывается, snackbar **«Created 1»** *(локализация не завершена — Reducer.kt:139)*.
- В списке `D1` появляется row `perDict_X` **без chip `global`** (это per-dict).

**Логи:** нет под фича-тегом (см. 1.1).

**Ожидаемый результат:** 1 row в `component_types` с `dictionary_id = D1.id`.

---

### Сценарий 2.3. Список не показывает компоненты других словарей

**Что проверяем:** scope-фильтр Per-Dictionary — видны только global + per-dict с `dictionaryId = currentDict.id`.

**Откуда требование:** spec § Per-dictionary — global components visible; phase 1 brief.

**Предусловие:**
- Словарь `D1` с компонентом `perDict_D1` (per-dict).
- Словарь `D2` с компонентом `perDict_D2` (per-dict).
- Global компонент `globalShared`.

**Шаги:**
1. Открыть `PerDictionaryComponentsScreen` для `D1` (через AppBar «молоток»).
2. Проверить список.

**На экране:**
- В списке: `perDict_D1` (без chip `global`) + `globalShared` (с chip `global`).
- `perDict_D2` **отсутствует**.

**Логи:** не требуется.

**Ожидаемый результат:** фильтр корректен; глобальные видимы во всех словарях.

---

### Сценарий 2.4. Rename / Delete с Per-Dictionary экрана

**Что проверяем:** те же ветки что в Manager, но через UI Per-Dict. Поведение идентично (shared widget из `:modules:widget:component_widgets`).

**Откуда требование:** spec § Per-dictionary; phase 1 brief.

**Предусловие:** PerDict экран словаря `D1` с компонентом `tmp`.

**Шаги:**
1. Edit-icon у `tmp` → EditDialog → изменить **«Название»** → **«Сохранить»** (parity с 1.4).
2. Trash-icon у `tmp` → DeleteConfirm с preview impact → Confirm (parity с 1.7).

**На экране:** идентично Manager (cascade snackbar / impact preview / soft-delete все одинаковые).

**Логи:** cascade rename / cascade soft-delete + prefs reset логи под фича-тегом — те же что в 1.4 / 1.7 (data layer общий).

**Ожидаемый результат:** rename / delete работают одинаково с двух экранов; PerDict-список перерисовывается без удалённого row.

---

## Группа 3: Phase 2 — Edit компонента + cardinality downgrade

Покрывает phase 2 task §1. UseCase метод — `editComponent(typeId, name, template, isMulti): EditOutcome` (9 outcome-веток: Success / NameEmpty / SameScopeCollision / CrossScopeCollision / CardinalityDowngradeBlocked / TemplateImmutable / BuiltInProtected / Removed / Failure).

### Сценарий 3.1. Edit name — happy path

**Что проверяем:** редактирование имени user-defined компонента сохраняется в БД и пересылается через cascade в `quiz_configs.component_refs`.

**Откуда требование:** phase 2 task §1; spec § Phase 2 § Edit — happy path.

**Предусловие:**
- user-defined `OldName` существует.
- Желательно: фигурирует в `quiz_configs.component_refs` (для cascade-лога).

**Шаги:**
1. Manager либо PerDict → edit-icon (карандаш) у `OldName`.
2. В EditDialog с заголовком **«Редактировать компонент»** (`R.string.components_edit_dialog_title`) стереть имя в поле **«Название»** (`R.string.components_edit_field_name`), ввести `NewName`.
3. Checkbox **«Разрешить несколько значений на карточке»** (`R.string.components_edit_field_is_multi`) **не трогать**.
4. Radio **«Тип значения»** (`R.string.components_edit_field_template`) **не трогать**.
5. Тапнуть **«Сохранить»**.

**На экране:**
- После шага 1: диалог с тремя секциями (Название + Тип значения radio group + чекбокс); preview_slot (CardinalityDowngrade) скрыт.
- После шага 5: диалог закрывается, snackbar **«Updated»** *(локализация не завершена — Reducer.kt:495)*, в списке `OldName` → `NewName`.

**Логи:**
- На каждый затронутый config:
  ```
  ###ComponentConstructor### cascade rename: configId=<id> refs=N→N write=true oldName=OldName newName=NewName
  ```
- На каждый незатронутый — то же с `write=false`.

**Ожидаемый результат:**
- `component_types.name = 'NewName'`, `updated_at` свежий.
- Refs в `quiz_configs.component_refs` JSON заменены.

**⚠ Известные проблемы:** entry/exit-логи `editComponent entry/exit outcome=Success` под фича-тегом — не пишутся для public методов; только в `cascade*` и `resetQuizPickerPrefsBestEffort`. См. Backlog `[IS481 phase 2: feature-tag не дублируется в success-путях…]`.

---

### Сценарий 3.2. Edit isMulti: false → true (upgrade — без preview)

**Что проверяем:** переключение `isMulti: false → true` (upgrade) всегда успешно; downgrade-guard не срабатывает (он только на downgrade-direction); `CardinalityDowngradePreviewWidget` не показывается.

**Откуда требование:** spec § Cardinality downgrade SELECT precondition.

**Предусловие:** user-defined компонент с `isMulti=false`.

**Шаги:**
1. EditDialog → checkbox **«Разрешить несколько значений на карточке»** поставить (`true`).
2. Название не трогать.
3. **«Сохранить»**.

**На экране:**
- Диалог закрывается, snackbar **«Updated»** *(локализация не завершена)*, в списке у строки chip **«много»** (`R.string.components_chip_multi`) вместо **«одно»**.

**Логи:** cascade rename не появляется (name не менялся).

**Ожидаемый результат:** `component_types.is_multi = 1`, `updated_at` свежий.

---

### Сценарий 3.3. Edit isMulti: true → false с 0 problematic лексем (legitimate downgrade)

**Что проверяем:** если ни у одной лексемы нет >1 value этого типа — downgrade-guard НЕ блокирует; switch проходит.

**Откуда требование:** spec § Phase 2 § Cardinality downgrade SELECT precondition (legitimate path); data bug #2 fix.

**Предусловие:**
- user-defined компонент `MultiOK` с `isMulti=true`.
- На нём есть active `component_values`, но у каждой лексемы — ровно один value (либо values совсем нет).

**Шаги:**
1. EditDialog для `MultiOK`.
2. Checkbox **«Разрешить несколько значений на карточке»** снять (`false`).
3. **«Сохранить»**.

**На экране:**
- Диалог закрывается, snackbar **«Updated»** *(локализация не завершена)*, chip переключается на **«одно»**.

**Логи:** cascade rename не появляется (name не менялся).

**Ожидаемый результат:** `is_multi = 0` без `CardinalityDowngradeBlocked`. Edge case data bug #2 fixed — реальный per-lexeme SELECT с deterministic `ORDER BY component_values.updated_at DESC, lexeme_id ASC`, не conservative «всегда блокировать».

---

### Сценарий 3.4. Edit isMulti: true → false с problematic лексемами (CardinalityDowngradeBlocked + preview)

**Что проверяем:** если хотя бы одна лексема имеет >1 value этого типа — downgrade блокируется; диалог НЕ закрывается; показывается `CardinalityDowngradePreviewWidget` с inline top-3 + (для >3) кнопкой «Показать все».

**Откуда требование:** phase 2 task §1; spec § Edit — cardinality downgrade blocked (size ≤ 3) + (size > 3).

**Предусловие:**
- user-defined компонент `MultiC` с `isMulti=true`.
- Хотя бы одна лексема, у которой 2+ active `component_values` этого типа. Подготовить можно через WordCard: добавить 2 values этого user-defined компонента к одной lexeme'е (если UI это не позволяет на user-defined типах — заранее подготовить SQL вручную либо через debug-build).

**Шаги:**
1. EditDialog для `MultiC`.
2. Checkbox **«Разрешить несколько значений на карточке»** снять (`false`).
3. **«Сохранить»**.

**На экране:**
- Диалог **остаётся открытым**.
- Под полями появляется блок `CardinalityDowngradePreviewWidget` (background `errorContainer`):
  - Заголовок: **«Нельзя переключить на одно — в этих карточках несколько значений:»** (`R.string.components_edit_cardinality_blocked_title`).
  - До 3 строк лексем: **«Лексема №<id>»** (формат `R.string.components_edit_lexeme_label = "Лексема №%1$d"` — это **placeholder**, см. ниже).
  - Если конфликтных лексем > 3 — TextButton **«Показать все (N)»** (`R.string.components_edit_show_all`).
- Также возможен snackbar **«Cardinality downgrade blocked»** *(локализация не завершена — Reducer.kt:534, fallback)*.

**Логи:** cascade rename не появляется (изменение не применено).

**Ожидаемый результат:** `is_multi` в БД остаётся `1`; пользователь видит inline preview какие лексемы блокируют.

**⚠ Известные проблемы:**
- **«Лексема №<id>»** — placeholder, **не реальное имя слова**. Backlog: `[IS481 phase 2: preview лексем в downgrade-блоке показывает технические ID вместо слов]`. Нужен `getLexemesByIds(ids)` UseCase-метод, замена `%1$d` на `%1$s`.
- Кнопка **«Показать все (N)» — no-op** (`onShowAll = { /* TBD */ }`). Тап ничего не делает. Backlog item — drill-in destination (bottom-sheet либо отдельный screen) не реализован. Implement-correction §2.

---

### Сценарий 3.5. Edit template — попытка изменить (TemplateImmutable)

**Что проверяем:** template после создания иммутабелен. UI radio-кнопка template **clickable** (можно тапнуть **«Изображение»**), но при Submit с changed template — отбивается через `EditOutcome.TemplateImmutable`.

**Откуда требование:** phase 2 task §1; spec § Edit — template immutability gate; concept template_model.md § Open Q10.

**Предусловие:** user-defined компонент с template = **«Текст»**.

**Шаги:**
1. EditDialog.
2. В блоке **«Тип значения»** (`R.string.components_edit_field_template`) тапнуть radio **«Изображение»** (`R.string.components_template_image`).
3. Тапнуть **«Сохранить»**.

**На экране:**
- Поведение: на data-уровне (`CoreDbApiImpl`) гарантированно возвращается `EditComponentOutcome.TemplateImmutable`. Reducer-mapping: диалог закрывается, snackbar **«Template cannot be changed»** *(локализация не завершена — Reducer.kt:553)*.

**Логи:** cascade rename не появляется; entry/exit под фича-тегом — нет.

**Ожидаемый результат:** template в БД не меняется.

**⚠ Известные проблемы:** UseCase **gate отсутствует** на UseCase-уровне (Backlog: `[IS481 phase 2: template-immutability gate в UseCase]`). Контракт обещает: «UseCaseImpl сравнивает new.template vs current.template → `TemplateImmutable` без обращения к data API». Реально — UseCase сразу делегирует на data API, защита только data-level (лишний DB round-trip). Юзер этого **не заметит** — outcome тот же, snackbar тот же; разница архитектурная.

---

### Сценарий 3.6. Edit failure (общий)

**Что проверяем:** при exception на data-layer (IO error / DB corruption) возвращается `EditOutcome.Failure(cause)`; UI показывает snackbar с описанием.

**Откуда требование:** spec § Edit — Failure handling.

**Предусловие:** трудно воспроизвести руками без debug-injection (нужно forced exception в `LexemeApi.editComponentType`). Можно пропустить (`⏭ skipped`) если нет инструмента.

**Шаги:** не применимо без debug-tooling.

**На экране (если бы вызвался):** диалог закрывается, snackbar **«Failed: <cause description>»** *(локализация не завершена — Reducer.kt:564 либо аналог, `failureLabel(cause)` helper)*.

**Логи:** под module-tag — `editComponent failed: <message>` (формат не подтверждён для editComponent — для create/rename/delete такой формат есть в module-tag).

**Ожидаемый результат:** ⏭ skipped (нет debug-инструмента).

---

## Группа 4: Phase 2 — Multi-dict scope picker (BLOCKER)

> 🚨 **BLOCKER: вся группа не работает в текущей сборке.**
>
> **Причина:** `DictionariesFlowHandler` создан, но **не зарегистрирован в `effectHandlerSet` ViewModel** (`ComponentsManagerViewModel.kt:42-47`). Subscription никогда не стартует → `state.availableDictionaries` всегда `emptyList()` → chip-list пустой → radio «Выбранные словари» (PerDictionaries) ведёт в тупик: `canSubmit = false` навсегда.
>
> **Источник:** `Backlog.md § ВекторныйПиздеж → [IS481 phase 2 BLOCKER: multi-dict scope picker в Manager-экране не работает в runtime]`.
>
> **Прогонять группу 4 только после фикса handler registration** (добавление `dictionariesFlowHandler` в `effectHandlerSet`). До фикса — все сценарии 4.1-4.4 помечать `🚨 BLOCKER`, не запускать.

### Сценарий 4.1. Выбор scope=Global (regression — chip-list скрыт)

**Что проверяем:** Global-режим работает (это regression — был и в phase 1, должен остаться рабочим после добавления picker'а).

**Откуда требование:** phase 2 task §2; spec § Create — happy path (Global).

**Предусловие:** минимум 1 словарь.

**Шаги:**
1. Settings → **«Компоненты»** → FAB **«Создать»**.
2. В диалоге **«Новый компонент»**:
   - **«Название»** = `globalA`.
   - Radio **«Контекст»** = **«Общий (все словари)»** (`R.string.components_create_scope_global`).
3. Тапнуть **«Создать»**.

**На экране:**
- Блок chip'ов словарей **скрыт** (Global → chip-list condition `scope is PerDictionaries` = false).
- После Create: диалог закрывается, snackbar **«Created 1»** *(локализация не завершена)*.

**Логи:** нет под фича-тегом (см. 1.1).

**Ожидаемый результат:** 1 row в `component_types` с `dictionary_id IS NULL`.

---

### Сценарий 4.2. Выбор scope=PerDictionaries + multi-select chip'ов (🚨 BLOCKER)

**Что проверяем:** создание N rows по N выбранным словарям одним диалогом.

**Откуда требование:** phase 2 task §2; spec § Create — multi-dict scope happy path.

**Предусловие:**
- 3 словаря (`EN`, `RU`, `DE`).
- 🚨 Group 4 BLOCKER fixed.

**Шаги:**
1. Settings → **«Компоненты»** → FAB **«Создать»**.
2. **«Название»** = `perDictB`.
3. Radio **«Контекст»** = **«Выбранные словари»** (`R.string.components_create_scope_per_dict`).
4. После выбора этого radio под ним должна появиться `FlowRow` chip'ов — по одной chip на словарь (`EN`, `RU`, `DE`).
5. Тапнуть chip `EN` и `RU` (выбрать). `DE` оставить.
6. Тапнуть **«Создать»**.

**На экране:**
- Шаг 4 (после фикса BLOCKER): chip-list `FlowRow + FilterChip` — каждая chip с именем словаря.
- Шаг 5: тапнутые chip'ы — selected (background `secondaryContainer`).
- Шаг 6: диалог закрывается, snackbar **«Created 2»** *(локализация не завершена — Reducer.kt:139, `Created ${o.created.size}`)*.
- В списке появляются 2 row'а `perDictB`.

**Логи:** нет под фича-тегом.

**Ожидаемый результат:** 2 row'а в `component_types` (`dictionary_id IN (EN.id, RU.id)`, оба с `name='perDictB'`).

**⚠ Известные проблемы:** BLOCKER (см. вверху группы).

---

### Сценарий 4.3. Submit с 0 selected (canSubmit disabled) (🚨 BLOCKER)

**Что проверяем:** preventive UX — submit-кнопка disabled пока выбрано 0 словарей в PerDictionaries-режиме.

**Откуда требование:** spec § Create — submit disabled при пустом PerDictionaries selection.

**Предусловие:** 🚨 BLOCKER fixed; минимум 1 словарь.

**Шаги:**
1. FAB **«Создать»**.
2. **«Название»** = `zeroD`.
3. Radio = **«Выбранные словари»**.
4. **НЕ отмечать ни одной chip.**

**На экране:**
- Кнопка **«Создать»** **disabled** (серая, не реагирует на тап).
- `canSubmit = name.trim().isNotEmpty() && (scope is Global || selectedDictionaryIds.isNotEmpty())` — без выбранных dict → false.

**Логи:** нет (UI-level guard, никакого UseCase call).

**Ожидаемый результат:** диалог не отправляется, ничего не создаётся.

---

### Сценарий 4.4. Chip staleness (out-of-band удаление словаря пока диалог открыт) (🚨 BLOCKER)

**Что проверяем:** при удалении словаря через другой экран — chip пропадает из selection реактивно (через `Msg.DictionariesLoaded` с filtered set); если selection опустеет — `canSubmit=false`.

**Откуда требование:** spec § Multi-dict — chip staleness filtering; aspect `dictionary_chip_staleness`.

**Предусловие:**
- 2 словаря (`A`, `B`); 🚨 BLOCKER fixed.
- Способ удалить словарь параллельно (другой экран DictionaryTab → swipe-delete, либо ADB SQL, либо debug-action).

**Шаги:**
1. FAB **«Создать»** в Manager.
2. Radio = **«Выбранные словари»**, отметить `A` и `B`.
3. **Не закрывая диалог** — параллельно удалить словарь `A` (другой экран DictionaryTab → swipe-delete).
4. Вернуться в диалог Manager.

**На экране:**
- Chip `A` пропадает из FlowRow (либо становится non-selected) — реакция на `Msg.DictionariesLoaded(updated)` filters `selectedDictionaryIds ∩ list.ids` → `A` уходит.
- Если был выбран только `A` — submit-кнопка disabled (см. 4.3).

**Логи:** нет фича-уровневых.

**Ожидаемый результат:** UI не залипает, submit не отправляет ссылку на несуществующий `A.id`.

**Invariant:** на `DictionariesLoaded` НЕ мутируется `editDialog` (F030; есть dedicated unit-test `whenEditDialogOpen_thenDictionariesLoaded_doesNotMutateEditState`).

---

## Группа 5: Phase 2 — Shared widget module regression

Покрывает phase 2 task §3. Чистый рефакторинг — пользователь визуально не должен заметить ничего нового; цель — убедиться что 14 widgets из `:modules:widget:component_widgets` рендерятся одинаково в обоих экранах (Manager / PerDict).

### Сценарий 5.1. Visual parity между Manager и Per-Dictionary

**Что проверяем:** одинаковый рендер CreateDialog / EditDialog / DeleteConfirmDialog / row-widgets / EmptyState / FAB между двумя экранами (за вычетом `HostVariant.Manager` показывает scope_slot, `HostVariant.PerDict` — скрывает).

**Откуда требование:** phase 2 task §3; spec § Shared widget module.

**Предусловие:** минимум 1 user-defined компонент.

**Шаги:**
1. Открыть `ComponentsManagerScreen` (Settings → **«Компоненты»**).
2. Запомнить визуально: layout row (icon + name + chips + edit/trash icons), FAB текст, диалоги (Create / Edit / Delete по очереди — открыть и закрыть).
3. Открыть `PerDictionaryComponentsScreen` (AppBar «молоток» в любом словаре).
4. Повторить шаг 2 для второго экрана.

**На экране:**
- **Row layout идентичен:** leading icon (ic_components) + name + chips (template / cardinality / global) + trailing edit + trailing trash. PerDict у global-компонента дополнительно показывает chip `global` в title row.
- **EmptyState одинаков** (виджет тот же `ComponentsEmptyStateWidget`); разные тексты headlineRes/bodyRes per host:
  - Manager: **«У вас пока нет своих компонентов»** (`R.string.components_empty_headline_manager`) / **«Перевод работает автоматически в любом словаре.»** (`R.string.components_empty_body_manager`).
  - PerDict: **«В этом словаре только перевод»** (`R.string.components_empty_headline_per_dict`) / **«Добавьте новый компонент, чтобы расширить карточки.»** (`R.string.components_empty_body_per_dict`).
- **FAB одинаков** (`CreateComponentFab`, icon `ic_add`, text **«Создать»** из `R.string.components_create_cta`).
- **Диалоги RenameComponentDialog / DeleteComponentConfirmDialog / EditComponentDialog** — рендерятся идентично.
- **CreateDialog различается:**
  - Manager — виден блок **«Контекст»** (radio + FlowRow chip'ов).
  - PerDict — блок «Контекст» скрыт (`HostVariant.PerDict` → `scope_slot` не рендерится).

**Логи:** не требуется.

**Ожидаемый результат:** никаких визуальных regress'ов от phase 1.

---

## Группа 6: Phase 2 — Removed semantics

Покрывает phase 2 task §4. После soft-delete (`removed_at IS NOT NULL`) операции rename / edit / delete возвращают `Removed` outcome (не `BuiltInProtected`), UI показывает snackbar **«Component removed»** *(локализация не завершена — Reducer.kt:297, 396, 561)*.

**Общая трудность воспроизведения группы:** в текущем UI списки фильтруют `removed_at IS NULL`, и нет UI-action открыть rename/edit/delete dialog для уже soft-deleted типа. Реалистично проверить только через debug-tooling либо параллельную модификацию БД пока диалог открыт. Варианты:
1. Открыть диалог; через `adb shell run-as co.lexeme.app.dev` + sqlite3 поставить `component_types.removed_at = strftime('%s','now')*1000` для нужного `typeId`; вернуться в диалог и Submit.
2. Открыть два инстанса (если возможно для multi-user/profile) — пока в одном открыт диалог, в другом удалить.
3. Если debug-action injection нет — сценарии помечаются `⏭ skipped`.

### Сценарий 6.1. Rename уже soft-deleted (Removed)

**Что проверяем:** попытка rename удалённого type → `RenameOutcome.Removed` → snackbar **«Component removed»** + close dialog.

**Откуда требование:** phase 2 task §4; spec § Phase 2 § Rename — Removed parity; data bug #0 fix.

**Предусловие:**
- user-defined `X` существует.
- Способ перевести `X.removed_at IS NOT NULL` пока RenameDialog для `X` открыт.

**Шаги:**
1. Открыть RenameDialog для `X` (через edit-icon — открывает EditDialog в phase 2; самостоятельный RenameDialog как отдельный entry-point в текущем UI может отсутствовать. Если так — пометить `⏭ N/A` и тестировать через EditDialog, сценарий 6.2).
2. Параллельно soft-delete `X` (debug / SQL).
3. В диалоге изменить имя на `Y`, тапнуть Submit.

**На экране:**
- Snackbar **«Component removed»** *(локализация не завершена — hardcoded в Reducer.kt:297)*.
- Диалог закрывается.
- Список перерисовывается без `X`.

**Логи:** под фича-тегом для Removed exit — не пишется (success/error logs только под module-tag).

**Ожидаемый результат:** outcome = `RenameOutcome.Removed`. Data bug #0 fix: проверка `removed_at` ПЕРЕД `system_key` в `CoreDbApiImpl.renameComponentType:532-533`.

**⚠ Известные проблемы:**
- В phase 2 UI edit-icon открывает **EditDialog**, не RenameDialog. RenameDialog как отдельный entry-point может отсутствовать. Если так — сценарий `⏭ N/A`, перевести на Сценарий 6.2.

---

### Сценарий 6.2. Edit уже soft-deleted (Removed)

**Что проверяем:** попытка edit удалённого type → `EditOutcome.Removed` → snackbar **«Component removed»** + close dialog.

**Откуда требование:** phase 2 task §1+§4; spec § Edit — race with soft-delete (Removed).

**Предусловие:** user-defined `X`; способ удалить `X` пока EditDialog открыт.

**Шаги:**
1. Открыть EditDialog для `X` (edit-icon на row).
2. Параллельно soft-delete `X` (debug / SQL).
3. В диалоге что-то поменять (например toggle **«Разрешить несколько значений на карточке»**).
4. Тапнуть **«Сохранить»**.

**На экране:**
- Snackbar **«Component removed»** *(локализация не завершена)*.
- Диалог закрывается.
- Список без `X`.

**Логи:** cascade rename не появляется. Под фича-тегом для Removed exit — не пишется (UseCase entry/exit-логи не пишутся в success / non-Failure путях).

**Ожидаемый результат:** outcome = `EditOutcome.Removed`. Native Removed branch в `LexemeApiImpl.editComponentType:572-573`.

**⚠ Известные проблемы:**
- Backlog: `[IS481 phase 2: порядок защит в editComponentType inconsistent vs rename/softDelete]`. В `editComponentType` порядок `BuiltIn → Removed` (обратный rename/softDelete). Для built-in + soft-deleted типа edit вернёт `BuiltInProtected`, не `Removed`. Built-in типы юзер не может soft-delete'нуть руками, но если каким-то путём это произошло — разные CRUD дадут разные snackbar'ы (Built-in protected vs Component removed). Issue невидим в обычном UX, но нарушает unified policy.

---

### Сценарий 6.3. Soft-delete уже soft-deleted (Removed)

**Что проверяем:** попытка confirm-delete уже удалённого type → `DeleteOutcome.Removed` → snackbar **«Component removed»**.

**Откуда требование:** phase 2 task §4; spec § Delete — Removed parity; data bug #1 fix.

**Предусловие:** user-defined `X`; способ удалить `X` пока `DeleteConfirmDialog` открыт.

**Шаги:**
1. Открыть `DeleteConfirmDialog` для `X` (trash-icon на row → дождаться preview impact).
2. Параллельно soft-delete `X` (debug / SQL).
3. Тапнуть «Confirm» (alarm-button).

**На экране:**
- Snackbar **«Component removed»** *(локализация не завершена)*.
- Диалог закрывается.

**Логи:** под фича-тегом для Removed exit — не пишется; cascade soft-delete тоже не появляется (race выиграл первый delete, второй вернулся `Removed`).

**Ожидаемый результат:** outcome = `DeleteOutcome.Removed`. Data bug #1 fix: проверка `removed_at` между lookup и system_key check в `CoreDbApiImpl.softDeleteComponentType:690-691`.

---

## Группа 7: Phase 2 — Feature-tag + migration logs

Покрывает phase 2 task §5. Фильтр `adb logcat | grep '###ComponentConstructor###'` должен показывать только фича-события и ничего лишнего.

Из реального кода — следующие точки производят логи под `LogTags.COMPONENT_CONSTRUCTOR`:

| Где | Лог-формат | Когда |
|---|---|---|
| `Migration_012_to_013.kt:57-89` | `M12→M13 step <1..9> <stepName>: ok` | Однократно при апгрейде БД с v12 на v13 |
| `CoreDbApiImpl.kt:647-648` | `cascade rename: configId=<id> refs=N→M write=<bool> oldName=<o> newName=<n>` | На каждый config при rename component_type |
| `CoreDbApiImpl.kt:709-710` | `cascade soft-delete: configId=<id> refs=N→M write=<bool> removedName=<n>` | На каждый config при soft-delete component_type |
| `ComponentsManagerUseCaseImpl.kt:186-188` | `resetQuizPickerPrefs start: count=<N>` | Начало prefs reset (после успешного soft-delete) |
| `ComponentsManagerUseCaseImpl.kt:195-198` | `resetQuizPickerPrefs ok: dictId=<id>` | На каждый успешно сброшенный pref |
| `ComponentsManagerUseCaseImpl.kt:206-209` | `resetQuizPickerPrefs fail: dictId=<id> cause=<msg>` | При failure per-pref (best-effort, не блокирует) |
| `ComponentsManagerUseCaseImpl.kt:212-214` | `resetQuizPickerPrefs done: ok=<N>/<total>` | Конец prefs reset |

**Важно:** UseCase entry/exit-логи (`createUserDefinedComponent entry…`, `editComponent entry…` и т.д.) которые brief обещает — в реальной сборке **не пишутся** под фича-тегом. Логируются только error-кейсы через module-tag `ComponentsManager`. См. Backlog `[IS481 phase 2: feature-tag не дублируется в success-путях public UseCase методов]` — техдолг.

### Сценарий 7.1. Migration M12→M13 на первом запуске (старая база) — 9 log-строк

**Что проверяем:** при первом запуске phase 2-сборки на устройстве с pre-M13 БД (schema v12) в logcat появляется 9 строк `M12→M13 step N <name>: ok`.

**Откуда требование:** phase 2 task §5; spec § Migration logs.

**Предусловие:**
- Старый билд (с schema v12) был установлен ранее, оставил БД.
- Свежая phase 2 сборка ещё не запускалась (миграция M12→M13 не применялась).
- Альтернатива: имитировать вручную через `adb shell run-as co.lexeme.app.dev` → подменить `room_master_table.identity_hash` или версию schema на v12 (сложно без debug-tooling). Если нет старой БД — `⏭ skipped`.

**Шаги:**
1. Запустить `adb logcat -c && adb logcat | grep '###ComponentConstructor###'`.
2. Установить и запустить новый билд `adb install -r app-debug.apk`.
3. Дождаться, пока на первом экране отрисуются данные.

**На экране:** ничего особенного (миграция прозрачна для UI).

**Логи (ожидаемые 9 строк, по порядку):**
```
###ComponentConstructor### M12→M13 step 1 renameComponentTypesRemoveDate: ok
###ComponentConstructor### M12→M13 step 2 addComponentTypesNewColumns: ok
###ComponentConstructor### M12→M13 step 3 dropUniqueComponentTypesDictName: ok
###ComponentConstructor### M12→M13 step 4 addComponentValuesNewColumns: ok
###ComponentConstructor### M12→M13 step 5 dropUniqueComponentValuesLexemeType: ok
###ComponentConstructor### M12→M13 step 6 createComponentValuesLexemeIdIndex: ok
###ComponentConstructor### M12→M13 step 7 consolidateLongTextTemplateKey: ok
###ComponentConstructor### M12→M13 step 8 rewriteTextJson: ok
###ComponentConstructor### M12→M13 step 9 rewriteImageJson: ok
```

**Ожидаемый результат:** все 9 шагов прошли, БД на v13, данные сохранены.

**⚠ Известные проблемы:**
- Логи **без `rowsAffected` counters** — формат `: ok`, без чисел. Backlog: `[IS481 phase 2: логи миграции M12→M13 — заглушки «ok» вместо реальных counters]`. Brief требует `SELECT changes()` после каждого UPDATE/DELETE — отложено.
- На свежеустановленной сборке без старой БД миграция не запустится — `⏭ skipped`.

---

### Сценарий 7.2. Cascade rename quiz_configs logs

**Что проверяем:** при rename user-defined типа, чьё имя фигурирует в `quiz_configs.component_refs`, появляется лог `cascade rename: configId=...` на каждый затронутый config.

**Откуда требование:** phase 2 task §5; spec § Cascade rename log.

**Предусловие:**
- user-defined компонент `CR` существует.
- В `quiz_configs.component_refs` минимум одного словаря ссылка на `CR` присутствует (можно организовать: после создания `CR` включить в quiz picker и поработать с lexeme'ой; либо подменить refs вручную через SQL).

**Шаги:**
1. EditDialog для `CR`.
2. **«Название»** — `CR2`. **«Сохранить»**.

**На экране:** диалог закрывается, snackbar **«Updated»** *(локализация не завершена)*, в списке `CR2`.

**Логи:**
- На каждый config с упоминанием `CR`:
  ```
  ###ComponentConstructor### cascade rename: configId=<id> refs=<N>→<N> write=true oldName=CR newName=CR2
  ```
- На каждый config БЕЗ упоминания:
  ```
  ###ComponentConstructor### cascade rename: configId=<id> refs=<N>→<N> write=false oldName=CR newName=CR2
  ```

**Ожидаемый результат:** все references на `CR` в JSON `component_refs` заменены на `CR2`.

---

### Сценарий 7.3. Cascade soft-delete + prefs reset logs

**Что проверяем:** при soft-delete user-defined компонента, чьё имя в `quiz_configs.component_refs` и в `quiz_picker_dict_<id>` prefs, появляются логи cascade soft-delete + полный prefs reset bundle (start / per-pref ok|fail / done).

**Откуда требование:** phase 2 task §5; spec § Cascade soft-delete + prefs reset.

**Предусловие:**
- user-defined `CD` существует.
- Есть ссылки в `quiz_configs.component_refs` (минимум 1).
- `CD` имеет хотя бы один active `component_value` (тогда `previewDeletionImpact.affectedPrefs` будет не пуст).

**Шаги:**
1. Manager (или PerDict) → trash-icon на row `CD`.
2. В DeleteConfirmDialog дождаться impact preview.
3. Тапнуть «Confirm».

**На экране:**
- Snackbar **«N values hidden»** *(локализация не завершена — hardcoded в Reducer.kt:388; N = `impact.valueCount`)*.
- Диалог закрывается, `CD` пропадает из списка.

**Логи (в таком порядке):**
- Внутри транзакции — cascade soft-delete:
  ```
  ###ComponentConstructor### cascade soft-delete: configId=<id> refs=<N>→<M> write=true removedName=CD       (на каждый затронутый)
  ###ComponentConstructor### cascade soft-delete: configId=<id> refs=<N>→<N> write=false removedName=CD      (на каждый незатронутый)
  ```
- После транзакции — prefs reset:
  ```
  ###ComponentConstructor### resetQuizPickerPrefs start: count=<N>
  ###ComponentConstructor### resetQuizPickerPrefs ok: dictId=<id>           (per-pref, если успех)
  ###ComponentConstructor### resetQuizPickerPrefs fail: dictId=<id> cause=<msg>  (если failure)
  ###ComponentConstructor### resetQuizPickerPrefs done: ok=<successCount>/<total>
  ```

**Ожидаемый результат:**
- `component_types.removed_at` свежий timestamp.
- Refs очищены из JSON `quiz_configs.component_refs`.
- `quiz_picker_dict_<id>` prefs сброшены для словарей из `affectedPrefs`.

---

### Сценарий 7.4. adb logcat фильтр чистый

**Что проверяем:** `grep '###ComponentConstructor###'` отсекает шум от других тегов (MATE, ComponentsManager module-tag, Crashlytics, и т.д.) и показывает только события фичи.

**Откуда требование:** phase 2 task §5; spec § Feature-tag.

**Шаги:**
1. Запустить `adb logcat -c && adb logcat | grep '###ComponentConstructor###'`.
2. Поработать в приложении:
   - Открыть-закрыть Manager.
   - Открыть-закрыть DictionaryTab.
   - Потыкать quiz.
3. Сделать одну фича-операцию: soft-delete user-defined компонента с непустым impact (см. 7.3 — там cascade + prefs reset = много логов).

**На экране:** обычная работа приложения.

**Логи:**
- При soft-delete — cascade soft-delete + prefs reset bundle (см. 7.3).
- При rename — cascade rename (см. 7.2).
- При остальной работе (открытие диалогов / навигация / quiz / dict tab) — **ничего**. Фильтр чистый.

**Ожидаемый результат:** фильтр `###ComponentConstructor###` показывает только события фичи; шумовых строк от других модулей нет.

---

## Чек-лист итога

Легенда: `☐` not started · `✅` passed · `❌` failed · `⏭` skipped (нет инструмента / N/A) · `🚨` blocker (не прогонять до фикса).

| Группа | Сценарий | Статус | Дата прогона | Замечания |
|---|---|:-:|---|---|
| 1 (P1 Manager CRUD) | 1.1 Create scope=Global | ☐ | | feature-tag entry/exit логи success — нет (Backlog) |
| 1 | 1.2 Create — попытка коллизии с built-in (`Перевод`) | ❌ | 2026-06-24 | collision check НЕ блокирует built-in имена; user-defined `Перевод` создаётся рядом с built-in — записать в Backlog (см. сценарий 1.2) |
| 1 | 1.3 Create — пустое name | ☐ | | UI-level guard, без UseCase call |
| 1 | 1.4 Rename — happy path | ☐ | | в phase 2 edit-icon открывает EditDialog (rename через него) |
| 1 | 1.5 Rename — collision | ☐ | | snackbar «Name already taken in this scope» fallback (локализация не завершена) |
| 1 | 1.6 Rename — built-in protected | ⏭ | | N/A в UI (built-in отсутствуют в списке) |
| 1 | 1.7 Soft-delete с preview impact | ☐ | | проверить snackbar «N values hidden» (локализация не завершена) |
| 1 | 1.8 Delete — built-in protected | ⏭ | | N/A в UI |
| 1 | 1.9 Cascade в quiz picker | ☐ | | depends on 1.7 |
| 2 (P1 PerDict) | 2.1 Открыть через «молоток» на 3 табах | ☐ | | icon visible iff currentDict != null |
| 2 | 2.2 Create в PerDict (scope hardcoded) | ☐ | | scope_slot скрыт |
| 2 | 2.3 Фильтр — не видны компоненты других словарей | ☐ | | |
| 2 | 2.4 Rename / Delete с PerDict экрана | ☐ | | parity с Manager |
| 3 (P2 Edit + downgrade) | 3.1 Edit name happy path | ☐ | | snackbar «Updated» (локализация не завершена) + cascade rename лог |
| 3 | 3.2 Edit isMulti false→true (upgrade) | ☐ | | без preview |
| 3 | 3.3 Edit isMulti true→false (legitimate) | ☐ | | data bug #2 fixed |
| 3 | 3.4 Edit isMulti true→false BLOCKED | ☐ | | `Лексема №N` placeholder; «Показать все» no-op (Backlog) |
| 3 | 3.5 Edit template — immutable | ☐ | | snackbar «Template cannot be changed» (локализация не завершена); UseCase gate отсутствует (Backlog) |
| 3 | 3.6 Edit failure (общий) | ⏭ | | требует debug-injection |
| 4 (P2 Multi-dict scope) | 4.1 Scope=Global (regression) | 🚨 | | BLOCKER (handler не зарегистрирован) |
| 4 | 4.2 Scope=PerDictionaries multi-select | 🚨 | | BLOCKER |
| 4 | 4.3 0 selected → submit disabled | 🚨 | | BLOCKER |
| 4 | 4.4 Chip staleness | 🚨 | | BLOCKER |
| 5 (P2 Shared widget regression) | 5.1 Visual parity Manager ↔ PerDict | ☐ | | смотрим без regressions; CreateDialog scope_slot — разный per HostVariant |
| 6 (P2 Removed semantics) | 6.1 Rename уже soft-deleted | ☐ / ⏭ | | RenameDialog как entry в UI может отсутствовать → N/A, проверять через 6.2 |
| 6 | 6.2 Edit уже soft-deleted | ☐ | | требует параллельной soft-delete tooling; backlog «порядок защит в editComponentType» |
| 6 | 6.3 Soft-delete уже soft-deleted | ☐ | | требует параллельной soft-delete tooling |
| 7 (P2 Feature-tag + migration) | 7.1 Migration M12→M13 (9 строк) | ☐ / ⏭ | | требует старой БД (pre-M13); логи без rowsAffected (Backlog) |
| 7 | 7.2 Cascade rename | ☐ | | |
| 7 | 7.3 Cascade soft-delete + prefs reset | ☐ | | |
| 7 | 7.4 Feature-tag filter clean | ☐ | | smoke verify |

---

_model: claude-opus-4-7[1m]_
