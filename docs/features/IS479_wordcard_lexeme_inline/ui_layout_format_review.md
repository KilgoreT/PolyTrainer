# Оценка формата `ui/ui_layout_example.md`

**Контекст:** IS479 wordcard-lexeme-inline. Сверка против Figma frame `9154:82509` (через `figma_dump.json`) и текущего кода `modules/screen/wordcard/`.

**Критерии оценки (по запросу user'а):**
1. Максимум по Figma (исключая зафиксированные отклонения — chip pill в обоих state'ах).
2. Минимум модификаций кода.
3. Переиспользование существующих виджетов.
4. Пригодность формата для рефакторинга и построения UI на Compose.

---

## Что формат делает хорошо

1. **Двухуровневое разделение** — "Карта экрана" (для человека) + "Анализ виджетов" (для агента). Правильное разделение: одно для review, второе для имплементации.
2. **Визуальный язык маркеров** ⚙️/❇️/🔄/📌 — компактно отражает delta. Читается с первого взгляда.
3. **Per-widget bullet-структура** (type/size/padding/colors/typography/slots/params/callbacks/behavior/notes/source) — покрывает все поля нужные для Compose.
4. **`source: figma <id>`** — навигация обратно в Figma.
5. **Notes с 📌/⚠** — место для отклонений и предупреждений.

---

## Где формат провисает (по критериям user'а)

### 1. ❌ Нет mapping на текущий код — главный пробел для рефакторинга

В файле есть `source: figma <id>`, но **нет `current_code: file.kt`**. По документу невозможно понять "что переиспользовать". Примеры:

- `WordFieldWidget` помечен 📌 ("без изменений") — но в коде `WordFieldWidget.kt` использует `LexemeEditableText` (упомянуто в notes как ⚠), что не view-only, а full edit flow. Это уже не 📌 — это **переиспользуем как есть, но осознавая edit-логику**.
- `LexemeTitleWidget` в файле описан как "убираем dropdown → chip" — но в коде он живой, рендерит dropdown с пунктами AddTranslation/AddDefinition/Delete. Реальный план — либо удалить, либо переподчинить под FlowRow. Документ это не говорит.

### 2. ❌ Расхождения Figma ↔ файл не выделены в отдельную секцию

Сейчас они "размазаны" по notes. Подтверждённые расхождения (из `figma_dump.json`):

- **`LazyColumn spacing=12`** в файле → реально `layout_UYS723: gap=8` во Figma. Это отход от Figma, **не зафиксировано** как проектное решение.
- **`alignItems: flex-end`** у Lexeme контейнера (Figma `layout_RVILFH`) → не упомянут в файле. Влияет на DeleteButton (alignSelf=Stretch) и Пример chip (одиночный, поедет вправо).
- **TopBar в Figma — `Variant=with btn`** (component с кнопкой "Сохранить") → в коде/файле `⋮ DropdownMenu`. Это **сильный отход от Figma**, замаскированный под 📌 "без изменений".
- **"Пример" placeholder в FlowRow** (файл) → реально sibling уровнем выше внутри Lexeme column, не внутри FlowRow (Figma). Структурное расхождение.
- **"Перевод" placeholder chip** в FlowRow в файле — отсутствует в этом frame Figma (он уже active в snapshot). Нормально для UX, но карта экрана это игнорирует.
- **"Сохранить" Frame `9158:72057`** (sibling chip'а Перевод, fill=`#B0B2B6` disabled) — в файле "роль перенесена в footer LexemeItem", но цвет/state #B0B2B6 не упомянуты.

### 3. ⚠️ `📌 без изменений` врёт — нет проверки против кода

Маркер ставится по намерению, а не по факту. Это опасно: создаёт ложное чувство "тут трогать не надо", а реально там расхождение с Figma уже есть (TopBar).

### 4. ⚠️ Не отделено "обязательно по Figma" от "проектное решение"

`SubentityChip` — единственное место где это сделано хорошо (есть Figma-spec + явное проектное решение объединить в один виджет с pill-shape в обоих state'ах). Для остальных — нет.

Например, `LexemeValueFieldWidget`: в файле "M3 OutlinedTextField с label-внутри 56dp" (как в Figma), но в коде сейчас `BasicTextField + confirm/cancel icons` (project pattern). Это **большое расхождение**, формат его не подсвечивает — непонятно, надо ли подгонять под Figma или оставить project pattern.

### 5. ⚠️ Дубликация с `ui_layout.md` (agent-doc)

В файле в конце ссылка "Полный список + параметры → `ui_layout.md`". Два дока с одинаковыми полями → теряется ответственность. Либо один cheat-sheet (компактный, для человека), либо один agent-doc (детальный, для имплементации). Или явное разделение: agent читает один, человек — другой.

### 6. ⚠️ Нет migration map для удалённых виджетов

Раздел `## ❌ УДАЛЯЕМ` называет 3 удалённых виджета, но **куда переехала их логика — не сказано**. Для рефакторинга это критично:

- `AddLexemeBottomWidget.onAdd` → `SubentityChip.onActivate` + `Msg.CreateTranslation` / `Msg.CreateDefinition`
- `LexemeMeaningWidget.onTypeSelect` → разделено на отдельные `Msg.Create*`
- `ActionsWidget` (commit/cancel buttons) → удалено бесследно — commit/cancel переехали в LexemeValueField (trailing icons) или в footer

Без migration map при review нельзя проверить "ничего не потеряно".

### 7. ⚠️ `source: проектное решение` без ссылки на decision log

`SubentityChip` 999dp pill в обоих state'ах помечено "pending design review" — но нет ссылки куда смотреть через 3 месяца. Нужен decision log (даже коротко в этом же файле в секции "Решения").

---

## Вердикт по критериям user'а

| Критерий | Оценка | Почему |
|---|---|---|
| Максимум по Figma | 🟡 средне | Размеры/токены есть, но численные отклонения от Figma (gap=8 vs 12, alignItems=flex-end, TopBar component variant) скрыты, не помечены как осознанный отход |
| Минимум модификаций кода | 🔴 слабо | Нет mapping `widget → current_code:file.kt` — по документу нельзя оценить объём изменений |
| Переиспользование виджетов | 🔴 слабо | Нет `reuse_status: reuse_as_is / modify / rewrite / new`. 📌 ставится по намерению, не по реальному состоянию кода |
| Читаемость для имплементации | 🟢 хорошо | Bullet-структура покрывает Compose-API. Слоты и params — ок |

---

## Что конкретно добавить в формат

### 1. `mapping_to_code` в каждый widget bullet

```
• current_code: widget/lexeme/LexemeValueFieldWidget.kt
• reuse_status: rewrite   (was BasicTextField+icons → target OutlinedTextField 56dp)
• migration: trailing icons → confirm gesture / out-of-field tap
```

Значения `reuse_status`:
- `reuse_as_is` — используем как есть.
- `modify_in_place` — точечная подкрутка (типографика, padding, цвет).
- `rewrite` — структурная переделка (другой M3-узел или другая API).
- `new` — нет в коде, создаём.

### 2. Отдельная секция `## 🔀 ОТКЛОНЕНИЯ ОТ FIGMA`

Явный список с обоснованием:

```
- LazyColumn spacing=12 (file) vs 8 (Figma)
  Решение: <причина>
- SubentityChip pill в обоих state'ах vs Figma (active=6dp, placeholder=999dp)
  Решение: UX consistency, см. § Решения
- TopBar ⋮ menu vs Figma "Сохранить" btn
  Решение: <причина>
```

Каждое отклонение — с явным обоснованием. Если обоснования нет — либо подгоняем под Figma, либо отклонение неправомерно.

### 3. `## 🔄 МИГРАЦИЯ КОДА`

Куда переехала логика удалённых виджетов:

```
- AddLexemeBottomWidget   → удалено; onAdd мигрирует в SubentityChip.onActivate + Msg.Create{Translation|Definition}
- LexemeMeaningWidget     → удалено; onTypeSelect мигрирует в отдельные Msg.Create{Translation|Definition}
- ActionsWidget           → удалено; commit/cancel buttons → переехали в LexemeValueField (trailing icons) и/или footer LexemeItem
```

### 4. `## ✅ VERIFY` checklist

Короткий список проверок после имплементации:

```
- [ ] LexemeItem alignItems=flex-end (или замена через alignSelf точечно)
- [ ] Cards gap=8 (а не 12)
- [ ] Lexeme placeholder pill br=999, padding=8/12
- [ ] FAB icon-only в правом нижнем углу
- [ ] LexemeValueField OutlinedTextField 56dp (если решили подгонять под Figma)
- [ ] LexemeTitleWidget удалён или переподчинён
```

### 5. Удалить дублирование с `ui_layout.md`

Выбрать одну роль. Если оба нужны — явно сказать, кто читает что:
- `ui_layout_example.md` → cheat-sheet для человека / review.
- `ui_layout.md` → agent-doc для имплементации.

И при изменениях обновлять оба синхронно (или один derive из другого).

### 6. Проверять `📌 без изменений` против кода

До постановки маркера сверять с реальным кодом. Если реальность расходится с Figma (как TopBar `⋮ menu` vs Figma `Сохранить btn`) — это 🔄, не 📌.

---

## Возражение по сути формата

**Возражение:** Текущий формат описывает **целевое состояние UI**, а user ожидает **рефакторинг-план**. Это разные жанры.

**Почему:** Целевое состояние = "вот как должно выглядеть". План рефакторинга = "вот что переиспользуем, вот что переписываем, вот что мигрирует, вот что выкидываем". Сейчас файл — target, а ожидается delta.

**Альтернатива:** Либо дополнить файл секциями `mapping_to_code` + `migration` + `verify` + `отклонения_от_Figma` (выше), либо завести отдельный `refactor_plan.md` рядом с `ui_layout.md` / `ui_layout_example.md` — где явно прописать reuse/modify/rewrite/new по каждому виджету.

**Рекомендуемый выбор:** дополнить, а не плодить третий документ. Четыре новые секции в этот же файл (отклонения / миграция / verify / mapping_to_code) + поля `current_code` + `reuse_status` в bullet-list — закрывают пробел без второго документа.

---

## Источники

- **Figma frame target:** `9154:82509` (word card)
- **Figma dump:** `figma_dump.json` (~19 МБ, full file)
- **Текущий код:** `modules/screen/wordcard/src/main/java/me/apomazkin/wordcard/`
  - `widget/`, `widget/lexeme/`, `widget/lexeme/menu/`
  - `mate/State.kt`, `mate/Message.kt`, `mate/WordCardReducer.kt`
  - `WordCardScreen.kt`, `WordCardViewModel.kt`
- **Оцениваемый файл:** `ui/ui_layout_example.md`
