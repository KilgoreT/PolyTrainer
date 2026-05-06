# Backlog

---

## Critical Bugs

- **[disableUserInput() инвертирован].**
  `modules/screen/quiz/chat/logic/State.kt:206` — устанавливает `isUserInputEnable = true` вместо `false`.
  Вызывается 6 раз в ChatReducer. Пользователь может вводить текст когда не должен.
  Нужно: заменить `true` на `false`, убрать дублированные вызовы в `Msg.GetAnswer` и `Msg.Skip`.

- **[TermNotLoaded не реализован].**
  `modules/screen/wordcard/mate/WordCardReducer.kt:16` — `TODO("TermNotLoaded is not implemented")`.
  Если слово не загрузится — краш. Нужно: добавить обработку ошибки (snackbar + закрытие экрана или retry).

- **[Нет error handling в эффект-хендлерах].**
  Ни один DatasourceEffectHandler не оборачивает вызовы в try-catch.
  Исключение из Room/UseCase убивает coroutine scope без recovery.
  WordCard явно бросает `IllegalStateException("Lexeme not found")`.
  Нужно: обернуть все `withContext(Dispatchers.IO)` в try-catch, генерировать Error-сообщения.

- **[Race condition в Mate.accept()].**
  Нет синхронизации между чтением `_state.value` и записью.
  Два эффекта вернувшие сообщения одновременно — один прочитает stale state.
  Нужно: Mutex или `_state.update {}` вместо read-then-write.

---

## ForgeFlow

- **[Шаг определения типа фичи в flow: contract vs UI-only].**
  Добавить шаг перед contract, который определяет нужен ли контракт TEA (State/Msg/Effect) или фича чисто UI. Если UI-only — пропустить contract group и usecase. Сейчас contract выполняется всегда и для UI-задач генерирует пустые артефакты.

- **[Conductor подхватывает не тот flow].**
  IS445: conductor взял `flows/feature.yml` вместо `flows/lexeme/feature.yml`, начал делать не ту фичу (шаг analysis вместо spec). Пришлось переименовать базовый flow в `zhopa.yml` чтобы не путался.
  Нужно: механизм явного выбора flow в planning() — не угадывать, а требовать точный путь. Или убрать/переименовать конфликтующие flow.

- **[Conductor пишет артефакты в чужую директорию фичи].**
  IS445: conductor на шаге design_tree записал 06_design_tree.md в директорию IS441 вместо IS445, проанализировал 55 файлов рефакторинга вместо одного баг-фикса. Полностью потерял контекст текущей фичи.
  Нужно: conductor должен валидировать feature_dir перед записью каждого артефакта. Путь к директории фичи — из plan.yml, не из головы.

- **[Шаг check: агент запускает одну и ту же проверку несколько раз].**
  IS445: агент на шаге check несколько раз запрашивал разрешение на один и тот же тип проверки (lint/test/build). Либо перезапускает при ошибке без диагностики, либо теряет результат и запускает заново.
  Нужно: разобраться почему агент дублирует запуски. Возможно check.md или runner не передаёт агенту что команда уже выполнена. Добавить в промпт check: "каждая команда запускается РОВНО ОДИН РАЗ, при ошибке — читай лог и фикси, не перезапускай молча".

- **[Модуль guides — полностью нерабочий].**
  Модуль guides подключен к шагам flow, но на практике conductor/агенты НИКОГДА не обновляют гайды. Артефакты фичи создаются, гайды игнорируются. Модуль — мёртвый груз.
  Проблемы: (1) guides стоит после checklist и после review — к этому моменту агент уже "выполнил шаг" и не возвращается. (2) Нет явного output для guides — агент не знает что от него ждут.
  Нужно: переместить guides РАНЬШЕ checklist и review. Сделать guides отдельной фазой с явным output (diff в файл гайда или "нет паттерна для гайда"). Или признать что модуль не работает и выпилить.

- **[Conductor игнорирует pause:true и mode:normal — лезет в implement].**
  IS449: conductor получил промпт со списком шагов (включая implement) и проигнорировал pause — сразу начал писать код. Plan.yml показывал implement:pending, а код уже был изменён.
  Причина: агент-субпроцесс не умеет "остановиться" между шагами — он выполняет всё что может за один вызов. Pause работает только если оркестратор вызывает conductor пошагово через SendMessage.
  Нужно: НИКОГДА не перечислять будущие шаги (implement, check) в промпте conductor — давать только текущий + следующий. Или переделать механизм пауз.

---

- **[Централизованная система ошибок и снекбаров].**
  Сейчас каждый экран по-своему обрабатывает ошибки: кто-то Toast, кто-то Snackbar с boolean флагом, кто-то просто молчит. Нет единого механизма показа ошибок пользователю.
  Нужно: централизованный ErrorHandler / SnackbarManager. Единый API для показа ошибок из любого места (EffectHandler, WebView, навигация). Единый UI компонент (Snackbar/BottomSheet). Типизированные ошибки (network, unknown, validation).

---

