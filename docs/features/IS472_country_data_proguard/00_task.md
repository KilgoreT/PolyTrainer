# Task

## IS472. Crash: NPE в country_data после R8 обфускации

Краш в продакшне. Версия 0.1.2 — первая с R8 обфускацией.

## Стектрейс

```
java.lang.NullPointerException: Attempt to invoke virtual method
  'java.lang.String java.lang.String.toLowerCase()' on a null object reference
    at com.blongho.country_data.WorldData.loadCurrencies(WorldData.java:236)
    at com.blongho.country_data.WorldData.loadAllData(WorldData.java:194)
    at com.blongho.country_data.WorldData.<init>(WorldData.java:64)
    at com.blongho.country_data.WorldData.getInstance(WorldData.java:91)
    at com.blongho.country_data.World.init(World.java:57)
    at me.apomazkin.flags.CountryProviderImpl.<init>(CountryProviderImpl.java:23)
```

## Суть

Библиотека `com.blongho:worldCountryData:v1.5.4-alpha-1` падает при инициализации.
`WorldData.loadCurrencies()` вызывает `String.toLowerCase()` на null.

R8 обфускация в 0.1.2 (`isMinifyEnabled = true`, `isShrinkResources = true`) вероятно:
- Strip'нула JSON-ресурсы библиотеки (raw/assets), от которых зависит `loadCurrencies()`
- Или обфусцировала имена полей в data-классах библиотеки, сломав JSON-парсинг (Gson reflection)

## Цепочка

```
Compose NavHost → MainRouter → appComponent.getVocabularyUseCase()
  → Dagger создаёт DictionaryTabUseCaseImpl
    → CountryProviderModule.provideCountryProvider()
      → CountryProviderImpl(context) → World.init(context)
        → WorldData.loadAllData() → loadCurrencies() → NPE
```

## Затронутые компоненты

| Файл | Роль |
|------|------|
| `modules/library/flags/.../CountryProviderImpl.kt` | Инициализация `World.init(context)` |
| `app/.../di/module/flags/CountryProviderModule.kt` | Dagger module, @Singleton |
| `app/proguard-rules.pro` | ProGuard/R8 правила — нет правила для country_data |
| `deps/other-libs.versions.toml` | Версия: `v1.5.4-alpha-1` |

## Корневая причина

В `proguard-rules.pro` нет keep-правила для библиотеки `com.blongho.country_data`. R8 при `isMinifyEnabled = true`:
1. Обфусцирует классы библиотеки (переименовывает поля)
2. Библиотека использует Gson для парсинга JSON из raw-ресурсов
3. Gson полагается на reflection — имена полей должны совпадать с JSON ключами
4. После обфускации поле `currency_name` → `a` → Gson не находит → null
5. `String.toLowerCase()` на null → NPE

## Фикс

Добавить в `app/proguard-rules.pro`:

```
# country_data library — Gson reflection на JSON ресурсы
-keep class com.blongho.country_data.** { *; }
```

## Воспроизведение

- Debug build (без R8) — работает
- Release build (с R8) — краш при первом запуске
