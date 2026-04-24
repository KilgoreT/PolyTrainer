# IS441. Рефакторинг экрана языков — Бриф

## Текущее состояние

Экран выбора языка (CreateDictionary) — экран для добавления нового словаря.
Пользователь выбирает язык из предустановленного списка и создаёт словарь.

**Модуль:** `modules/screen/createdictionary/`

---

## Точки входа (откуда вызывается)

Экран переиспользуется из трёх разных мест, но во всех случаях это один и тот же route `CREATE_DICTIONARY` в `RootRouter`. Это значит, что при открытии из настроек или dropdown — навигация идёт через корневой роутер, а не внутри таба. UX не различается между сценариями — пользователь всегда видит одинаковый fullscreen-экран.

### 1. Первый запуск (RootRouter)

```
SplashScreen → isInitLaunch == true → CREATE_DICTIONARY → onClose → MAIN_ROUTER
```

Единственный сценарий, где экран обязателен. Без словаря приложение не работает — слова привязаны к языку через FK.

**Файл:** `app/.../route/RootRouter.kt`

### 2. Настройки → Управление языками

```
SettingsTab → LangManageWidget → onLangManagementClick → openAddDict → CREATE_DICTIONARY
```

Пункт называется "Управление языками", но по факту это просто повторное открытие экрана создания. Никакого управления (удаление, переименование, переключение) нет.

**Файлы:** `SettingsTabScreen.kt:101`, `Settings.kt:17`, `MainRouter.kt:39`

### 3. Выпадающее меню словаря (DictionaryAppBar)

```
DictionaryAppBar → DictDropDownWidget → "Добавить словарь" → openAddDict → CREATE_DICTIONARY
```

Доступно из табов: Vocabulary, Quiz, Statistics — через общий AppBar с выпадающим списком словарей. Callback `openAddDict` пробрасывается через всю цепочку: `RootRouter → MainRouter → MainScreen → NavGraphBuilder.vocabulary/quiz/settings → DictionaryAppBar → DictDropDownWidget`.

**Файлы:** `DictionaryAppBar.kt:75`, `DictDropDownWidget.kt:44`

---

## Слои

### State

```kotlin
data class CreateDictionaryState(
    val isLoading: Boolean = true,
    val needClose: Boolean = false,
    val langState: LangState = LangState(),
)

data class LangState(
    val langList: List<PresetLangUi> = listOf(),
    val selectedNumericCode: Int? = null,
    val addLangButtonEnable: Boolean = false,
)
```

**Поля `CreateDictionaryState`:**
- `isLoading` — показывать ли spinner. `true` при старте, `false` после загрузки списка языков.
- `needClose` — флаг закрытия экрана. Когда становится `true`, composable вызывает `onClose()` через `LaunchedEffect`. Паттерн "one-shot navigation event через стейт" — тот же что `closeScreen` в WordCard.
- `langState` — вложенный стейт для логики выбора языка.

**Поля `LangState`:**
- `langList` — список `PresetLangUi` для отображения. Заполняется один раз при загрузке из захардкоженного `LanguageData`.
- `selectedNumericCode` — numericCode выбранного языка или `null` если ничего не выбрано. Используется как toggle: повторный тап на тот же язык сбрасывает в `null`.
- `addLangButtonEnable` — активна ли кнопка "Создать". Вычисляется редьюсером при `SelectLang` на основании `selectedNumericCode`, но явно присутствует в стейте как отдельное поле. Каждый элемент UI должен иметь свой флаг в стейте — не вычислять на лету в composable.

Стейт простой, но есть проблемы:
- **Нет аннотаций `@Immutable` / `@Stable`** — Compose не может оптимизировать skip рекомпозиции.
- **Нет extension-функций** — модификации стейта делаются через `.copy()` прямо в редьюсере, что противоречит текущей конвенции (ChatReducer, WordCardReducer).

### Messages

```kotlin
sealed interface Msg {
    data class ShowLangList(val list: List<PresetLangUi>) : Msg
    data class SelectLang(val numericCode: Int) : Msg
    data class SaveLang(val numericCode: Int, val langName: String) : Msg
    data object Close : Msg
    data object Empty : Msg
}
```

> **Комментарий: `langName` в `SaveLang` и хранение в БД**
>
> `SaveLang` содержит `langName: String`, который резолвится в composable через `context.getString(langNameRes)`.
> Имя попадает в Room-таблицу `languages.name`.
>
> Сейчас это выглядит как протекание UI в бизнес-слой: в БД ложится локализованная строка,
> зависящая от локали устройства. Русская локаль → "Английский", английская → "English".
>
> Но в будущем `name` — это **пользовательское название словаря**, не название языка.
> Пользователь сможет написать что угодно: "Биология на английском", "Сленг" и т.д.
> В этом контексте хранить `name` в БД — правильно. Это user-defined строка.
>
> Проблема текущей реализации: сейчас `name` генерируется из string resource, а не вводится пользователем.
> На каком языке устройство — такое и ляжет. Это не критично, пока нет кастомных словарей,
> но при рефакторинге нужно учитывать, что `name` станет editable полем.

