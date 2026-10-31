# IS536 | HLD WCL — диалог карточки слова, черновик

Диалог лексем-кандидатов в карточке слова по спеке WCL. На этапе 2
меняется только запрос: он уходит леммой без контекста на
`lexemes/from-lemma` (Д114), диалог, стейт, редьюсер и запись — как в
первой версии. Остальные тезисы WCL v2 — после этапа 2, отдельным
заходом. Сервер — `hld-lfl.md`, экран снимка и шлюз — `hld-awp.md`.

## 1. Рамка

- **Цель** — заведённое слово получает значения из словаря прямо в
  карточке, без снимка и ручного ввода переводов; здесь — только
  смена операции, чтобы диалог не умер вместе с `words/lookup` (Д46).
- **Вход и выход** — вход: слово карточки и его словарь из Room.
  Выход: исход по слову — найдено, лексем нет, слово удалено, отказ,
  сервер не ответил, слово «ошибка» — как был.
- **Что меняется на устройстве** — реализация порта карточки шлёт
  лемму без контекста вместо одного токена; исход слоя сети новый;
  правило «исход запроса → исход по слову» читает новый исход.
- **Что не меняется** — стейт и редьюсер диалога, разделы лемм,
  правки, запись в слово диалога, переименование и перенос.
- **Документы** — [бриф](brief.md), [решения](decisions.md), спека WCL,
  спека LFL и её `contract.md`.

## 2. Приложение: схема и код

Схемы нет: слои и шлюз те же, что у экрана снимка (`hld-awp.md`,
«Слои по C4» и «Шлюз»); меняется один вызов в реализации порта.

### Модули и экран

- **`screen/wordcard` — не меняется.** Диалог, сообщения, атомы и
  виджеты как в первой версии.
- **`app` — меняется.** `WordCardUseCaseImpl.lookupWord` зовёт
  операцию `lexemesFromLemma` леммой без контекста.
- **`domain/lexeme` — меняется.** `wordLookupOf` читает исход
  `LexemesOutcome` вместо исхода со списком слов.

Экран, что видно: как в первой версии — кнопка над «+», диалог с
разделами лемм, «Добавить N значений».

### Диалог карточки слова

Стейт и редьюсер не меняются.

### Юзкейсы

Порт `WordCardUseCase`:

```kotlin
// lookupWord — WCL Ш1: слово по id и его словарь; лемма
// — текст слова без краёв; дерево компонентов словаря;
// на сервер без контекста (LFL-104); исход по слову.
// Меняется: операция lexemesFromLemma вместо lookup.
suspend fun lookupWord(
    wordId: Long,
    runId: Long,
): WordLookupOutcome
```

Реализация в `app`, `WordCardUseCaseImpl`:

```kotlin
// lookupWord — WCL-03, LFL-104: слова или словаря нет —
// «слово удалено» (WCL-01.3); пустой текст на сервер не
// уходит (WCL-03.4); лемма — текст слова без краёв, в
// регистре записи. Меняется: запрос леммой.
override suspend fun lookupWord(
    wordId: Long,
    runId: Long,
): WordLookupOutcome {
    val term = termApi.getTermById(wordId)
        ?: return WordLookupOutcome.WordMissing
    val dictionary = dictionaryApi
        .getDictionaryById(term.word.dictionaryId)
        ?: return WordLookupOutcome.WordMissing
    val text = term.word.value.trim()
    if (text.isEmpty()) return WordLookupOutcome.NotFound
    // LFL-104: лемма — текст слова, контекста нет
    val request = lexemesRequestOf(
        dictionary,
        lemma = text,
        context = null,
        components = componentTree.treeOf(dictionary.id),
    )
    val outcome = wordsApi
        .lexemesFromLemma(request, runId)
        .toDomain()
    return wordLookupOf(outcome)
}
```

Что зовёт реализация:

```kotlin
// lexemesRequestOf — запрос lexemes/from-lemma: языки
// словаря, лемма, контекст или null, дерево компонентов
// (LFL-78). Новая, вместо lookupRequestOf с токенами;
// общая с экраном снимка.
fun lexemesRequestOf(
    dictionary: DictionaryApiEntity,
    lemma: String,
    context: LemmaContext?,
    components: List<ComponentNodeApiEntity>,
): LexemesRequestApiEntity

// toDomain — исход слоя сети → LexemesOutcome: статус
// слова, леммы без лексем — «значений нет», отказ,
// недоступен. Новая, вместо LookupResult.toDomain.
fun LexemesResult.toDomain(): LexemesOutcome

// wordLookupOf — исход запроса → исход по слову карточки
// (WCL-04.7, WCL-11): «ошибка» — сервер по слову не
// справился (WCL-11.7); не найдено и значений нет — лексем
// нет; иначе найдено. Меняется: вход — LexemesOutcome.
fun wordLookupOf(outcome: LexemesOutcome): WordLookupOutcome
```

### Шлюз

`hld-awp.md`, «Шлюз»: операция `lexemesFromLemma`, зеркала запроса с
леммой и контекстом, ответ одним словом; у лексемы `inContext` — без
контекста всегда `false`, диалог его не читает.

## 3. Контракт

Диалог читает `word` ответа как раньше: статус, леммы с `formNote`,
лексемы с `componentMatches`, `remarks`; `recommendations` — в
«Замечания». Дельта контракта — `hld-lfl.md`.

## 4. Ключевые решения

- **Запрос из карточки — леммой на ту же операцию** (Д114, Д82): слово
  пользователя считается леммой, словарная форма — на его
  ответственности (LFL Ш1 Б).
- **Старая операция не держится** (Д46): сборка первой версии не в
  проде, `words/lookup` уходит вместе с бэкендом этапа 2.
- **Диалог не трогается** (Д116): тезисы WCL v2 про диалог — после
  этапа 2.

## 5. Сквозное

- **Отказы** — тексты по причине и «Повторить», как в первой версии
  (WCL-11); 413 от сервера у карточки не бывает: контекста нет.
- **Лог** — `###WORDCARD###`: запрос леммой, исход по слову.

## 6. Выкатка

Вместе с этапом 2: бэкенд с `lexemes/from-lemma` раньше сборки.

## 7. Риски

- **Диалог против спеки WCL v2** — после этапа 2 диалог всё ещё по
  первой версии: тезисы WCL в статусе «принято» остаются открытыми до
  своего захода.
