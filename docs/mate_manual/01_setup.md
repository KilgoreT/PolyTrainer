# Внедрение в проект

[← Оглавление](README.md)

## Репозиторий

Mate публикуется через JitPack. В `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        maven(url = "https://jitpack.io")
    }
}
```

## Зависимости

Прод-модули:

```kotlin
dependencies {
    implementation("com.github.KilgoreT.mate:mate-core:<version>")
    // Если используете навигацию-как-данные:
    implementation("com.github.KilgoreT.mate:mate-navigation:<version>")
}
```

Тесты:

```kotlin
dependencies {
    // Юниты раннера (виртуальное время, mateScope):
    testImplementation("com.github.KilgoreT.mate:mate-test:<version>")
    // Сценарный харнес (обычно только в app-модуле):
    testImplementation("com.github.KilgoreT.mate:mate-app-test:<version>")
}
```

Версии смотрите в тегах репозитория `github.com/KilgoreT/mate`.
Артефакты собираются под `androidTarget` и `jvm`; сценарные тесты
идут на jvm-таргете как обычные юнит-тесты.

## Паттерн «мост» для многомодульного проекта

Чтобы не прописывать JitPack-координаты в каждом экранном модуле,
удобно завести тонкий модуль-мост (например, `:core:mate`), который
отдаёт библиотеку транзитивно и хранит проектную обвязку (сахар для
reducer'ов, общие лог-теги):

```kotlin
// :core:mate/build.gradle.kts
dependencies {
    api("com.github.KilgoreT.mate:mate-core:<version>")
    api("com.github.KilgoreT.mate:mate-navigation:<version>")
}
```

Экранные модули зависят только от `:core:mate` — поднятие версии
библиотеки происходит в одном файле.

## Мок-движок для сценариев

`mate-app-test` сознательно НЕ тянет мок-библиотеку: чем стабить
use case'ы — выбор проекта. На jvm-тестах Android-проекта естественный
выбор — mockk; KMP-проект может взять Mokkery или рукописные фейки.
Харнесу всё равно — он принимает готовые объекты.

---

Дальше: [Основные понятия](02_concepts.md)
