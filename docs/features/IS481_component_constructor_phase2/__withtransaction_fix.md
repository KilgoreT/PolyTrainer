# Fix `database.withTransaction` regression на bundled SQLite driver

## Юзерскими словами

### Что сломано

Не сохраняется новый перевод для лексемы. И не только перевод — **9 операций с базой** падают молча в логах (юзер видит «ничего не произошло» либо снэкбар «Failed»):

- Сохранить перевод
- Сохранить определение (или любой user-defined компонент) у лексемы
- Создать лексему с переводом за один шаг
- Добавить второе значение к существующему компоненту
- Обновить значение существующего компонента
- Переименовать компонент через Edit
- Soft-удалить компонент (через trash-icon)
- Edit компонента (phase 2)
- Внутри soft-delete — сбросить prefs picker'а

То есть **базовое использование приложения сломано**. Без этого юзер не может ничего ввести.

### Почему сломано

В IS481 prereq мы переключили работу с базой на новый драйвер (BundledSQLiteDriver — встроенная версия SQLite, не системная). Это была правильная архитектура. Но **одна важная функция Room** (`withTransaction` — обертка для нескольких SQL-операций в одну атомарную сделку) **не была переписана под новый драйвер**. Она пытается дёрнуть старый механизм, его теперь нет — крах.

Это **regression от IS481 prereq**, не от phase 2. Просто пока никто не ткнул в save translation после prereq install — никто не заметил. Manual smoke prereq (acceptance 6.2/6.3) тоже не был пройден до конца, поэтому regression проскочил незамеченным.

### План починки — без компромиссов

Никаких hot-fix. Никаких dual-mode. **Сразу правильно.**

Room 2.8.4 предоставляет правильный driver-aware API — `useWriterConnection { it.immediateTransaction { ... } }`. Это **прямая замена** старого `withTransaction { ... }`, одна строка → одна строка. DAO suspend методы внутри корректно используют ту же транзакцию через coroutine context — это гарантировано source Room (не предположение).

**План:**

1. Заменить во всех **9 точках** в `CoreDbApiImpl.kt` старый `database.withTransaction { ... }` на новый `database.useWriterConnection { it.immediateTransaction { ... } }`.
2. Убрать import `androidx.room.withTransaction`. Добавить `androidx.room.immediateTransaction` + `androidx.room.useWriterConnection`.
3. Прогнать `:app:testDebugUnitTest` + `:core:core-db-impl:testDebugUnitTest` — все существующие тесты должны остаться зелёными (поведение операций не меняется).
4. Манульный smoke: попробовать сохранить перевод через WordCard — должно работать.

### Что НЕ трогаю в этой итерации

- **`Service.openDatabase()` + `getDbInfo()`** в `CoreDbApiImpl.kt:77-99` тоже используют legacy `openHelper`. Они дёргаются только из Settings debug-экрана. Это отдельный пункт — в `Backlog.md` после fix'а основной проблемы.
- **Версию Room не обновляю** — 2.8.4 уже последняя стабильная. Driver-aware API в ней есть.

### Риски

1. **DAO suspend методы внутри `useWriterConnection { it.immediateTransaction { ... } }` гарантированно работают через тот же transaction** — verified через source Room (`DBUtil.android.kt:111-143` — `performSuspending` → `compatCoroutineExecute` → `withContext(getCoroutineContext(inTransaction))` → если есть `TransactionElement` в coroutine context, DAO работает через тот же dispatcher). То есть semantics транзакции сохраняются.
2. **Manual smoke critical** — code-level тесты на mock'ах не покрывают runtime-проблему. Только реальное приложение на девайсе подтвердит fix.
3. **Не fixed legacy `openHelper` в Service** — Settings debug-экран остаётся broken, но это редко используется.

---

## Детали для программистов

### Точная ошибка

```
java.lang.IllegalStateException: Cannot return a SupportSQLiteOpenHelper since no SupportSQLiteOpenHelper.Factory was configured with Room.
    at androidx.room.RoomDatabase.getOpenHelper(RoomDatabase.android.kt:134)
    at androidx.room.RoomDatabase.internalBeginTransaction(RoomDatabase.android.kt:695)
    at androidx.room.RoomDatabase.beginTransaction(RoomDatabase.android.kt:690)
    at androidx.room.RoomDatabaseKt__RoomDatabase_androidKt$withTransaction$2.invokeSuspend(RoomDatabase.android.kt:2042)
    ...
```

