# Checklist

- ✅ Пользователь тапает "Конфиденциальность" в настройках → открывается WebView с Privacy Policy [spec]
  - ✅ Лог: ###Settings### navigate: privacy_policy [implement]
  - ✅ PrivacyPolicyWidget принимает onClick callback [implement]
  - ✅ SettingsTabScreen прокидывает onPrivacyPolicyClick [implement]
  - ✅ MainUiDeps.SettingsTabScreenDep имеет onPrivacyPolicyClick [implement]
  - ✅ Settings.kt: route "webview/{pageKey}" + WebPage enum резолвит url/title [implement]
- ✅ Пользователь нажимает Back на WebView экране → возврат в настройки [spec]
  - ✅ Лог: ###WebView### back: {pageKey} [implement]
- ✅ WebView показывает индикатор загрузки во время загрузки страницы → индикатор скрывается по завершении [spec]
  - ✅ Лог: ###WebView### loading: {url} [implement]
  - ✅ Лог: ###WebView### loaded: {url} [implement]
  - ✅ WebViewScreen: LinearProgressIndicator под AppBar [implement]
  - ✅ WebViewClient.onPageStarted → show progress, onPageFinished → hide [implement]
- ✅ При ошибке загрузки отображается сообщение "Не удалось загрузить страницу" + кнопка "Повторить" [spec]
  - ✅ Лог: ###WebView### error: {url} [implement]
  - ✅ Лог: ###WebView### retry: {url} (после нажатия "Повторить") [implement]
  - ✅ WebViewClient.onReceivedError → показать error state [implement]
  - ✅ Security: javaScriptEnabled=false, allowFileAccess=false [implement]

## Ручное тестирование

### Открытие Privacy Policy
1. Открыть приложение
2. Перейти в Settings (таб настройки)
3. Тапнуть пункт "Конфиденциальность"
4. Ожидание: открывается экран с AppBar ("Конфиденциальность" + кнопка Back), WebView загружает страницу
5. Логи:
   - `###Settings### navigate: privacy_policy`

### Навигация назад
1. Открыть Privacy Policy (шаги выше)
2. Нажать кнопку Back в AppBar
3. Ожидание: возврат на экран Settings
4. Логи:
   - `###WebView### back: privacy_policy`

### Индикатор загрузки
1. Открыть Privacy Policy
2. Ожидание: виден LinearProgressIndicator под AppBar
3. Дождаться загрузки страницы
4. Ожидание: индикатор исчезает, страница отображается
5. Логи:
   - `###WebView### loading: https://kilgoret.github.io/lexeme-docs/privacy-policy`
   - `###WebView### loaded: https://kilgoret.github.io/lexeme-docs/privacy-policy`

### Ошибка загрузки
1. Включить авиарежим
2. Открыть Privacy Policy
3. Ожидание: текст "Не удалось загрузить страницу" + кнопка "Повторить"
4. Выключить авиарежим
5. Нажать "Повторить"
6. Ожидание: страница загружается
7. Логи:
   - `###WebView### error: https://kilgoret.github.io/lexeme-docs/privacy-policy`
   - `###WebView### retry: https://kilgoret.github.io/lexeme-docs/privacy-policy`
   - `###WebView### loading: https://kilgoret.github.io/lexeme-docs/privacy-policy`
   - `###WebView### loaded: https://kilgoret.github.io/lexeme-docs/privacy-policy`