- **[DictUiEntity.flagRes: Int → Int?].**
  flagRes = 0 как "нет флага" — магическое значение. Сделать nullable, убрать `?: 0` из маппинга в UseCase. 11 файлов затронуто.

---

## Архитектура

- **[Разобраться в слоях entity].**
  Нет доменного слоя. ApiEntity (core-db-api) де-факто выполняет роль доменной сущности. UI модели (UiItem, CountryFlagItem) содержат и бизнес-поля и UI-поля. Суффиксы непоследовательны (ApiEntity, UiItem, UiEntity, Item, Info).
  Нужно: определить конвенцию слоёв entity, решить нужен ли отдельный domain layer или ApiEntity = доменная (с переименованием). Описать в гайде.

- **[NavigationEffect в модуле mate].**
  Навигация через Effect вместо boolean флагов в State (needClose, closeScreen, exit). NavigationEffect как sealed interface в core/mate. NavigationEffectHandler принимает callback'и (onClose, onBackPress, onExit). Убирает навигационные флаги из State, LaunchedEffect из Screen. Базовая иерархия в mate: Close (popBackStack), ExitApp (finish()). Переходы вперёд — конкретные Effect'ы в модулях, не generic Navigate(route). Миграция постепенная — начать с DictionaryForm, потом WordCard, ChatScreen.

- **[DictionaryUseCase.getDictionaryList() — кандидат на удаление].**
  suspend-версия не вызывается — используется только flowDictionaryList(). CoreDbApi.getDictionaryList() нужен другим UseCase'ам, но в DictionaryUseCase — мёртвый код.

- **[Placeholder флага в AppBar].**
  В DictionaryPicker (AppBar главного экрана) placeholder словаря без флага — белый, не видно. Добавить серый круг с буквой (как в форме словаря).

- **[Шиммеры при загрузке].**
  Добавить loading-состояние для цепочек эффектов. Пока цепочка не завершена — показывать шиммеры вместо контента. Шиммеров в проекте ещё нет — нужно создать компонент. Начать с формы словаря (загрузка флагов + данных словаря).

- **[Reducer не чистый — ChatReducer].**
  ChatReducer принимает ResourceManager и LexemeLogger. Логирование — сайд-эффект.
  WordCardReducer чистый — несоответствие reference-реализации.
  Нужно: вынести строки в сообщения или предвычислять в эффект-хендлере. Логирование — через обёртку Mate.

- **[Service Locator вместо DI].**
  `Context.appComponent` — service locator anti-pattern. Каждый composable сам достаёт зависимости.
  Нужно: рассмотреть миграцию на Hilt или component-per-feature.

- **[UseCaseImpl в app-модуле].**
  9 реализаций в `app/di/module/`. App знает про ВСЕ реализации, фичи не независимы.
  Нужно: перенести UseCaseImpl в соответствующие feature-модули.

- **[MainUiDeps — god-object].**
  Интерфейс с 7 `@Composable` методами, реализация с 11 параметрами.
  Нужно: разбить на per-tab интерфейсы или передавать UseCase напрямую.

- **[Монолитный WordDao — 40+ методов].**
  Languages, Words, Lexemes, Quiz, Statistics — всё в одном DAO.
  Нужно: разбить на `LanguageDao`, `WordDao`, `LexemeDao`, `QuizDao`, `StatisticDao`.

- **[O(n*m) в Mate — эффекты через все хендлеры].**
  Каждый эффект прогоняется через ВСЕ хендлеры. 10 из 15 вызовов — `null -> Msg.Empty`.
  Нужно: dispatch map или фильтрация по типу эффекта перед вызовом.

- **[@UnsafeVariance в MateEffectHandler].**
  Ломает type safety. Компилятор не проверяет тип эффекта для хендлера.
  Нужно: пересмотреть generic-дизайн интерфейса без `@UnsafeVariance`.

- **[ViewModelFactory boilerplate].**
  Каждый ViewModel — идентичный inner class Factory с `@Suppress("UNCHECKED_CAST")`.
  Нужно: Hilt `@HiltViewModel` или generic Factory.

- **[Int/Long мисматч в TermApi и других API].**
  `TermApi.getTermList(dictionaryId: Int)`, `searchTermsPaging(dictionaryId: Int)` принимают Int, но dictionary id теперь Long.
  `.toInt()` в DictionaryTabUseCaseImpl и StatisticUseCaseImpl — потенциальное переполнение.
  Нужно: перевести все API методы на Long для dictionaryId.

- **[Двойной тап на навигационных кнопках].**
  `navController.navigate()` без `launchSingleTop = true`. Быстрый двойной тап создаёт два экрана в стеке.
  Нужно: добавить `launchSingleTop = true` во все navigate вызовы.

---

## State Management

- **[Snackbar pattern fragile].**
  `show: Boolean` + `title: String` — temporal coupling. Может показаться старый title.
  Нужно: sealed class `SnackbarState { Hidden, Showing(id, message) }`.