Triggered at `WordCardUseCaseImpl.addLexemeWithBuiltInComponent` → `LexemeApiImpl.addLexemeWithBuiltInComponent` → `database.withTransaction { ... }` → `beginTransaction()` → `getOpenHelper()` → throws.

### Verified через Room 2.8.4 source

**`androidx.room.withTransaction` extension — НЕ driver-aware.** `room-runtime-android/2.8.4/RoomDatabase.android.kt:2040-2050`:

```kotlin
public suspend fun <R> RoomDatabase.withTransaction(block: suspend () -> R): R =
    withTransactionContext {
        @Suppress("DEPRECATION") beginTransaction()  // ← legacy
        try {
            val result = block.invoke()
            @Suppress("DEPRECATION") setTransactionSuccessful()
            result
        } finally {
            @Suppress("DEPRECATION") endTransaction()
        }
    }
```

В отличие от `runInTransaction` (line 770-786) у которого есть `if (inCompatibilityMode()) { beginTransaction() } else { performBlocking(...) }` branch — suspend `withTransaction` всегда дёргает deprecated `beginTransaction()` → `openHelper.writableDatabase` → `getOpenHelper()` → бросает `IllegalStateException` в driver-only mode.

KDoc Room сам признаёт (line 2179): *«behaviour in withTransaction when Room is in compatibility mode executing driver transactions»* — то есть `withTransaction` нужна compatibility mode для работы.

**Документированная замена** (KDoc на `runInTransaction`, line 745, 760):

> «If a [SQLiteDriver] is configured with this database, then it is best to use **[useWriterConnection]** along with **[immediateTransaction]** to perform transactional operations.»

### API replacement

```kotlin
import androidx.room.useWriterConnection      // вместо androidx.room.withTransaction
import androidx.room.immediateTransaction

// Было:
suspend fun foo(): Long = database.withTransaction {
    val a = dao1.someSuspendMethod()
    dao2.anotherSuspendMethod(a)
}

// Стало:
suspend fun foo(): Long = database.useWriterConnection { transactor ->
    transactor.immediateTransaction {
        val a = dao1.someSuspendMethod()
        dao2.anotherSuspendMethod(a)
    }
}
```

**Signatures (verified):**

- `androidx.room.useWriterConnection` (`RoomDatabase.kt:500`):
  ```kotlin
  public suspend fun <R> RoomDatabase.useWriterConnection(block: suspend (Transactor) -> R): R
  ```
- `androidx.room.immediateTransaction` (`Transactor.kt:132`):
  ```kotlin
  public suspend fun <R> Transactor.immediateTransaction(
      block: suspend TransactionScope<R>.() -> R
  ): R = withTransaction(SQLiteTransactionType.IMMEDIATE, block)
  ```

### Гарантия что DAO suspend методы используют переданный transactor

`androidx.room.util.performSuspending` (`DBUtil.android.kt:47-58`) — используется generated DAO Impl кодом (verified в `core-db-impl/build/generated/ksp/debug/.../WordDao_Impl.kt:342`):

```kotlin
public actual suspend fun <R> performSuspending(
    db: RoomDatabase,
    isReadOnly: Boolean,
    inTransaction: Boolean,
    block: (SQLiteConnection) -> R,
): R =
    db.compatCoroutineExecute(inTransaction) {
        db.internalPerform(isReadOnly, inTransaction) { connection -> ... }
    }
```

`compatCoroutineExecute` (`DBUtil.android.kt:111-119`):

```kotlin
private suspend inline fun <R> RoomDatabase.compatCoroutineExecute(
    inTransaction: Boolean, crossinline block: suspend () -> R,
): R {
    if (inCompatibilityMode() && isOpenInternal && inTransaction()) {
        return block.invoke()
    }
    return withContext(getCoroutineContext(inTransaction)) { block.invoke() }
}
```

