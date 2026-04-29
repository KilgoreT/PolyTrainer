# IS441. Задача 2 — Два режима экрана: onboarding vs management

## Описание

Экран создания словаря используется из двух контекстов:
- **Onboarding** (первый запуск) — нет кнопки назад, после создания → главный экран
- **Management** (настройки, dropdown) — AppBar с кнопкой назад, после создания → popBackStack

Сейчас экран не различает эти сценарии. Нет AppBar, нет навигации назад.

## Что сделать

1. Добавить nav argument `isOnboarding: Boolean` в route `CREATE_DICTIONARY`
2. Передать `isOnboarding = true` из Splash, `isOnboarding = false` из mainRouter (openAddDict)
3. В `CreateDictionaryScreen` получить аргумент и пробросить в State или напрямую в UI
4. Если `isOnboarding = false` → показать AppBar с иконкой назад
5. Если `isOnboarding = true` → без AppBar (как сейчас)
6. onClose: onboarding → `openMainScreen()`, management → `popBackStack()`

## Критерии приёмки

- [ ] Из splash → экран без AppBar, после создания → главный экран
- [ ] Из настроек → экран с AppBar и кнопкой назад, после создания → возврат назад
- [ ] Из dropdown словаря → то же что из настроек
- [ ] Кнопка назад в AppBar → popBackStack (не создаёт словарь)
- [ ] Системный жест назад → то же что кнопка назад
- [ ] Билд проходит
- [ ] Юнит-тесты проходят
