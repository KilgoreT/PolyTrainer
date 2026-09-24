# Mate library — этапы выделения

Принцип — как у фич: каждый этап завершён и ПРОВЕРЯЕМ; следующий — после
проверки предыдущего. Основа: [mate_analysis.md](mate_analysis.md)
(роадмап v0/v1/v2), [app_scenario_impl.md](app_scenario_impl.md),
организация — [brief.md](brief.md).

Статус: Э0–Э3 ✅ (v0.1.0, PolyTrainer на JitPack-зависимости, PR #498);
Э4 ✅ библиотека (v0.1.1) + миграция всех 12 экранов PolyTrainer;
Э5 ✅ библиотека (v0.1.2: mate-navigation, смерть MateFlowHandler) +
PolyTrainer на appNavGraph/едином handler'е с lifecycle-гейтом
(Backlog-краш «navigate после фона» закрыт; смоук навигации 6/6);
Э6 ✅ библиотека (v0.1.3: mate-app-test — узловой стек, ScenarioScope,
RecordingObserver-трасса, stubFlow) + PolyTrainer: Assembly всех 13
экранов (VM — тонкие обёртки), io-диспатчер инъектируем, пилот из 5
сценариев в app/src/test (идут в CI обычными юнитами) — всё в ветке
MT_mate_e4_subscriptions.

## Э0. Репо-каркас

- `KilgoreT/mate`: KMP-сборка (androidTarget + jvm + iosArm64/
  SimulatorArm64), Apache 2.0, README с принципами (П1 с девизом — на
  витрину), CI (build + tests).
- Проверка JitPack: тег `v0.0.1` → пустой `mate-core` резолвится в
  тестовом пустом проекте.

## Э1. Перенос ядра as-is

- 9 чистых файлов текущего `modules/core/mate` → `commonMain`
  mate-core БЕЗ изменения поведения; `mate-test` на `kotlin.test`
  (внимание: порядок аргументов assertEquals ≠ JUnit).
- Проверка: юниты раннера в библиотеке, зелёные на jvm + android.

## Э2. Ядро v0

- Mailbox (О1: UNDISPATCHED-старт, init-эффекты первой итерацией,
  порядок state→дифф→эффекты) + «Политика ошибок» (цикл не умирает,
  mateFail-параметр) + `MateObserver` (О4: полный цикл, каузальность,
  Duration) + роутинг-табличка (О3: effectFamily, fail на дубль/
  сироту/двойной матч; базовый nav-handler переведён).
- Решить: debug-подсказка П1 (лог при схлопывании дубля в Set).
- Проверка: TDD в библиотеке — FIFO/каскады/сирота/дубль/трасса.

## Э3. Обкатка на PolyTrainer

- Модуль `modules/core/mate` умирает → зависимость JitPack;
  `ReducerLogging`/`LogTags`/`Constants` остаются проектной обвязкой;
  handler'ы экранов получают `effectFamily` (миграция механическая).
- Проверка: все юниты проекта, lint, сборка, смоук на девайсе —
  поведение неотличимо. Самая ранняя точка, где библиотека доказана
  настоящим приложением.

## Э4. v1: декларативные подписки (О2)

- `subscriptions(State) → Set<Sub>` + дифф по equality в библиотеке;
  `MateSubscriptionHandler`.
- Миграция экранов PolyTrainer ПО ОДНОМУ (первый — groupstab:
  `expandedGroupWindows` — готовая декларация); смерть
  `MateFlowHandler` и Subscribe-эффектов.
- Проверка: юниты + ручники живых окон (регресс Э5-механики групп).

## Э5. v1: навигация как данные (О5 + navGraph)

- Артефакт `mate-navigation`: navGraph DSL, ЕДИНЫЙ shared nav-handler
  (гейт StateFlow + FIFO-очередь без лимита, staleness-параметр);
  прод-интерпретатор push/pop в app.
- Смерть шести звеньев ([navigation_as_data.md](navigation_as_data.md)):
  NavigatorImpl'ы, per-экранные Navigator-интерфейсы и nav-handler'ы,
  лямбды-пропсы.
- Проверка: смоук ВСЕЙ навигации приложения + закрытие Backlog-краша
  «navigate после ухода в фон».

Примечание: Э4 и Э5 независимы — можно переставить или вести
параллельно.

## Э6. v2: сценарный харнес

- ScreenSpec-рефакторинг PolyTrainer (сборка Mate из VM → фабрики);
  `mate-app-test`: AppHarness, стек экранов, RecordingObserver,
  ScenarioScope DSL, стаб-движок (решение: mockk vs фейки).
- Пилот: 3–5 сценариев из [app_scenario_impl.md](app_scenario_impl.md)
  (создание слова→карточка; группы+membership; деструктив с
  виртуальными тиками; live-обновление через stubFlow).
- Проверка: сценарии в CI; падение сценария даёт полную трассу.

## Э7. Релиз 1.0 → Maven Central

- Стабилизация API по итогам обкатки, Sonatype/GPG, публикация в
  Central (пакет `io.github.kilgoret.*` готов с Э0).
- Проверка: артефакт резолвится из Central, PolyTrainer переезжает с
  JitPack.