- **[TextValueState vs EditableTextState — дублирование].**
  Два идентичных паттерна с разными именами (`origin/edited` vs `text/editedText`).
  Нужно: унифицировать в один тип, использовать во всех модулях.

- **[TextValueState.edited зависит от origin].**
  `val edited: String = origin` — default parameter зависит от другого. Неочевидное поведение при copy().
  Нужно: сделать `edited: String = ""` явно.

- **[Некосистентные аннотации @Stable/@Immutable].**
  WordCard — `@Stable`, Chat — `@Immutable`, CreateDictionary — без аннотаций. 9 классов без маркировки.
  Нужно: договориться на один подход (`@Immutable` для всех data class стейтов), пройти по всем модулям.

- **[termListMap растёт без лимита].**
  DictionaryTabState добавляет Flow на каждый поисковый паттерн. Memory leak при активном поиске.
  Нужно: ограничить размер map или очищать при смене паттерна.

- **[MateFlowHandler.job — public var].**
  Можно перезаписать без отмены предыдущего job. Subscription leak.
  Нужно: сделать private set или список job'ов.

---

## Тестирование — конвенция

- **[Конвенция тестов extension-функций].**
  Один тест-файл = одна extension-функция (`<FunctionName>ExtTest.kt`).
  Doc-комментарий класса: полный список тест-кейсов.
  Каждая тестовая функция: комментарий UI-логики, бизнес-логики (если есть), конкретного тест-кейса.

## Тестирование — каркас миграций

- **[`getFromDatabase()` — копипаста на каждый Schemable].**
  50-80 строк boilerplate на версию. Нужно: generic cursor→map парсер.

- **[`checkMatcher` — нет exhaustiveness].**
  Новое поле — компилятор молчит. Нужно: assertEquals на entity или генерировать matcher.

- **[Schemable.data() использует актуальные entity].**
  При переименовании полей ломаются старые Schemable. Нужно: отдельный data class на каждую версию.

- **[Дублирование интерфейсов в Schemable.kt и Schema.kt].**
  Два набора одинаковых интерфейсов. Нужно: убрать дубли.

- **[Schema.kt — god object, 400+ строк].**
  Часть вынесена в schemable/, часть нет. Нужно: вынести всё.

- **[afterCreateCheck дублирует afterMigrationCheck].**
  Нужно: извлечь helper verifySchemable().

- **[Нет теста на удаление данных].**
  Нужно: негативные проверки — дропнутая колонка не существует.

- **[Date — хак с погрешностью 1000ms].**
  isEqualTo() с погрешностью, даты закомментированы в checkMatcher. Нужно: починить.

## Тестирование

- **[18+ модулей без тестов].**
  Только placeholder `ExampleUnitTest.kt`. Реальные тесты есть у VocabularyTab и DB-миграций.
  Нужно: покрыть reducer'ы всех TEA-модулей (минимум 8 screen-модулей).

- **[Ноль тестов на эффект-хендлеры].**
  8+ реализаций DatasourceEffectHandler полностью не покрыты.
  Нужно: тесты с моками UseCase для каждого хендлера.

- **[Ноль Compose UI тестов].**
  При тяжёлом использовании Compose — ни одного ComposeTestRule теста.
  Нужно: добавить smoke-тесты хотя бы для критических экранов.

---

## Tech Debt

- **[40 TODO в кодовой базе].**
  Включая баг с именем БД (данные теряются), неработающий convention plugin, неоптимальная загрузка Flow (#377).
  Нужно: разобрать каждый TODO — или починить, или создать задачу, или удалить.

- **[@Deprecated("Outdated") код всё ещё используется].**
  `WriteQuizStep`, цвета в theme.
  Нужно: заменить или удалить deprecated код.

- **[@SuppressLint("CheckResult") на 6+ классах].**
  RxJava observable errors молча игнорируются в legacy feature-модулях.
  Нужно: добавить error handling или удалить legacy код (если feature/ мёртв — просто удалить).

- **[java.util.Date вместо java.time].**
  Deprecated API по всей кодовой базе.
  Нужно: мигрировать на `java.time.LocalDate` / `Instant` (minSdk 26+ или desugaring).

---

## После публикации

- **[Восстановить "Оцените нас" в настройках].**
  `RateWidget` скрыт из `SettingsTabScreen.kt`. После публикации в Google Play — вернуть в секцию и добавить intent на страницу приложения в маркете.

- **[Восстановить "Поделитесь приложением" в настройках].**
  `AppShareWidget` скрыт из `SettingsTabScreen.kt`. После публикации — вернуть в секцию, обновить текст share с ссылкой на маркет.

---

## ВекторныйПиздеж

- **[Room RANDOM + @Relation].**
  После обновления Room с 2.6.1 до 2.8.4 — проверить запросы с RANDOM() + LIMIT + @Relation.
  Ранее Room 2.7.1 давал IllegalStateException (https://issuetracker.google.com/issues/413924560).
  Нужно: прогнать квиз-выборку и убедиться что рандомные запросы работают корректно.
