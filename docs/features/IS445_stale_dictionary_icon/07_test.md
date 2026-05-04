# IS445 — Тесты: Устаревшая иконка словаря в appbar picker

## Файл

`app/src/test/java/me/apomazkin/polytrainer/di/module/widget/DictionaryAppBarUseCaseImplTest.kt`

## Тест-кейсы

### flowCurrentDict — реактивность (основной фикс)

| # | Кейс | Что проверяет |
|---|------|---------------|
| 1 | emits current dict matching prefs ID | Базовый сценарий: combine(prefs, dictList) возвращает словарь по ID |
| 2 | emits updated dict when dictionary data changes without ID change | **Ключевой тест бага:** при изменении данных словаря (смена флага) без смены ID — Flow эмитит обновление |
| 3 | emits new dict when prefs ID changes | Существующее поведение: переключение словаря через prefs |
| 4 | falls back to first dict when prefs ID not found | Fallback: ID не найден в списке — берём первый |
| 5 | throws DictionaryNotFoundException when list is empty | Ошибка: пустой список → exception |
| 6 | maps numericCode to flagRes via CountryProvider | Маппинг numericCode → flagRes через CountryProvider |
| 7 | flagRes=0 when numericCode is null | Словарь без флага → flagRes=0, numericCode=0 |

### flowAvailableDict

| # | Кейс | Что проверяет |
|---|------|---------------|
| 8 | emits mapped list from Room Flow | Список словарей маппится корректно |

### changeDict

| # | Кейс | Что проверяет |
|---|------|---------------|
| 9 | writes id to prefs | Запись ID в SharedPreferences |

## Подход

- Тесты написаны по контракту (04_contract.md) и спеке (01_spec.md), не по текущему коду
- Используются `MutableStateFlow` для имитации prefs и Room — проверяем реактивность combine
- Тест #2 — ключевой: эмитим новый список словарей (с изменённым numericCode) без смены prefs ID → проверяем что Flow эмитит обновление
- Тесты могут не компилироваться до реализации (flowCurrentDict() пока использует map, не combine)

_model: claude-opus-4-6_