### Reducer (CreateDictionaryReducer)

```kotlin
class CreateDictionaryReducer : MateReducer<CreateDictionaryState, Msg, Effect> {
    override fun reduce(state: CreateDictionaryState, message: Msg): ReducerResult<CreateDictionaryState, Effect> {
        return when (message) {
            is Msg.ShowLangList -> state.loadLang(message.list) to setOf()
            is Msg.SelectLang -> state.selectLang(message.numericCode) to setOf()
            is Msg.SaveLang -> state.saveLang() to setOf(
                DatasourceEffect.SaveLangList(message.numericCode, message.langName)
            )
            is Msg.Close -> state.closeScreen() to setOf()
            is Msg.Empty -> state to setOf()
        }
    }
}
```

Редьюсер сам по себе чистый (в отличие от ChatReducer — без ResourceManager и Logger). Это хорошо. Но extension-функции (`loadLang()`, `selectLang()`, `saveLang()`, `closeScreen()`) определены прямо в файле редьюсера, а не в `State.kt`. По текущей конвенции они должны быть в `State.kt`.

### Effects

```kotlin
sealed interface DatasourceEffect : Effect {
    object LoadLangList : DatasourceEffect
    data class SaveLangList(val numericCode: Int, val langName: String) : DatasourceEffect
}
```

Всего два эффекта — загрузка и сохранение. Простая логика. Но стоит заметить: `LoadLangList` на самом деле **не обращается к БД**. Он читает захардкоженный `LanguageData.langList` и маппит его в UI-модель через `FlagProvider`. Название "Datasource" вводит в заблуждение — это по сути маппинг статических данных.

### EffectHandler

```kotlin
class DatasourceEffectHandler(
    private val createDictionaryUseCase: CreateDictionaryUseCase,
) : MateEffectHandler<Msg, Effect>
```

- `LoadLangList` → читает `LanguageData.langList` (захардкоженный список), маппит каждый `Country` в `PresetLangUi` (подтягивая drawable флага через `FlagProvider`), шлёт `ShowLangList`
- `SaveLangList` → вызывает `addLang()` (запись в Room) + `saveCurrentLang()` (запись numericCode в DataStore prefs), шлёт `Close`

**Критический момент:** нет try-catch. Если `addLang()` упадёт (например, UNIQUE constraint — язык уже добавлен), эффект-хендлер крашнется и экран зависнет без обратной связи.

### UseCase

```kotlin
interface CreateDictionaryUseCase {
    suspend fun getFlagRes(numericCode: Int): Int
    suspend fun addLang(numericCode: Int, name: String): Long
    suspend fun saveCurrentLang(numericCode: Int)
}
```

**Реализация** (`app/.../CreateDictionaryUseCaseImpl`):
- `getFlagRes` → `FlagProvider` (библиотека blongho/country-data — даёт drawable по numericCode страны)
- `addLang` → `CoreDbApi.LangApi.addLang()` → Room INSERT в таблицу `languages`
- `saveCurrentLang` → `PrefsProvider.setInt(CURRENT_LANG_NUMERIC_CODE_INT, ...)` — запоминает текущий активный язык

Реализация лежит в app-модуле, как и все остальные UseCaseImpl (известная архитектурная проблема, см. Backlog).

### ViewModel

```kotlin
class CreateDictionaryViewModel(
    createDictionaryUseCase: CreateDictionaryUseCase,
) : ViewModel(), MateStateHolder<CreateDictionaryState, Msg> {
    private val stateHolder = Mate(
        initState = CreateDictionaryState(),
        initEffects = setOf(DatasourceEffect.LoadLangList),
        coroutineScope = viewModelScope,
        reducer = CreateDictionaryReducer(),
        effectHandlerSet = setOf(DatasourceEffectHandler(createDictionaryUseCase))
    )
}
```

Стандартная Mate-обвязка. При создании сразу запускает `LoadLangList`. UiEffectHandler отсутствует — нет snackbar'ов и UI-эффектов. Это упрощение, но из-за этого и нет обратной связи при ошибках.

---

## UI

### Дерево компонентов

```
Box (fullscreen, white background)
├── if loading → LoadingWidget (CircularProgressIndicator)
└── if loaded  → LangPickerWidget
                  ├── LangListWidget
                  │   ├── ListHeaderWidget ("Создание словаря" / "Выберите язык")
                  │   └── LazyColumn
                  │       └── LanguageItemWidget (для каждого языка)
                  │           ├── ImageFlagWidget (флаг)
                  │           ├── Text (название языка)
                  │           └── Icon (ic_selected, если выбран)
                  ├── Spacer(weight=1f)
                  ├── PrimaryFullButtonWidget ("Создать", disabled пока не выбран язык)
                  └── Spacer(16dp)
```

Экран не использует `Scaffold` — нет topBar, нет навигации назад. Это осознанное решение для onboarding-сценария (первый запуск), но при открытии из настроек пользователь не может вернуться назад кроме как системным жестом.

### Поведение

