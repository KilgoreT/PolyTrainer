# Implement — IS476

## Реализованные узлы

- **#1 [~]** `DictionaryAppBarUseCase.kt` — `flowCurrentDict(): Flow<DictUiEntity?>`.
- **#2 [~]** `DictionaryTabUseCase.kt` — `getCurrentDict(): DictUiEntity?` + `flowCurrentDict(): Flow<DictUiEntity?>`; удалён класс `DictionaryNotFoundException` из того же файла. **(atomic wave `dict_tab_contract_wave`)**
- **#3 [~]** `QuizChatUseCase.kt` — `getCurrentDictionaryId(): Long?`.
- **#4 [~]** `DictionaryAppBarUseCaseImpl.kt` — убран `throw DictionaryNotFoundException()`, эмит `null`; удалён импорт. **(atomic wave)**
> 📎 guide: docs/guides/prefs-datastore.md — "ЗАПРЕЩЕНО — throw на null: ...краш при первом запуске"
- **#5 [~]** `DictionaryTabUseCaseImpl.kt` — `getCurrentDict()` / `flowCurrentDict()` возвращают `null` вместо throw; удалён импорт `DictionaryNotFoundException`. `addWord` / `getWordList` оставлены с `throw IllegalStateException` по требованию design_tree (вне скоупа). **(atomic wave)**
- **#6 [~]** `QuizChatUseCaseImpl.kt` — `return null` вместо `throw IllegalStateException`.
- **#7 [~]** `DictionaryUseCaseImpl.kt` — чистка orphaned pref: `prefsProvider.setLong(CURRENT_DICTIONARY_ID_LONG, null)` при удалении последнего словаря.
> 📎 guide: docs/guides/prefs-datastore.md — "null = ключ не существует"
- **#8 [~]** `dictionaryappbar/mate/Message.kt` — `Msg.CurrentDict(current: DictUiEntity?)`.
- **#9 [~]** `dictionaryappbar/mate/State.kt` — `state.currentDict(current: DictUiEntity?)` extension nullable. Поле State `currentDict` уже было nullable.
- **#10 [~]** `dictionaryappbar/mate/DatasourceEffectHandler.kt` — тип Flow обновился через сигнатуру useCase, тело `collectLatest` пропускает `null` напрямую (без изменений).
- **#11 [~]** `dictionaryappbar/mate/DictionaryAppBarReducer.kt` — ветка `is Msg.CurrentDict` без изменений, тип параметра обновился через сигнатуру `Message.CurrentDict.current`.
- **#12 [~]** `dictionaryTab/logic/State.kt` — добавлено поле `hasNoDictionary: Boolean = false`; добавлены helpers `markNoDictionary()` (`hasNoDictionary=true, isLoading=false`) и `markDictionaryPresent()` (`hasNoDictionary=false`).
> 📎 guide: docs/guides/state-and-extensions.md — "Явные поля для каждого UI-элемента... вычисление происходит в редьюсере, а не в composable"
> 📎 guide: docs/guides/state-and-extensions.md — "Extension-функции для всех мутаций стейта"
- **#13 [~]** `dictionaryTab/logic/Message.kt` — `Msg.SelectDictionary(current: DictUiEntity?)`.
- **#14 [~]** `dictionaryTab/logic/VocabularyTabReducer.kt` — ветка `Msg.SelectDictionary`: `null` → `markNoDictionary() + emptySet()`; non-null → `markDictionaryPresent().showLoading() + setOf(LoadTermFlow())` (Minor 1 из Architect Review).
> 📎 guide: docs/guides/reducer-patterns.md — "Паттерн 5: Динамические эффекты — когда эффекты зависят от содержимого сообщения"
- **#15 [~]** `dictionaryTab/logic/DatasourceEffectHandler.kt` — `subscribe` пропускает `null` как `Msg.SelectDictionary(null)`; `LoadTermFlow` защищён через `getCurrentDict()?.id?.toInt()`, при `null` возвращается `Msg.NoOperation`.
> 📎 guide: docs/guides/effect-handlers.md — "FlowHandler решает эту задачу: подписывается на Flow при старте, каждый emit конвертирует в Message"
- **#16 [~]** `dictionaryTab/ui/VocabularyTabScreen.kt` — добавлена ветка `state.hasNoDictionary -> EmptyWidget()` в `when`. Импорт `EmptyWidget` добавлен.
> 📎 guide: docs/guides/ui-patterns.md — "Условные оверлеи... по флагам стейта"
- **#17 [~]** `quiz/chat/quiz/QuizGameImpl.kt` — при `dictionaryId == null` логирование `logger.w(LogTags.CHAT, ...)` и `return emptyList()`. Опираемся на IS461 (пустой quizList не крашит).
> 📎 guide: docs/guides/logging.md — "WARNING: Проблема, но приложение работает → Crashlytics breadcrumb"
> 📎 guide: docs/guides/logging.md — "Использовать ТОЛЬКО константы. НЕ хардкодить строки"
- **#19 [~]** `app/.../widget/DictionaryAppBarUseCaseImplTest.kt` — уже актуален (test step), импорт `DictionaryNotFoundException` отсутствует, ассерты `assertNull(...)` на пустой список. **(atomic wave)**
- **#20 [~]** `app/.../dictionary/DictionaryUseCaseImplTest.kt` — уже актуален (test step), test case #11 ожидает `setLong(CURRENT_DICTIONARY_ID_LONG, null)`.
- **#21 [+]** `app/.../dictionarytab/DictionaryTabUseCaseImplTest.kt` — уже создан (test step), 6 кейсов для nullable-контракта `flowCurrentDict()` и `getCurrentDict()`.
- **#22 [~]** `modules/screen/dictionaryTab/.../VocabularyTabReducerKtTest.kt` — уже актуален (test step), тесты #19/#20 для nullable `SelectDictionary` + переход null → non-null.
- **#23 [+]** `modules/widget/dictionaryappbar/.../DictionaryAppBarReducerTest.kt` — уже создан (test step). Добавлены недостающие импорты `me.apomazkin.mate.state` и `me.apomazkin.mate.effects` (тестовый файл не компилировался без них — это компиляционный фикс импортов, не правка логики).

