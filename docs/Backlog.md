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

## Архитектура

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
