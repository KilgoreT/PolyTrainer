# WebView Screen

Универсальный экран с WebView для отображения веб-страниц внутри приложения.

## UI

- AppBar: заголовок + кнопка Back (NavigationIcon)
- LinearProgressIndicator под AppBar — виден во время загрузки, скрывается по onPageFinished
- WebView: занимает всё пространство под AppBar
- JavaScript отключен
- Ошибка загрузки: текст "Не удалось загрузить страницу" + кнопка "Повторить"

## Параметры экрана

- `url: String` — URL для загрузки
- `title: String` — заголовок AppBar
- `onBackPress: () -> Unit` — навигация назад

## Навигация

Route: `webview/{pageKey}`

`pageKey` — строковый ключ. URL и title резолвятся в `app` модуле (MainUiDepsProvider) по ключу.

Пример: тап PrivacyPolicyWidget в Settings → navigate("webview/privacy_policy") → WebViewScreen → Back → popBackStack() → Settings.

## Security

- `allowFileAccess = false`
- `javaScriptEnabled = false`
- Навигация ограничена доменом `kilgoret.github.io` — внешние ссылки открываются в системном браузере

## Доступные страницы

| Ключ | URL | Заголовок |
|------|-----|-----------|
| `privacy_policy` | `https://kilgoret.github.io/lexeme-docs/privacy-policy` | Privacy Policy (stringRes) |

## Модули

- `settingstab` — WebViewScreen.kt, WebViewAppBar.kt, PrivacyPolicyWidget
- `main` — route в Settings.kt, WebViewScreenDep в MainUiDeps
- `app` — MainUiDepsProvider, WebPage enum с URL и titleRes
