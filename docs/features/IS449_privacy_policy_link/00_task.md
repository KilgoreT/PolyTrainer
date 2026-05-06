# Task

## IS449. Privacy Policy: open link from settings

Пункт «Конфиденциальность» в настройках должен открывать экран с WebView,
в котором загружается страница политики конфиденциальности.

## Требования

- WebView экран — универсальный, принимает URL извне (не захардкожен на privacy policy)
- Расположение: модуль `settingstab` (пока)
- AppBar с заголовком и кнопкой Back
- Заголовок AppBar — передаётся извне (вместе с URL)
- URL: `https://kilgoret.github.io/lexeme-docs/privacy-policy`
- Пользователь остаётся внутри приложения (не открывать браузер)

## Навигация

Settings → тап "Конфиденциальность" → WebView Screen (title + url) → Back → Settings