1. Экран открывается → loading spinner (`LoadLangList` эффект)
2. Показывается список из 6 языков: English, Spanish, French, German, Italian, Portuguese
3. Пользователь тапает язык → выделяется (toggle), кнопка "Создать" активируется
4. Повторный тап → снимает выделение, кнопка деактивируется
5. Можно выбрать только **один** язык — `selectedNumericCode: Int?`, не список
6. Тап "Создать" → сохранение в БД + prefs → экран закрывается

**Нюанс:** loading spinner показывается пока маппится захардкоженный список из 6 элементов. На практике это мгновенно — spinner мелькает на долю секунды. Единственная причина задержки — `FlagProvider.getFlagRes()` использует `suspendCoroutine`, хотя `World.getFlagOf()` синхронный.

---

## Данные

### Зачем Room, если список захардкожен?

Список доступных языков (6 штук) захардкожен в `LanguageData`. Но Room используется для другого — хранить **какие языки пользователь уже добавил** (т.е. созданные словари). Таблица `languages` — это реестр активных словарей, не справочник.

Цепочка FK:
```
languages → words (через lang_id) → lexemes → write_quiz
```

Все слова привязаны к конкретному языку. Без записи в `languages` нельзя добавлять слова.

### Предустановленный список языков

```kotlin
object LanguageData {
    val langList = listOf(
        Country.ENGLISH,       // 826
        Country.SPANISH,       // 724
        Country.FRENCH,        // 250
        Country.GERMAN,        // 276
        Country.ITALIAN,       // 380
        Country.PORTUGUESE,    // 620
    )
}
```

Это **не** данные из БД. Это UI-список для выбора. Маппится в `PresetLangUi` с флагами и строковыми ресурсами.

### Entity

```kotlin
data class PresetLangUi(
    @DrawableRes val flagRes: Int,        // Drawable флага страны
    val countryNumericCode: Int,          // ISO 3166-1 numeric code
    @StringRes val langNameRes: Int,      // R.string.lang_english и т.д.
)
```

Используется только на этом экране. Маппинг `Country → PresetLangUi` происходит в `DatasourceEffectHandler` — каждому `Country` подтягивается `flagRes` через `FlagProvider` и `langNameRes` через `toLangNameRes()`.

### БД (Room)

```kotlin
@Entity(tableName = "languages")
data class LanguageDb(
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    val numericCode: Int,        // UNIQUE index
    val code: String,
    val name: String? = null,
    val addDate: Date,
    val changeDate: Date? = null,
)
```

**UNIQUE index на `numericCode`** — защита от дублирования на уровне БД. Но на уровне UI проверки нет: если пользователь попробует добавить English повторно, `addLang()` упадёт с SQLite UNIQUE constraint violation. Строка `lang_selection_error` ("Language already added") существует в ресурсах, но нигде не используется — обработка этого кейса не реализована.

---

## Строковые ресурсы

```xml
<string name="lang_selection_title">Creating a dictionary</string>
<string name="lang_selection_subtitle">Select language</string>
<string name="lang_english">English</string>
<string name="lang_german">German</string>
<string name="lang_french">French</string>
<string name="lang_spanish">Spanish</string>
<string name="lang_italian">Italian</string>
<string name="lang_portuguese">Portuguese</string>
<string name="lang_selection_button">Create</string>
<string name="lang_selection_error">Language already added</string>  <!-- НЕ ИСПОЛЬЗУЕТСЯ -->
```

---

## Известные проблемы

1. **Краш при повторном добавлении языка.** `numericCode` имеет UNIQUE constraint в Room. Попытка добавить уже существующий язык → SQLite constraint violation → необработанное исключение → краш. Строка `lang_selection_error` заготовлена, но не подключена.

2. **Нет `@Immutable` на стейт-классах.** `CreateDictionaryState` и `LangState` без аннотаций — Compose не может гарантировать skip рекомпозиции.

3. **Extension-функции стейта в неправильном месте.** `loadLang()`, `selectLang()`, `saveLang()` определены в `Reducer.kt`, а не в `State.kt`. По конвенции (см. guides/state-and-extensions) должны быть в `State.kt`.

4. **`langName` резолвится в UI и передаётся в Message.** `context.getString(langNameRes)` вызывается в `LangPickerWidget`. Сейчас это не критично (name — user-defined строка, в будущем станет editable), но способ получения через локализованный ресурс зависит от локали устройства.

5. **Нет обработки ошибок в эффект-хендлере.** Если `addLang()` или `getFlagRes()` бросит исключение — экран зависнет. Нет `UiEffectHandler`, нет snackbar'а, нет retry.

6. **Один экран на три сценария без адаптации.** Первый запуск (onboarding), добавление из настроек, добавление из dropdown — везде одинаковый UX. Нет кнопки "назад" (нет topBar). Из настроек/dropdown пользователь может вернуться только системным жестом.

7. **Фиктивный loading.** Загрузка 6 захардкоженных элементов через `suspendCoroutine` — мгновенная операция, spinner мелькает бесполезно.

8. **Не фильтруются уже добавленные языки.** Список всегда показывает все 6 языков, даже если 5 из них уже добавлены. Нет визуальной индикации "уже добавлен".