## Решения принятые в процессе (нетривиальные)

- **`PrefsProvider.setLong(value: Long?)`** — сигнатура расширена до nullable. Это потребовалось чтобы выполнить #7 (`prefsProvider.setLong(_, null)` при удалении последнего словаря) и удовлетворить ассерт теста #20 (`coVerify { prefsProvider.setLong(PrefKey.CURRENT_DICTIONARY_ID_LONG, null) }`). В реализации `value == null` приводит к `it.remove(key)` — корректно чистит ключ из DataStore. Существующие 4 вызова с non-null `Long` остались валидными благодаря Kotlin'овской автоконвертации `Long → Long?`. Это согласуется с `prefs-datastore.md` guide ("`null = ключ не существует`"): теперь pref можно явно удалить вместо хранения orphaned-значения.
> 📎 guide: docs/guides/prefs-datastore.md — "null = ключ не существует"
> 📎 guide: docs/guides/prefs-datastore.md — "Весь доступ — через PrefsProvider"

- **Дополнительные импорты в `DictionaryAppBarReducerTest.kt`** — тестовый файл (созданный test step) использует `result.state()` / `result.effects()`, но не имел импортов `me.apomazkin.mate.state` / `me.apomazkin.mate.effects` (extension-функции на `ReducerResult`). Соседний `VocabularyTabReducerKtTest.kt` использует `import me.apomazkin.mate.*`. Добавил эти два импорта явно — поведение тестов не изменилось, добавились только символы для разрешения существующих вызовов.

- **`addWord` / `getWordList` оставлены с `throw IllegalStateException`** — design_tree #5 явно указал: "оставить как есть в рамках этой фичи: они вызываются только после `Msg.SelectDictionary(non-null)`, где State уже гарантирует наличие словаря через ветку в reducer'е". Не править.

## Результат тестов

`./gradlew testDebugUnitTest` — **зелёный** (exit 0).

Ключевые тестовые классы (failures=0, errors=0):

| Класс | Тестов |
|---|---|
| `DictionaryUseCaseImplTest` | 13 |
| `DictionaryAppBarUseCaseImplTest` | 10 |
| `DictionaryTabUseCaseImplTest` | 6 |
| `QuizChatUseCaseImplTest` | 3 |
| `VocabularyTabReducerKtTest` | 20 |
| `DictionaryAppBarReducerTest` | 6 |
| `QuizGameImplEmptyListTest` | 3 |

Все 61 ключевых теста для фичи прошли. Остальной testDebugUnitTest также зелёный.

## Файлы изменены

### Контракты (deps)
- `modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/deps/DictionaryAppBarUseCase.kt`
- `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/deps/DictionaryTabUseCase.kt`
- `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/deps/QuizChatUseCase.kt`

### Реализации (app)
- `app/src/main/java/me/apomazkin/polytrainer/di/module/widget/DictionaryAppBarUseCaseImpl.kt`
- `app/src/main/java/me/apomazkin/polytrainer/di/module/dictionarytab/DictionaryTabUseCaseImpl.kt`
- `app/src/main/java/me/apomazkin/polytrainer/di/module/quizchat/QuizChatUseCaseImpl.kt`
- `app/src/main/java/me/apomazkin/polytrainer/di/module/dictionary/DictionaryUseCaseImpl.kt`

### Mate-layer (modules)
- `modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/mate/Message.kt`
- `modules/widget/dictionaryappbar/src/main/java/me/apomazkin/dictionaryappbar/mate/State.kt`
- `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/logic/State.kt`
- `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/logic/Message.kt`
- `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/logic/VocabularyTabReducer.kt`
- `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/logic/DatasourceEffectHandler.kt`

### UI
- `modules/screen/dictionaryTab/src/main/java/me/apomazkin/dictionarytab/ui/VocabularyTabScreen.kt`

### Quiz
- `modules/screen/quiz/chat/src/main/java/me/apomazkin/quiz/chat/quiz/QuizGameImpl.kt`

### Prefs
- `modules/datasource/prefs/src/main/java/me/apomazkin/prefs/PrefsProvider.kt` — `setLong(value: Long?)`.

### Тесты (только импорт-фикс)
- `modules/widget/dictionaryappbar/src/test/java/me/apomazkin/dictionaryappbar/mate/DictionaryAppBarReducerTest.kt` — добавлены 2 импорта (`me.apomazkin.mate.state`, `me.apomazkin.mate.effects`).

_model: claude-opus-4-7[1m]_
