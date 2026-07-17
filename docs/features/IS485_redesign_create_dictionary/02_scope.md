# Scope analysis: IS485

## Резюме

Редизайн UI экрана «Создание словаря» (`modules/screen/dictionary`, пакет `form/`) под Figma-фрейм 5027:1108: новый заголовок экрана, Material-поле имени, поиск-пилюля, сетка флагов 4×56 с маркером выбора (кольцо + бейдж-галочка), кнопка с radius 16 и цветной тенью, тёплый фон. Mate-контракт (State/Msg/Reducer/Effect) не меняется — бриф содержит закрытый анализ «Mate vs чистый UI» с решением «фича 100% UI», и код это подтверждает: все данные для макета уже есть в `DictionaryFormScreenState` (`name`, `flagFilter`, `flags`, `selectedFlag`, `saveButtonEnabled`, `editingDictionaryId`), новых взаимодействий макет не добавляет. Затронут только UI-слой: composables экрана + строковые ресурсы обеих локалей.

## Замысел задачи

Пользователь хочет, чтобы первый содержательный экран приложения (онбординг нового юзера) перестал выглядеть «утилитарной формой» и соответствовал новому визуальному языку из Figma — это первый экран зонтичного редизайна #485. Поведение экрана во всех трёх режимах (онбординг / создание / редактирование) остаётся прежним: та же механика выбора флага, валидации имени и активации кнопки — меняется только внешний вид, плюс появляются заголовок/подзаголовок, состояние «ничего не найдено» в поиске и выравнивается рассинхрон EN-строки хинта поиска.

## Затронутые слои

- **Infrastructure** — нет — DI, mate, navigation, build, manifest не трогаются. `modules/core/theme` в базовом сценарии не меняется: палитра макета почти совпадает с текущим брендом (`LexemeColor.primary = #4A49BC` против макетного `#4749B8`; `secondary #19191B` против `textPrimary #1A1A1E`), решение брифа №1 фиксирует «шрифт — текущая тема», решение №3 — «не вводить новых цветов под плейсхолдер». Возможные точечные добавления цветовых `val` — см. Open questions №1.
- **Business logic** — нет — закрытое решение брифа (2026-07-08): Msg-набор покрывает все взаимодействия макета; «ничего не найдено» — производная в UI (`flags.isEmpty() && flagFilter.isNotBlank()`), явный флаг в стейт не заводится; заголовок и текст кнопки в edit-режиме — по уже существующему `editingDictionaryId` (прецедент `DictionaryFormWidget.kt:115`). Reducer/State/Msg/Effect/UseCase — без изменений.
- **UI** — да — вся фича: рестайл `DictionaryFormScreen.kt` (фон, system bars — сейчас `whiteColor`) и `DictionaryFormWidget.kt` (заголовок+подзаголовок, превью флага с кольцами, Material-поле имени, поиск-пилюля, кнопка), правки `FlagGridWidget.kt` (5→4 колонки, 48→56dp, кольцо+бейдж+bold-подпись выбранного, подписи без ellipsis, «ничего не найдено»), новые строки в обеих локалях + выравнивание EN-хинта.
  **Схема заголовка (РЕШЕНИЕ ПОЛЬЗОВАТЕЛЯ 2026-07-09, закрывает F003/F004):** текущий экран в create/edit УЖЕ рендерит `DictionaryAppBar` (тайтл + «назад»; `RootRouter` не передаёт `showAppBar=false`, дефолт true), заголовка нет только в онбординге (`showAppBar=false`). Редизайн: онбординг — внутренний заголовок «Новый словарь» как в макете; create/edit — значение заголовка уезжает в AppBar (`titleResId` для `DictionaryAppBar` в `DictionaryFormScreen.kt`; для edit — свой wording, определит UI sub-flow), внутренний дубль НЕ рисуется; подзаголовок «Выберите язык и назовите словарь» — всегда в контенте. AppBar в create/edit ОСТАЁТСЯ, навигация «назад» — как сейчас. Осознанная адаптация: в create/edit экран отличается от макета (макет рисован под онбординг-кейс). `RootRouter.kt` НЕ меняется.
