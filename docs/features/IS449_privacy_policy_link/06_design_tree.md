# Design Tree

## Граф

```yaml
- id: 0
  file: modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/WebViewScreen.kt
  action: "+"
  depends: [6]

- id: 6
  file: modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/WebViewAppBar.kt
  action: "+"
  depends: []

- id: 1
  file: modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/widgets/settings/items/PrivacyPolicyWidget.kt
  action: "~"
  depends: []

- id: 2
  file: modules/screen/settingstab/src/main/java/me/apomazkin/settingstab/SettingsTabScreen.kt
  action: "~"
  depends: [1]

- id: 3
  file: modules/screen/main/src/main/java/me/apomazkin/main/MainUiDeps.kt
  action: "~"
  depends: []

- id: 4
  file: modules/screen/main/src/main/java/me/apomazkin/main/Settings.kt
  action: "~"
  depends: [0, 3]

- id: 5
  file: app/src/main/java/me/apomazkin/polytrainer/uiDeps/MainUiDepsProvider.kt
  action: "~"
  depends: [0, 2, 3, 4]
```

## Детали

### #0 WebViewScreen.kt [+]

> 📎 guide: docs/guides/navigation.md — "Зависимости через MainUiDeps, не напрямую"
> 📎 guide: docs/guides/ui-patterns.md — "Двухуровневый паттерн не применяется: нет ViewModel, стейт локальный"
> 📎 guide: docs/guides/code-style.md — "Формат логов ###СЛОВО###"

Новый composable-экран с WebView. Параметры: `url: String`, `title: String`, `logger: LexemeLogger`, `onBackPress: () -> Unit`. Без ViewModel — стейт (isLoading, isError) локальный через `remember`.

Логи:
- `###WebView### loading: {url}` — onPageStarted
- `###WebView### loaded: {url}` — onPageFinished
- `###WebView### error: {url}` — onReceivedError
- `###WebView### retry: {url}` — нажатие "Повторить"
- `###WebView### back: {pageKey}` — onBackPress

Содержит:
- Scaffold с `WebViewAppBar(title, onBackPress)` в topBar
- LinearProgressIndicator под AppBar (видим во время загрузки)
- AndroidView с WebView (javaScriptEnabled=false, allowFileAccess=false)
- WebViewClient: onPageStarted → показать progress, onPageFinished → скрыть progress, onReceivedError → показать ошибку
- Error state: текст "Не удалось загрузить страницу" + кнопка "Повторить" (перезагрузка URL)
- Навигация по ссылкам: только домен `kilgoret.github.io`, остальное → системный браузер

### #6 WebViewAppBar.kt [+]

> 📎 guide: docs/guides/ui-patterns.md — "AppBar всегда отдельный виджет"

AppBar с заголовком и кнопкой Back. Параметры: `title: String`, `onBackPress: () -> Unit`.
Содержит только `TopAppBar(title = { Text(title) }, navigationIcon = { IconButton(onClick = onBackPress) })`.

### #1 PrivacyPolicyWidget.kt [~]

**Было:**
```kotlin
@Composable
fun PrivacyPolicyWidget() {
    SettingsItemWidget(
        iconRes = R.drawable.ic_privacy_policy,
        titleRes = R.string.settings_section_privacy_policy,
        onClick = null,
    )
}
```

**Стало:**
```kotlin
@Composable
fun PrivacyPolicyWidget(
    onClick: () -> Unit,
) {
    SettingsItemWidget(
        iconRes = R.drawable.ic_privacy_policy,
        titleRes = R.string.settings_section_privacy_policy,
        onClick = onClick,
    )
}
```

### #2 SettingsTabScreen.kt [~]

**Было:**
```kotlin
@Composable
fun SettingsTabScreen(
    onLangManagementClick: () -> Unit,
    onAboutAppClick: () -> Unit,
    ...
)
```

**Стало:**
```kotlin
@Composable
fun SettingsTabScreen(
    onLangManagementClick: () -> Unit,
    onAboutAppClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    ...
)
```

Прокинуть `onPrivacyPolicyClick` в `PrivacyPolicyWidget(onClick = onPrivacyPolicyClick)`.

### #3 MainUiDeps.kt [~]

> 📎 guide: docs/guides/navigation.md — "Передача зависимостей через MainUiDeps, не напрямую из appComponent"

**Было:**
```kotlin
@Composable
fun SettingsTabScreenDep(
    onLangManagementClick: () -> Unit,
    onAboutAppClick: () -> Unit,
)
```

**Стало:**
```kotlin
@Composable
fun SettingsTabScreenDep(
    onLangManagementClick: () -> Unit,
    onAboutAppClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
)

@Composable
fun WebViewScreenDep(
    pageKey: String,
    onBackPress: () -> Unit,
)
// logger прокидывается внутри MainUiDepsProvider, не в interface
```

### #4 Settings.kt [~]

> 📎 guide: docs/guides/navigation.md — "Навигационные функции — private extensions на NavController"
> 📎 guide: docs/guides/navigation.md — "Аргументы через navArgument с явным типом NavType.StringType"

**Было:** только routes `settings` и `about_app`.

**Стало:** добавить route `webview/{pageKey}` + composable с `arguments = listOf(navArgument("pageKey") { type = NavType.StringType })`. Навигация через private extension `goToWebView(pageKey)`. pageKey извлекается из `backStackEntry.arguments?.getString("pageKey")` и передаётся в `mainUiDeps.WebViewScreenDep(pageKey, onBackPress)`.

```kotlin
private const val WEBVIEW_ROUTE = "webview/{pageKey}"
```

### #5 MainUiDepsProvider.kt [~]

**Было:** `SettingsTabScreenDep` без `onPrivacyPolicyClick`.

**Стало:** добавить `onPrivacyPolicyClick` в `SettingsTabScreenDep`. Реализовать `WebViewScreenDep(pageKey, onBackPress)` — резолвить `pageKey` через `WebPage` enum в url/title, вызвать `WebViewScreen(url, title, logger, onBackPress)`.

Лог: `###Settings### navigate: {pageKey}` — при вызове навигации в `onPrivacyPolicyClick`.

```kotlin
enum class WebPage(val url: String, val titleRes: Int) {
    PRIVACY_POLICY(
        url = "https://kilgoret.github.io/lexeme-docs/privacy-policy",
        titleRes = R.string.settings_section_privacy_policy
    )
}
```

## Заметки

- String resources в `core-resources/strings.xml`:
  - `webview_error_title` — en: "Failed to load page" / ru: "Не удалось загрузить страницу"
  - `webview_retry` — en: "Retry" / ru: "Повторить"
- WebViewScreen без ViewModel — стейт (isLoading, isError) локальный через `remember`
- Preview: для WebView с AndroidView preview не работает — skip или preview только error state
