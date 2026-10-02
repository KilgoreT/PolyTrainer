# IS521 | Аналитика: переименование слова в карточке

## Корень

Цепочка вызова при сохранении слова из карточки:

1. `wordcard/mate/DatasourceEffectHandler.kt` — эффект
   `DatasourceEffect.UpdateWord` → `wordCardUseCase.updateWord(...)`. Хендлер
   выполняется в потоке раннера mate — **главном** (без `withContext`).
2. `app/.../wordCard/WordCardUseCaseImpl.updateWord` →
   `wordApi.updateWordSuspend(wordId, value.trim())`.
3. `core-db-impl/CoreDbApiImpl.WordApiImpl.updateWordSuspend` —
   `suspend`: читает слово `wordDao.getWordSuspend(id)` (настоящий
   `suspend`, Room сам уходит в свой пул) и пишет
   `wordDao.updateWorldSuspend(wordDb)`.
4. `core-db-impl/room/WordDao.kt:89`:

   ```kotlin
   @Update
   fun updateWorldSuspend(wordDb: WordDb): Int   // НЕ suspend, вопреки имени
   ```

   Блокирующий Room-метод, вызванный в главном потоке, → Room бросает
   `IllegalStateException: Cannot access database on the main thread`.
   `UpdateWord` — `RecoverableEffect`, исключение превращается в
   `Msg.OperationFailed(word_card_error_save_word)` → снекбар «Не удалось
   сохранить».

## Почему сломалось и когда

- В IS481 (`52c7c18f`, 2026-07-03) из обработки эффектов карточки убран
  `withContext(Dispatchers.IO)` — по истории коммита (`-val msg: Msg =
  withContext(Dispatchers.IO) {`). Остальные эффекты карточки работают на
  главном потоке нормально, потому что все их DAO-методы — настоящие
  `suspend` (Room переключает поток сам). Единственный блокирующий вызов в
  этом пути — `updateWorldSuspend`, он и падает.
- Список слов (`wordstab`) тот же `updateWordSuspend` зовёт внутри
  `withContext(io)` — там переименование работает, поэтому баг виден только
  в карточке.

## Решение (рекомендую)

**Сделать DAO-метод настоящим `suspend`:**

```kotlin
@Update
suspend fun updateWordSuspend(wordDb: WordDb): Int
```

- Room сам выполнит запись в своём пуле → вызов безопасен из любого потока,
  включая главный. Чинит корень, а не симптом: любой будущий вызов тоже
  безопасен.
- Заодно опечатка `updateWorld` → `updateWord` (один вызов в
  `CoreDbApiImpl`).
- Публичный `CoreDbApi.WordApi.updateWordSuspend` уже `suspend` — API
  наружу не меняется, правки только в `core-db-impl`.

**Отвергнутая альтернатива:** обернуть `UpdateWord` в карточке в
`withContext(Dispatchers.IO)`. Чинит один вызов, оставляет ловушку: метод
с именем `…Suspend` остаётся блокирующим, следующий вызов из главного потока
упадёт так же.

## Родственное (тот же класс дефекта) — чиним в этой задаче

- Аудит `core-db-api` + `core-db-impl`: методов с именем `…Suspend` без
  `suspend` ровно два — `updateWorldSuspend` и `addWordSuspend` (DAO и
  `CoreDbApi.WordApi`).
- `addWordSuspend` сейчас не падает (единственный вызов
  `WordsTabUseCaseImpl.addWord` идёт под `withContext(io)`), но это та же
  ловушка. **Решение юзера (ревью 2026-10-02): чинить сейчас, не в Backlog.**
  `WordDao.addWordSuspend` и `CoreDbApi.WordApi.addWordSuspend` →
  `suspend`; вызывающий `addWord` уже `suspend`. Попутно — моки
  (`every` → `coEvery`) и androidTest-хелперы, вызывающие DAO вне корутины.

## Тесты

- **Регрессия (androidTest, `core-db-impl`):** вызвать
  `WordApi.updateWordSuspend` из главного потока
  (`runOnMainSync { runBlocking { … } }`) → без исключения, значение слова в
  БД обновлено. До фикса тест падает с тем же `IllegalStateException` —
  проверю это перед фиксом. Тест попадает в CI-джоб эмулятора (IS517).
- Юнит-тесты карточки (reducer/хендлер) не меняются — логика эффекта та же.

## Проверка на девайсе (`.dev`)

- Переименование из карточки → новое значение в карточке и в списке, без
  снекбара; в логе нет `Cannot access database on the main thread`.
- Переименование из списка слов — по-прежнему работает.

## Риски

- Минимальные: меняется сигнатура внутреннего DAO-метода (один вызов).
  Схема БД и миграции не затрагиваются.
