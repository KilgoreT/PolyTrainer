# Review Findings: AssistedInject решение

Три ревью: Android Architect, Dagger DI Specialist, Compose Specialist.

---

## КРИТИЧНЫЕ

### 1. Документ не показывает полную цепочку Dagger-конфигурации

Не описано:
- Какие Module'ы создать для `@IntoMap` (по одному на каждый `@Inject` ViewModel)
- Где и как `MainUiDepsProvider` биндится в граф (`@Binds MainUiDeps`)
- Что добавить в `AppModule.includes`
- `ViewModelKey` аннотация

Без этого при реализации будут ошибки "не найден binding".

---

## ВАЖНЫЕ

### 4. ViewModel key для runtime параметров

`viewModel(factory = viewModelFactory { factory.create(wordId) })` — без `key`. Если WordCard для `wordId=1` и `wordId=2` в одном NavBackStackEntry (маловероятно, но возможно) — вернётся старый ViewModel.

**Решение:** добавить key:
```kotlin
viewModel(
    key = "wordCard_$wordId",
    factory = viewModelFactory { factory.create(wordId) }
)
```

Pre-existing проблема — есть и в текущем коде. Но при миграции стоит зафиксировать.

## ЗАМЕЧАНИЯ


---

## Сводка

| # | Проблема | Критичность | Решение |
|---|----------|-------------|---------|
| 1 | Неполная Dagger-конфигурация в доке | КРИТИЧНАЯ | Дополнить |
| 2 | ViewModel key для runtime params | ВАЖНАЯ | Добавить key |
| 3 | Постепенная миграция | ВАЖНАЯ | Инфраструктура → один простой экран → один с runtime → остальные |