- **Data** — нет — БД, миграции, Prefs, библиотеки-обёртки не затрагиваются; источник флагов (blongho) прежний.

## Аспекты

- `string_resources_change` — новые ключи (заголовок, подзаголовок, «ничего не найдено») в `values/` и `values-ru-rRU/` + правка `dictionary_filter_flags_hint` EN («Filter flags…» → по смыслу RU-версии; рассинхрон зафиксирован в реестре `01_create_dictionary.md`).
- `shared_widget_consumers_risk` — виджеты, вызываемые формой, имеют внешних потребителей: `FlagPlaceholderWidget`/`ImageFlagWidget` (dictionarypicker, dictionary/list), `LexemeTextFieldWidget` (component_widgets ×4 + обёртка `PrimaryTextFieldWidget` в core:ui), `PrimaryFullButtonWidget` (quiz/chat ChatWidget, component_widgets, dictionary/list, core:ui ErrorStateWidget). Бриф формулирует изменения как «замена/рестайл **вызова**» — глобальный рестайл общих `core:ui`-виджетов в скоуп НЕ входит (сломал бы 3–5 экранных систем). Реализация должна быть локальной для экрана; способ (локальные composables vs параметризация общих) — решение UI sub-flow.
- `theme_tokens_change` (условный, минимальный) — макет вводит значения, которых нет в `Color.kt` (фон `#FCFCFA`, серые тексты `#8A8A90`/`#55555C`/`#9A9AA2`, подложка поиска `#F1F0EC`, тень `rgba(71,73,184,0.5)`). Куда их класть — Open question №1; изменение `LexemeColor.primary` в скоуп НЕ входит.
- `public_contract_change` — нет; `db_migration` — нет; `new_dependency` — нет.

## Затронутые файлы

Правки:

- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/widget/DictionaryFormWidget.kt` — центральный файл: композиция всех элементов макета (заголовок, превью, поле имени, поиск, сетка, кнопка), производная «ничего не найдено».
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/widget/FlagGridWidget.kt` — 4 колонки, флаг 56dp, маркер выбора (кольцо 2.5 + бейдж-галочка 24 + акцентная bold-подпись), подписи с переносом вместо ellipsis. Потребитель один — сама форма, менять безопасно.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/screen/dictionary/src/main/java/me/apomazkin/dictionary/form/DictionaryFormScreen.kt` — фон экрана и `SystemBarsWidget` (сейчас захардкожен `whiteColor` → фон макета), паддинги контента; смена `titleResId` для `DictionaryAppBar` в create/edit (схема заголовка — см. «Затронутые слои → UI»); условный внутренний заголовок только при `showAppBar=false`.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/core/core-resources/src/main/res/values/strings.xml` и `values-ru-rRU/strings.xml` — новые строки + правка `dictionary_filter_flags_hint` (EN). R-классы транзитивные (`android.nonTransitiveRClass=false` в `gradle.properties`), строки доступны через `me.apomazkin.dictionary.R` — доп. проводки не нужны.

Вероятно новые файлы (решит UI sub-flow): локальные composables в `form/widget/` (Material-поле имени, поиск-пилюля, превью с двойным кольцом, кнопка с тенью) — взамен прямых вызовов общих виджетов.

Читать, но НЕ менять (референс + риск-лист):

