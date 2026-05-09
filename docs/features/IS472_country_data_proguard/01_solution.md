# Solution: IS472 — ProGuard rule для country_data

## Корневая причина (подтверждена)

Библиотека `com.blongho:worldCountryData:v1.5.4-alpha-1`:

1. **Не содержит consumer proguard rules** — в AAR нет `consumer-rules.pro`. Есть `res/raw/keep.xml` — защищает только drawable (флаги), не классы.

2. **Использует Gson reflection** — `WorldData.loadCurrencies()` вызывает `new Gson().fromJson(json, Currency[].class)`. Gson маппит JSON-ключи на поля Java-классов **по имени** через reflection.

3. **R8 обфусцирует имена полей** — `Currency.country` → `a`. Gson не находит соответствия → поля = null. Далее `currency.getCountry().toLowerCase()` → NPE.

## Варианты

### Вариант A: Keep только data-классы

```proguard
-keep class com.blongho.country_data.Country { *; }
-keep class com.blongho.country_data.Currency { *; }
```

Минимальный scope — только два класса, десериализуемые через Gson.

| | |
|---|---|
| Плюсы | Точный scope, R8 оптимизирует остальное |
| Минусы | Если автор добавит новые data-классы с reflection — сломается |
| Размер | Минимальное влияние |

### Вариант B: Keep весь пакет

```proguard
-keep class com.blongho.country_data.** { *; }
```

Все классы библиотеки защищены.

| | |
|---|---|
| Плюсы | Пуленепробиваемо, работает при любых обновлениях |
| Минусы | R8 не оптимизирует код библиотеки (17 KB — пренебрежимо) |
| Размер | +17 KB worst case |

### Вариант C: Keep только поля

```proguard
-keepclassmembers class com.blongho.country_data.Currency { <fields>; }
-keepclassmembers class com.blongho.country_data.Country { <fields>; }
```

Сохраняет имена полей, но не имена классов/методов.

| | |
|---|---|
| Плюсы | Максимально точный |
| Минусы | Хрупкий — если библиотека ссылается на классы по строковым именам |
| Размер | Минимальное влияние |

## Рекомендация: Вариант B

Обоснование:
- Библиотека **17 KB** — разница в размере неизмеримо мала
- Версия **alpha** (`v1.5.4-alpha-1`) — может измениться, добавить классы
- Автор **не поставляет consumer rules** — ответственность на нас
- Один раз написать — не возвращаться при обновлениях

## Фикс

Файл: `app/proguard-rules.pro`

Добавить:
```proguard
# country_data — Gson reflection, нет consumer rules
-keep class com.blongho.country_data.** { *; }
```

## Верификация

1. Release build (`./gradlew assembleRelease`)
2. Установить на устройство
3. Открыть любой экран со словарями (DictionaryTab, QuizTab) — где инициализируется CountryProvider
4. Не должно быть NPE
