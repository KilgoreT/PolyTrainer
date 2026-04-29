# Implementation Log

## Нетривиальные решения

### 1. `loadAllFlags()` — `runBlocking` для suspend `getFlagRes`

`CountryProvider.getFlagRes()` — suspend, но `loadAllFlags()` вызывается из `lazy` property (non-suspend). Под капотом `World.getFlagOf()` синхронный, обёрнут в `suspendCoroutine`. Использован `runBlocking` внутри `loadAllFlags()`. Альтернатива (добавить `getFlagResSync` в `CountryProvider`) потребовала бы изменения интерфейса за пределами скоупа фичи.

### 2. `BackHandler` в Screen

`BackHandler { sendMsg(DictionaryFormMsg.Back) }` добавлен во внутреннюю composable-функцию. Навигация `Back` идёт через Effect → NavigationEffectHandler, не через прямой вызов callback. Это соответствует гайду effect-handlers.md (навигация через Effect, не boolean в State).

### 3. FlagGridWidget — `modifier` параметр

Добавлен `modifier: Modifier = Modifier` параметр в `FlagGridWidget`, чтобы DictionaryFormWidget мог передать `Modifier.weight(1f)` для заполнения оставшегося пространства. Без этого grid не получал weight из Column.

### 4. NavigationEffectHandler — return для нерелевантных эффектов

`NavigationEffectHandler` обрабатывает только `Close` и `Back`. Для остальных `DictionaryFormEffect` — `return` (не вызывает `consumer`). Это предотвращает двойной `Msg.Empty` от NavigationEffectHandler и DictionaryFormEffectHandler на одних и тех же эффектах.

### 5. DictionaryFormEffectHandler — `return` для FilterFlags/Close/Back

`DictionaryFormEffectHandler` явно игнорирует (`return`) эффекты `FilterFlags`, `Close`, `Back` — они обрабатываются другими handlers. Без `return` handler вызвал бы `consumer(Msg.Empty)`, создавая лишний no-op message.

### 6. String resource `dictionary_filter_flags_hint`

Добавлена новая строка в `core-resources/values/strings.xml`. Русская локализация не добавлена — аналогично другим строкам в файле.

_model: claude-opus-4-6[1m]_
