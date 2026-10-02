# IS517 | Бриф: прогон androidTest в CI на эмуляторе

Issue: https://github.com/KilgoreT/PolyTrainer/issues/517
Ветка: `IS517_ci_android_tests` (от master `6f9e0fe8`).
Источник: Backlog, «ВекторныйПиздеж» → «CI: androidTest (миграции, DAO) не
гоняется — добавить emulator-job».

## Проблема

CI (`.github/workflows/on_feature_push.yml`, ветки `IS**`/`MT**`) гоняет
только lint, unit-тесты и сборку debug-APK. Инструментальные тесты
(`src/androidTest`) запускаются только руками на девайсе юзера — с
разблокировкой экрана, Wi-Fi adb и прочим. Самые рискованные изменения
(миграции БД, DAO-запросы, анимации чата) в CI не проверяются вообще.
Цена промаха показана в IS515: если забыть зарегистрировать миграцию,
Room молча стирает базу (destructive fallback), и ловит это только ручник.

## Что есть

| Модуль | Тесты androidTest | Что проверяют |
|---|---|---|
| `core/core-db-impl` | 16 классов: `MigrationFrom11to12`, `…12to13`, `…13to14`, `…14to15`, `MigrationFrom11to12IdempotencyTest`, `Is486DataLayerTest`, `Phase3ConstructorDataTest`, `QuizGroupFilterDaoTest`, `GroupDaoTest`, `GroupMembershipTest`, `GroupMutationsTest`, `GroupDeleteWithWordsTest`, `CaptionSuggestionsDaoTest`, `CascadeExecutorTest`, `BundledSqliteFeatureTest`, `ExampleInstrumentedTest` | миграции БД по цепочке, DAO-запросы (квиз-фильтры, группы, каскады), засев встроенных компонентов |
| `modules/screen/quiz/chat` | 2: `ChatMessageMotionTest`, `QuestionBubbleTest` | Compose: движение ленты чата по кадрам, раскладка пузыря вопроса |
| `app` | 1: `ExampleInstrumentedTest` | шаблонный, смысла нет |

- Прогон всего на девайсе: core-db-impl ≈ 1 мин, chat ≈ 1 мин (без сборки).
- SDK: minSdk 24, target/compile 36. Тесты БД — bundled SQLite (не зависят от
  версии системного SQLite). Compose-тесты чата — на тестовых часах
  (`mainClock.autoAdvance = false`), к скорости машины не привязаны, но
  требуют разблокированный экран (на эмуляторе — по умолчанию).
- Mate-тесты, ChatSubHandler и др. — JVM, уже в CI.

## Решения (2026-10-02)

- **Д1. Только на PR в master** (`pull_request` → `master`): ловит всё
  перед мержем, промежуточные пуши ветки не тормозит.
- **Д2. Всё androidTest одним джобом:** `core-db-impl` (миграции, DAO) +
  Compose-тесты квиз-чата.
- **Д3. Блокирует мерж:** красный эмулятор = красный PR, на красном не
  мержим. Обязательный статус в правилах ветки master — по желанию юзера
  (настройка GitHub, не код).
- **Д4. Один эмулятор API 34, x86_64, `google_apis`** — стабильный и быстрый
  образ на раннерах GitHub; матрица версий не нужна (БД на bundled SQLite).
- **Д5. Шаблонные `ExampleInstrumentedTest`** (`app`, `core-db-impl`) —
  удалить.

## Исходные вопросы

- **Д1. Когда гонять.** На каждый пуш ветки `IS**`/`MT**`, как остальной
  CI (+ время на каждый пуш), только на PR в master, или по расписанию /
  вручную?
- **Д2. Что гонять.** Всё androidTest, или только `core-db-impl` (миграции и
  DAO — самый ценный и стабильный кусок), а Compose-тесты чата — отдельно?
- **Д3. Блокирует ли мерж.** Падение эмулятор-джоба = красный PR, как
  unit-тесты, или пока информационный (не обязательный) статус, пока не
  убедимся в стабильности на CI?
- **Д4. Версия Android на эмуляторе.** Одна (какая) или матрица
  (например, minSdk-близкая и свежая)?
- **Д5. Шаблонные `ExampleInstrumentedTest`** в `app` и `core-db-impl`
  удалить заодно?
