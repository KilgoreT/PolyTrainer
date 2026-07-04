# Infra test decision: IS481 component_constructor phase 2

## Решение: тесты не нужны

Infrastructure sub-flow (phase 2, iteration 1) состоит из 4 узлов (см. `infra_design_tree.md`). Ни один из них не требует unit-тестов. Обоснование по узлам:

### Узел 1 — NEW `:modules:core:logger/LogTags.kt`

```kotlin
object LogTags {
    const val COMPONENT_CONSTRUCTOR: String = "###ComponentConstructor###"
}
```

Typed `const val` без логики. Поведения нет — есть literal константа. Тестировать `assertEquals("###ComponentConstructor###", LogTags.COMPONENT_CONSTRUCTOR)` = тавтология, дублирующая определение. Согласно `code-style.md` (минимализм / YAGNI) и `testing-extensions.md` («Тесты расширений: основная функциональность... Given/When/Then») — нечего проверять.

### Узлы 2, 3 — `:modules:screen:components_manager/build.gradle.kts` и `:modules:screen:per_dictionary_components/build.gradle.kts`

Добавление одной строки `implementation(project(":modules:widget:component_widgets"))` в Gradle build-файл. Это не Kotlin-код, unit-тестами не покрывается. Корректность dep автоматически валидируется Gradle при `assembleDebug` / `compileDebugKotlin` (если import из `:modules:widget:component_widgets` не разрешится — build падает). Это и есть «тест» данного изменения, на CI он выполняется в pipeline `Build APK`.

### Узел 4 — `Migration_012_to_013.kt` (per-step `android.util.Log.d` вызовы)

Изменение **logging-only**: добавляются `Log.d(LogTags.COMPONENT_CONSTRUCTOR, "step N <name>: ok")` вызовы между уже существующими step-функциями. Schema (columns / indices / constraints) и порядок DDL/DML не меняются — `state.md` для M13 идентичен phase 1 baseline. Существующий `Migration_012_to_013Test.kt` (если присутствует) продолжает работать без правок и подтверждает schema-стабильность.

Дополнительные unit-тесты «лог эмитнут» технически возможны, но:
- Требуют LogSink-spy infrastructure, которой в проекте нет.
- `android.util.Log` не moc'ается без Robolectric (статический Android-метод; здесь — known violation `logging.md`, обоснованная в `infra_design_tree.md` § Узел 4 Возражение, см. backlog item для будущего рефакторинга Migration → DI).
- Smoke-фильтрация `adb logcat | grep '###ComponentConstructor###'` — это integration / manual QA-плоскость, не unit.

Соотношение цена/польза: тесты потребуют setup инфраструктуры под одно поведение «строка ушла в logcat», которое верифицируется ручным smoke за 1 минуту. Согласно `code-style.md` («Минимализм API (YAGNI)») — не делать.

### Aggregate verdict

Infrastructure phase 2 = pure configuration (deps) + typed const (LogTags) + logging-only поверх неизменной миграции. Бизнес-поведения не добавляется, behavioral surface не расширяется. Существующие migration tests (если есть) покрывают schema-инвариант M12→M13. **Pre-implementation тесты не требуются.**

Behavioral testing будет покрыт в business / data sub-flow:
- `editComponent` ветки (Success / NameEmpty / Collision / CardinalityDowngradeBlocked / TemplateImmutable / BuiltInProtected / Removed / Failure) — `app/src/test/.../ComponentsManagerUseCaseImplTest.kt`.
- Reducer Edit-ветка + mutual exclusion (F138) + chip-staleness + cardinality preview edge-cases (F023) — `ComponentsManagerReducerTest.kt` / `PerDictionaryComponentsReducerTest.kt`.

## log_messages

- infra_test (iter 1): тесты НЕ требуются — 4 узла (LogTags const, 2 build.gradle.kts deps, Migration logging-only). Behavioral changes отсутствуют; schema M13 стабильна; existing migration test покрывает invariant.
- Скип Pre-impl tests согласован с инструкцией шага test.md § 1 ("Тесты НЕ нужны если: Изменение внутренней реализации без изменения поведения / Конфигурационные изменения без логики").

_model: claude-opus-4-7[1m]_
