# Task

## IS471. Refactor: Dagger DI graph analysis and restructuring

Анализ текущего Dagger DI графа и рефакторинг. Устранение архитектурных проблем.

## Текущие проблемы (из бэклога)

1. **Service Locator вместо DI.** `Context.appComponent` — service locator anti-pattern. Каждый composable сам достаёт зависимости.

2. **MainUiDeps — god-object.** Интерфейс с 7+ `@Composable` методами, реализация с 11+ параметрами. Растёт с каждой фичей.

3. **UseCaseImpl в app-модуле.** 9 реализаций в `app/di/module/`. App знает про ВСЕ реализации, фичи не независимы.

4. **LoggerComponent создаётся отдельно.** Вынесен из AppComponent чтобы решить циклическую зависимость с RoomComponent. Работает, но добавляет сложность.

5. **CoreDbComponent.init/get — два метода.** `init(context, logger)` для первого вызова, `get(context)` для последующих. Хрупко — если забыть init, RuntimeException.

## Текущий граф

См. `docs/guides/dagger-di.md`

```
LoggerComponent → RoomComponent → CoreDbComponent → AppComponent
```

## Цели

- Понять что можно улучшить без полного переписывания
- Определить порядок рефакторинга (что первым, что зависит от чего)
- Каждый шаг — отдельная задача в бэклоге
