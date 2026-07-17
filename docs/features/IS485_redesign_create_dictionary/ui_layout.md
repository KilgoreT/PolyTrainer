# IS485 — Редизайн экрана «Создание словаря» · UI cheat-sheet

Источники: Figma-фрейм `5027:1108` (через `figma_reference.jsx` + `figma_create_dictionary.png`), `02_scope.md`, `ui_walkthrough.md` (факты кода с file:line), `00_task.md` (токены и закрытые решения), текущий код `modules/screen/dictionary/form/**`. Артефакт = финальное состояние UI после реализации (snapshot).

## 📋 ЧТО ДЕЛАЕМ

- Фон экрана и system bars: `whiteColor` → тёплый `#FCFCFA` (новый токен).
- Заголовочная зона по схеме пользователя: онбординг — крупный in-content заголовок «Новый словарь» + подзаголовок; create/edit — заголовок в локальном AppBar (новый параметр `titleResId`), в контенте только подзаголовок.
- Поле имени: вызов `LexemeTextFieldWidget` → новый локальный `<𝗡𝗮𝗺𝗲𝗙𝗶𝗲𝗹𝗱>` (плавающий uppercase-лейбл, underline 2dp, акцентный курсор).
- Превью выбранного флага: 48dp без выделения → 58dp с двойным кольцом (новый локальный `<𝗙𝗹𝗮𝗴𝗣𝗿𝗲𝘃𝗶𝗲𝘄>`; плейсхолдер-буква сохраняется).
- Поиск: сырой M3 `OutlinedTextField` → капсула `<𝗦𝗲𝗮𝗿𝗰𝗵𝗣𝗶𝗹𝗹>` (#F1F0EC, radius 14, h=45).
- Сетка флагов: 5→4 колонки, флаг 48→56dp, маркер выбора = кольцо 2.5dp + бейдж-галочка 24dp + bold-подпись primary; подписи без ellipsis (перенос до 2 строк); empty-state «Ничего не найдено».
- Кнопка: вызов `PrimaryFullButtonWidget` → новый локальный `<𝗦𝘂𝗯𝗺𝗶𝘁𝗕𝘂𝘁𝘁𝗼𝗻>` (radius 16, цветная тень, отступы 22, disabled без тени).
- Строки: +подзаголовок, +«Ничего не найдено», +edit-заголовок (обе локали); фикс EN-хинта поиска.
- Общие `core:ui`-виджеты НЕ меняются (project_decision №4) — всё новое живёт в `form/widget/`.

## 🏷 ЛЕГЕНДА

- ⚙️ — системный Material3 / Compose
- ❇️ — новый кастомный виджет (в этой фиче впервые)
- 🔄 — кастомный, меняется в этой фиче
- 📌 — кастомный, не меняется в этой фиче
- 🚨 — отход от Figma / предупреждение · ℹ️ — пояснение

## 🗺 Карта экрана

```
⚙️ Scaffold                                    containerColor=formBackground(#FCFCFA)
├─ 🔄 <𝗗𝗶𝗰𝘁𝗶𝗼𝗻𝗮𝗿𝘆𝗔𝗽𝗽𝗕𝗮𝗿>                     только create/edit (showAppBar=true); title=режимный
└─ ⚙️ Column                                   padding=h:24  (контент)
   ├─ ❇️ <𝗙𝗼𝗿𝗺𝗛𝗲𝗮𝗱𝗲𝗿>                         title только при showAppBar=false; subtitle всегда
   │
   ├─ ⚙️ Row                                   spacing=14  vAlign=center
   │  ├─ ❇️ <𝗙𝗹𝗮𝗴𝗣𝗿𝗲𝘃𝗶𝗲𝘄>                     58×58
   │  └─ ❇️ <𝗡𝗮𝗺𝗲𝗙𝗶𝗲𝗹𝗱>                       weight=1
   │
   ├─ ❇️ <𝗦𝗲𝗮𝗿𝗰𝗵𝗣𝗶𝗹𝗹>                         width=fill  height=45
   │
   ├─ ⚙️ LazyVerticalGrid (× N флагов)         columns=4  weight=1  spacing=8
   │  │  ∀ flag:
   │  └─ 🔄 <𝗙𝗹𝗮𝗴𝗚𝗿𝗶𝗱𝗜𝘁𝗲𝗺>                    56dp круг + подпись
   │  ∀ flags.isEmpty() && flagFilter.isNotBlank():
   │  └─ ⚙️ Text «Ничего не найдено»           по центру зоны сетки
   │
   └─ ❇️ <𝗦𝘂𝗯𝗺𝗶𝘁𝗕𝘂𝘁𝘁𝗼𝗻>                       padding=h:22 bottom:16  height=56
```

ℹ️ Три режима — один экран: онбординг (`showAppBar=false`, есть in-content title), create (AppBar title «Новый словарь»), edit (AppBar title «Редактирование словаря», префилл, кнопка «Сохранить»).

## 🔍 Анализ виджетов

🔄 <𝗗𝗶𝗰𝘁𝗶𝗼𝗻𝗮𝗿𝘆𝗔𝗽𝗽𝗕𝗮𝗿> (локальный, `modules/screen/dictionary/.../widget/DictionaryAppBar.kt`)

```
   • structure:
       row  height=appbar  vAlign=center
         leading_slot: icon  iconRes=ic_back  size=44  (IconBoxed, как сейчас)
         title_slot:   text  source=titleResId
   • type:        TopAppBar (как сейчас, DictionaryAppBar.kt:16-34)
   • params:
       – onBackPress: () -> Unit
       – titleResId: Int = R.string.dictionary_selection_title   (НОВЫЙ параметр; default = текущее поведение)
   • typography:
       – title: LexemeStyle.H5 (как сейчас, DictionaryAppBar.kt:29-34)
   • colors:
       – background: formBackground (#FCFCFA) — вместо текущего белого
   • callbacks:   onBackPress → Msg.Back (как сейчас)
   • behavior:    рендерится только при showAppBar=true (create/edit). Форма передаёт:
                  create → dictionary_new; edit → dictionary_edit_title (новая строка).
   • notes:
       🚨 Figma: AppBar в фрейме 5027:1108 отсутствует → реализация: AppBar в create/edit ОСТАЁТСЯ.
          Причина: project_decision №1 (схема заголовка, решение пользователя 2026-07-09).
       ⚠ у виджета ВТОРОЙ потребитель — DictionaryListScreen.kt:64 («Все словари»): параметр
          titleResId обязан иметь default = dictionary_selection_title, чтобы список не изменился.
   • source:      проектное решение — project_decision №1
```

❇️ <𝗙𝗼𝗿𝗺𝗛𝗲𝗮𝗱𝗲𝗿>

```
   • structure:
       column  spacing=4
         title_slot:    text  source=dictionary_new  visible=when !showAppBar
         subtitle_slot: text  source=dictionary_form_subtitle (новая строка)
   • type:        Column + Text
   • size:        width=fill × height=hug
   • padding:     top=12
   • typography:
       – title: LexemeStyle.H4 (28sp SemiBold)
       – subtitle: LexemeStyle.BodyM
   • colors:
       – title: LexemeColor.secondary (#19191B)
       – subtitle: formTextSecondary (#8A8A90, новый токен)
   • params:
       – showTitle: Boolean   (= !showAppBar, прокидывается с экрана)
   • behavior:    title только в онбординге; subtitle — во всех трёх режимах.
   • notes:
       🚨 Figma: title 26sp Bold tracking −0.26 → реализация LexemeStyle.H4 (28sp SemiBold).
          Причина: project_decision №2 (шрифт/стили — текущая тема, ближайший токен).
       🚨 Figma: subtitle 13.5sp #8A8A90 → BodyM (14sp). Причина: project_decision №2.
   • source:      figma 5027:1108 (nodes 5027:1118, 5027:1119)
```

❇️ <𝗙𝗹𝗮𝗴𝗣𝗿𝗲𝘃𝗶𝗲𝘄>

```
   • structure:
       box  size=58  shape=circle
         flag_slot: image-or-text (mode-dependent)
           # selectedFlag != null → image (ImageFlagWidget, size=50 внутри колец)
           # selectedFlag == null → text-in-circle (FlagPlaceholderWidget: первая буква имени
           #                        live; пустое имя → пустой круг) — как сейчас
   • type:        Box + существующие ImageFlagWidget / FlagPlaceholderWidget (не меняются)
   • size:        58 × 58 (Figma node 5027:1120)
   • shape:       circle; двойное кольцо: 2dp LexemeColor.primary (внешний край), зазор 2dp
                  formBackground, контент 50 (Figma shadow 0-2px фон + 2-4px primary → толщина 2)
   • colors:
       – ring: LexemeColor.primary
       – gap: formBackground (#FCFCFA)
   • params:
       – selectedFlag: CountryFlagItem?
       – name: String
   • behavior:    кольца видны всегда (и у плейсхолдера) — зона выбора визуально акцентна.
   • notes:
       🚨 Figma primary #4749B8 → LexemeColor.primary #4A49BC. Причина: project_decision №3.
       🚨 Плейсхолдер-буква (в Figma нет) сохраняется. Причина: project_decision №6.
       ℹ️ Общие виджеты ImageFlagWidget/FlagPlaceholderWidget НЕ меняются (decision №4) —
          размер задаётся внешним Modifier (паттерн уже используется, DictionaryFormWidget.kt:50-58).
   • source:      figma 5027:1120 + проектные решения №3/№6
```

❇️ <𝗡𝗮𝗺𝗲𝗙𝗶𝗲𝗹𝗱>

```
   • structure:
       column  spacing=6
         label_slot: text  uppercase  source=dictionary_name_hint
         input_slot: input  single-line  (BasicTextField)
         underline:  divider  height=2  color=primary
   • type:        кастомный BasicTextField (замена вызова LexemeTextFieldWidget — см. ❌ УДАЛЯЕМ)
   • size:        width=fill(weight 1) × height=hug
   • typography:
       – label: LexemeStyle.BodySBold, letterSpacing=0.3sp, uppercase
       – value: LexemeStyle.H6 (20sp Medium)
   • colors:
       – label: LexemeColor.primary
       – value: LexemeColor.secondary (#19191B)
       – underline: LexemeColor.primary
       – cursor: LexemeColor.primary
   • params:
       – value: String
       – onValueChange: (String) -> Unit
       – labelRes: Int
   • callbacks:   onValueChange → Msg.NameChanged (как сейчас, DictionaryFormWidget.kt:64)
   • behavior:    single-line; никакой валидации в виджете — активация кнопки живёт в стейте
                  (saveButtonEnabled), как сейчас.
   • notes:
       🚨 Figma: value 22sp regular → LexemeStyle.H6 (20sp Medium). Причина: project_decision №2.
       🚨 Figma: label 11.5sp Bold track +0.345 → BodySBold + letterSpacing. Причина: decision №2.
       🚨 принимает onValueChange из удалённого вызова LexemeTextFieldWidget (миграция IS485).
   • source:      figma 5027:1131-1134
```

❇️ <𝗦𝗲𝗮𝗿𝗰𝗵𝗣𝗶𝗹𝗹>

```
   • structure:
       row  height=45  vAlign=center  padding=h:16  spacing=12
         leading_slot:  icon  Icons.Default.Search  size=19
         input_slot:    input  single-line  hint=dictionary_filter_flags_hint
         trailing_slot: icon  Icons.Default.Close  visible=when flagFilter.isNotEmpty()
   • type:        Surface + BasicTextField (замена сырого M3 OutlinedTextField — см. ❌ УДАЛЯЕМ)
   • size:        width=fill × height=45
   • shape:       borderRadius=14
   • colors:
       – background: searchPillColor (#F1F0EC, новый токен)
       – icon: formTextHint (#9A9AA2)
       – hint: formTextHint (#9A9AA2)
       – value: LexemeColor.secondary
   • typography:
       – hint/value: LexemeStyle.BodyM
   • params:
       – value: String
       – onValueChange: (String) -> Unit
   • callbacks:
       – onValueChange → Msg.FlagFilterChanged (как сейчас, DictionaryFormWidget.kt:74)
       – trailing clear → Msg.FlagFilterChanged("") (как сейчас, :93)
   • notes:
       🚨 Figma hint 15sp → BodyM (14sp). Причина: project_decision №2.
       🚨 принимает фильтрацию и clear из удалённого OutlinedTextField (миграция IS485).
       ℹ️ иконки поиска в ресурсах нет (ui_walkthrough §5) — остаёмся на Icons.Default.Search.
   • source:      figma 5027:1135-1139
```

🔄 <𝗙𝗹𝗮𝗴𝗚𝗿𝗶𝗱𝗜𝘁𝗲𝗺> (внутри LazyVerticalGrid, `FlagGridWidget.kt`)

```
   • structure:
       column  spacing=8  hAlign=center
         flag_slot: box  size=56  shape=circle
           image_slot: image (ImageFlagWidget)
           ring:       border 2.5dp primary + внутренний зазор  visible=when isSelected
           badge_slot: icon-in-circle  size=24  align=BottomEnd  visible=when isSelected
         label_slot: text  maxLines=2  textAlign=center
   • type:        Column + Surface(circle) + Box-бейдж (рестайл текущего item, FlagGridWidget.kt:42-71)
   • size:        флаг 56 × 56 (было 48); колонок 4 (было 5, :37)
   • spacing:     grid spacedBy=8×8 (как сейчас)
   • colors:
       – ring: LexemeColor.primary
       – badge background: LexemeColor.primary, border 2dp formBackground
       – badge icon: whiteColor (ic_confirm, тонируемая)
       – label: formTextTertiary (#55555C); selected → LexemeColor.primary
   • typography:
       – label: LexemeStyle.BodyS; selected → LexemeStyle.BodySBold
   • params:      (как сейчас) flags, selectedFlag, onFlagClick
   • callbacks:   onFlagClick → Msg.SelectFlag (toggle — повторный тап снимает, реducer как есть)
   • behavior:    подпись переносится на 2 строки (убрать maxLines=1 + Ellipsis, :66-67);
                  empty-state: при flags.isEmpty() && flagFilter.isNotBlank() вместо сетки —
                  текст dictionary_flags_not_found по центру (formTextSecondary, BodyM).
   • notes:
       🚨 Figma badge-галочка (node 5027:1153) — иконка из ресурсов ic_confirm (галочка 24dp,
          тонируемая) вместо экспорта SVG из Figma. Причина: decision №4 + walkthrough §5
          (галочки уже есть, импорт не нужен).
       🚨 Figma «Велико-британия» с soft-hyphen → перенос стандартным wrap без ручных дефисов.
          Причина: локализованные названия стран приходят из справочника (данные, не разметка).
       🚨 empty-state в Figma не нарисован → текст по центру. Причина: project_decision №7.
   • source:      figma 5027:1140-1234 (grid), 5027:1142-1154 (selected + badge)
```

❇️ <𝗦𝘂𝗯𝗺𝗶𝘁𝗕𝘂𝘁𝘁𝗼𝗻>

```
   • structure:
       button  height=56  width=fill
         label_slot: text  source=dictionary_create | dictionary_save (по editingDictionaryId)
   • type:        M3 Button (замена вызова PrimaryFullButtonWidget — см. ❌ УДАЛЯЕМ)
   • size:        width=fill × height=56
   • padding:     контейнер h:22, bottom=16
   • shape:       borderRadius=16
   • colors:
       – enabled background: LexemeColor.primary; content: whiteColor
       – enabled shadow: primaryButtonShadow rgba(74,73,188,0.5), offsetY=10 blur=22 spread=−6
       – disabled background/content: colorScheme.secondary / onSecondary (как LexemeButton
         disabled, LexemeButton.kt:41-44); БЕЗ тени
   • typography:
       – label: LexemeStyle.BodyLBold
   • params:
       – titleRes: Int, enabled: Boolean, onClick: () -> Unit
   • callbacks:   onClick → Msg.Save (как сейчас, DictionaryFormWidget.kt:124)
   • behavior:    enabled = saveButtonEnabled (только от непустого имени, decision из брифа);
                  текст по editingDictionaryId — механика как сейчас (:115-119).
   • notes:
       🚨 Figma тень rgba(71,73,184,0.5) → rgba(74,73,188,0.5) от LexemeColor.primary.
          Причина: project_decision №3 (primary не меняем — тень от него же).
       🚨 Figma label 16.5sp Bold → BodyLBold. Причина: project_decision №2.
       🚨 disabled-вида в Figma нет → приглушённый контейнер темы без тени. Причина: decision №5.
       🚨 принимает onClick/enabled из удалённого вызова PrimaryFullButtonWidget (миграция IS485).
   • source:      figma 5027:1235-1237 + проектные решения №2/№3/№5
```

📌 <𝗦𝘆𝘀𝘁𝗲𝗺𝗕𝗮𝗿𝘀𝗪𝗶𝗱𝗴𝗲𝘁> (core:ui)

```
   • type:        существующий, принимает любой цвет (SystemBarsWidget.kt:10-15)
   • behavior:    вызов на экране меняется: color=whiteColor → formBackground (#FCFCFA)
   • notes:       ℹ️ сам виджет не меняется — меняется аргумент вызова в DictionaryFormScreen.kt:51-53.
                  Нет расхождений с Figma (фон совпадает с задуманным).
   • source:      код DictionaryFormScreen.kt:51-53
```

## ❇️ НОВЫЕ ВИДЖЕТЫ

- `<𝗙𝗼𝗿𝗺𝗛𝗲𝗮𝗱𝗲𝗿>` — заголовочная зона (title режимный + subtitle) — `form/widget/`
- `<𝗙𝗹𝗮𝗴𝗣𝗿𝗲𝘃𝗶𝗲𝘄>` — превью выбранного флага 58dp с двойным кольцом — `form/widget/`
- `<𝗡𝗮𝗺𝗲𝗙𝗶𝗲𝗹𝗱>` — material-поле имени (лейбл/underline/курсор) — `form/widget/`
- `<𝗦𝗲𝗮𝗿𝗰𝗵𝗣𝗶𝗹𝗹>` — капсула поиска — `form/widget/`
- `<𝗦𝘂𝗯𝗺𝗶𝘁𝗕𝘂𝘁𝘁𝗼𝗻>` — кнопка с тенью — `form/widget/`

## 🔧 МЕНЯЕМ (ключевое)

- `DictionaryAppBar` (локальный) — +параметр `titleResId` с default (второй потребитель DictionaryListScreen не меняется).
- `DictionaryFormScreen.kt` — фон/system bars → formBackground; передача titleResId по режиму; прокидывание showTitle в FormHeader.
- `DictionaryFormWidget.kt` — новая композиция (FormHeader, Row превью+имя, SearchPill, сетка, SubmitButton); горизонтальный паддинг 16→24.
- `FlagGridWidget.kt` — 4 колонки, item 56dp, кольцо+бейдж+bold-подпись, перенос подписей, empty-state.
- `theme/Color.kt` — +5 val: formBackground, formTextSecondary, formTextTertiary, formTextHint, searchPillColor (+ alpha-производная primaryButtonShadow — можно локально в виджете кнопки).
- Строки (обе локали): +`dictionary_form_subtitle`, +`dictionary_flags_not_found`, +`dictionary_edit_title`; фикс EN `dictionary_filter_flags_hint` («Filter flags…» → «Search by country or language…»).

## ❌ УДАЛЯЕМ (с миграцией)

- `LexemeTextFieldWidget` (вызов в DictionaryFormWidget.kt:61-67)   → `<𝗡𝗮𝗺𝗲𝗙𝗶𝗲𝗹𝗱>`.onValueChange (Msg.NameChanged)
- `OutlinedTextField` фильтра (DictionaryFormWidget.kt:72-102)       → `<𝗦𝗲𝗮𝗿𝗰𝗵𝗣𝗶𝗹𝗹>`.onValueChange + clear (Msg.FlagFilterChanged / (""))
- `PrimaryFullButtonWidget` (вызов в DictionaryFormWidget.kt:121-125) → `<𝗦𝘂𝗯𝗺𝗶𝘁𝗕𝘂𝘁𝘁𝗼𝗻>`.onClick (Msg.Save, enabled=saveButtonEnabled)

ℹ️ Удаляются ВЫЗОВЫ из формы; сами общие виджеты остаются в core:ui нетронутыми (decision №4).

## 🖼 ИКОНКИ К ИМПОРТУ

- Не требуется: поиск/clear — M3 `Icons.Default.Search`/`Close` (как сейчас), галочка бейджа — существующий `ic_confirm` (тонируемый 24dp path).

## 🆕 НОВЫЕ UX-СЦЕНАРИИ

- Empty-state поиска флагов: непустой запрос без совпадений → «Ничего не найдено» по центру зоны сетки; на старте экрана (flags ещё не загружены, фильтр пуст) текст НЕ показывается (составное условие).

## 🎨 ПАЛИТРА

- formBackground `#FCFCFA` — фон экрана/system bars/зазор колец (новый val)
- formTextSecondary `#8A8A90` — подзаголовок, empty-state (новый val)
- formTextTertiary `#55555C` — подписи флагов (новый val)
- formTextHint `#9A9AA2` — hint и иконки поиска (новый val)
- searchPillColor `#F1F0EC` — подложка поиска (новый val)
- primaryButtonShadow `rgba(74,73,188,0.5)` — тень кнопки (производная от LexemeColor.primary)
- LexemeColor.primary `#4A49BC` — кольца, лейбл/underline/курсор, бейдж, подпись выбранного, кнопка (существующий; Figma #4749B8 маппится на него — decision №3)
- LexemeColor.secondary `#19191B` — заголовок, значения полей (существующий)
- whiteColor — контент кнопки, обводка бейджа-галочки (существующий)

---

## Чек-лист дисциплины (§7 формат-спеки)

1. **Маркеры 📌:** SystemBarsWidget — расхождений с Figma нет (фон совпадает) ✓
2. **Миграция:** все 3 строки в ❌ УДАЛЯЕМ имеют явную цель миграции ✓
3. **Зеркало:** NameField/SearchPill/SubmitButton несут 🚨-пометки о принятой логике ✓
4. **Шаблон буллетов:** порядок полей §4 соблюдён во всех блоках ✓
5. **Карта:** кастомные — Unicode bold без тегов, системные — обычным шрифтом ✓
6. **Структура:** у каждого ❇️/🔄 есть `structure:` через примитивы гайда (column/row/box/слоты) ✓

_model: claude-fable-5 (артефакт составлен conductor'ом без суб-агентов и без ревью — по указанию пользователя)_