В driver-only mode (`inCompatibilityMode() = false`) — выполняется `withContext(getCoroutineContext(inTransaction))`. `getCoroutineContext` (line 126-143):

```kotlin
internal actual suspend fun RoomDatabase.getCoroutineContext(inTransaction: Boolean): CoroutineContext {
    val transactionDispatcher = coroutineContext[TransactionElement]?.transactionDispatcher
    return if (inCompatibilityMode()) {
        // ...
    } else {
        getQueryContext() + (transactionDispatcher ?: EmptyCoroutineContext)
    }
}
```

**Если в coroutine context есть `TransactionElement`** (т.е. мы внутри `useWriterConnection { it.immediateTransaction { ... } }` блока) — `transactionDispatcher` будет non-null → DAO suspend методы работают через тот же dispatcher и transaction. ✓

То есть существующие suspend DAO calls внутри блока **работают как раньше**, ничего в DAO interface не нужно менять.

### Затронутые точки (9)

В `core/core-db-impl/src/main/java/me/apomazkin/core_db_impl/CoreDbApiImpl.kt`:

| Line | Метод | Что делает |
|---|---|---|
| 258 | `LexemeApiImpl.addLexemeWithBuiltInComponent` | INSERT lexeme + write_quiz + component_value (built-in) |
| 287 | `LexemeApiImpl.addLexemeWithUserDefinedComponent` | INSERT lexeme + write_quiz + component_value (user-defined) |
| 322 | `LexemeApiImpl.addLexemeWithComponents` | compound INSERT lexeme + N component_values |
| 375 | `LexemeApiImpl.addComponentValue` | INSERT single component_value к existing lexeme |
| 397 | `LexemeApiImpl.updateComponentValue` | UPDATE value+updatedAt |
| 517 | `LexemeApiImpl.renameComponentType` | UPDATE name + cascade quiz_configs |
| 555 | `LexemeApiImpl.softDeleteComponentType` (path A) | soft-delete + cascade quiz_configs |
| 615 | `LexemeApiImpl.editComponentType` (phase 2) | UPDATE name/template/isMulti + cardinality SELECT + cascade |
| 698 | `LexemeApiImpl.softDeleteComponentType` (path B) | повторный inline блок |

Также НЕ через `withTransaction`, но также падают в driver-only mode (см. `CoreDbApiImpl.kt:77-99`):
- `Service.openDatabase()` — `db.openHelper.writableDatabase.isOpen`
- `Service.getDbInfo()` — `db.openHelper.databaseName / readableDatabase.version / readableDatabase.path`

Эти **не в скоупе** текущего fix'а — отдельная задача в Backlog (затрагивают только Settings debug экран).

### Тест-план

1. **Unit tests без regressions:**
   - `./scripts/cc-build.sh :core:core-db-impl:testDebugUnitTest` — `WordDao` / DAO mock тесты должны остаться зелёными.
   - `./scripts/cc-build.sh :app:testDebugUnitTest` — `ComponentsManagerUseCaseImplTest` / `WordCardUseCaseImplTest` / `PerDictionaryComponentsUseCaseImplTest` через mock'и LexemeApi — не должны заметить изменений.
2. **Manual smoke на реальном девайсе:**
   - Сохранить перевод через WordCard (повторение исходного crash сценария).
   - Сценарии 1.1 / 1.4 / 1.7 / 3.1 из `__manual_smoke.md` — все они дёргают `withTransaction`.

### Записи в backlog после fix

- **`[IS481 phase 2 follow-up: переписать openHelper-вызовы в Service.openDatabase / getDbInfo на driver-aware API]`** — оставшийся cleanup для Settings debug экрана.
- **`[IS481 prereq follow-up: добавить smoke test для add translation в acceptance 6.2/6.3 — regression от bundled driver не был пойман]`** — process improvement (smoke не был пройден до конца на prereq).
- **`[IS481 retro: latent regression от prereq всплыл только через phase 2 use — нужно правило «после prereq миграции обязателен полный smoke базовых пользовательских операций до начала следующей фазы»]`** — process rule.

_model: claude-opus-4-7[1m]_
