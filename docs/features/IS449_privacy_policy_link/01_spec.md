# Spec: WebView Screen

## Описание

Универсальный экран с WebView для отображения веб-страниц внутри приложения.
Первый use case — открытие Privacy Policy из настроек.

## Экран WebViewScreen

**Параметры:**
- `url: String` — URL для загрузки
- `title: String` — заголовок AppBar (stringRes или строка)
- `onBackPress: () -> Unit` — навигация назад

**UI:**
- AppBar: заголовок + кнопка Back (NavigationIcon)
- LinearProgressIndicator под AppBar — виден во время загрузки, скрывается по onPageFinished
- WebView: занимает всё пространство под AppBar
- JavaScript: отключен (не нужен для статического markdown)
- Ошибка загрузки: текст "Не удалось загрузить страницу" + кнопка "Повторить"

## Навигация

```
Settings (settingstab)
  → тап PrivacyPolicyWidget
    → onPrivacyPolicyClick()
      → navigate("webview/privacy_policy")
        → MainUiDepsProvider резолвит URL и title по ключу
          → WebViewScreen(url, title, onBackPress)
            → Back → popBackStack() → Settings
```

## Расположение файлов

| Файл | Модуль | Действие |
|------|--------|----------|
| `WebViewScreen.kt` | settingstab | создать |
| `Settings.kt` | main | добавить route + composable |
| `MainUiDeps.kt` | main | добавить WebViewScreenDep |
| `MainUiDepsProvider.kt` | app | реализовать WebViewScreenDep |
| `SettingsTabScreen.kt` | settingstab | прокинуть onPrivacyPolicyClick |
| `PrivacyPolicyWidget.kt` | settingstab | добавить onClick |

## Route

```kotlin
private const val WEBVIEW_ROUTE = "webview/{pageKey}"
```

`pageKey` — строковый ключ (например `"privacy_policy"`).
URL и title резолвятся в `app` модуле (MainUiDepsProvider) по ключу.
Это избавляет от проблем с URL-encoding в nav arguments.

## Security

- `allowFileAccess = false` — запрет доступа к файловой системе
- `javaScriptEnabled = false` — JS отключен
- Навигация ограничена доменом `kilgoret.github.io` — внешние ссылки открываются в системном браузере

## Зависимости

- `android.webkit.WebView` — стандартный Android SDK, дополнительных зависимостей не нужно
- `INTERNET` permission — уже есть (Firebase), отдельно добавлять не нужно

## Константы (app модуль)

```kotlin
enum class WebPage(val url: String, val titleRes: Int) {
    PRIVACY_POLICY(
        url = "https://kilgoret.github.io/lexeme-docs/privacy-policy",
        titleRes = R.string.settings_section_privacy_policy
    )
}
```

Хранить в `app` модуле. settingstab не знает про URL — только передаёт callback.