- `/Users/kilg/AndroidStudioProjects/PolyTrainer/app/src/main/java/me/apomazkin/polytrainer/route/RootRouter.kt` — маршруты DICTIONARY_SETUP (`showAppBar=false`, онбординг) и DICTIONARY_CREATE (AppBar включён) — проводка режимов; по решению пользователя не меняется.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/DictionaryAppBar.kt` — AppBar create/edit-режимов, принимает `titleResId`; сам виджет не меняется, меняется передаваемый ресурс.

- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/core/ui/src/main/java/me/apomazkin/ui/FlagPlaceholderWidget.kt`, `.../ImageFlagWidget.kt`, `.../input/base/LexemeTextFieldWidget.kt`, `.../btn/PrimaryFullButtonWidget.kt` — общие виджеты с внешними потребителями (см. аспект).
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/modules/core/theme/src/main/java/me/apomazkin/theme/Color.kt`, `.../LexemeStyle.kt` — источник существующих токенов (правка возможна точечная, см. Open question №1).
- Mate-файлы `form/` (`DictionaryFormState.kt`, `DictionaryFormMsg.kt`, `DictionaryFormReducer.kt`, эффекты/хендлеры, `FormNavigator.kt`) — не меняются по решению брифа.
- Тесты `modules/screen/dictionary/src/test/java/.../form/` (`FormActionsTest`, `FormDataLoadingTest`, `FormFieldsExtTest`) — логика не меняется, должны остаться зелёными без правок.

## Релевантные спеки и гайды

- `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/handbook/specs/dictionary-create/spec.md` — действующая спека экрана (навигация/режимы, поля формы, бизнес-логика, паттерн раздельных effect-хендлеров). Секции «Поля формы» (размеры превью 48dp, ellipsis подписей) устареют после редизайна — обновить.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/handbook/guides/theme-and-resources.md` — правила размещения цветов/типографики и строковых ресурсов.
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/handbook/guides/ui-primitives.md` и `.../ui-patterns.md` — существующие примитивы (кнопки, инпуты) и паттерны — база для решения «локальный composable vs параметризация общего».
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/handbook/guides/mate-framework.md` — только как подтверждение границы «UI не лезет в контракт».
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/features/IS485_redesign/01_create_dictionary.md` — реестр текущего поведения экрана (режимы, состояния, известные боли).
- `/Users/kilg/AndroidStudioProjects/PolyTrainer/docs/handbook/specs/flag-placeholder-widget/` — спека общего виджета-плейсхолдера (потребители, поведение) — для оценки риска рестайла превью.

## Spec target

`dictionary-create/spec.md` — спека уже существует, новая не нужна. Обновить UI-описания (превью, сетка, кнопка, новые заголовок и empty-state поиска) после имплементации; поведенческие секции остаются валидными.

## Sub-flow для запуска

| Sub-flow | Запускать? | Обоснование |
|---|---|---|
| Infrastructure | нет | DI/mate/navigation/build не трогаются; возможная точечная правка `Color.kt` — декларативная, закрывается в UI sub-flow (Open question №1) |
| Business | нет | Закрытое решение брифа «Mate-изменений НЕТ», подтверждено кодом: стейт и Msg-набор покрывают макет полностью |
| UI | да | Вся фича: рестайл composables экрана, сетка флагов, строки обеих локалей |
| Data | нет | БД/Prefs/библиотеки не затрагиваются |

## Open questions

1. **Куда класть новые цветовые значения макета** (`#FCFCFA`, `#8A8A90`, `#55555C`, `#9A9AA2`, `#F1F0EC`, тень кнопки). Best-guess: точечные `val` в `modules/core/theme/Color.kt` — файл уже состоит из таких одиночных значений, а зонтичный #485 сделает их переиспользуемыми на следующих экранах; `LexemeColor.primary` НЕ менять (расхождение `#4A49BC` vs `#4749B8` трактовать в пользу существующего токена — в духе решения брифа №3 «не вводить новых цветов»). Альтернативы: (а) приватные константы в модуле экрана — изолированно, но следующие экраны редизайна будут дублировать; (б) считать правку theme инфраструктурой и запускать Infra sub-flow — избыточно для добавления констант без изменения API/DI. Из-за best-guess (а точечные правки theme = часть UI-работы) `infra_touched=false`.
2. **Форма реализации нового вида элементов**: локальные composables в `form/widget/` vs параметризация общих `core:ui`-виджетов. Best-guess: локальные composables — у `LexemeTextFieldWidget` и `PrimaryFullButtonWidget` по 3–5 внешних потребителей, глобальный рестайл вышел бы за скоуп одного экрана; бриф формулирует «замена/рестайл вызова». Альтернатива — опциональные параметры стиля у общих виджетов (без изменения дефолтного вида) — допустимо, решает UI sub-flow.
3. **Заголовок в edit-режиме**: макет даёт только «Новый словарь». По решению пользователя заголовок create/edit живёт в AppBar (`titleResId`); для edit нужен свой wording (типа «Редактирование словаря») — конкретную формулировку определит UI sub-flow, строка добавляется в обе локали.

_model: claude-fable-5_
