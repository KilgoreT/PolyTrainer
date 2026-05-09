# Crash: CountryProviderImpl — NullPointerException в country_data

**Дата:** 2026-05-09 04:55:59
**Версия:** 0.1.2 (10002)
**Issue ID:** f4f86b34881dc48f4f14e47d18319585

## Fatal Exception

```
java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.String java.lang.String.toLowerCase()' on a null object reference
    at com.blongho.country_data.WorldData.loadCurrencies(WorldData.java:236)
    at com.blongho.country_data.WorldData.loadAllData(WorldData.java:194)
    at com.blongho.country_data.WorldData.<init>(WorldData.java:64)
    at com.blongho.country_data.WorldData.getInstance(WorldData.java:91)
    at com.blongho.country_data.World.init(World.java:57)
    at me.apomazkin.flags.CountryProviderImpl.<init>(CountryProviderImpl.java:23)
    at me.apomazkin.polytrainer.di.module.flags.CountryProviderModule.provideCountryProvider(CountryProviderModule.java:16)
    at me.apomazkin.polytrainer.di.DaggerAppComponent$AppComponentImpl.dictionaryTabUseCaseImpl(DaggerAppComponent.java:107)
    at me.apomazkin.polytrainer.di.DaggerAppComponent$AppComponentImpl.getVocabularyUseCase(DaggerAppComponent.java:204)
    at me.apomazkin.polytrainer.route.MainRouterKt.mainRouter$lambda$1$lambda$0(MainRouter.kt:28)
```

## Описание

Краш при инициализации `CountryProviderImpl`. Библиотека `com.blongho.country_data` (World.init → WorldData.loadAllData → loadCurrencies) падает с NPE — `String.toLowerCase()` на null. 

Проблема в сторонней библиотеке: `WorldData.loadCurrencies()` (строка 236) получает null вместо строки валюты. Вероятно R8 обфускация удалила/переименовала JSON-ресурс или поле, от которого зависит библиотека.

## Цепочка вызовов

1. Compose NavHost → MainRouter → `appComponent.getVocabularyUseCase()`
2. Dagger создаёт `DictionaryTabUseCaseImpl` → нуждается в `CountryProvider`
3. `CountryProviderModule.provideCountryProvider()` → `CountryProviderImpl(context)`
4. `CountryProviderImpl.<init>` → `World.init(context)` → `WorldData.loadAllData()`
5. `WorldData.loadCurrencies()` → NPE на `String.toLowerCase()`

## Версия

0.1.2 — первая версия с R8 обфускацией (`isMinifyEnabled = true`). Возможно R8 strip'нул ресурсы библиотеки country_data.
